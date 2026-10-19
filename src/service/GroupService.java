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
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
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

        if (group.getCreatedBy() == null || !group.getCreatedBy().isActive()) {
            throw new IllegalStateException("Utilizadores inativos não podem criar grupos.");
        }
        User persistedCreator = userDao.findById(group.getCreatedBy().getId());
        if (persistedCreator == null || !persistedCreator.isActive()) {
            throw new IllegalStateException("O utilizador que cria o grupo não está ativo.");
        }
        if (!persistedCreator.isVerified() && (persistedCreator.getUserRole() == null
                || !persistedCreator.getUserRole().isAdministrator())) {
            throw new IllegalStateException("A criação de grupos requer verificação KYC aprovada por um administrador. Pode continuar a participar nos grupos existentes.");
        }
        if (groupDao.findByName(group.getGroupName()) != null) {
            throw new IllegalArgumentException("Já existe um grupo com este nome.");
        }

        User user = persistedCreator;
        group.setCreatedBy(user);
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
        requireOwnerOrAdministrator(group, actorId);

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

        persistMembership(groupId, userId, nextOrder);
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
        requireOwnerOrAdministrator(group, actorId);

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

        if (!paymentDate.isAfter(paymentDueDate(ctx.group(), ctx.cycle()))) {
            User member = userGroup.getUser();
            member.setPunctualityPoints(member.getPunctualityPoints() + 10);
            userDao.updateUser(member);
        }

        BigDecimal newBalance = (userGroup.getBalance() != null ? userGroup.getBalance() : BigDecimal.ZERO).add(amount);
        userGroup.setBalance(newBalance);
        userGroupDao.update(userGroup);

        checkAndFinalizeCycleIfComplete(ctx.group(), ctx.cycle());
    }

    private LocalDate paymentDueDate(Group group, Cycle cycle) {
        YearMonth month = YearMonth.from(cycle.getStartDate());
        if (cycle.getStartDate().getDayOfMonth() > group.getPayday()) {
            month = month.plusMonths(1);
        }
        return month.atDay(Math.min(group.getPayday(), month.lengthOfMonth()));
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
        User user = userDao.findById(userId);
        boolean elevated = user != null && user.getUserRole() != null
                && (user.getUserRole().isAdministrator() || user.getUserRole().isGroupLeader());
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
                            "WHERE g.privateGroup = false ORDER BY g.createdAt DESC", Group.class).getResultList();
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
        if (group.isPrivateGroup()) {
            throw new IllegalStateException("Este grupo é privado e requer um convite.");
        }
        if (userGroupDao.isUserInGroup(userId, groupId)) {
            throw new IllegalStateException("Já participa neste grupo.");
        }
        if (!cycleDao.findByGroupId(groupId).isEmpty()) {
            throw new IllegalStateException("Este grupo já iniciou ciclos e não aceita novos membros.");
        }
        persistMembership(groupId, userId, userGroupDao.findByGroupId(groupId).stream()
                .map(UserGroup::getUserReceivingOrder).filter(order -> order != null)
                .max(Integer::compareTo).orElse(0) + 1);
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
        boolean manager = group.getCreatedBy().getId().equals(inviterId)
                || inviter.getUserRole() != null && inviter.getUserRole().isAdministrator();
        if (!manager) {
            throw new SecurityException("Apenas o responsável do grupo, um chefe de grupo ou um administrador pode convidar membros.");
        }
        if (userGroupDao.isUserInGroup(inviteeId, groupId)) {
            throw new IllegalStateException("Este utilizador já participa no grupo.");
        }
        if (!cycleDao.findByGroupId(groupId).isEmpty()) {
            throw new IllegalStateException("Não é possível convidar membros depois de iniciar ciclos.");
        }
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
            MembershipInvitation invitation = new MembershipInvitation();
            invitation.setGroup(em.getReference(Group.class, groupId));
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
                if (!cycleDao.findByGroupId(invitation.getGroup().getGroupId()).isEmpty()) {
                    throw new IllegalStateException("O grupo já iniciou ciclos e não aceita novos membros.");
                }
                User user = em.getReference(User.class, userId);
                Group group = invitation.getGroup();
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
                && (actor.getUserRole().isAdministrator() || actor.getUserRole().isGroupLeader());
        if (!owner && !privileged) {
            throw new SecurityException("Esta operação requer permissões do responsável do grupo, chefe de grupo ou administrador.");
        }
    }

    private void persistMembership(Long groupId, Long userId, int order) {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            UserGroup membership = new UserGroup();
            membership.setGroup(em.getReference(Group.class, groupId));
            membership.setUser(em.getReference(User.class, userId));
            membership.setBalance(BigDecimal.ZERO);
            membership.setReceived(false);
            membership.setUserReceivingOrder(order);
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
}