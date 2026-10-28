
package dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import model.SupportTicket;

import java.util.List;

/**
 * Implementação JPA para operações de persistência
 * relativas a pedidos de suporte.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public class SupportTicketDao {

    private final EntityManagerFactory entityManagerFactory;

    public SupportTicketDao(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * Regista um novo pedido de suporte.
     *
     * @param supportTicket Pedido a guardar
     */
    public void save(SupportTicket supportTicket) {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();
            em.persist(supportTicket);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException(
                    "Erro ao registar o pedido de suporte.", e
            );
        } finally {
            em.close();
        }
    }

    /**
     * Procura um pedido pelo seu identificador.
     *
     * @param supportTicketId Identificador do pedido
     * @return Pedido encontrado ou null
     */

    public SupportTicket findById(Integer supportTicketId) {
        EntityManager em = entityManagerFactory.createEntityManager();

        try {
            return em.createQuery(
                            "SELECT st FROM SupportTicket st " +
                                    "JOIN FETCH st.user " +
                                    "WHERE st.id = :supportTicketId",
                            SupportTicket.class
                    )
                    .setParameter("supportTicketId", supportTicketId)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
        } finally {
            em.close();
        }
    }

    public List<SupportTicket> findByUserId(Long userId) {
        EntityManager em = entityManagerFactory.createEntityManager();

        try {
            return em.createQuery(
                            "SELECT st FROM SupportTicket st " +
                                    "JOIN FETCH st.user " +
                                    "WHERE st.user.id = :userId " +
                                    "ORDER BY st.createdAt DESC",
                            SupportTicket.class
                    )
                    .setParameter("userId", userId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<SupportTicket> findAll() {
        EntityManager em = entityManagerFactory.createEntityManager();

        try {
            return em.createQuery(
                    "SELECT st FROM SupportTicket st " +
                            "JOIN FETCH st.user " +
                            "ORDER BY st.createdAt DESC",
                    SupportTicket.class
            ).getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Atualiza um pedido de suporte.
     *
     * @param supportTicket Pedido com os dados atualizados
     */
    public void update(SupportTicket supportTicket) {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();
            em.merge(supportTicket);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException(
                    "Erro ao atualizar o pedido de suporte.", e
            );
        } finally {
            em.close();
        }
    }
}
