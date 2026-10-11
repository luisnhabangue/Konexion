package dao;

import dao.interfaces.ContributionDaoInterface;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import model.Contribution;
import model.enums.ContributionStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ContributionDao implements ContributionDaoInterface {

    private final EntityManagerFactory entityManagerFactory;

    public ContributionDao(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void save(Contribution contribution) {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(contribution);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Erro ao registrar a contribuição.", e);
        } finally {
            em.close();
        }
    }

    @Override
    public Contribution findById(Long id) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            return em.find(Contribution.class, id);
        } finally {
            em.close();
        }
    }

    /**
     * Busca a contribuição/pagamento de um determinado membro num ciclo específico.
     */
    @Override
    public Contribution findByUserGroupAndCycle(Long userGroupId, Integer cycleId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT c FROM Contribution c WHERE c.userGroup.id = :userGroupId AND c.cycle.id = :cycleId",
                            Contribution.class
                    )
                    .setParameter("userGroupId", userGroupId)
                    .setParameter("cycleId", cycleId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null; // Membro ainda não possui registro de pagamento neste ciclo
        } finally {
            em.close();
        }
    }

    /**
     * Retorna todas as contribuições efetuadas em um ciclo específico.
     */
    @Override
    public List<Contribution> findByCycleId(Integer cycleId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT c FROM Contribution c WHERE c.cycle.id = :cycleId",
                            Contribution.class
                    )
                    .setParameter("cycleId", cycleId)
                    .getResultList();
        } catch (Exception e) {
            return new ArrayList<>();
        } finally {
            em.close();
        }
    }

    /**
     * Retorna o histórico de todas as contribuições de um determinado membro.
     */
    @Override
    public List<Contribution> findByUserGroupId(Long userGroupId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT c FROM Contribution c WHERE c.userGroup.id = :userGroupId ORDER BY c.paymentDate DESC",
                            Contribution.class
                    )
                    .setParameter("userGroupId", userGroupId)
                    .getResultList();
        } catch (Exception e) {
            return new ArrayList<>();
        } finally {
            em.close();
        }
    }

    /**
     * Soma o valor total já arrecadado/pago pelos membros no ciclo atual.
     * Útil para saber se já é possível pagar o bolo do Xitique ao beneficiário!
     */
    @Override
    public BigDecimal sumTotalPaidByCycle(Integer cycleId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            BigDecimal total = em.createQuery(
                            "SELECT SUM(c.amount) FROM Contribution c WHERE c.cycle.id = :cycleId AND c.status = :status",
                            BigDecimal.class
                    )
                    .setParameter("cycleId", cycleId)
                    .setParameter("status", ContributionStatus.PAID)
                    .getSingleResult();

            return total != null ? total : BigDecimal.ZERO;
        } finally {
            em.close();
        }
    }

    @Override
    public void update(Contribution contribution) {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.merge(contribution);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Erro ao atualizar a contribuição.", e);
        } finally {
            em.close();
        }
    }

    @Override
    public void delete(Contribution contribution) {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Contribution toDelete = em.find(Contribution.class, contribution.getId());
            if (toDelete != null) {
                em.remove(toDelete);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Erro ao remover a contribuição.", e);
        } finally {
            em.close();
        }
    }
}