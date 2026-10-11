package dao;

import dao.interfaces.GroupDaoInterface;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import model.Group;
import model.User;

import java.util.ArrayList;

public class GroupDao implements GroupDaoInterface {


    private final EntityManagerFactory entityManagerFactory;

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
        EntityManager em =  entityManagerFactory.createEntityManager();
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

            throw new RuntimeException(
                    "Erro ao guardar o grupo.",
                    e
            );
        }
    }

    @Override
    public ArrayList<Group> findAll() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();

        try {

            return new ArrayList<>(
                    entityManager
                            .createQuery(
                                    "SELECT g FROM Group g",
                                    Group.class
                            )
                            .getResultList()
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Erro ao listar os grupos.",
                    e
            );
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

            throw new RuntimeException(
                    "Erro ao atualizar o grupo.",
                    e
            );
        }
    }
    @Override
    public void deleteGroup(Group group) {

        // Regra de Negócio Proteção: Verificar se o grupo tem membros ou ciclos ativos antes de eliminar
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

            Group groupToDelete =
                    entityManager.find(
                            Group.class,
                            group.getGroupId()
                    );

            if (groupToDelete == null) {
                throw new RuntimeException(
                        "Grupo não encontrado."
                );
            }

            entityManager.remove(groupToDelete);

            transaction.commit();

        } catch (Exception e) {

            if (transaction.isActive()) {
                transaction.rollback();
            }

            throw new RuntimeException(
                    "Erro ao eliminar o grupo.",
                    e
            );
        }
    }
}
