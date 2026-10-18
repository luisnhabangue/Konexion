package service;

import dao.PaymentTransactionDao;
import model.Cycle;
import model.Group;
import model.PaymentTransaction;
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

public class PaymentService {

    private static final Set<String> IN_FLIGHT = ConcurrentHashMap.newKeySet();

    private final GroupService groupService = new GroupService();
    private final PaymentGatewayRegistry registry = new PaymentGatewayRegistry();
    private final PaymentTransactionDao transactionDao =
            new PaymentTransactionDao(EntityManagerFactoryUtil.getEntityManagerFactory());

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

    private PaymentResponse doPayContribution(Long groupId, Long userId,
                                              PaymentMethod method, String account) {
        Group group = groupService.findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Grupo não encontrado com o ID: " + groupId);
        }
        BigDecimal amount = group.getAmountPerUser();
        LocalDate today = LocalDate.now();

        // 1. Validar ANTES de cobrar (lança exceção se algo estiver errado)
        groupService.validateContribution(groupId, userId, amount, today);
        Cycle cycle = groupService.findActiveCycle(groupId);

        // Dinheiro vivo: não há provedor, regista-se como antes
        if (method == PaymentMethod.CASH) {
            groupService.addContributionToCycle(groupId, userId, amount, method, today);
            return new PaymentResponse(true, "CASH", "INS-0",
                    "Pagamento em numerário registado.", LocalDateTime.now());
        }

        // 2. Cobrar no provedor simulado
        String cleanAccount = account == null ? null
                : account.replaceAll("\\s+", "").replaceFirst("^\\+258", "");
        PaymentResponse resp = registry.get(method).charge(new PaymentRequest(
                cleanAccount, amount,
                "XQ-" + cycle.getId() + "-" + userId,
                "Contribuição Xitique - " + group.getGroupName()));

        // 3. Guardar a tentativa (sucesso ou falha)
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

        // 4. Se foi cobrado, usar a lógica que já existe para registar a contribuição
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
}