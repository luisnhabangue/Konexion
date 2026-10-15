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

/**
 * Serviço responsável pelas regras de negócio ligadas à gestão de Grupos, Membros,
 * Ciclos de Poupança e Pagamentos de Contribuições (Xitique).
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public class GroupService {

    private final GroupDao groupDao = new GroupDao(EntityManagerFactoryUtil.getEntityManagerFactory());
    private final UserDao userDao = new UserDao(EntityManagerFactoryUtil.getEntityManagerFactory());
    private final UserGroupDao userGroupDao = new UserGroupDao(EntityManagerFactoryUtil.getEntityManagerFactory());
    private final CycleDao cycleDao = new CycleDao(EntityManagerFactoryUtil.getEntityManagerFactory());
    private final ContributionDao contributionDao = new ContributionDao(EntityManagerFactoryUtil.getEntityManagerFactory());

    /**
     * Cria um novo grupo de Xitique aplicando validações de atributos e associando o criador como 1º membro.
     *
     * @param group Objeto {@link Group} a criar
     * @throws IllegalArgumentException Se o grupo for nulo ou dados inválidos
     * @throws IllegalStateException Se o criador estiver inativo
     */
    public void createNewGroup(Group group) {
        if (group == null) {
            throw new IllegalArgumentException("Grupo não pode ser nulo.");
        }
        GroupValidator.validateGroupNameLength(group.getGroupName());
        GroupValidator.validateAmountPerUser(group.getAmountPerUser());
        GroupValidator.validatePayday(group.getPayday());
        GroupValidator.validateGroupName(group.getGroupName());
        GroupValidator.validateGroupDescription(group.getGroupDescription());
        GroupValidator.validateFrequency(group.getFrequency());

        if (group.getCreatedBy() == null || !group.getCreatedBy().isActive()) {
            throw new IllegalStateException("Usuários inativos não podem criar grupos.");
        }
        if (groupDao.findByName(group.getGroupName()) != null) {
            throw new IllegalArgumentException("Já existe um grupo com este nome.");
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

    /**
     * Adiciona um novo utilizador como membro de um grupo.
     *
     * @param groupId ID do grupo
     * @param userId ID do utilizador a adicionar
     * @throws IllegalArgumentException Se IDs forem nulos ou entidades não existirem
     * @throws IllegalStateException Se o utilizador já pertencer ao grupo
     */
    public void addMemberToGroup(Long groupId, Long userId) {
        if (groupId == null || userId == null) {
            throw new IllegalArgumentException("IDs do grupo e do usuário são obrigatórios.");
        }

        Group group = findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Grupo não encontrado com o ID: " + groupId);
        }

        if (!cycleDao.findByGroupId(groupId).isEmpty()) {
            throw new IllegalStateException("Não é possível adicionar membros depois de iniciar o histórico de ciclos.");
        }

        User user = userDao.findById(userId);
        if (user == null || !user.isActive()) {
            throw new IllegalArgumentException("Usuário não encontrado ou está inativo.");
        }

        if (userGroupDao.isUserInGroup(userId, groupId)) {
            throw new IllegalStateException("O usuário '" + user.getFirstName() + "' já é membro deste grupo.");
        }

        int nextOrder = userGroupDao.findByGroupId(groupId).stream()
                .map(UserGroup::getUserReceivingOrder)
                .filter(order -> order != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        UserGroup userGroup = new UserGroup();
        userGroup.setGroup(group);
        userGroup.setUser(user);
        userGroup.setBalance(BigDecimal.ZERO);
        userGroup.setReceived(false);
        userGroup.setUserReceivingOrder(nextOrder);

        userGroupDao.save(userGroup);
    }

    /**
     * Remove a associação de um utilizador a um grupo.
     *
     * @param groupId ID do grupo
     * @param userId ID do utilizador a remover
     * @throws IllegalArgumentException Se o grupo ou o vínculo não forem encontrados
     */
    public void removeMemberFromGroup(Long groupId, Long userId) {
        Group group = findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Grupo não encontrado com o ID: " + groupId);
        }

        UserGroup userGroup = userGroupDao.findByUserAndGroup(userId, groupId);
        if (userGroup == null) {
            throw new IllegalArgumentException("Usuário não faz parte deste grupo.");
        }
        if (group.getCreatedBy() != null && group.getCreatedBy().getId().equals(userId)) {
            throw new IllegalStateException("O criador não pode sair do grupo enquanto for responsável por ele.");
        }
        if (!cycleDao.findByGroupId(groupId).isEmpty()) {
            throw new IllegalStateException("Não é possível remover membros depois de iniciar o histórico de ciclos.");
        }
        if (userGroup.getBalance() != null && userGroup.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Não é possível remover um membro com saldo no grupo.");
        }
        userGroupDao.delete(userGroup);
    }

    /**
     * Cria e inicia uma nova rodada/ciclo de pagamentos no grupo.
     *
     * @param groupId ID do grupo
     * @param userIdBeneficiary ID do membro selecionado para receber o montante acumulado
     * @throws IllegalArgumentException Se os parâmetros forem inválidos
     * @throws IllegalStateException Se já houver um ciclo ativo ou o beneficiário já tiver recebido na rotação
     */
    public void addNewCycleToGroup(Long groupId, Long userIdBeneficiary) {
        if (groupId == null || userIdBeneficiary == null) {
            throw new IllegalArgumentException("Parâmetros do grupo e beneficiário são obrigatórios.");
        }

        Group group = findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Grupo não encontrado com o ID: " + groupId);
        }

        if (userGroupDao.countMembersInGroup(groupId) < 2) {
            throw new IllegalStateException("Adicione pelo menos dois membros antes de abrir um ciclo.");
        }

        Cycle activeCycle = cycleDao.findActiveCycleByGroup(groupId);
        if (activeCycle != null) {
            throw new IllegalStateException("Já existe um ciclo em andamento (Ciclo nº " + activeCycle.getNumero() + ") para este grupo.");
        }

        List<UserGroup> members = userGroupDao.findByGroupId(groupId);
        if (userGroupDao.findByUserAndGroup(userIdBeneficiary, groupId) == null) {
            throw new IllegalArgumentException("O usuário selecionado para receber o prêmio não faz parte deste grupo.");
        }
        if (!members.isEmpty() && members.stream().allMatch(UserGroup::isReceived)) {
            members.forEach(member -> {
                member.setReceived(false);
                userGroupDao.update(member);
            });
        }

        UserGroup beneficiary = userGroupDao.findByUserAndGroup(userIdBeneficiary, groupId);
        if (beneficiary == null) {
            throw new IllegalArgumentException("O usuário selecionado para receber o prêmio não faz parte deste grupo.");
        }

        if (beneficiary.isReceived()) {
            throw new IllegalStateException("Este membro já recebeu o valor do Xitique na rodada atual.");
        }

        Cycle cycle = new Cycle(null, group, null, LocalDate.now(), beneficiary);
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
    }

    /**
     * Regista a contribuição financeira de um membro no ciclo em andamento.
     *
     * @param groupId ID do grupo
     * @param userId ID do membro pagador
     * @param amount Valor pago
     * @param paymentMethod Método de pagamento empregue
     * @throws IllegalArgumentException Se faltarem dados obrigatórios ou o valor for menor que a quota
     * @throws IllegalStateException Se não houver ciclo ativo ou se o membro já tiver pago
     */
    public void addContributionToCycle(Long groupId, Long userId, BigDecimal amount, PaymentMethod paymentMethod) {
        addContributionToCycle(groupId, userId, amount, paymentMethod, LocalDate.now());
    }

    public void addContributionToCycle(Long groupId, Long userId, BigDecimal amount,
                                       PaymentMethod paymentMethod, LocalDate paymentDate) {
        if (groupId == null || userId == null || amount == null || paymentMethod == null || paymentDate == null) {
            throw new IllegalArgumentException("Todos os dados do pagamento são obrigatórios.");
        }

        Group group = findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Grupo não encontrado com o ID: " + groupId);
        }

        Cycle cycle = cycleDao.findActiveCycleByGroup(groupId);
        if (cycle == null) {
            throw new IllegalStateException("Não existe nenhum ciclo em andamento para este grupo.");
        }
        if (paymentDate.isAfter(LocalDate.now()) || paymentDate.isBefore(cycle.getStartDate())) {
            throw new IllegalArgumentException("A data de pagamento deve estar entre o início do ciclo e hoje.");
        }

        UserGroup userGroup = userGroupDao.findByUserAndGroup(userId, groupId);
        if (userGroup == null) {
            throw new IllegalArgumentException("O usuário não faz parte deste grupo.");
        }

        Contribution existingContribution = contributionDao.findByUserGroupAndCycle(userGroup.getId(), cycle.getId());
        if (existingContribution != null) {
            throw new IllegalStateException("O membro '" + userGroup.getUser().getFirstName() + "' já pagou a cota do ciclo atual.");
        }

        if (amount.compareTo(group.getAmountPerUser()) < 0) {
            throw new IllegalArgumentException("O valor informado (" + amount + ") é inferior à quota estipulada no grupo (" + group.getAmountPerUser() + ").");
        }

        Contribution contribution = new Contribution(null, userGroup, cycle, amount, paymentDate, paymentMethod, ContributionStatus.PAID);

        contributionDao.save(contribution);

        BigDecimal newBalance = (userGroup.getBalance() != null ? userGroup.getBalance() : BigDecimal.ZERO).add(amount);
        userGroup.setBalance(newBalance);

        userGroupDao.update(userGroup);
        checkAndFinalizeCycleIfComplete(group, cycle);
    }

    /**
     * Verifica internamente se todas as contribuições do ciclo foram concluídas e, em caso afirmativo,
     * encerra a rodada e marca o beneficiário como contemplado.
     *
     * @param group Grupo afetado
     * @param cycle Ciclo a verificar
     */
    private void checkAndFinalizeCycleIfComplete(Group group, Cycle cycle) {

        int totalMembers = userGroupDao.countMembersInGroup(group.getGroupId());

        List<Contribution> paidContributions = contributionDao.findByCycleId(cycle.getId())
                .stream()
                .filter(c -> c.getStatus() == ContributionStatus.PAID)
                .toList();

       // System.out.println("Membros Totais: " + totalMembers + " | Pagamentos Confirmados: " + paidContributions.size());

        if (paidContributions.size() >= totalMembers) {
            cycle.setStatus(CycleStatus.CONCLUDED);
            cycleDao.update(cycle);

            Long beneficiaryUserId = cycle.getBeneficiary().getUser().getId();
            UserGroup beneficiaryGroup = userGroupDao.findByUserAndGroup(beneficiaryUserId, group.getGroupId());

            if (beneficiaryGroup != null) {
                beneficiaryGroup.setReceived(true);
                userGroupDao.update(beneficiaryGroup);
            }
        }
    }

    /**
     * Define um membro como o primeiro na fila de receção, ajustando a posição do atual primeiro lugar.
     *
     * @param groupId ID do grupo
     * @param userGroupIdTarget ID do vínculo do membro a colocar em primeiro
     * @throws IllegalArgumentException Se o grupo ou o membro não forem encontrados
     */
    public void setFirstReceiver(Long groupId, Long userGroupIdTarget) {
        Group group = findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Grupo não encontrado.");

        }

        UserGroup newFirstMember = userGroupDao.findByUserAndGroup(userGroupIdTarget, groupId);
        if (newFirstMember == null || !newFirstMember.getGroup().getGroupId().equals(groupId)) {
            throw new IllegalArgumentException("O membro especificado não pertence a este grupo.");
        }

        UserGroup currentFirstMember = userGroupDao.findByGroupId(groupId).stream()
                .filter(m -> m.getUserReceivingOrder() != null && m.getUserReceivingOrder() == 1)
                .findFirst()
                .orElse(null);

        if (currentFirstMember != null && !currentFirstMember.getUser().getId().equals(userGroupIdTarget)) {
            currentFirstMember.setUserReceivingOrder(newFirstMember.getUserReceivingOrder());
            userGroupDao.update(currentFirstMember);
        }

        newFirstMember.setUserReceivingOrder(1);
        userGroupDao.update(newFirstMember);
    }

    /**
     * Procura um grupo pelo seu ID.
     * @param id ID do grupo
     * @return Objeto {@link Group}
     * @throws IllegalArgumentException Se o ID for inválido
     */
    public Group findById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID inválido.");
        }
        return groupDao.findById(id);
    }

    public Group findByIdWithMembers(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID inválido.");
        }
        return groupDao.findByIdWithMembers(id);
    }

    public Group findByIdForUser(Long groupId, Long userId) {
        Group group = findByIdWithMembers(groupId);
        if (group == null) {
            return null;
        }
        if (group.getCreatedBy() == null || !group.getCreatedBy().getId().equals(userId)) {
            if (!userGroupDao.isUserInGroup(userId, groupId)) {
                throw new IllegalStateException("Não tem acesso a este grupo.");
            }
        }
        return group;
    }

    public List<Group> findAllForUser(Long userId) {
        return groupDao.findAllForUser(userId);
    }

    public List<User> findActiveUsers() {
        return userDao.listAllUsers().stream().filter(User::isActive).toList();
    }

    public List<UserGroup> findMembers(Long groupId) {
        return userGroupDao.findByGroupId(groupId);
    }

    public Cycle findActiveCycle(Long groupId) {
        return cycleDao.findActiveCycleByGroup(groupId);
    }

    public List<Cycle> findCycles(Long groupId) {
        return cycleDao.findByGroupId(groupId);
    }

    public List<Contribution> findContributionsByCycle(Integer cycleId) {
        return contributionDao.findByCycleId(cycleId);
    }

    public List<Contribution> findContributionsForUser(Long userId) {
        return contributionDao.findByUserId(userId);
    }

    public void deleteGroup(Long groupId, Long actorId) {
        Group group = findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Grupo não encontrado.");
        }
        if (group.getCreatedBy() == null || !group.getCreatedBy().getId().equals(actorId)) {
            throw new IllegalStateException("Apenas o criador do grupo pode eliminá-lo.");
        }
        if (!cycleDao.findByGroupId(groupId).isEmpty()) {
            throw new IllegalStateException("Não é possível eliminar um grupo que já iniciou ciclos.");
        }
        groupDao.deleteGroup(group);
    }

    /**
     * Retorna a lista completa de todos os grupos registados.
     * @return Lista de grupos
     */
    public List<Group> findAll() {
        return groupDao.findAll();
    }
}