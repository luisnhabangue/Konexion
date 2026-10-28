package dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import model.PaymentTransaction;

/**
 * Implementação de acesso a dados responsável por consultar e persistir transações de pagamento.
 *
 * @since 1.0
 */
public class PaymentTransactionDao {

    private final EntityManagerFactory entityManagerFactory;

    public PaymentTransactionDao(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public void save(PaymentTransaction transaction) {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(transaction);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Erro ao registar a transação de pagamento.", e);
        } finally {
            em.close();
        }
    }

    public void update(PaymentTransaction transaction) {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.merge(transaction);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Erro ao atualizar a transação de pagamento.", e);
        } finally {
            em.close();
        }
    }

    public boolean hasSuccessfulPayout(Integer cycleId) {
        return countPayouts(cycleId, true) > 0;
    }

    public boolean hasPayoutAttempt(Integer cycleId) {
        return countPayouts(cycleId, false) > 0;
    }

    private long countPayouts(Integer cycleId, boolean successfulOnly) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            String statusFilter = successfulOnly ? " AND t.status = 'SUCCESS'" : "";
            return em.createQuery(
                            "SELECT COUNT(t) FROM PaymentTransaction t WHERE t.cycleId = :cycleId " +
                                    "AND t.payout = true" + statusFilter, Long.class)
                    .setParameter("cycleId", cycleId)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }
}
