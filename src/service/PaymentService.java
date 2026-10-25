package service;

import dao.PaymentTransactionDao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import model.Cycle;
import model.Group;
import model.PaymentTransaction;
import model.User;
import model.UserGroup;
import model.UserNotification;
import model.enums.PaymentMethod;
import payment.PaymentGatewayRegistry;
import payment.PaymentRequest;
import payment.PaymentResponse;
import util.EntityManagerFactoryUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço de aplicação que coordena o processamento de transações e a integração com carteiras móveis[cite: 119].
 * <p>
 * Garante o controlo de simultaneidade de pagamentos por utilizador/grupo e regista o resultado na BD[cite: 119].
 * </p>
 *
 * @author Sistema iXitique
 * @version 1.0
 * @since 1.0
 */
public class PaymentService {

    /** Conjunto concorrente para evitar múltiplas tentativas simultâneas de pagamento do mesmo utilizador[cite: 119]. */
    private static final Set<String> IN_FLIGHT = ConcurrentHashMap.newKeySet();

    /** Serviço de grupos[cite: 119]. */
    private final GroupService groupService = new GroupService();

    /** Registo de gateways de pagamento integrados[cite: 119]. */
    private final PaymentGatewayRegistry registry = new PaymentGatewayRegistry();

    /** DAO de transações financeiras[cite: 119]. */
    private final PaymentTransactionDao transactionDao =
            new PaymentTransactionDao(EntityManagerFactoryUtil.getEntityManagerFactory());

    private final EntityManagerFactory entityManagerFactory = EntityManagerFactoryUtil.getEntityManagerFactory();

    /**
     * Efetua o pagamento de uma contribuição através do método pretendido (M-Pesa, E-Mola, Numerário)[cite: 119].
     *
     * @param groupId ID do grupo[cite: 119].
     * @param userId ID do utilizador[cite: 119].
     * @param method Método de pagamento {@link PaymentMethod}[cite: 119].
     * @param account Número de conta/telefone pagador[cite: 119].
     * @return Resposta contendo o estado do pagamento {@link PaymentResponse}[cite: 119].
     * @throws IllegalStateException Se já existir uma transação em curso para o mesmo utilizador e grupo[cite: 119].
     */
    public PaymentResponse payContribution(Long groupId, Long userId,
                                           PaymentMethod method, String account) {
        if (groupId == null || userId == null || method == null) {
            throw new IllegalArgumentException("Grupo, utilizador e método de pagamento são obrigatórios.");
        }

        String key = groupId + ":" + userId;
        if (!IN_FLIGHT.add(key)) {
            throw new IllegalStateException("Já existe um pagamento em curso para este membro.");
        }
        try {
            return doPayContribution(groupId, userId, method, account);
        } finally {
            IN_FLIGHT.remove(key);
        }
    }

    /**
     * Executa a cobrança no gateway simulado e regista a contribuição na base de dados[cite: 119].
     */
    private PaymentResponse doPayContribution(Long groupId, Long userId,
                                              PaymentMethod method, String account) {
        Group group = groupService.findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Grupo não encontrado com o ID: " + groupId);
        }
        BigDecimal amount = group.getAmountPerUser();
        LocalDate today = LocalDate.now();

        groupService.validateContribution(groupId, userId, amount, today);
        Cycle cycle = groupService.findActiveCycle(groupId);

        if (method == PaymentMethod.CASH) {
            groupService.addContributionToCycle(groupId, userId, amount, method, today);
            return new PaymentResponse(true, "CASH", "INS-0",
                    "Pagamento em numerário registado.", LocalDateTime.now());
        }

        String cleanAccount = account == null ? null
                : account.replaceAll("\\s+", "").replaceFirst("^\\+258", "");
        PaymentResponse resp = registry.get(method).charge(new PaymentRequest(
                cleanAccount, amount,
                "XQ-" + cycle.getId() + "-" + userId,
                "Contribuição Xitique - " + group.getGroupName()));

        PaymentTransaction t = new PaymentTransaction();
        t.setGroupId(groupId);
        t.setUserId(userId);
        t.setCycleId(cycle.getId());
        t.setMethod(method);
        t.setAccount(cleanAccount);
        t.setAmount(amount);
        t.setStatus(resp.success() ? "SUCCESS" : "FAILED");
        t.setTransactionRef(resp.transactionRef());
        t.setProviderCode(resp.providerCode());
        t.setMessage(resp.message());
        transactionDao.save(t);

        if (resp.success()) {
            try {
                groupService.addContributionToCycle(groupId, userId, amount, method, today);
            } catch (RuntimeException e) {
                t.setStatus("SUCCESS_NOT_RECORDED");
                transactionDao.update(t);
                throw e;
            }
        }
        return resp;
    }

    public PaymentResponse payCyclePayout(Long groupId, Integer cycleId, PaymentMethod method, String account) {
        if (groupId == null || cycleId == null || method == null) {
            throw new IllegalArgumentException("Grupo, ciclo e método de pagamento são obrigatórios.");
        }

        Group group = groupService.findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Grupo não encontrado com o ID: " + groupId);
        }

        Cycle cycle = groupService.findCycles(groupId).stream()
                .filter(c -> c.getId() != null && c.getId().equals(cycleId))
                .findFirst()
                .orElse(null);
        if (cycle == null) {
            throw new IllegalArgumentException("Ciclo não encontrado para o grupo informado.");
        }

        UserGroup beneficiaryGroup = cycle.getBeneficiary();
        if (beneficiaryGroup == null || beneficiaryGroup.getUser() == null) {
            throw new IllegalStateException("Não foi definido um beneficiário para este ciclo.");
        }

        User beneficiary = beneficiaryGroup.getUser();
        String recipientAccount = account == null || account.isBlank()
                ? beneficiary.getPhoneNumber()
                : account;
        if (recipientAccount == null || recipientAccount.isBlank()) {
            throw new IllegalArgumentException("É obrigatório indicar o número do beneficiário para transferir o saldo do ciclo.");
        }

        BigDecimal availableBalance = group.getGroupBalance() == null ? BigDecimal.ZERO : group.getGroupBalance();
        if (availableBalance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("O grupo não tem saldo disponível para transferir ao beneficiário.");
        }

        String cleanAccount = normalizeAccount(recipientAccount);
        String transactionReference = "CYCLE-PAYOUT-" + groupId + "-" + cycleId + "-" + beneficiary.getId();
        PaymentRequest request = new PaymentRequest(
                cleanAccount,
                availableBalance,
                transactionReference,
                "Transferência de saldo do ciclo " + cycle.getNumero() + " para " + beneficiary.getFirstName() + " " + beneficiary.getLastName()
        );

        PaymentResponse resp = registry.get(method).charge(request);
        PaymentTransaction t = new PaymentTransaction();
        t.setGroupId(groupId);
        t.setUserId(beneficiary.getId());
        t.setCycleId(cycleId);
        t.setMethod(method);
        t.setAccount(cleanAccount);
        t.setAmount(availableBalance);
        t.setStatus(resp.success() ? "SUCCESS" : "FAILED");
        t.setTransactionRef(resp.transactionRef());
        t.setProviderCode(resp.providerCode());
        t.setMessage(resp.message());
        transactionDao.save(t);

        if (resp.success()) {
            group.setGroupBalance(BigDecimal.ZERO);
            groupService.updateGroupBalance(groupId, BigDecimal.ZERO);
            persistPayoutNotification(beneficiary, cycle, beneficiaryGroup, availableBalance, method);
        }

        return resp;
    }

    public PaymentResponse payBeneficiary(Long groupId, Integer cycleId, PaymentMethod method, String account) {
        return payCyclePayout(groupId, cycleId, method, account);
    }

    public PaymentResponse transferGroupBalance(Long groupId, Integer cycleId, PaymentMethod method, String account) {
        return payCyclePayout(groupId, cycleId, method, account);
    }

    private String normalizeAccount(String account) {
        String cleaned = account == null ? null : account.replaceAll("\\s+", "").replaceFirst("^\\+258", "");
        return cleaned == null || cleaned.isBlank() ? null : cleaned;
    }

    private void persistPayoutNotification(User beneficiary, Cycle cycle, UserGroup beneficiaryGroup,
                                          BigDecimal amount, PaymentMethod method) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            String message = "O saldo do ciclo " + cycle.getNumero() + " foi transferido com sucesso para si. " +
                    "Recebeu " + amount + " via " + method + ". Já recebeu o seu bolo.";
            UserNotification notification = new UserNotification();
            notification.setRecipient(beneficiary);
            notification.setCycle(cycle);
            notification.setUserGroup(beneficiaryGroup);
            notification.setType("CYCLE_PAYOUT");
            notification.setMessage(message);
            em.getTransaction().begin();
            em.persist(notification);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
}