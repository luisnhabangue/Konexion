package service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import model.Contribution;
import model.Cycle;
import model.Group;
import model.User;
import model.UserGroup;
import model.UserNotification;
import model.enums.ContributionStatus;
import model.enums.CycleStatus;
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

    public void processDeadlines() {
        EntityManager em = factory.createEntityManager();
        try {
            List<Cycle> cycles = em.createQuery(
                            "SELECT DISTINCT c FROM Cycle c JOIN FETCH c.group g JOIN FETCH g.createdBy " +
                                    "LEFT JOIN FETCH g.members ug LEFT JOIN FETCH ug.user",
                            Cycle.class)
                    .getResultList();
            LocalDate today = LocalDate.now();
            for (Cycle cycle : cycles) {
                processCycle(cycle.getId(), today);
            }
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

    private void processCycle(Integer cycleId, LocalDate today) {
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
            LocalDate reminderDate = dueDate.minusDays(1);
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
                        applyPenalty(em, cycle, member, false);
                        User leader = group.getCreatedBy();
                        if (!leader.getId().equals(member.getUser().getId())
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

                if (active && !today.isBefore(reminderDate) && !today.isAfter(dueDate)) {
                    notifyUpcomingDeadline(em, cycle, member, dueDate);
                } else if (today.isAfter(dueDate)) {
                    applyPenalty(em, cycle, member, true);
                    notifyLeaderOverdue(em, cycle, member, dueDate);
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

    private void notifyUpcomingDeadline(EntityManager em, Cycle cycle, UserGroup member, LocalDate dueDate) {
        Integer cycleId = cycle.getId();
        Long memberId = member.getId();
        User user = member.getUser();
        if (!hasNotification(em, user.getId(), cycleId, memberId, REMINDER_MEMBER)) {
            createNotification(em, user, cycle, member, REMINDER_MEMBER,
                    "A contribuição de " + member.getGroup().getGroupName() + " no ciclo "
                            + cycle.getNumero() + " vence em " + dueDate + ". Pague até essa data para evitar "
                            + "a perda de " + LATE_PENALTY_POINTS + " pontos.");
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

    private void notifyLeaderOverdue(EntityManager em, Cycle cycle, UserGroup member, LocalDate dueDate) {
        User leader = cycle.getGroup().getCreatedBy();
        if (!leader.getId().equals(member.getUser().getId())
                && !hasNotification(em, leader.getId(), cycle.getId(), member.getId(), OVERDUE_LEADER)) {
            createNotification(em, leader, cycle, member, OVERDUE_LEADER,
                    memberName(member) + " não pagou a contribuição de "
                            + member.getGroup().getGroupName() + " até " + dueDate
                            + ". Foram descontados " + LATE_PENALTY_POINTS + " pontos.");
        }
    }

    private void applyPenalty(EntityManager em, Cycle cycle, UserGroup member, boolean overdue) {
        Long userId = member.getUser().getId();
        if (hasNotification(em, userId, cycle.getId(), member.getId(), OVERDUE_MEMBER)) {
            return;
        }
        UserNotification penalty = notification(em, member.getUser(), cycle, member, OVERDUE_MEMBER,
                overdue
                        ? "O prazo da contribuição de " + member.getGroup().getGroupName() + " no ciclo "
                        + cycle.getNumero() + " expirou sem pagamento. Foram descontados "
                        + LATE_PENALTY_POINTS + " pontos."
                        : "A contribuição de " + member.getGroup().getGroupName() + " no ciclo "
                        + cycle.getNumero() + " foi paga depois do prazo. Foram descontados "
                        + LATE_PENALTY_POINTS + " pontos.");
        em.persist(penalty);
        member.getUser().setPunctualityPoints(
                member.getUser().getPunctualityPoints() - LATE_PENALTY_POINTS);
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
