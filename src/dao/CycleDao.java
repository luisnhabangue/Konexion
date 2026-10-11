package dao;

import dao.interfaces.CycleDaoInterface;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import model.Cycle;
import model.enums.CycleStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementação JPA para persistência de ciclos de rodadas ({@link Cycle}).
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public class CycleDao implements CycleDaoInterface {

    private final EntityManagerFactory entityManagerFactory;

    public CycleDao(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void save(Cycle cycle) {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(cycle);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Erro ao criar o ciclo no banco de dados.", e);
        } finally {
            em.close();
        }
    }

    @Override
    public Cycle findById(Integer id) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT c FROM Cycle c JOIN FETCH c.beneficiary b JOIN FETCH b.user WHERE c.id = :id",
                            Cycle.class
                    )
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

    @Override
    public Cycle findActiveCycleByGroup(Long groupId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT c FROM Cycle c " +
                                    "JOIN FETCH c.beneficiary b " +
                                    "JOIN FETCH b.user " +
                                    "WHERE c.group.groupId = :groupId AND c.status = :status",
                            Cycle.class
                    )
                    .setParameter("groupId", groupId)
                    .setParameter("status", CycleStatus.IN_PROGRESS)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

    @Override
    public List<Cycle> findByGroupId(Long groupId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT c FROM Cycle c WHERE c.group.groupId = :groupId ORDER BY c.numero ASC",
                            Cycle.class
                    )
                    .setParameter("groupId", groupId)
                    .getResultList();
        } catch (Exception e) {
            return new ArrayList<>();
        } finally {
            em.close();
        }
    }

    @Override
    public Integer findLastCycleNumberByGroup(Long groupId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            Integer maxNumero = em.createQuery(
                            "SELECT MAX(c.numero) FROM Cycle c WHERE c.group.groupId = :groupId",
                            Integer.class
                    )
                    .setParameter("groupId", groupId)
                    .getSingleResult();

            return maxNumero != null ? maxNumero : 0;
        } finally {
            em.close();
        }
    }

    @Override
    public void update(Cycle cycle) {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.merge(cycle);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Erro ao atualizar o ciclo.", e);
        } finally {
            em.close();
        }
    }

    @Override
    public void delete(Cycle cycle) {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Cycle toDelete = em.find(Cycle.class, cycle.getId());
            if (toDelete != null) {
                em.remove(toDelete);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Erro ao eliminar o ciclo.", e);
        } finally {
            em.close();
        }
    }
}