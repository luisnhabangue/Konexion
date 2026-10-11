package service;

import dao.ContributionDao;
import dao.CycleDao;
import dao.GroupDao;
import dao.UserDao;
import dao.UserGroupDao;
import model.*;
import model.enums.ContributionStatus;
import model.enums.CycleStatus;
import model.enums.PaymentMethod;
import util.EntityManagerFactoryUtil;
import validation.GroupValidator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class GroupService {

    private final GroupDao groupDao = new GroupDao(EntityManagerFactoryUtil.getEntityManagerFactory());
    private final UserDao userDao = new UserDao(EntityManagerFactoryUtil.getEntityManagerFactory());
    private final UserGroupDao userGroupDao = new UserGroupDao(EntityManagerFactoryUtil.getEntityManagerFactory());
    private final CycleDao cycleDao = new CycleDao(EntityManagerFactoryUtil.getEntityManagerFactory());
    private final ContributionDao contributionDao = new ContributionDao(EntityManagerFactoryUtil.getEntityManagerFactory());

    public void createNewGroup(Group group) {
        if (group == null) {
            throw new IllegalArgumentException("Grupo não pode ser nulo.");
        }
        GroupValidator.validateGroupNameLength(group.getGroupName());
        GroupValidator.validateAmountPerUser(group.getAmountPerUser());
        GroupValidator.validatePayday(group.getPayday());
        GroupValidator.validateGroupName(group.getGroupName());
        GroupValidator.validateFrequency(group.getFrequency());

        if (group.getCreatedBy() == null || !group.getCreatedBy().isActive()) {
            throw new IllegalStateException("Usuários inativos não podem criar grupos.");
        }

        User user = group.getCreatedBy();
        UserGroup userGroup = new UserGroup();
        userGroup.setGroup(group);
        userGroup.setUser(user);
        userGroup.setBalance(BigDecimal.ZERO);
        userGroup.setReceived(false);
        userGroup.setUserReceivingOrder(1);

        group.addMember(userGroup);

        if (group.getCreatedAt() == null) {
            group.setCreatedAt(LocalDate.now());
        }

        group.setGroupName(group.getGroupName().trim());
        groupDao.createGroup(group);
    }

    public void addMemberToGroup(Long groupId, Long userId) {
        if (groupId == null || userId == null) {
            throw new IllegalArgumentException("IDs do grupo e do usuário são obrigatórios.");
        }

        Group group = findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Grupo não encontrado com o ID: " + groupId);
        }

        User user = userDao.findById(userId);
        if (user == null || !user.isActive()) {
            throw new IllegalArgumentException("Usuário não encontrado ou está inativo.");
        }

        if (userGroupDao.isUserInGroup(userId, groupId)) {
            throw new IllegalStateException("O usuário '" + user.getFirstName() + "' já é membro deste grupo.");
        }

        int currentMembers = userGroupDao.countMembersInGroup(groupId);
        int nextOrder = currentMembers + 1;

        UserGroup userGroup = new UserGroup();
        userGroup.setGroup(group);
        userGroup.setUser(user);
        userGroup.setBalance(BigDecimal.ZERO);
        userGroup.setReceived(false);
        userGroup.setUserReceivingOrder(nextOrder);

        //group.addMember(userGroup);
        userGroupDao.save(userGroup);
       // groupDao.updateGroup(group);
    }

    public void removeMemberFromGroup(Long groupId, Long userId) {
        Group group = findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Grupo não encontrado com o ID: " + groupId);
        }

        UserGroup userGroup = userGroupDao.findByUserAndGroup(userId, groupId);
        if (userGroup == null) {
            throw new IllegalArgumentException("Usuário não faz parte deste grupo.");
        }

       // group.removeMember(userGroup);
        //groupDao.updateGroup(group);
        userGroupDao.update(userGroup);

    }

    /**
     * Adiciona um novo Ciclo/Rodada ao Grupo
     */
    public void addNewCycleToGroup(Long groupId, Long userIdBeneficiary) {
        if (groupId == null  || userIdBeneficiary == null) {
            throw new IllegalArgumentException("Parâmetros do grupo e beneficiário são obrigatórios.");
        }

        Group group = findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Grupo não encontrado com o ID: " + groupId);
        }

        //Não permitir criar novo ciclo se já houver um em andamento
        Cycle activeCycle = cycleDao.findActiveCycleByGroup(groupId);
        if (activeCycle != null) {
            throw new IllegalStateException("Já existe um ciclo em andamento (Ciclo nº " + activeCycle.getNumero() + ") para este grupo.");
        }

        // Buscar o membro beneficiário
        UserGroup beneficiary = userGroupDao.findByUserAndGroup(userIdBeneficiary, groupId);
        if (beneficiary == null) {
            throw new IllegalArgumentException("O usuário selecionado para receber o prêmio não faz parte deste grupo.");
        }

        // Verificar se o beneficiário já recebeu neste giro
        if (beneficiary.isReceived()) {
            throw new IllegalStateException("Este membro já recebeu o valor do Xitique na rodada atual.");
        }

        Cycle cycle = new Cycle(null,group,null,LocalDate.now(),beneficiary);
        cycle.setGroup(group);
        cycle.setBeneficiary(beneficiary);
        cycle.setNumero(cycleDao.findLastCycleNumberByGroup(groupId) + 1);

        if (cycle.getStartDate() == null) {
            cycle.setStartDate(LocalDate.now());
        }

        LocalDate calculatedCycleEndDate = cycle.getStartDate().plusDays(group.getFrequency());
        cycle.setStatus(CycleStatus.IN_PROGRESS);
        cycle.setEndDate(calculatedCycleEndDate);

        cycleDao.save(cycle);
       // group.addCycle(cycle);
       // groupDao.updateGroup(group);
    }

    /**
     * Registra o Pagamento da Contribuição de um Membro no Ciclo Activo
     */
    public void addContributionToCycle(Long groupId, Long userId, BigDecimal amount, PaymentMethod paymentMethod) {


        if (groupId == null || userId == null || amount == null || paymentMethod == null) {
            throw new IllegalArgumentException("Todos os dados do pagamento são obrigatórios.");
        }

        Group group = findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Grupo não encontrado com o ID: " + groupId);
        }

        //Buscar o ciclo ativo
        Cycle cycle = cycleDao.findActiveCycleByGroup(groupId);
        if (cycle == null) {
            throw new IllegalStateException("Não existe nenhum ciclo em andamento para este grupo.");
        }

        // Buscar o membro
        UserGroup userGroup = userGroupDao.findByUserAndGroup(userId, groupId);
        if (userGroup == null) {
            throw new IllegalArgumentException("O usuário não faz parte deste grupo.");
        }

        // Impedir pagamento duplo no mesmo ciclo
        Contribution existingContribution = contributionDao.findByUserGroupAndCycle(userGroup.getId(), cycle.getId());

        if (existingContribution != null && existingContribution.getStatus() == ContributionStatus.PAID) {
            throw new IllegalStateException("O membro '" + userGroup.getUser().getFirstName() + "' já pagou a cota do ciclo atual.");
        }

        // Validar se o valor pago bate com o que ele deve pagar
        if (amount.compareTo(group.getAmountPerUser()) < 0) {
            throw new IllegalArgumentException("O valor informado (" + amount + ") é inferior à quota estipulada no grupo (" + group.getAmountPerUser() + ").");
        }

        // Criar a contribuição
        LocalDate paymentDayDate = LocalDate.now();
        Contribution contribution = new Contribution(null, userGroup, cycle, amount, paymentDayDate, paymentMethod, ContributionStatus.PAID);

        // Persistir no banco de dados via ContributionDao
        contributionDao.save(contribution);

        // Atualizar o saldo acumulado do membro no grupo
        BigDecimal newBalance = (userGroup.getBalance() != null ? userGroup.getBalance() : BigDecimal.ZERO).add(amount);

        userGroup.setBalance(newBalance);

        checkAndFinalizeCycleIfComplete(group, cycle);

        userGroupDao.update(userGroup);

        // Verificar se todos pagaram para encerrar a rodada
        checkAndFinalizeCycleIfComplete(group, cycle);
    }

    /**
     * Verifica se todas as contribuições do ciclo foram pagas.

     * Se sim, encerra o ciclo e marca a flag 'received' do beneficiário.
     */
    private void checkAndFinalizeCycleIfComplete(Group group, Cycle cycle) {
        int totalMembers = userGroupDao.countMembersInGroup(group.getGroupId());


        List<Contribution> paidContributions = contributionDao.findByCycleId(cycle.getId())
                .stream()
                .filter(c -> c.getStatus() == ContributionStatus.PAID)
                .toList();

        System.out.println("Membros Totais: " + totalMembers + " | Pagamentos Confirmados: " + paidContributions.size());

        if (paidContributions.size() >= totalMembers) {

            cycle.setStatus(CycleStatus.CONCLUDED);
            cycleDao.update(cycle);


            Long beneficiaryUserId = cycle.getBeneficiary().getUser().getId();
            UserGroup beneficiaryGroup = userGroupDao.findByUserAndGroup(beneficiaryUserId, group.getGroupId());

            if (beneficiaryGroup != null) {
                beneficiaryGroup.setReceived(true); // Altera o valor
                userGroupDao.update(beneficiaryGroup); // Salva no MySQL via em.merge()
            }
        }
    }

    /**
     * Regra de Negócio: Define um membro específico como o 1º a receber (troca com o 1º atual).
     */
    public void setFirstReceiver(Long groupId, Long userGroupIdTarget) {
        Group group = findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Grupo não encontrado.");
        }

        UserGroup newFirstMember = userGroupDao.findByUserAndGroup(userGroupIdTarget,groupId);
        if (newFirstMember == null || !newFirstMember.getGroup().getGroupId().equals(groupId)) {
            throw new IllegalArgumentException("O membro especificado não pertence a este grupo.");
        }

        // Localiza quem é o atual 1º lugar
        UserGroup currentFirstMember = group.getMembers().stream()
                .filter(m -> m.getUserReceivingOrder() != null && m.getUserReceivingOrder() == 1)
                .findFirst()
                .orElse(null);

        if (currentFirstMember != null && !currentFirstMember.getId().equals(userGroupIdTarget)) {
            // Troca as posições: o atual 1º assume a posição que o novo 1º ocupava
            currentFirstMember.setUserReceivingOrder(newFirstMember.getUserReceivingOrder());
            userGroupDao.update(currentFirstMember);
        }

        // Coloca o escolhido na posição 1
        newFirstMember.setUserReceivingOrder(1);
        userGroupDao.update(newFirstMember);
    }

    public Group findById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID inválido.");
        }
        return groupDao.findById(id);
    }

    public List<Group> findAll() {
        return groupDao.findAll();
    }
}