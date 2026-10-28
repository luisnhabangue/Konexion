package service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import exceptions.UserNotFoundException;
import model.Contribution;
import model.Cycle;
import model.Group;
import model.MembershipInvitation;
import model.User;
import model.UserGroup;
import model.UserNotification;
import model.UserReport;
import model.enums.ContributionStatus;
import model.enums.CycleStatus;
import model.enums.Gender;
import model.enums.PaymentMethod;
import model.enums.ReceivingOrderMode;
import model.enums.UserRole;
import org.mindrot.jbcrypt.BCrypt;
import util.EntityManagerFactoryUtil;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Seeds a repeat-safe, explicitly named demo dataset into the configured database.
 * All accounts use the demo-only password "Lulerk@12" and non-deliverable .test email addresses.
 */
public final class DemoDataSeeder {
    private static final String DEMO_PASSWORD = "Lulerk@12";
    private static final String DEMO_PASSWORD_HASH = BCrypt.hashpw(DEMO_PASSWORD, BCrypt.gensalt(12));

    private DemoDataSeeder() {
    }

    public static void main(String[] args) {
        if (args.length > 0 && "--verify".equals(args[0])) {
            verifySeededData();
        } else {
            seed();
        }
    }

    public static void seed() {
        EntityManager em = EntityManagerFactoryUtil.getEntityManagerFactory().createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            Long existingDemoAdmin = em.createQuery(
                            "SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class)
                    .setParameter("username", "demo.admin")
                    .getSingleResult();
            if (existingDemoAdmin > 0) {
                throw new IllegalStateException("Os dados de demonstração já foram carregados; nenhuma alteração foi feita.");
            }

            transaction.begin();
            Map<String, User> users = createUsers(em);
            em.flush();
            createActiveGroup(em, users);
            createPublicOpenGroup(em, users);
            createPendingInvitationsGroup(em, users);
            createConcludedGroup(em, users);
            createReports(em, users);
            transaction.commit();
            System.out.println("Dados de demonstração carregados. Contas: " + users.size()
                    + " | grupos: 4 | palavra-passe: " + DEMO_PASSWORD);
            System.out.println("Inicie sessão como demo.admin para abrir o painel administrativo.");
        } catch (RuntimeException exception) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw exception;
        } finally {
            em.close();
        }
    }

    public static void verifySeededData() {
        EntityManager em = EntityManagerFactoryUtil.getEntityManagerFactory().createEntityManager();
        try {
            long accounts = em.createQuery(
                            "SELECT COUNT(u) FROM User u WHERE u.email LIKE :pattern", Long.class)
                    .setParameter("pattern", "demo.%@ixitique.example.test").getSingleResult();
            long groups = em.createQuery(
                            "SELECT COUNT(g) FROM Group g WHERE g.groupName LIKE :pattern", Long.class)
                    .setParameter("pattern", "DEMO - %").getSingleResult();
            long reports = em.createQuery(
                            "SELECT COUNT(r) FROM UserReport r JOIN r.reporter u WHERE u.email LIKE :pattern",
                            Long.class)
                    .setParameter("pattern", "demo.%@ixitique.example.test").getSingleResult();
            long notifications = em.createQuery(
                            "SELECT COUNT(n) FROM UserNotification n JOIN n.recipient u WHERE u.email LIKE :pattern",
                            Long.class)
                    .setParameter("pattern", "demo.%@ixitique.example.test").getSingleResult();
            long invitations = em.createQuery(
                            "SELECT COUNT(i) FROM MembershipInvitation i JOIN i.inviter u WHERE u.email LIKE :pattern",
                            Long.class)
                    .setParameter("pattern", "demo.%@ixitique.example.test").getSingleResult();
            long pendingInvitations = em.createQuery(
                            "SELECT COUNT(i) FROM MembershipInvitation i WHERE i.status = 'PENDING' " +
                                    "AND i.group.groupName LIKE :pattern", Long.class)
                    .setParameter("pattern", "DEMO - %").getSingleResult();
            long publicGroups = em.createQuery(
                            "SELECT COUNT(g) FROM Group g WHERE g.groupName LIKE :pattern AND g.privateGroup = false",
                            Long.class)
                    .setParameter("pattern", "DEMO - %").getSingleResult();
            long inactiveUsers = em.createQuery(
                            "SELECT COUNT(u) FROM User u WHERE u.email LIKE :pattern AND u.active = false",
                            Long.class)
                    .setParameter("pattern", "demo.%@ixitique.example.test").getSingleResult();
            long rejectedVerifications = em.createQuery(
                            "SELECT COUNT(u) FROM User u WHERE u.email LIKE :pattern AND u.kycStatus = 'REJECTED'",
                            Long.class)
                    .setParameter("pattern", "demo.%@ixitique.example.test").getSingleResult();
            long usersWithStrikes = em.createQuery(
                            "SELECT COUNT(u) FROM User u WHERE u.email LIKE :pattern AND u.strikeCount > 0",
                            Long.class)
                    .setParameter("pattern", "demo.%@ixitique.example.test").getSingleResult();
            long contributions = em.createQuery(
                            "SELECT COUNT(c) FROM Contribution c JOIN c.userGroup.user u " +
                                    "WHERE u.email LIKE :pattern", Long.class)
                    .setParameter("pattern", "demo.%@ixitique.example.test").getSingleResult();
            User administrator = em.createQuery(
                            "SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", "demo.admin").getSingleResult();
            boolean validPassword = BCrypt.checkpw(DEMO_PASSWORD, administrator.getPassword())
                    && new AuthService().login("demo.admin", DEMO_PASSWORD)
                    .getUserRole().isAdministrator();
            User pendingKycUser = em.createQuery(
                            "SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", "demo.sara").getSingleResult();
            boolean validKycImages = pendingKycUser.isKycPending()
                    && pendingKycUser.getIdentityDocumentImage() != null
                    && pendingKycUser.getIdentitySelfieImage() != null;
            boolean disabledLoginBlocked = false;
            try {
                new AuthService().login("demo.paulo", DEMO_PASSWORD);
            } catch (UserNotFoundException expected) {
                disabledLoginBlocked = true;
            }
            if (accounts != 11 || groups != 4 || reports < 3 || notifications < 3
                    || invitations < 3 || pendingInvitations < 2 || publicGroups < 2
                    || inactiveUsers < 1 || rejectedVerifications < 1 || usersWithStrikes < 1
                    || contributions < 7 || !validPassword || !validKycImages || !disabledLoginBlocked) {
                throw new IllegalStateException("A validação dos dados de demonstração falhou.");
            }
            System.out.println("Verificação concluída: " + accounts + " contas, " + groups + " grupos, "
                    + reports + " denúncias, " + notifications + " notificações, " + contributions
                    + " contribuições e " + invitations + " convites. Confirmados: KYC recusado, conta "
                    + "desativada (login bloqueado), KYC pendente com imagens e strikes; credencial "
                    + "demo.admin válida.");
        } finally {
            em.close();
        }
    }

    private static Map<String, User> createUsers(EntityManager em) {
        Map<String, User> users = new LinkedHashMap<>();
        addUser(em, users, "admin", "Celso", "Matavele", Gender.M, UserRole.ADMINISTRATOR,
                true, true, "VERIFIED", 0, 12);
        addUser(em, users, "amelia", "Amélia", "Cossa", Gender.F, UserRole.USER,
                true, true, "VERIFIED", 0, 8);
        addUser(em, users, "ernesto", "Ernesto", "Mabunda", Gender.M, UserRole.USER,
                true, true, "VERIFIED", 0, 5);
        addUser(em, users, "lidia", "Lídia", "Nhantumbo", Gender.F, UserRole.USER,
                true, true, "VERIFIED", 0, 10);
        addUser(em, users, "joaquim", "Joaquim", "Mucavele", Gender.M, UserRole.USER,
                true, true, "VERIFIED", 1, 2);
        addUser(em, users, "sara", "Sara", "Macuácua", Gender.F, UserRole.USER,
                true, false, "PENDING", 0, 0);
        addUser(em, users, "paulo", "Paulo", "Bila", Gender.M, UserRole.USER,
                false, true, "VERIFIED", 3, -5);
        addUser(em, users, "rosa", "Rosa", "Chongo", Gender.F, UserRole.USER,
                true, false, "REJECTED", 2, -2);
        addUser(em, users, "david", "David", "Sitoe", Gender.M, UserRole.USER,
                true, false, "NOT_SUBMITTED", 0, 0);
        addUser(em, users, "mateus", "Mateus", "Jamal", Gender.M, UserRole.USER,
                true, true, "VERIFIED", 4, -10);
        addUser(em, users, "helena", "Helena", "Matsinhe", Gender.F, UserRole.USER,
                true, true, "VERIFIED", 0, 7);
        addDemoKyc(users.get("sara"), "110000000000A");
        addDemoKyc(users.get("rosa"), "110000000001B");
        return users;
    }

    private static void addUser(EntityManager em, Map<String, User> users, String key,
                                String firstName, String lastName, Gender gender, UserRole role,
                                boolean active, boolean verified, String kycStatus,
                                int strikes, int punctualityPoints) {
        User user = new User(null, firstName, lastName, null, gender,
                "demo." + key, DEMO_PASSWORD_HASH,
                "demo." + key + "@ixitique.example.test", null);
        user.setUserRole(role);
        user.setActive(active);
        user.setVerified(verified);
        user.setKycStatus(kycStatus);
        user.setStrikeCount(strikes);
        user.setPunctualityPoints(punctualityPoints);
        em.persist(user);
        users.put(key, user);
    }

    private static void addDemoKyc(User user, String documentId) {
        user.setDocumentId(documentId);
        user.setIdentityDocumentImage(createDemoImage("DOCUMENTO FICTÍCIO", new Color(15, 82, 87)));
        user.setIdentitySelfieImage(createDemoImage("IMAGEM DE TESTE", new Color(22, 167, 161)));
    }

    private static byte[] createDemoImage(String title, Color background) {
        BufferedImage image = new BufferedImage(720, 480, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(background);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
            int textWidth = graphics.getFontMetrics().stringWidth(title);
            graphics.drawString(title, (image.getWidth() - textWidth) / 2, 215);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 22));
            String disclaimer = "DADOS DE DEMONSTRAÇÃO - SEM VALIDADE";
            textWidth = graphics.getFontMetrics().stringWidth(disclaimer);
            graphics.drawString(disclaimer, (image.getWidth() - textWidth) / 2, 270);
        } finally {
            graphics.dispose();
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível gerar as imagens fictícias de KYC.", exception);
        }
    }

    private static void createActiveGroup(EntityManager em, Map<String, User> users) {
        Group group = createGroup(users.get("amelia"), "DEMO - Xitique Activo",
                "Grupo de demonstração com contribuições pagas, pendentes e em atraso.",
                "500.00", 4, false, true);
        List<UserGroup> members = addMembers(em, group, users,
                List.of("amelia", "ernesto", "lidia", "mateus"));
        Cycle cycle = createCycle(em, group, members.get(0), 1,
                LocalDate.now().minusDays(2), CycleStatus.IN_PROGRESS);
        addContribution(em, members.get(0), cycle, "500.00", ContributionStatus.PAID,
                PaymentMethod.MPESA, LocalDate.now().minusDays(1));
        addContribution(em, members.get(1), cycle, "500.00", ContributionStatus.PENDING,
                PaymentMethod.EMOLA, null);
        addContribution(em, members.get(2), cycle, "500.00", ContributionStatus.OVERDUE,
                PaymentMethod.BIM, LocalDate.now().minusDays(10));
        addContribution(em, members.get(3), cycle, "500.00", ContributionStatus.PENDING,
                PaymentMethod.CASH, null);
        group.setGroupBalance(new BigDecimal("500.00"));
        addNotification(em, users.get("ernesto"), cycle, members.get(1),
                "MANUAL_PAYMENT_REMINDER", "O responsável do grupo DEMO - Xitique Activo lembrou-o de pagar a contribuição do ciclo 1.");
        addNotification(em, users.get("lidia"), cycle, members.get(2),
                "OVERDUE_MEMBER", "A contribuição do ciclo 1 está em atraso. Contacte o responsável do grupo.");
        addNotification(em, users.get("amelia"), cycle, members.get(2),
                "OVERDUE_LEADER", "Lídia Nhantumbo tem uma contribuição em atraso no ciclo 1.");
    }

    private static void createPublicOpenGroup(EntityManager em, Map<String, User> users) {
        Group group = createGroup(users.get("ernesto"), "DEMO - Grupo Público Aberto",
                "Grupo público demonstrativo disponível para receber novos participantes.",
                "250.00", 6, true, true);
        addMembers(em, group, users, List.of("ernesto", "lidia", "helena"));
    }

    private static void createPendingInvitationsGroup(EntityManager em, Map<String, User> users) {
        Group group = createGroup(users.get("lidia"), "DEMO - Convites Pendentes",
                "Grupo demonstrativo para testar convites e gestão de membros.",
                "300.00", 5, false, true);
        addMembers(em, group, users, List.of("lidia", "amelia"));
        addInvitation(em, group, users.get("lidia"), users.get("sara"), "PENDING");
        addInvitation(em, group, users.get("lidia"), users.get("david"), "PENDING");
        addInvitation(em, group, users.get("lidia"), users.get("rosa"), "DECLINED");
    }

    private static void createConcludedGroup(EntityManager em, Map<String, User> users) {
        Group group = createGroup(users.get("helena"), "DEMO - Ciclo Concluído",
                "Grupo demonstrativo com um ciclo já concluído e contribuições confirmadas.",
                "200.00", 3, true, true);
        List<UserGroup> members = addMembers(em, group, users,
                List.of("helena", "joaquim", "mateus"));
        Cycle cycle = createCycle(em, group, members.get(0), 1,
                LocalDate.now().minusDays(35), CycleStatus.CONCLUDED);
        for (UserGroup member : members) {
            addContribution(em, member, cycle, "200.00", ContributionStatus.PAID,
                    PaymentMethod.MPESA, LocalDate.now().minusDays(34));
        }
        members.get(0).setReceived(true);
    }

    private static Group createGroup(User owner, String name, String description, String quota,
                                     int capacity, boolean isPublic, boolean capacityConfigured) {
        Group group = new Group(null, name, description, new BigDecimal(quota), 25,
                LocalDate.now().minusDays(40), owner, 30);
        group.setMaxMembers(capacity);
        group.setCapacityConfigured(capacityConfigured);
        group.setReceivingOrderMode(ReceivingOrderMode.MANUAL);
        group.setPrivateGroup(!isPublic);
        return group;
    }

    private static List<UserGroup> addMembers(EntityManager em, Group group, Map<String, User> users,
                                               List<String> keys) {
        em.persist(group);
        em.flush();
        List<UserGroup> members = new java.util.ArrayList<>();
        int order = 1;
        for (String key : keys) {
            UserGroup member = new UserGroup(null, users.get(key), group, BigDecimal.ZERO, false, order++);
            group.addMember(member);
            em.persist(member);
            members.add(member);
        }
        em.flush();
        return members;
    }

    private static Cycle createCycle(EntityManager em, Group group, UserGroup beneficiary,
                                     int number, LocalDate startDate, CycleStatus status) {
        Cycle cycle = new Cycle(null, group, number, startDate, beneficiary);
        cycle.setStatus(status);
        cycle.setEndDate(startDate.plusDays(group.getFrequency()));
        cycle.setAutomaticPayoutEnabled(false);
        group.addCycle(cycle);
        em.persist(cycle);
        em.flush();
        return cycle;
    }

    private static void addContribution(EntityManager em, UserGroup member, Cycle cycle, String amount,
                                        ContributionStatus status, PaymentMethod method, LocalDate paymentDate) {
        em.persist(new Contribution(null, member, cycle, new BigDecimal(amount), paymentDate, method, status));
    }

    private static void addInvitation(EntityManager em, Group group, User inviter, User invitee, String status) {
        MembershipInvitation invitation = new MembershipInvitation();
        invitation.setGroup(group);
        invitation.setInviter(inviter);
        invitation.setInvitee(invitee);
        invitation.setStatus(status);
        em.persist(invitation);
    }

    private static void addNotification(EntityManager em, User recipient, Cycle cycle, UserGroup member,
                                        String type, String message) {
        UserNotification notification = new UserNotification();
        notification.setRecipient(recipient);
        notification.setCycle(cycle);
        notification.setUserGroup(member);
        notification.setType(type);
        notification.setMessage(message);
        em.persist(notification);
    }

    private static void createReports(EntityManager em, Map<String, User> users) {
        addReport(em, users.get("amelia"), users.get("mateus"),
                em.createQuery("SELECT g FROM Group g WHERE g.groupName = :name", Group.class)
                        .setParameter("name", "DEMO - Xitique Activo").getSingleResult(),
                "Atrasos repetidos nas contribuições e falta de comunicação com os membros.", "OPEN");
        addReport(em, users.get("helena"), users.get("joaquim"),
                em.createQuery("SELECT g FROM Group g WHERE g.groupName = :name", Group.class)
                        .setParameter("name", "DEMO - Ciclo Concluído").getSingleResult(),
                "Comportamento inadequado durante a organização do ciclo do grupo.", "OPEN");
        addReport(em, users.get("ernesto"), users.get("lidia"),
                em.createQuery("SELECT g FROM Group g WHERE g.groupName = :name", Group.class)
                        .setParameter("name", "DEMO - Grupo Público Aberto").getSingleResult(),
                "Denúncia demonstrativa já analisada pela equipa administrativa.", "RESOLVED");
    }

    private static void addReport(EntityManager em, User reporter, User reported, Group group,
                                  String reason, String status) {
        UserReport report = new UserReport();
        report.setReporter(reporter);
        report.setReportedUser(reported);
        report.setGroup(group);
        report.setReason(reason);
        report.setStatus(status);
        em.persist(report);
    }
}
