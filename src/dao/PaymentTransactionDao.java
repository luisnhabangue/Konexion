package dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import model.PaymentTransaction;
/**
 * Implementação de acesso a dados responsável por consultar e persistir as entidades correspondentes através de JPA.
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
}