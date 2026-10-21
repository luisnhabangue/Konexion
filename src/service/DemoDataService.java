package service;

import dao.GroupDao;
import dao.UserDao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import model.Contribution;
import model.Cycle;
import model.Group;
import model.User;
import model.UserGroup;
import model.enums.ContributionStatus;
import model.enums.CycleStatus;
import model.enums.Gender;
import model.enums.PaymentMethod;
import util.EntityManagerFactoryUtil;
import util.PasswordUtil;
import util.SessionContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class DemoDataService {
    private final GroupDao groupDao = new GroupDao(EntityManagerFactoryUtil.getEntityManagerFactory());
    private final UserDao userDao = new UserDao(EntityManagerFactoryUtil.getEntityManagerFactory());

    public String seedForCurrentUser() {
        User currentUser = SessionContext.getCurrentUser();
        if (!currentUser.isVerified()) {
            throw new IllegalStateException("Apenas contas verificadas podem criar grupos, incluindo grupos de demonstração.");
        }
        String groupName = "Demo Xitique " + currentUser.getUsername();
        if (groupDao.findByName(groupName) != null) {
            return "Os dados de demonstração já foram carregados para esta conta.";
        }

        List<User> demoUsers = List.of(createDemoUser(1), createDemoUser(2));
        EntityManager em = EntityManagerFactoryUtil.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            User creator = em.find(User.class, currentUser.getId());
            demoUsers.forEach(em::persist);

            LocalDate today = LocalDate.now();
            BigDecimal quota = new BigDecimal("500.00");
            Group group = new Group(null, groupName,
                    "Grupo fictício para demonstrar membros, ciclo e contribuições.",
                    quota, 25, today, creator, 30);
            em.persist(group);
            em.flush();

            UserGroup creatorMembership = createMembership(group, creator, 1);
            UserGroup firstDemoMembership = createMembership(group, demoUsers.get(0), 2);
            UserGroup secondDemoMembership = createMembership(group, demoUsers.get(1), 3);
            group.addMember(creatorMembership);
            group.addMember(firstDemoMembership);
            group.addMember(secondDemoMembership);
            em.persist(creatorMembership);
            em.persist(firstDemoMembership);
            em.persist(secondDemoMembership);
            em.flush();

            Cycle cycle = new Cycle(null, group, 1, today, firstDemoMembership);
            cycle.setStatus(CycleStatus.IN_PROGRESS);
            cycle.setEndDate(today.plusDays(group.getFrequency()));
            group.addCycle(cycle);
            em.persist(cycle);
            em.flush();

            creatorMembership.setBalance(quota);
            secondDemoMembership.setBalance(quota);
            em.persist(new Contribution(null, creatorMembership, cycle,
                    quota, today, PaymentMethod.MPESA, ContributionStatus.PAID));
            em.persist(new Contribution(null, secondDemoMembership, cycle,
                    quota, today, PaymentMethod.EMOLA, ContributionStatus.PAID));
            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new IllegalStateException("Não foi possível carregar os dados de demonstração.", e);
        } finally {
            em.close();
        }

        return "Foram criados um grupo de demonstração, dois membros fictícios, um ciclo e duas contribuições.";
    }

    private User createDemoUser(int number) {
        String token;
        String username;
        String email;
        do {
            token = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            username = "demo" + token;
            email = "demo-" + token + "@example.test";
        } while (userDao.findByUsername(username) != null || userDao.findByEmail(email) != null);

        String digits = String.format("%07d", (int) (Math.random() * 10_000_000));
        while (userDao.findByPhone("84" + digits) != null) {
            digits = String.format("%07d", (int) (Math.random() * 10_000_000));
        }
        return new User(null, "Membro", "Demonstração " + number,
                "DEMO-" + token.toUpperCase(), Gender.M,
                username, PasswordUtil.createHashPassword(UUID.randomUUID().toString()),
                email, "84" + digits);
    }

    private UserGroup createMembership(Group group, User user, int order) {
        return new UserGroup(null, user, group, BigDecimal.ZERO, false, order);
    }
}
