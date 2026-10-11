package dao;

import dao.interfaces.GroupDaoInterface;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import model.Group;

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
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            return em.createQuery(
                    "SELECT g FROM Group g WHERE g.groupName = :name",
                    Group.class
            ).setParameter("name", name).getSingleResult();
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
                    entityManager.createQuery("SELECT g FROM Group g", Group.class).getResultList()
            );
        } catch (Exception e) {
            throw new RuntimeException("Erro ao listar os grupos.", e);
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
        if (group.getMembers() != null && !group.getMembers().isEmpty()) {
            throw new IllegalStateException("Não é possível remover um grupo que já possui membros vinculados.");
        }

        if (group.getCycles() != null && !group.getCycles().isEmpty()) {
            throw new IllegalStateException("Não é possível remover um grupo que já possui histórico de ciclos/rodadas.");
        }

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