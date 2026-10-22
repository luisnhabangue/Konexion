package dao;

import dao.interfaces.GroupDaoInterface;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import model.Group;
import model.User;

import java.util.ArrayList;

/**
 * Implementação JPA para persistência e gestão de {@link Group}.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public class GroupDao implements GroupDaoInterface {

    private final EntityManagerFactory entityManagerFactory;

    /**
     * Construtor da DAO de Grupo.
     * @param entityManagerFactory Fábrica do Entity Manager
     */
    public GroupDao(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public Group findById(Long id) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            return em.find(Group.class, id);
        } finally {
            em.close();
        }
    }

    @Override
    public Group findByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            return em.createQuery(
                    "SELECT g FROM Group g WHERE LOWER(g.groupName) = LOWER(:name)",
                    Group.class
            ).setParameter("name", name.trim()).getResultStream().findFirst().orElse(null);
        } finally {
            em.close();
        }
    }

    @Override
    public void createGroup(Group group) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            User creator = entityManager.getReference(User.class, group.getCreatedBy().getId());
            group.setCreatedBy(creator);
            group.getMembers().forEach(member -> member.setUser(creator));
            entityManager.persist(group);
            transaction.commit();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Erro ao guardar o grupo.", e);
        } finally {
            entityManager.close();
        }
    }

    @Override
    public ArrayList<Group> findAll() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            return new ArrayList<>(
                    entityManager.createQuery(
                            "SELECT DISTINCT g FROM Group g LEFT JOIN FETCH g.members m LEFT JOIN FETCH m.user ORDER BY g.createdAt DESC",
                            Group.class
                    ).getResultList()
            );
        } catch (Exception e) {
            throw new RuntimeException("Erro ao listar os grupos.", e);
        } finally {
            entityManager.close();
        }
    }

    @Override
    public ArrayList<Group> findAllForUser(Long userId) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            return new ArrayList<>(entityManager.createQuery(
                            "SELECT DISTINCT g FROM Group g " +
                                    "LEFT JOIN FETCH g.members m LEFT JOIN FETCH m.user " +
                                    "WHERE g.createdBy.id = :userId OR EXISTS " +
                                    "(SELECT ug.id FROM UserGroup ug WHERE ug.group = g AND ug.user.id = :userId) " +
                                    "ORDER BY g.createdAt DESC",
                            Group.class
                    ).setParameter("userId", userId).getResultList());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao listar os grupos do utilizador.", e);
        } finally {
            entityManager.close();
        }
    }

    @Override
    public Group findByIdWithMembers(Long id) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            return entityManager.createQuery(
                            "SELECT DISTINCT g FROM Group g LEFT JOIN FETCH g.createdBy "
                                    + "LEFT JOIN FETCH g.members m LEFT JOIN FETCH m.user "
                                    + "WHERE g.groupId = :id",
                            Group.class
                    ).setParameter("id", id).getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            entityManager.close();
        }
    }

    @Override
    public void updateGroup(Group group) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            entityManager.merge(group);
            transaction.commit();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Erro ao atualizar o grupo.", e);
        } finally {
            entityManager.close();
        }
    }

    @Override
    public void deleteGroup(Group group) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            Group groupToDelete = entityManager.find(Group.class, group.getGroupId());
            if (groupToDelete == null) {
                throw new RuntimeException("Grupo não encontrado.");
            }
            entityManager.remove(groupToDelete);
            transaction.commit();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Erro ao eliminar o grupo.", e);
        } finally {
            entityManager.close();
        }
    }
}
