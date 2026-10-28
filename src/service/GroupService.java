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
import model.enums.ReceivingOrderMode;
import payment.PaymentResponse;
import util.EntityManagerFactoryUtil;
import validation.GroupValidator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Collections;

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
    private final EntityManagerFactory entityManagerFactory = EntityManagerFactoryUtil.getEntityManagerFactory();

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
        if (!group.isCapacityConfigured()) {
            throw new IllegalArgumentException("Defina o número total de membros do grupo.");
        }
        GroupValidator.validateMemberCapacity(group.getMaxMembers());
        GroupValidator.validateReceivingOrderMode(group.getReceivingOrderMode());

        if (group.getCreatedBy() == null || !group.getCreatedBy().isActive()) {
            throw new IllegalStateException("Utilizadores inativos não podem criar grupos.");
        }
        User persistedCreator = userDao.findById(group.getCreatedBy().getId());
        if (persistedCreator == null || !persistedCreator.isActive()) {
            throw new IllegalStateException("O utilizador que cria o grupo não está ativo.");
        }
        if (persistedCreator.getUserRole() != null && persistedCreator.getUserRole().isAdministrator()) {
            throw new SecurityException("Administradores não podem criar nem participar em grupos.");
        }
        if (!persistedCreator.isVerified()) {
            throw new IllegalStateException("A criação de grupos requer verificação KYC aprovada por um administrador. Pode continuar a participar nos grupos existentes.");
        }
        if (groupDao.findByName(group.getGroupName()) != null) {
            throw new IllegalArgumentException("Já existe um grupo com este nome.");
        }

        User user = persistedCreator;
        group.setCreatedBy(user);
        group.setGroupBalance(BigDecimal.ZERO);
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
    public void addMemberToGroup(Long groupId, Long actorId, Long userId) {
        if (groupId == null || userId == null || actorId == null) {
            throw new IllegalArgumentException("IDs do grupo, responsável e utilizador são obrigatórios.");
        }

        Group group = findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Grupo não encontrado com o ID: " + groupId);
        }
        ensureGroupIsNotArchived(group);
        requireOwnerOrAdministrator(group, actorId);

        if (!cycleDao.findByGroupId(groupId).isEmpty()) {
            throw new IllegalStateException("Não é possível adicionar membros depois de iniciar o histórico de ciclos.");
        }

        User user = userDao.findById(userId);
        if (user == null || !user.isActive()) {
            throw new IllegalArgumentException("Usuário não encontrado ou está inativo.");
        }
        if (user.getUserRole() != null && user.getUserRole().isAdministrator()) {
            throw new IllegalStateException("Administradores não podem participar em grupos.");
        }

        if (userGroupDao.isUserInGroup(userId, groupId)) {
            throw new IllegalStateException("O usuário '" + user.getFirstName() + "' já é membro deste grupo.");
        }

        ensureCapacityAvailable(group, groupId);
        persistMembership(groupId, userId);
        initializeRotationIfReady(groupId);
    }

    /**
     * Remove a associação de um utilizador a um grupo.
     *
     * @param groupId ID do grupo
     * @param userId ID do utilizador a remover
     * @throws IllegalArgumentException Se o grupo ou o vínculo não forem encontrados
     */
    public void removeMemberFromGroup(Long groupId, Long actorId, Long userId) {
        Group group = findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Grupo não encontrado com o ID: " + groupId);
        }
        requireManagerOrAdministrator(group, actorId);

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
    public void addNewCycleToGroup(Long groupId, Long actorId, Long userIdBeneficiary) {
        if (groupId == null || userIdBeneficiary == null) {
            throw new IllegalArgumentException("Parâmetros do grupo e beneficiário são obrigatórios.");
        }

        Group group = findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Grupo não encontrado com o ID: " + groupId);
        }
        ensureGroupIsNotArchived(group);
        requireOwnerOrAdministrator(group, actorId);

        if (group.isCapacityConfigured() && group.getGroupBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Transfira o bolo do ciclo anterior antes de iniciar outro ciclo.");
        }

        if (userGroupDao.countMembersInGroup(groupId) != getGroupMemberCapacity(groupId)) {
            throw new IllegalStateException("O ciclo será iniciado quando o grupo atingir os "
                    + getGroupMemberCapacity(groupId) + " membros definidos.");
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
            if (group.isCapacityConfigured()) {
                throw new IllegalStateException("Todos os membros já receberam o bolo deste grupo.");
            }
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
        UserGroup nextBeneficiary = nextBeneficiary(members);
        if (nextBeneficiary != null && !nextBeneficiary.getId().equals(beneficiary.getId())) {
            throw new IllegalStateException("O próximo beneficiário da ordem definida é "
                    + nextBeneficiary.getUser().getFirstName() + " "
                    + nextBeneficiary.getUser().getLastName() + ".");
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
        cycle.setAutomaticPayoutEnabled(group.isCapacityConfigured());
        cycle.setEndDate(calculatedCycleEndDate);

        cycleDao.save(cycle);
    }

    private UserGroup nextBeneficiary(List<UserGroup> members) {
        return members.stream().filter(member -> !member.isReceived())
                .min(java.util.Comparator.comparing(UserGroup::getUserReceivingOrder))
                .orElse(null);
    }

    private void initializeRotationIfReady(Long groupId) {
        Group group = findById(groupId);
        List<UserGroup> members = userGroupDao.findByGroupId(groupId);
        if (group == null || !group.isCapacityConfigured() || members.size() != group.getMaxMembers()
                || !cycleDao.findByGroupId(groupId).isEmpty()) {
            return;
        }
        if (group.getReceivingOrderMode() == ReceivingOrderMode.RANDOM) {
            Collections.shuffle(members);
            for (int index = 0; index < members.size(); index++) {
                UserGroup member = members.get(index);
                member.setUserReceivingOrder(index + 1);
                userGroupDao.update(member);
            }
            members = userGroupDao.findByGroupId(groupId);
            createCycle(group, nextBeneficiary(members));
        }
    }

    private void createCycle(Group group, UserGroup beneficiary) {
        if (beneficiary == null) {
            throw new IllegalStateException("Não há beneficiário elegível para o próximo ciclo.");
        }
        Cycle cycle = new Cycle(null, group, cycleDao.findLastCycleNumberByGroup(group.getGroupId()) + 1,
                LocalDate.now(), beneficiary);
        cycle.setStatus(CycleStatus.IN_PROGRESS);
        cycle.setAutomaticPayoutEnabled(group.isCapacityConfigured());
        cycle.setEndDate(cycle.getStartDate().plusDays(group.getFrequency()));
        cycleDao.save(cycle);
    }

    public void startNextCycleAfterPayout(Long groupId) {
        Group group = findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Grupo não encontrado.");
        }
        if (group.getGroupBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("O saldo do ciclo anterior ainda não foi transferido.");
        }
        List<UserGroup> members = userGroupDao.findByGroupId(groupId);
        if (!group.isCapacityConfigured() || members.size() != group.getMaxMembers()
                || cycleDao.findActiveCycleByGroup(groupId) != null) {
            return;
        }
        UserGroup next = nextBeneficiary(members);
        if (next != null) {
            createCycle(group, next);
        }
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

    private record ContributionContext(Group group, Cycle cycle, UserGroup userGroup) {}

    /** Valida tudo o que addContributionToCycle valida, mas sem gravar nada. */
    public void validateContribution(Long groupId, Long userId, BigDecimal amount, LocalDate paymentDate) {
        prepareContribution(groupId, userId, amount, paymentDate);
    }

    private ContributionContext prepareContribution(Long groupId, Long userId,
                                                    BigDecimal amount, LocalDate paymentDate) {
        if (groupId == null || userId == null || amount == null || paymentDate == null) {
            throw new IllegalArgumentException("Todos os dados do pagamento são obrigatórios.");
        }
        Group group = findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Grupo não encontrado com o ID: " + groupId);
        }
        ensureGroupIsNotArchived(group);
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
        if (contributionDao.findByUserGroupAndCycle(userGroup.getId(), cycle.getId()) != null) {
            throw new IllegalStateException("O membro '" + userGroup.getUser().getFirstName()
                    + "' já pagou a cota do ciclo atual.");
        }
        if (amount.compareTo(group.getAmountPerUser()) < 0) {
            throw new IllegalArgumentException("O valor informado (" + amount
                    + ") é inferior à quota estipulada no grupo (" + group.getAmountPerUser() + ").");
        }
        return new ContributionContext(group, cycle, userGroup);
    }

    public void addContributionToCycle(Long groupId, Long userId, BigDecimal amount,
                                       PaymentMethod paymentMethod, LocalDate paymentDate) {
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Todos os dados do pagamento são obrigatórios.");
        }
        ContributionContext ctx = prepareContribution(groupId, userId, amount, paymentDate);
        UserGroup userGroup = ctx.userGroup();

        Contribution contribution = new Contribution(null, userGroup, ctx.cycle(), amount,
                paymentDate, paymentMethod, ContributionStatus.PAID);
        contributionDao.save(contribution);

        if (!paymentDate.isAfter(ContributionDeadlineService.paymentDueDate(ctx.group(), ctx.cycle()))) {
            User member = userGroup.getUser();
            member.setPunctualityPoints(member.getPunctualityPoints() + 10);
            userDao.updateUser(member);
        }

        BigDecimal newBalance = (userGroup.getBalance() != null ? userGroup.getBalance() : BigDecimal.ZERO).add(amount);
        userGroup.setBalance(newBalance);
        userGroupDao.update(userGroup);

        BigDecimal newGroupBalance = (ctx.group().getGroupBalance() != null ? ctx.group().getGroupBalance() : BigDecimal.ZERO).add(amount);
        ctx.group().setGroupBalance(newGroupBalance);
        groupDao.updateGroup(ctx.group());

        checkAndFinalizeCycleIfComplete(ctx.group(), ctx.cycle());
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
    public void setFirstReceiver(Long groupId, Long actorId, Long userGroupIdTarget) {
        Group group = findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Grupo não encontrado.");

        }
        requireOwnerOrAdministrator(group, actorId);
        if (group.getReceivingOrderMode() == ReceivingOrderMode.RANDOM) {
            throw new IllegalStateException("A ordem aleatória é definida automaticamente pelo sistema.");
        }
        if (!cycleDao.findByGroupId(groupId).isEmpty()) {
            throw new IllegalStateException("A ordem só pode ser alterada antes do início do primeiro ciclo.");
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
    public BigDecimal getGroupBalance(Long groupId) {
        Group group = findById(groupId);
        return group == null || group.getGroupBalance() == null ? BigDecimal.ZERO : group.getGroupBalance();
    }

    public void updateGroupBalance(Long groupId, BigDecimal balance) {
        if (groupId == null || groupId <= 0) {
            throw new IllegalArgumentException("ID inválido.");
        }
        Group group = findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Grupo não encontrado com o ID: " + groupId);
        }
        group.setGroupBalance(balance == null ? BigDecimal.ZERO : balance);
        groupDao.updateGroup(group);
    }

    public Group findById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID inválido.");
        }
        return groupDao.findById(id);
    }

    public PaymentResponse transferCycleBalanceToBeneficiary(Long groupId, Long actorId, Integer cycleId,
                                                            PaymentMethod method, String account) {
        Group group = findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Grupo não encontrado com o ID: " + groupId);
        }
        requireOwnerOrAdministrator(group, actorId);
        Cycle cycle = findCycles(groupId).stream()
                .filter(c -> c.getId() != null && c.getId().equals(cycleId))
                .findFirst()
                .orElse(null);
        if (cycle == null) {
            throw new IllegalArgumentException("Ciclo não encontrado para este grupo.");
        }
        return new PaymentService().payCyclePayout(groupId, cycleId, method, account);
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
        User user = userDao.findById(userId);
        boolean elevated = user != null && user.getUserRole() != null
                && user.getUserRole().isAdministrator();
        if (!elevated && (group.getCreatedBy() == null || !group.getCreatedBy().getId().equals(userId))) {
            if (!userGroupDao.isUserInGroup(userId, groupId)) {
                throw new IllegalStateException("Não tem acesso a este grupo.");
            }
        }
        return group;
    }

    public List<Group> findAllForUser(Long userId) {
        return groupDao.findAllForUser(userId);
    }

    public List<Group> findPublicGroups() {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            return em.createQuery("SELECT DISTINCT g FROM Group g JOIN FETCH g.createdBy " +
                            "LEFT JOIN FETCH g.members m LEFT JOIN FETCH m.user " +
                            "WHERE g.privateGroup = false AND g.archived = false ORDER BY g.createdAt DESC",
                            Group.class).getResultList();
        } finally {
            em.close();
        }
    }

    public void joinPublicGroup(Long groupId, Long userId) {
        Group group = findById(groupId);
        User user = userDao.findById(userId);
        if (group == null || user == null || !user.isActive()) {
            throw new IllegalArgumentException("Grupo ou utilizador inválido.");
        }
        if (user.getUserRole() != null && user.getUserRole().isAdministrator()) {
            throw new SecurityException("Administradores podem consultar grupos, mas não participar neles.");
        }
        if (group.isArchived()) {
            throw new IllegalStateException("Este grupo está arquivado.");
        }
        if (group.isPrivateGroup()) {
            throw new IllegalStateException("Este grupo é privado e requer um convite.");
        }
        if (userGroupDao.isUserInGroup(userId, groupId)) {
            throw new IllegalStateException("Já participa neste grupo.");
        }
        if (!cycleDao.findByGroupId(groupId).isEmpty()) {
            throw new IllegalStateException("Este grupo já iniciou ciclos e não aceita novos membros.");
        }
        ensureCapacityAvailable(group, groupId);
        persistMembership(groupId, userId);
        initializeRotationIfReady(groupId);
    }

    public User findActiveUserByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }
        String normalized = identifier.trim();
        User user = userDao.findByUsername(normalized.toLowerCase(java.util.Locale.ROOT));
        if (user == null) {
            user = userDao.findByDocumentId(normalized.toUpperCase(java.util.Locale.ROOT));
        }
        return user != null && user.isActive() ? user : null;
    }

    public List<UserGroup> findMembers(Long groupId) {
        return userGroupDao.findByGroupId(groupId);
    }

    public int getGroupMemberCapacity(Long groupId) {
        Group group = findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Grupo não encontrado.");
        }
        return group.isCapacityConfigured() ? group.getMaxMembers() : userGroupDao.countMembersInGroup(groupId);
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

    public void inviteMember(Long groupId, Long inviterId, Long inviteeId) {
        Group group = findByIdWithMembers(groupId);
        User inviter = userDao.findById(inviterId);
        User invitee = userDao.findById(inviteeId);
        if (group == null || inviter == null || invitee == null || !invitee.isActive()) {
            throw new IllegalArgumentException("Grupo ou utilizador inválido.");
        }
        ensureGroupIsNotArchived(group);
        boolean manager = group.getCreatedBy().getId().equals(inviterId)
                || inviter.getUserRole() != null && inviter.getUserRole().isAdministrator();
        if (!manager) {
            throw new SecurityException("Apenas o responsável do grupo ou um administrador pode convidar membros.");
        }
        if (userGroupDao.isUserInGroup(inviteeId, groupId)) {
            throw new IllegalStateException("Este utilizador já participa no grupo.");
        }
        if (!cycleDao.findByGroupId(groupId).isEmpty()) {
            throw new IllegalStateException("Não é possível convidar membros depois de iniciar ciclos.");
        }
        ensureCapacityAvailable(group, groupId);
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            Long pending = em.createQuery(
                            "SELECT COUNT(i) FROM MembershipInvitation i WHERE i.group.groupId = :groupId " +
                                    "AND i.invitee.id = :inviteeId AND i.status = 'PENDING'", Long.class)
                    .setParameter("groupId", groupId).setParameter("inviteeId", inviteeId).getSingleResult();
            if (pending > 0) {
                throw new IllegalStateException("Já existe um convite pendente para este utilizador.");
            }
            EntityTransaction transaction = em.getTransaction();
            transaction.begin();
            Group lockedGroup = em.find(Group.class, groupId, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            if (lockedGroup.isCapacityConfigured()) {
                Long occupiedSlots = em.createQuery(
                                "SELECT COUNT(ug) FROM UserGroup ug WHERE ug.group.groupId = :groupId",
                                Long.class)
                        .setParameter("groupId", groupId)
                        .getSingleResult();
                Long reservedSlots = em.createQuery(
                                "SELECT COUNT(i) FROM MembershipInvitation i WHERE i.group.groupId = :groupId " +
                                        "AND i.status = 'PENDING'", Long.class)
                        .setParameter("groupId", groupId)
                        .getSingleResult();
                if (occupiedSlots + reservedSlots >= lockedGroup.getMaxMembers()) {
                    throw new IllegalStateException("O grupo já atingiu a capacidade definida de membros.");
                }
            }
            MembershipInvitation invitation = new MembershipInvitation();
            invitation.setGroup(lockedGroup);
            invitation.setInviter(em.getReference(User.class, inviterId));
            invitation.setInvitee(em.getReference(User.class, inviteeId));
            em.persist(invitation);
            transaction.commit();
        } catch (RuntimeException exception) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw exception;
        } finally {
            em.close();
        }
    }

    public List<MembershipInvitation> findPendingInvitations(Long userId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT i FROM MembershipInvitation i JOIN FETCH i.group JOIN FETCH i.inviter " +
                                    "WHERE i.invitee.id = :userId AND i.status = 'PENDING' ORDER BY i.createdAt DESC",
                            MembershipInvitation.class)
                    .setParameter("userId", userId).getResultList();
        } finally {
            em.close();
        }
    }

    public List<MembershipInvitation> findPendingInvitationsByGroup(Long groupId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT i FROM MembershipInvitation i JOIN FETCH i.group JOIN FETCH i.invitee " +
                                    "JOIN FETCH i.inviter WHERE i.group.groupId = :groupId AND i.status = 'PENDING' " +
                                    "ORDER BY i.createdAt ASC",
                            MembershipInvitation.class)
                    .setParameter("groupId", groupId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public void revokeInvitation(Long groupId, Long actorId, Long invitationId) {
        Group group = findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Grupo não encontrado.");
        }
        requireOwnerOrAdministrator(group, actorId);
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            MembershipInvitation invitation = em.createQuery(
                            "SELECT i FROM MembershipInvitation i JOIN FETCH i.group " +
                                    "WHERE i.id = :id AND i.group.groupId = :groupId", MembershipInvitation.class)
                    .setParameter("id", invitationId)
                    .setParameter("groupId", groupId)
                    .getResultStream().findFirst().orElse(null);
            if (invitation == null || !"PENDING".equals(invitation.getStatus())) {
                throw new IllegalStateException("O convite já não está pendente.");
            }
            invitation.setStatus("REVOKED");
            transaction.commit();
        } catch (RuntimeException exception) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw exception;
        } finally {
            em.close();
        }
    }

    public void respondToInvitation(Long invitationId, Long userId, boolean accepted) {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            MembershipInvitation invitation = em.createQuery(
                            "SELECT i FROM MembershipInvitation i JOIN FETCH i.group " +
                                    "WHERE i.id = :id AND i.invitee.id = :userId", MembershipInvitation.class)
                    .setParameter("id", invitationId).setParameter("userId", userId)
                    .getResultStream().findFirst().orElse(null);
            if (invitation == null || !"PENDING".equals(invitation.getStatus())) {
                throw new IllegalStateException("O convite já não está disponível.");
            }
            if (accepted) {
                User invitee = em.find(User.class, userId);
                if (invitee != null && invitee.getUserRole() != null
                        && invitee.getUserRole().isAdministrator()) {
                    throw new SecurityException("Administradores podem consultar grupos, mas não participar neles.");
                }
                if (invitation.getGroup().isArchived()) {
                    throw new IllegalStateException("Este grupo está arquivado.");
                }
                if (!cycleDao.findByGroupId(invitation.getGroup().getGroupId()).isEmpty()) {
                    throw new IllegalStateException("O grupo já iniciou ciclos e não aceita novos membros.");
                }
                Group group = em.find(Group.class, invitation.getGroup().getGroupId(),
                        jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
                if (group.isCapacityConfigured()
                        && userGroupDao.countMembersInGroup(group.getGroupId()) >= group.getMaxMembers()) {
                    throw new IllegalStateException("O grupo já atingiu o limite de membros.");
                }
                User user = em.getReference(User.class, userId);
                int nextOrder = em.createQuery(
                                "SELECT COALESCE(MAX(ug.userReceivingOrder), 0) FROM UserGroup ug WHERE ug.group.groupId = :groupId",
                                Integer.class)
                        .setParameter("groupId", group.getGroupId()).getSingleResult() + 1;
                UserGroup membership = new UserGroup();
                membership.setUser(user);
                membership.setGroup(group);
                membership.setBalance(BigDecimal.ZERO);
                membership.setReceived(false);
                membership.setUserReceivingOrder(nextOrder);
                em.persist(membership);
            }
            invitation.setStatus(accepted ? "ACCEPTED" : "DECLINED");
            transaction.commit();
        } catch (RuntimeException exception) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw exception;
        } finally {
            em.close();
        }
        if (accepted) {
            initializeRotationIfReady(getInvitationGroupId(invitationId, userId));
        }
    }

    private Long getInvitationGroupId(Long invitationId, Long userId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT i.group.groupId FROM MembershipInvitation i " +
                                    "WHERE i.id = :id AND i.invitee.id = :userId", Long.class)
                    .setParameter("id", invitationId)
                    .setParameter("userId", userId)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    private void ensureCapacityAvailable(Group group, Long groupId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            Long pendingInvitations = em.createQuery(
                            "SELECT COUNT(i) FROM MembershipInvitation i WHERE i.group.groupId = :groupId " +
                                    "AND i.status = 'PENDING'", Long.class)
                    .setParameter("groupId", groupId)
                    .getSingleResult();
            int memberCount = userGroupDao.countMembersInGroup(groupId);
            if (group.isCapacityConfigured() && memberCount + pendingInvitations >= group.getMaxMembers()) {
                throw new IllegalStateException("O grupo já atingiu a capacidade definida de "
                        + group.getMaxMembers() + " membros.");
            }
        } finally {
            em.close();
        }
    }

    public void sendContributionReminder(Long groupId, Long actorId, Long memberUserId) {
        Group group = findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Grupo não encontrado.");
        }
        requireOwnerOrAdministrator(group, actorId);
        Cycle cycle = cycleDao.findActiveCycleByGroup(groupId);
        if (cycle == null) {
            throw new IllegalStateException("Não existe um ciclo em andamento para enviar lembretes.");
        }
        UserGroup member = userGroupDao.findByUserAndGroup(memberUserId, groupId);
        if (member == null) {
            throw new IllegalArgumentException("O utilizador não pertence a este grupo.");
        }
        if (contributionDao.findByUserGroupAndCycle(member.getId(), cycle.getId()) != null) {
            throw new IllegalStateException("Este membro já pagou a contribuição do ciclo atual.");
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            Long existing = em.createQuery(
                            "SELECT COUNT(n) FROM UserNotification n WHERE n.recipient.id = :recipientId " +
                                    "AND n.cycle.id = :cycleId AND n.userGroup.id = :memberId " +
                                    "AND n.type = 'MANUAL_PAYMENT_REMINDER'", Long.class)
                    .setParameter("recipientId", memberUserId)
                    .setParameter("cycleId", cycle.getId())
                    .setParameter("memberId", member.getId())
                    .getSingleResult();
            if (existing > 0) {
                throw new IllegalStateException("Este membro já recebeu um lembrete neste ciclo.");
            }
            transaction.begin();
            UserNotification notification = new UserNotification();
            notification.setRecipient(em.getReference(User.class, memberUserId));
            notification.setCycle(em.getReference(Cycle.class, cycle.getId()));
            notification.setUserGroup(em.getReference(UserGroup.class, member.getId()));
            notification.setType("MANUAL_PAYMENT_REMINDER");
            notification.setMessage("O responsável do grupo " + group.getGroupName()
                    + " lembrou-o de pagar a contribuição do ciclo " + cycle.getNumero() + ".");
            em.persist(notification);
            transaction.commit();
        } catch (RuntimeException exception) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw exception;
        } finally {
            em.close();
        }
    }

    public boolean hasContributionReminder(Integer cycleId, Long userGroupId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT COUNT(n) FROM UserNotification n WHERE n.cycle.id = :cycleId " +
                                    "AND n.userGroup.id = :userGroupId AND n.type = 'MANUAL_PAYMENT_REMINDER'",
                            Long.class)
                    .setParameter("cycleId", cycleId)
                    .setParameter("userGroupId", userGroupId)
                    .getSingleResult() > 0;
        } finally {
            em.close();
        }
    }

    public void reportMember(Long groupId, Long reporterId, Long reportedUserId, String reason) {
        if (reason == null || reason.trim().length() < 10 || reason.trim().length() > 1000) {
            throw new IllegalArgumentException("Descreva a denúncia com pelo menos 10 caracteres.");
        }
        if (reporterId.equals(reportedUserId)
                || !userGroupDao.isUserInGroup(reporterId, groupId)
                || !userGroupDao.isUserInGroup(reportedUserId, groupId)) {
            throw new IllegalArgumentException("A denúncia deve ser sobre outro membro do seu grupo.");
        }
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            UserReport report = new UserReport();
            report.setGroup(em.getReference(Group.class, groupId));
            report.setReporter(em.getReference(User.class, reporterId));
            report.setReportedUser(em.getReference(User.class, reportedUserId));
            report.setReason(reason.trim());
            em.persist(report);
            transaction.commit();
        } catch (RuntimeException exception) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw exception;
        } finally {
            em.close();
        }
    }

    public void setGroupPrivate(Long groupId, Long actorId, boolean privateGroup) {
        Group group = findById(groupId);
        User actor = userDao.findById(actorId);
        if (group == null || actor == null) {
            throw new IllegalArgumentException("Grupo ou utilizador não encontrado.");
        }
        requireManagerOrAdministrator(group, actorId);
        group.setPrivateGroup(privateGroup);
        groupDao.updateGroup(group);
    }

    public void deleteGroup(Long groupId, Long actorId) {
        Group group = findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Grupo não encontrado.");
        }
        requireManagerOrAdministrator(group, actorId);
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

    private void requireOwnerOrAdministrator(Group group, Long actorId) {
        User actor = userDao.findById(actorId);
        boolean owner = group.getCreatedBy() != null && group.getCreatedBy().getId().equals(actorId);
        boolean privileged = actor != null && actor.getUserRole() != null
                && actor.getUserRole().isAdministrator();
        if (!owner && !privileged) {
            throw new SecurityException("Esta operação requer permissões do responsável do grupo ou de um administrador.");
        }
    }

    private void requireManagerOrAdministrator(Group group, Long actorId) {
        User actor = userDao.findById(actorId);
        boolean owner = group.getCreatedBy() != null && group.getCreatedBy().getId().equals(actorId);
        boolean privileged = actor != null && actor.getUserRole() != null
                && actor.getUserRole().isAdministrator();
        if (!owner && !privileged) {
            throw new SecurityException("Esta operação requer permissões do responsável do grupo ou de um administrador.");
        }
    }

    private void persistMembership(Long groupId, Long userId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            Group group = em.find(Group.class, groupId, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            Long memberCount = em.createQuery(
                            "SELECT COUNT(ug) FROM UserGroup ug WHERE ug.group.groupId = :groupId", Long.class)
                    .setParameter("groupId", groupId)
                    .getSingleResult();
            Long pendingInvitations = em.createQuery(
                            "SELECT COUNT(i) FROM MembershipInvitation i WHERE i.group.groupId = :groupId " +
                                    "AND i.status = 'PENDING'", Long.class)
                    .setParameter("groupId", groupId)
                    .getSingleResult();
            if (group == null || group.isCapacityConfigured()
                    && memberCount + pendingInvitations >= group.getMaxMembers()) {
                throw new IllegalStateException("O grupo já atingiu a capacidade definida de membros.");
            }
            Integer lastOrder = em.createQuery(
                            "SELECT MAX(ug.userReceivingOrder) FROM UserGroup ug WHERE ug.group.groupId = :groupId",
                            Integer.class)
                    .setParameter("groupId", groupId)
                    .getSingleResult();
            UserGroup membership = new UserGroup();
            membership.setGroup(em.getReference(Group.class, groupId));
            membership.setUser(em.getReference(User.class, userId));
            membership.setBalance(BigDecimal.ZERO);
            membership.setReceived(false);
            membership.setUserReceivingOrder(lastOrder == null ? 1 : lastOrder + 1);
            em.persist(membership);
            transaction.commit();
        } catch (RuntimeException exception) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw exception;
        } finally {
            em.close();
        }
    }

    private void ensureGroupIsNotArchived(Group group) {
        if (group.isArchived()) {
            throw new IllegalStateException("Este grupo está arquivado.");
        }
    }
}