package service;

import dao.UserDao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import model.Group;
import model.AdminUserSummary;
import model.LeadershipRequest;
import model.User;
import model.UserReport;
import model.enums.ContributionStatus;
import util.EntityManagerFactoryUtil;

import java.math.BigDecimal;
import java.util.List;
/**
 * Serviço de aplicação que coordena as regras de negócio e as operações de domínio correspondentes.
 *
 * @since 1.0
 */

public class AdminService {
    public record Metrics(long users, long activeUsers, long verifiedUsers, long groups,
                          long publicGroups, long paidContributions, BigDecimal contributed,
                          long openReports, long pendingKyc) {}

    private final EntityManagerFactory factory = EntityManagerFactoryUtil.getEntityManagerFactory();
    private final UserDao userDao = new UserDao(factory);
    public Metrics getMetrics(Long administratorId) {
        requireAdministrator(administratorId);
        EntityManager em = factory.createEntityManager();
        try {
            long users = em.createQuery("SELECT COUNT(u) FROM User u", Long.class).getSingleResult();
            long activeUsers = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.active = true",
                    Long.class).getSingleResult();
            long verifiedUsers = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.verified = true",
                    Long.class).getSingleResult();
            long groups = em.createQuery("SELECT COUNT(g) FROM Group g", Long.class).getSingleResult();
            long publicGroups = em.createQuery("SELECT COUNT(g) FROM Group g WHERE g.privateGroup = false",
                    Long.class).getSingleResult();
            long pendingKyc = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.kycStatus = 'PENDING'",
                    Long.class).getSingleResult();
            long paidCount = em.createQuery(
                    "SELECT COUNT(c) FROM Contribution c WHERE c.status = :status", Long.class)
                    .setParameter("status", ContributionStatus.PAID).getSingleResult();
            BigDecimal contributed = em.createQuery(
                    "SELECT COALESCE(SUM(c.amount), 0) FROM Contribution c WHERE c.status = :status", BigDecimal.class)
                    .setParameter("status", ContributionStatus.PAID).getSingleResult();
            long reports = em.createQuery(
                    "SELECT COUNT(r) FROM UserReport r WHERE r.status = 'OPEN'", Long.class).getSingleResult();
            return new Metrics(users, activeUsers, verifiedUsers, groups, publicGroups,
                    paidCount, contributed == null ? BigDecimal.ZERO : contributed,
                    reports, pendingKyc);
        } finally {
            em.close();
        }
    }

    public List<User> getPendingKyc(Long administratorId) {
        requireAdministrator(administratorId);
        EntityManager em = factory.createEntityManager();
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.kycStatus = 'PENDING' ORDER BY u.id",
                    User.class).getResultList();
        } finally {
            em.close();
        }
    }

    public List<AdminUserSummary> getUsers(Long administratorId) {
        requireAdministrator(administratorId);
        EntityManager em = factory.createEntityManager();
        try {
            return em.createQuery("SELECT new model.AdminUserSummary(u.id, u.firstName, u.lastName, " +
                            "u.username, u.userRole, u.active, u.verified, u.strikeCount, u.punctualityPoints) " +
                            "FROM User u ORDER BY u.id", AdminUserSummary.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<UserReport> getOpenReports(Long administratorId) {
        requireAdministrator(administratorId);
        EntityManager em = factory.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT r FROM UserReport r JOIN FETCH r.reporter JOIN FETCH r.reportedUser " +
                                    "JOIN FETCH r.group WHERE r.status = 'OPEN' ORDER BY r.createdAt DESC",
                            UserReport.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<LeadershipRequest> getLeadershipRequests(Long administratorId) {
            requireAdministrator(administratorId);
            EntityManager em = factory.createEntityManager();
            try {
                return em.createQuery(
                                "SELECT r FROM LeadershipRequest r JOIN FETCH r.requester " +
                                        "WHERE r.status = 'PENDING' ORDER BY r.createdAt",
                                LeadershipRequest.class)
                        .getResultList();
            } finally {
                em.close();
            }
    }

    public void requestGroupLeadership(Long userId, String reason) {
            if (reason == null || reason.trim().length() < 10 || reason.trim().length() > 1000) {
                throw new IllegalArgumentException("Explique o pedido com pelo menos 10 caracteres.");
            }
            User requester = userDao.findById(userId);
            if (requester == null || !requester.isVerified()) {
                throw new IllegalStateException("Apenas utilizadores verificados podem pedir nomeação como chefe de grupo.");
            }
            if (requester.getUserRole() != null && requester.getUserRole().isGroupLeader()) {
                throw new IllegalStateException("Já tem o perfil de chefe de grupo.");
            }
            EntityManager em = factory.createEntityManager();
            try {
                Long pending = em.createQuery(
                                "SELECT COUNT(r) FROM LeadershipRequest r " +
                                        "WHERE r.requester.id = :userId AND r.status = 'PENDING'", Long.class)
                        .setParameter("userId", userId).getSingleResult();
                if (pending > 0) {
                    throw new IllegalStateException("Já tem um pedido de nomeação pendente.");
                }
                EntityTransaction transaction = em.getTransaction();
                transaction.begin();
                LeadershipRequest request = new LeadershipRequest();
                request.setRequester(em.getReference(User.class, userId));
                request.setReason(reason.trim());
                em.persist(request);
                transaction.commit();
            } finally {
                em.close();
            }
    }

    public void resolveLeadershipRequest(Long administratorId, Long requestId, boolean approved) {
            requireAdministrator(administratorId);
            EntityManager em = factory.createEntityManager();
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();
                LeadershipRequest request = em.createQuery(
                                "SELECT r FROM LeadershipRequest r JOIN FETCH r.requester WHERE r.id = :id",
                                LeadershipRequest.class)
                        .setParameter("id", requestId).getResultStream().findFirst().orElse(null);
                if (request == null || !"PENDING".equals(request.getStatus())) {
                    throw new IllegalArgumentException("O pedido já foi analisado ou não existe.");
                }
                if (approved) {
                    if (!request.getRequester().isVerified()) {
                        throw new IllegalStateException("O utilizador precisa de verificação KYC aprovada.");
                    }
                    request.getRequester().setUserRole(model.enums.UserRole.GROUP_LEADER);
                }
                request.setStatus(approved ? "APPROVED" : "REJECTED");
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

    public void resolveKyc(Long administratorId, Long userId, boolean approved) {
        requireAdministrator(administratorId);
        new AuthService().reviewKyc(administratorId, userId, approved);
    }

    public void strikeUser(Long administratorId, Long userId) {
        requireAdministrator(administratorId);
        updateUser(userId, user -> user.setStrikeCount(user.getStrikeCount() + 1));
    }

    public void disableUser(Long administratorId, Long userId) {
        requireAdministrator(administratorId);
        if (administratorId.equals(userId)) {
            throw new IllegalArgumentException("Não pode desativar a própria conta de administrador.");
        }
        updateUser(userId, user -> user.setActive(false));
    }

    public void resolveReport(Long administratorId, Long reportId) {
        requireAdministrator(administratorId);
        EntityManager em = factory.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            UserReport report = em.find(UserReport.class, reportId);
            if (report == null || !"OPEN".equals(report.getStatus())) {
                throw new IllegalArgumentException("A denúncia já foi resolvida ou não existe.");
            }
            report.setStatus("RESOLVED");
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

    private void updateUser(Long userId, java.util.function.Consumer<User> update) {
        User user = userDao.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Utilizador não encontrado.");
        }
        update.accept(user);
        userDao.updateUser(user);
    }

    private void requireAdministrator(Long administratorId) {
        User administrator = userDao.findById(administratorId);
        if (administrator == null || administrator.getUserRole() == null
                || !administrator.getUserRole().isAdministrator()) {
            throw new SecurityException("Esta ação está disponível apenas para administradores.");
        }
    }
}
