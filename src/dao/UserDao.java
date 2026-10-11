package dao;

import dao.interfaces.UserDaoInterface;
import exceptions.UserNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import model.User;

import java.util.List;

/**
 * Implementação JPA da interface {@link UserDaoInterface} para operações com a entidade {@link User}.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public class UserDao implements UserDaoInterface {

    private final EntityManagerFactory entityManagerFactory;

    /**
     * Construtor que recebe a fábrica de Entity Managers.
     * @param entityManagerFactory Instância de {@link EntityManagerFactory}
     */
    public UserDao(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void createUser(User user) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(user);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    @Override
    public User findById(Long id) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            return em.find(User.class, id);
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

    @Override
    public User findByUsername(String username) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            return em.createQuery(
                    "SELECT u FROM User u WHERE u.username = :username",
                    User.class
            ).setParameter("username", username).getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

    @Override
    public User findByEmail(String email) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            return em.createQuery(
                    "SELECT u FROM User u WHERE u.email = :phoneNumber",
                    User.class
            ).setParameter("phoneNumber", email).getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

    /**
     * Procura um utilizador pelo número de telefone.
     * @param phone Número de telefone
     * @return Utilizador encontrado ou {@code null}
     */
    public User findByPhone(String phone) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            return em.createQuery(
                    "SELECT u FROM User u WHERE u.phoneNumber = :phone",
                    User.class
            ).setParameter("phone", phone).getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

    @Override
    public void updateUser(User user) {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction et = em.getTransaction();
        try {
            et.begin();
            em.merge(user);
            et.commit();
        } catch (Exception e) {
            if (et.isActive()) {
                et.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    @Override
    public void deleteUser(Long id) {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction et = em.getTransaction();
        try {
            et.begin();
            User user = em.find(User.class, id);
            if (user == null) throw new UserNotFoundException("Usuário não encontrado!");
            em.remove(user);
            et.commit();
        } catch (Exception e) {
            if (et.isActive()) {
                et.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    @Override
    public void disableUser(Long id) {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction et = em.getTransaction();
        try {
            et.begin();
            User user = findById(id);
            if (user == null) throw new UserNotFoundException("Usuário não encontrado!");
            user.setActive(false);
            et.commit();
        } catch (Exception e) {
            if (et.isActive()) {
                et.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    @Override
    public List<User> listAllUsers() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        String hql = "FROM User";
        try {
            return entityManager.createQuery(hql, User.class).getResultList();
        } finally {
            entityManager.close();
        }
    }
}