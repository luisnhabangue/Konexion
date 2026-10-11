package dao;

import dao.interfaces.UserGroupInterface;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import model.UserGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementação DAO para a gestão de vínculos entre utilizadores e grupos ({@link UserGroup}).
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public class UserGroupDao implements UserGroupInterface {

    private final EntityManagerFactory entityManagerFactory;

    public UserGroupDao(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void save(UserGroup userGroup) {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(userGroup);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Erro ao associar usuário ao grupo", e);
        } finally {
            em.close();
        }
    }

    @Override
    public void update(UserGroup userGroup) {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.merge(userGroup);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Erro ao atualizar dados do membro no grupo.", e);
        } finally {
            em.close();
        }
    }

    @Override
    public UserGroup findByUserAndGroup(Long userId, Long groupId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT ug FROM UserGroup ug JOIN FETCH ug.user WHERE ug.user.id = :userId AND ug.group.groupId = :groupId",
                            UserGroup.class
                    )
                    .setParameter("userId", userId)
                    .setParameter("groupId", groupId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

    @Override
    public List<UserGroup> findByGroupId(Long groupId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT ug FROM UserGroup ug JOIN FETCH ug.user WHERE ug.group.groupId = :groupId ORDER BY ug.userReceivingOrder ASC",
                            UserGroup.class
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
    public boolean isUserInGroup(Long userId, Long groupId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            Long count = em.createQuery(
                            "SELECT COUNT(ug) FROM UserGroup ug WHERE ug.user.id = :userId AND ug.group.groupId = :groupId", Long.class)
                    .setParameter("userId", userId)
                    .setParameter("groupId", groupId)
                    .getSingleResult();
            return count > 0;
        } finally {
            em.close();
        }
    }

    @Override
    public int countMembersInGroup(Long groupId) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            Long count = em.createQuery(
                            "SELECT COUNT(ug) FROM UserGroup ug WHERE ug.group.groupId = :groupId", Long.class)
                    .setParameter("groupId", groupId)
                    .getSingleResult();
            return count.intValue();
        } finally {
            em.close();
        }
    }
}