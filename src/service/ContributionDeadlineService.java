package service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import model.Contribution;
import model.Cycle;
import model.Group;
import model.PlatformSettings;
import model.User;
import model.UserGroup;
import model.UserNotification;
import model.PaymentTransaction;
import model.enums.ContributionStatus;
import model.enums.CycleStatus;
import model.enums.PaymentMethod;
import payment.PaymentResponse;
import dao.PaymentTransactionDao;
import util.EntityManagerFactoryUtil;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ContributionDeadlineService {
    public static final int LATE_PENALTY_POINTS = 10;

    private static final String REMINDER_MEMBER = "DUE_SOON_MEMBER";
    private static final String REMINDER_LEADER = "DUE_SOON_LEADER";
    private static final String OVERDUE_MEMBER = "OVERDUE_MEMBER";
    private static final String OVERDUE_LEADER = "OVERDUE_LEADER";
    private static final String LATE_PAYMENT_LEADER = "LATE_PAYMENT_LEADER";

    private final EntityManagerFactory factory = EntityManagerFactoryUtil.getEntityManagerFactory();
    private final PaymentTransactionDao transactionDao = new PaymentTransactionDao(factory);

    public void processDeadlines() {
        EntityManager em = factory.createEntityManager();
        try {
            List<Cycle> cycles = em.createQuery(
                            "SELECT DISTINCT c FROM Cycle c JOIN FETCH c.group g JOIN FETCH g.createdBy " +
                                    "LEFT JOIN FETCH g.members ug LEFT JOIN FETCH ug.user",
                            Cycle.class)
                    .getResultList();
            LocalDate today = LocalDate.now();
            PlatformSettings settings = em.find(PlatformSettings.class, 1);
            if (settings == null) {
                settings = new PlatformSettings();
            }
            for (Cycle cycle : cycles) {
                processCycle(cycle.getId(), today, settings);
                processAutomaticPayout(cycle.getId(), today);
            }
        } finally {
            em.close();
        }
    }

    private void processAutomaticPayout(Integer cycleId, LocalDate today) {
        Cycle cycle = new dao.CycleDao(factory).findById(cycleId);
        if (cycle == null || !cycle.isAutomaticPayoutEnabled()
                || cycle.getStatus() != CycleStatus.CONCLUDED) {
            return;
        }
        Long groupId = cycle.getGroup().getGroupId();
        Group group = new dao.GroupDao(factory).findById(groupId);
        if (group == null || today.isBefore(paymentDueDate(group, cycle))
                || transactionDao.hasPayoutAttempt(cycleId)) {
            return;
        }

        User beneficiary = cycle.getBeneficiary().getUser();
        String phone = beneficiary.getPhoneNumber();
        if (phone == null || phone.isBlank()) {
            notifyAutomaticPayoutFailure(cycle, group,
                    "Não foi possível enviar automaticamente o bolo do ciclo " + cycle.getNumero()
                            + ": o beneficiário não tem telefone cadastrado. O responsável deve transferir manualmente.");
            return;
        }

        PaymentResponse response = new service.PaymentService().payCyclePayout(
                groupId, cycleId, PaymentMethod.MPESA, phone);
        if (!response.success()) {
            notifyAutomaticPayoutFailure(cycle, group,
                    "A transferência automática do ciclo " + cycle.getNumero() + " falhou: "
                            + response.message() + " O responsável pode tentar novamente manualmente.");
        }
    }

    private void notifyAutomaticPayoutFailure(Cycle cycle, Group group, String message) {
        EntityManager em = factory.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            Long existing = em.createQuery(
                            "SELECT COUNT(n) FROM UserNotification n WHERE n.recipient.id = :recipientId " +
                                    "AND n.cycle.id = :cycleId AND n.userGroup.id = :memberId " +
                                    "AND n.type = 'AUTO_PAYOUT_FAILED'", Long.class)
                    .setParameter("recipientId", group.getCreatedBy().getId())
                    .setParameter("cycleId", cycle.getId())
                    .setParameter("memberId", cycle.getBeneficiary().getId())
                    .getSingleResult();
            if (existing > 0) {
                return;
            }
            transaction.begin();
            UserNotification notification = new UserNotification();
            notification.setRecipient(em.getReference(User.class, group.getCreatedBy().getId()));
            notification.setCycle(em.getReference(Cycle.class, cycle.getId()));
            notification.setUserGroup(em.getReference(UserGroup.class, cycle.getBeneficiary().getId()));
            notification.setType("AUTO_PAYOUT_FAILED");
            notification.setMessage(message);
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

    public List<UserNotification> findUnread(Long userId) {
        EntityManager em = factory.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT n FROM UserNotification n JOIN FETCH n.cycle c JOIN FETCH c.group " +
                                    "JOIN FETCH n.userGroup ug JOIN FETCH ug.user " +
                                    "WHERE n.recipient.id = :userId AND n.opened = false ORDER BY n.createdAt DESC",
                            UserNotification.class)
                    .setParameter("userId", userId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public int countUnread(Long userId) {
        EntityManager em = factory.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT COUNT(n) FROM UserNotification n " +
                                    "WHERE n.recipient.id = :userId AND n.opened = false",
                            Long.class)
                    .setParameter("userId", userId)
                    .getSingleResult().intValue();
        } finally {
            em.close();
        }
    }

    public void markRead(Long userId, Long notificationId) {
        EntityManager em = factory.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            UserNotification notification = em.createQuery(
                            "SELECT n FROM UserNotification n WHERE n.id = :notificationId " +
                                    "AND n.recipient.id = :userId",
                            UserNotification.class)
                    .setParameter("notificationId", notificationId)
                    .setParameter("userId", userId)
                    .getResultStream().findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Notificação não encontrada."));
            notification.setRead(true);
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

    private void processCycle(Integer cycleId, LocalDate today, PlatformSettings settings) {
        EntityManager em = factory.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            Cycle cycle = em.createQuery(
                            "SELECT DISTINCT c FROM Cycle c JOIN FETCH c.group g JOIN FETCH g.createdBy " +
                                    "LEFT JOIN FETCH g.members ug LEFT JOIN FETCH ug.user WHERE c.id = :cycleId",
                            Cycle.class)
                    .setParameter("cycleId", cycleId)
                    .getSingleResult();
            Group group = cycle.getGroup();
            LocalDate dueDate = paymentDueDate(group, cycle);
            LocalDate reminderDate = dueDate.minusDays(settings.getReminderDaysBefore());
            Set<Long> paidMemberIds = new HashSet<>(em.createQuery(
                            "SELECT c.userGroup.id FROM Contribution c WHERE c.cycle.id = :cycleId " +
                                    "AND c.status = :status",
                            Long.class)
                    .setParameter("cycleId", cycleId)
                    .setParameter("status", ContributionStatus.PAID)
                    .getResultList());
            boolean active = cycle.getStatus() == CycleStatus.IN_PROGRESS;

            for (UserGroup member : cycle.getGroup().getMembers()) {
                if (paidMemberIds.contains(member.getId())) {
                    Contribution contribution = em.createQuery(
                                    "SELECT c FROM Contribution c WHERE c.cycle.id = :cycleId " +
                                            "AND c.userGroup.id = :userGroupId",
                                    Contribution.class)
                            .setParameter("cycleId", cycleId)
                            .setParameter("userGroupId", member.getId())
                            .getResultStream().findFirst().orElse(null);
                    if (contribution != null && contribution.getPaymentDate() != null
                            && contribution.getPaymentDate().isAfter(dueDate)) {
                        applyPenalty(em, cycle, member, false, settings);
                        User leader = group.getCreatedBy();
                        if (settings.isLatePaymentNotificationsEnabled()
                                && !leader.getId().equals(member.getUser().getId())
                                && !hasNotification(em, leader.getId(), cycleId, member.getId(),
                                OVERDUE_LEADER)
                                && !hasNotification(em, leader.getId(), cycleId, member.getId(),
                                LATE_PAYMENT_LEADER)) {
                            createNotification(em, leader, cycle, member, LATE_PAYMENT_LEADER,
                                    memberName(member) + " pagou a contribuição do ciclo "
                                            + cycle.getNumero() + " depois do prazo.");
                        }
                    }
                    continue;
                }

                if (settings.isRemindersEnabled() && active
                        && !today.isBefore(reminderDate) && !today.isAfter(dueDate)) {
                    notifyUpcomingDeadline(em, cycle, member, dueDate, settings);
                } else if (today.isAfter(dueDate)) {
                    applyPenalty(em, cycle, member, true, settings);
                    if (settings.isOverdueNotificationsEnabled()) {
                        notifyLeaderOverdue(em, cycle, member, dueDate, settings);
                    }
                }
            }
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

    private void notifyUpcomingDeadline(EntityManager em, Cycle cycle, UserGroup member, LocalDate dueDate,
                                        PlatformSettings settings) {
        Integer cycleId = cycle.getId();
        Long memberId = member.getId();
        User user = member.getUser();
        if (!hasNotification(em, user.getId(), cycleId, memberId, REMINDER_MEMBER)) {
            String penaltyWarning = settings.getLatePenaltyPoints() > 0
                    ? " para evitar a perda de " + settings.getLatePenaltyPoints() + " pontos."
                    : " para evitar ficar em atraso.";
            createNotification(em, user, cycle, member, REMINDER_MEMBER,
                    "A contribuição de " + member.getGroup().getGroupName() + " no ciclo "
                            + cycle.getNumero() + " vence em " + dueDate + ". Pague até essa data"
                            + penaltyWarning);
        }
        User leader = cycle.getGroup().getCreatedBy();
        if (!leader.getId().equals(user.getId())
                && !hasNotification(em, leader.getId(), cycleId, memberId, REMINDER_LEADER)) {
            createNotification(em, leader, cycle, member, REMINDER_LEADER,
                    memberName(member) + " ainda não pagou a contribuição de "
                            + member.getGroup().getGroupName() + " no ciclo " + cycle.getNumero()
                            + ". O prazo termina em " + dueDate + ".");
        }
    }

    private void notifyLeaderOverdue(EntityManager em, Cycle cycle, UserGroup member, LocalDate dueDate,
                                     PlatformSettings settings) {
        User leader = cycle.getGroup().getCreatedBy();
        if (!leader.getId().equals(member.getUser().getId())
                && !hasNotification(em, leader.getId(), cycle.getId(), member.getId(), OVERDUE_LEADER)) {
            createNotification(em, leader, cycle, member, OVERDUE_LEADER,
                    memberName(member) + " não pagou a contribuição de "
                            + member.getGroup().getGroupName() + " até " + dueDate + ". "
                            + penaltySummary(settings.getLatePenaltyPoints()));
        }
    }

    private void applyPenalty(EntityManager em, Cycle cycle, UserGroup member, boolean overdue,
                              PlatformSettings settings) {
        Long userId = member.getUser().getId();
        if (hasNotification(em, userId, cycle.getId(), member.getId(), OVERDUE_MEMBER)) {
            return;
        }
        UserNotification penalty = notification(em, member.getUser(), cycle, member, OVERDUE_MEMBER,
                overdue
                        ? "O prazo da contribuição de " + member.getGroup().getGroupName() + " no ciclo "
                                + cycle.getNumero() + " expirou sem pagamento. "
                                + penaltySummary(settings.getLatePenaltyPoints())
                        : "A contribuição de " + member.getGroup().getGroupName() + " no ciclo "
                                + cycle.getNumero() + " foi paga depois do prazo. "
                                + penaltySummary(settings.getLatePenaltyPoints()));
        em.persist(penalty);
        member.getUser().setPunctualityPoints(
                member.getUser().getPunctualityPoints() - settings.getLatePenaltyPoints());
    }

    private boolean hasNotification(EntityManager em, Long recipientId, Integer cycleId,
                                    Long userGroupId, String type) {
        return em.createQuery(
                        "SELECT COUNT(n) FROM UserNotification n WHERE n.recipient.id = :recipientId " +
                                "AND n.cycle.id = :cycleId AND n.userGroup.id = :userGroupId AND n.type = :type",
                        Long.class)
                .setParameter("recipientId", recipientId)
                .setParameter("cycleId", cycleId)
                .setParameter("userGroupId", userGroupId)
                .setParameter("type", type)
                .getSingleResult() > 0;
    }

    private void createNotification(EntityManager em, User recipient, Cycle cycle,
                                    UserGroup member, String type, String message) {
        em.persist(notification(em, recipient, cycle, member, type, message));
    }

    private UserNotification notification(EntityManager em, User recipient, Cycle cycle,
                                          UserGroup member, String type, String message) {
        UserNotification notification = new UserNotification();
        notification.setRecipient(em.getReference(User.class, recipient.getId()));
        notification.setCycle(cycle);
        notification.setUserGroup(member);
        notification.setType(type);
        notification.setMessage(message);
        return notification;
    }

    private String memberName(UserGroup member) {
        return member.getUser().getFirstName() + " " + member.getUser().getLastName();
    }

    private String penaltySummary(int penaltyPoints) {
        return penaltyPoints > 0
                ? "Foram descontados " + penaltyPoints + " pontos."
                : "Não há penalização activa.";
    }

    public static LocalDate paymentDueDate(Group group, Cycle cycle) {
        if (group.getFrequency() == 1) {
            return cycle.getEndDate() != null
                    ? cycle.getEndDate()
                    : cycle.getStartDate().plusDays(group.getFrequency());
        }

        YearMonth month = YearMonth.from(cycle.getStartDate());
        if (cycle.getStartDate().getDayOfMonth() > group.getPayday()) {
            month = month.plusMonths(1);
        }
        return month.atDay(Math.min(group.getPayday(), month.lengthOfMonth()));
    }
}
