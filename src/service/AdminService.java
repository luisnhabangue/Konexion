package service;

import dao.UserDao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import model.AdminGroupSummary;
import model.AdminLateMemberSummary;
import model.AdminUserSummary;
import model.Cycle;
import model.Group;
import model.PlatformSettings;
import model.User;
import model.UserReport;
import model.enums.ContributionStatus;
import model.enums.CycleStatus;
import util.EntityManagerFactoryUtil;
import util.PasswordUtil;

import java.math.BigDecimal;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Operações administrativas globais, sempre limitadas a utilizadores com perfil de administrador.
 */
public class AdminService {
    public record MonthlyTotal(YearMonth month, BigDecimal amount) {}
    public record GlobalSettings(int latePenaltyPoints, int reminderDaysBefore,
                                 boolean remindersEnabled, boolean overdueNotificationsEnabled,
                                 boolean latePaymentNotificationsEnabled) {}

    public record Metrics(long users, long activeUsers, long verifiedUsers, long groups,
                          long publicGroups, long paidContributions, BigDecimal contributed,
                          long openReports, long pendingKyc, BigDecimal pendingAmount,
                          BigDecimal overdueAmount) {}

    private final EntityManagerFactory factory = EntityManagerFactoryUtil.getEntityManagerFactory();
    private final UserDao userDao = new UserDao(factory);

    public Metrics getMetrics(Long administratorId) {
        requireAdministrator(administratorId);
        EntityManager em = factory.createEntityManager();
        try {
            long users = em.createQuery("SELECT COUNT(u) FROM User u", Long.class).getSingleResult();
            long activeUsers = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.active = true",
                    Long.class).getSingleResult();
            long verifiedUsers = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.verified = true",
                    Long.class).getSingleResult();
            long groups = em.createQuery("SELECT COUNT(g) FROM Group g WHERE g.archived = false",
                    Long.class).getSingleResult();
            long publicGroups = em.createQuery(
                    "SELECT COUNT(g) FROM Group g WHERE g.privateGroup = false AND g.archived = false",
                    Long.class).getSingleResult();
            long pendingKyc = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.kycStatus = 'PENDING'",
                    Long.class).getSingleResult();
            long paidCount = em.createQuery(
                            "SELECT COUNT(c) FROM Contribution c WHERE c.status = :status", Long.class)
                    .setParameter("status", ContributionStatus.PAID).getSingleResult();
            BigDecimal contributed = contributionTotal(em, ContributionStatus.PAID);
            BigDecimal pending = contributionTotal(em, ContributionStatus.PENDING);
            BigDecimal overdue = contributionTotal(em, ContributionStatus.OVERDUE);
            long reports = em.createQuery(
                    "SELECT COUNT(r) FROM UserReport r WHERE r.status = 'OPEN'", Long.class).getSingleResult();
            return new Metrics(users, activeUsers, verifiedUsers, groups, publicGroups, paidCount,
                    contributed, reports, pendingKyc, pending, overdue);
        } finally {
            em.close();
        }
    }

    public GlobalSettings getGlobalSettings(Long administratorId) {
            requireAdministrator(administratorId);
            EntityManager em = factory.createEntityManager();
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();
                PlatformSettings settings = em.find(PlatformSettings.class, 1);
                if (settings == null) {
                    settings = new PlatformSettings();
                    em.persist(settings);
                }
                GlobalSettings result = globalSettings(settings);
                transaction.commit();
                return result;
            } catch (RuntimeException exception) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw exception;
            } finally {
                em.close();
            }
        }

    public void updateGlobalSettings(Long administratorId, GlobalSettings settings) {
            requireAdministrator(administratorId);
            if (settings == null || settings.latePenaltyPoints() < 0 || settings.latePenaltyPoints() > 100
                    || settings.reminderDaysBefore() < 0 || settings.reminderDaysBefore() > 30) {
                throw new IllegalArgumentException(
                        "A penalização deve estar entre 0 e 100 pontos e o lembrete entre 0 e 30 dias.");
            }
            EntityManager em = factory.createEntityManager();
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();
                PlatformSettings entity = em.find(PlatformSettings.class, 1);
                if (entity == null) {
                    entity = new PlatformSettings();
                    em.persist(entity);
                }
                entity.setLatePenaltyPoints(settings.latePenaltyPoints());
                entity.setReminderDaysBefore(settings.reminderDaysBefore());
                entity.setRemindersEnabled(settings.remindersEnabled());
                entity.setOverdueNotificationsEnabled(settings.overdueNotificationsEnabled());
                entity.setLatePaymentNotificationsEnabled(settings.latePaymentNotificationsEnabled());
                transaction.commit();
            } catch (RuntimeException exception) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw exception;
            } finally {
                em.close();
            }
        }

    private GlobalSettings globalSettings(PlatformSettings settings) {
        return new GlobalSettings(settings.getLatePenaltyPoints(), settings.getReminderDaysBefore(),
                settings.isRemindersEnabled(), settings.isOverdueNotificationsEnabled(),
                settings.isLatePaymentNotificationsEnabled());
    }

    private BigDecimal contributionTotal(EntityManager em, ContributionStatus status) {
        BigDecimal result = em.createQuery(
                        "SELECT COALESCE(SUM(c.amount), 0) FROM Contribution c WHERE c.status = :status",
                        BigDecimal.class)
                .setParameter("status", status).getSingleResult();
        return result == null ? BigDecimal.ZERO : result;
    }

    public List<User> getPendingKyc(Long administratorId) {
        requireAdministrator(administratorId);
        EntityManager em = factory.createEntityManager();
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.kycStatus = 'PENDING' ORDER BY u.id",
                    User.class).getResultList();
        } finally {
            em.close();
        }
    }

    public List<AdminUserSummary> getUsers(Long administratorId) {
        requireAdministrator(administratorId);
        EntityManager em = factory.createEntityManager();
        try {
            return em.createQuery("SELECT new model.AdminUserSummary(u.id, u.firstName, u.lastName, " +
                            "u.username, u.userRole, u.active, u.verified, u.strikeCount, u.punctualityPoints) " +
                            "FROM User u ORDER BY u.id", AdminUserSummary.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<AdminGroupSummary> getGroups(Long administratorId) {
        requireAdministrator(administratorId);
        EntityManager em = factory.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT new model.AdminGroupSummary(g.groupId, g.groupName, " +
                                    "CONCAT(CONCAT(g.createdBy.firstName, ' '), g.createdBy.lastName), g.groupBalance, " +
                                    "g.amountPerUser, (SELECT COUNT(m) FROM UserGroup m WHERE m.group = g), " +
                                    "(SELECT COALESCE(MAX(c.numero), 0) FROM Cycle c WHERE c.group = g), " +
                                    "(SELECT COUNT(c) FROM Cycle c WHERE c.group = g AND c.status = :active), " +
                                    "g.archived) FROM Group g ORDER BY g.archived, g.groupName",
                            AdminGroupSummary.class)
                    .setParameter("active", CycleStatus.IN_PROGRESS)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<AdminLateMemberSummary> getLateMembers(Long administratorId) {
        requireAdministrator(administratorId);
        EntityManager em = factory.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT new model.AdminLateMemberSummary(u.id, " +
                                    "CONCAT(CONCAT(u.firstName, ' '), u.lastName), u.username, " +
                                    "SUM(CASE WHEN c.status = :status THEN 1 ELSE 0 END), COUNT(c), " +
                                    "COALESCE(SUM(CASE WHEN c.status = :status THEN c.amount ELSE 0 END), 0)) " +
                                    "FROM Contribution c JOIN c.userGroup.user u " +
                                    "GROUP BY u.id, u.firstName, u.lastName, u.username " +
                                    "HAVING SUM(CASE WHEN c.status = :status THEN 1 ELSE 0 END) > 0",
                            AdminLateMemberSummary.class)
                    .setParameter("status", ContributionStatus.OVERDUE)
                    .getResultList().stream()
                    .sorted(java.util.Comparator.comparingDouble(AdminLateMemberSummary::overdueRate)
                            .reversed())
                    .limit(10)
                    .toList();
        } finally {
            em.close();
        }
    }

    public List<MonthlyTotal> getFinancialTrend(Long administratorId) {
        requireAdministrator(administratorId);
        EntityManager em = factory.createEntityManager();
        try {
            YearMonth firstMonth = YearMonth.now().minusMonths(5);
            List<Object[]> rows = em.createQuery(
                            "SELECT c.paymentDate, SUM(c.amount) FROM Contribution c " +
                                    "WHERE c.status = :status AND c.paymentDate >= :from " +
                                    "GROUP BY c.paymentDate ORDER BY c.paymentDate",
                            Object[].class)
                    .setParameter("status", ContributionStatus.PAID)
                    .setParameter("from", firstMonth.atDay(1))
                    .getResultList();
            Map<YearMonth, BigDecimal> totals = new TreeMap<>();
            for (int month = 0; month < 6; month++) {
                totals.put(firstMonth.plusMonths(month), BigDecimal.ZERO);
            }
            for (Object[] row : rows) {
                YearMonth month = YearMonth.from((LocalDate) row[0]);
                totals.merge(month, (BigDecimal) row[1], BigDecimal::add);
            }
            return totals.entrySet().stream()
                    .map(entry -> new MonthlyTotal(entry.getKey(), entry.getValue()))
                    .toList();
        } finally {
            em.close();
        }
    }

    public String getGroupDetails(Long administratorId, Long groupId) {
        requireAdministrator(administratorId);
        EntityManager em = factory.createEntityManager();
        try {
            Group group = em.createQuery(
                            "SELECT DISTINCT g FROM Group g LEFT JOIN FETCH g.createdBy " +
                                    "LEFT JOIN FETCH g.members m LEFT JOIN FETCH m.user WHERE g.groupId = :id",
                            Group.class)
                    .setParameter("id", groupId).getResultStream().findFirst().orElse(null);
            if (group == null) {
                throw new IllegalArgumentException("Grupo não encontrado.");
            }
            StringBuilder details = new StringBuilder()
                    .append(group.getGroupName()).append(" · responsável: ")
                    .append(group.getCreatedBy().getFirstName()).append(" ")
                    .append(group.getCreatedBy().getLastName()).append("\n")
                    .append("Estado: ").append(group.isArchived() ? "Arquivado" : "Activo")
                    .append(" · Saldo: ").append(group.getGroupBalance()).append(" MT\n")
                    .append("Contribuição: ").append(group.getAmountPerUser()).append(" MT\n\nMembros:\n");
            group.getMembers().forEach(member -> details.append("• ")
                    .append(member.getUser().getFirstName()).append(" ")
                    .append(member.getUser().getLastName()).append(" (@")
                    .append(member.getUser().getUsername()).append(")\n"));

            List<Cycle> cycles = em.createQuery(
                            "SELECT DISTINCT c FROM Cycle c LEFT JOIN FETCH c.contributions p " +
                                    "LEFT JOIN FETCH p.userGroup ug LEFT JOIN FETCH ug.user " +
                                    "WHERE c.group.groupId = :id ORDER BY c.numero DESC",
                            Cycle.class)
                    .setParameter("id", groupId)
                    .getResultList();
            details.append("\nCiclos:\n");
            for (Cycle cycle : cycles) {
                details.append("Ciclo ").append(cycle.getNumero()).append(" · ")
                        .append(cycle.getStatus()).append(" · início ")
                        .append(cycle.getStartDate()).append('\n');
                cycle.getContributions().forEach(contribution -> details.append("  • ")
                        .append(contribution.getUserGroup().getUser().getFirstName()).append(" ")
                        .append(contribution.getUserGroup().getUser().getLastName()).append(" — ")
                        .append(contribution.getAmount()).append(" MT · ")
                        .append(contribution.getStatus()).append('\n'));
            }
            return details.toString();
        } finally {
            em.close();
        }
    }

    public String getUserProfile(Long administratorId, Long userId) {
        requireAdministrator(administratorId);
        EntityManager em = factory.createEntityManager();
        try {
            User user = em.createQuery(
                            "SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.userGroups ug " +
                                    "LEFT JOIN FETCH ug.group WHERE u.id = :id", User.class)
                    .setParameter("id", userId).getResultStream().findFirst().orElse(null);
            if (user == null) {
                throw new IllegalArgumentException("Utilizador não encontrado.");
            }
            StringBuilder profile = new StringBuilder()
                    .append(user.getFirstName()).append(" ").append(user.getLastName())
                    .append(" (@").append(user.getUsername()).append(")\n")
                    .append("Perfil: ").append(user.getUserRole() != null
                            && user.getUserRole().isAdministrator() ? "Administrador" : "Membro").append("\n")
                    .append("Estado: ").append(user.isActive() ? "Activo" : "Inactivo").append("\n")
                    .append("Verificação: ").append(user.isVerified() ? "Verificado" : "Pendente").append("\n")
                    .append("E-mail: ").append(user.getEmail() == null ? "—" : user.getEmail()).append("\n")
                    .append("Telefone: ").append(user.getPhoneNumber() == null ? "—" : user.getPhoneNumber())
                    .append("\n\nGrupos onde participa:\n");
            if (user.getUserGroups().isEmpty()) {
                profile.append("Sem grupos.");
            } else {
                user.getUserGroups().forEach(membership -> profile
                        .append("• ").append(membership.getGroup().getGroupName()).append('\n'));
            }
            return profile.toString();
        } finally {
            em.close();
        }
    }

    public String resetUserPassword(Long administratorId, Long userId) {
        requireAdministrator(administratorId);
        if (administratorId.equals(userId)) {
            throw new IllegalArgumentException("Não pode redefinir a própria palavra-passe por este suporte.");
        }
        EntityManager em = factory.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            User user = em.find(User.class, userId);
            if (user == null) {
                throw new IllegalArgumentException("Utilizador não encontrado.");
            }
            if (user.getUserRole() != null && user.getUserRole().isAdministrator()) {
                throw new IllegalArgumentException(
                        "Não é permitido redefinir a palavra-passe de outro administrador.");
            }
            String temporaryPassword = createTemporaryPassword();
            user.setPassword(PasswordUtil.createHashPassword(temporaryPassword));
            transaction.commit();
            return temporaryPassword;
        } catch (RuntimeException exception) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw exception;
        } finally {
            em.close();
        }
    }

    private static String createTemporaryPassword() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
        SecureRandom random = new SecureRandom();
        List<Character> characters = new ArrayList<>(List.of('A', 'a', '7', '!'));
        for (int i = 0; i < 12; i++) {
            characters.add(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        Collections.shuffle(characters, random);
        StringBuilder password = new StringBuilder();
        characters.forEach(password::append);
        return password.toString();
    }

    public void archiveGroup(Long administratorId, Long groupId) {
        requireAdministrator(administratorId);
        EntityManager em = factory.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            Group group = em.find(Group.class, groupId);
            if (group == null) {
                throw new IllegalArgumentException("Grupo não encontrado.");
            }
            if (group.isArchived()) {
                throw new IllegalStateException("Este grupo já está arquivado.");
            }
            Long activeCycles = em.createQuery(
                            "SELECT COUNT(c) FROM Cycle c WHERE c.group.groupId = :id AND c.status = :status",
                            Long.class)
                    .setParameter("id", groupId).setParameter("status", CycleStatus.IN_PROGRESS)
                    .getSingleResult();
            if (activeCycles > 0) {
                throw new IllegalStateException("Não é possível arquivar um grupo com um ciclo em curso.");
            }
            group.setArchived(true);
            transaction.commit();
        } catch (RuntimeException exception) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw exception;
        } finally {
            em.close();
        }
    }

    public String getFinancialCsv(Long administratorId, LocalDate from, LocalDate to) {
        requireAdministrator(administratorId);
        if (from == null || to == null || from.isAfter(to)) {
            throw new IllegalArgumentException("Seleccione um período válido.");
        }
        EntityManager em = factory.createEntityManager();
        try {
            List<Object[]> rows = em.createQuery(
                            "SELECT c.paymentDate, c.userGroup.group.groupName, " +
                                    "c.userGroup.user.username, c.amount, c.status " +
                                    "FROM Contribution c WHERE c.paymentDate BETWEEN :from AND :to " +
                                    "ORDER BY c.paymentDate, c.userGroup.group.groupName",
                            Object[].class)
                    .setParameter("from", from).setParameter("to", to).getResultList();
            StringBuilder csv = new StringBuilder("data,grupo,utilizador,valor_mt,estado\n");
            for (Object[] row : rows) {
                csv.append(csvCell(row[0])).append(',')
                        .append(csvCell(row[1])).append(',')
                        .append(csvCell(row[2])).append(',')
                        .append(csvCell(row[3])).append(',')
                        .append(csvCell(row[4])).append('\n');
            }
            return csv.toString();
        } finally {
            em.close();
        }
    }

    public byte[] getFinancialPdf(Long administratorId, LocalDate from, LocalDate to) {
        String[] csvRows = getFinancialCsv(administratorId, from, to).split("\\R");
        List<String> lines = new ArrayList<>();
        lines.add("Relatorio financeiro consolidado");
        lines.add("Periodo: " + from + " ate " + to);
        lines.add("");
        for (String csvRow : csvRows) {
            String normalized = Normalizer.normalize(csvRow, Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .replaceAll("[^\\x20-\\x7E]", "?");
            for (int offset = 0; offset < normalized.length(); offset += 88) {
                lines.add(normalized.substring(offset, Math.min(offset + 88, normalized.length())));
            }
        }
        int linesPerPage = 52;
        int pageCount = Math.max(1, (lines.size() + linesPerPage - 1) / linesPerPage);
        int[] pageObjectIds = new int[pageCount];
        int[] contentObjectIds = new int[pageCount];
        for (int i = 0; i < pageCount; i++) {
            pageObjectIds[i] = 4 + i * 2;
            contentObjectIds[i] = pageObjectIds[i] + 1;
        }

        List<byte[]> objects = new ArrayList<>();
        objects.add(pdfObject("<< /Type /Catalog /Pages 2 0 R >>"));
        StringBuilder kids = new StringBuilder("<< /Type /Pages /Kids [");
        for (int id : pageObjectIds) {
            kids.append(id).append(" 0 R ");
        }
        kids.append("] /Count ").append(pageCount).append(" >>");
        objects.add(pdfObject(kids.toString()));
        objects.add(pdfObject("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"));

        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            int start = pageIndex * linesPerPage;
            int end = Math.min(lines.size(), start + linesPerPage);
            StringBuilder content = new StringBuilder("BT /F1 9 Tf 36 806 Td 12 TL\n");
            for (int lineIndex = start; lineIndex < end; lineIndex++) {
                String line = lines.get(lineIndex).replace("\\", "\\\\")
                        .replace("(", "\\(").replace(")", "\\)");
                content.append('(').append(line).append(") Tj T*\n");
            }
            content.append("ET\n");
            byte[] stream = content.toString().getBytes(StandardCharsets.ISO_8859_1);
            objects.add(pdfObject("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] " +
                    "/Resources << /Font << /F1 3 0 R >> >> /Contents "
                    + contentObjectIds[pageIndex] + " 0 R >>"));
            objects.add(pdfStreamObject(stream));
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writePdf(output, "%PDF-1.4\n");
        int[] offsets = new int[objects.size() + 1];
        for (int index = 0; index < objects.size(); index++) {
            offsets[index + 1] = output.size();
            writePdf(output, (index + 1) + " 0 obj\n");
            output.writeBytes(objects.get(index));
            writePdf(output, "\nendobj\n");
        }
        int xrefOffset = output.size();
        writePdf(output, "xref\n0 " + offsets.length + "\n0000000000 65535 f \n");
        for (int index = 1; index < offsets.length; index++) {
            writePdf(output, String.format(java.util.Locale.ROOT, "%010d 00000 n \n", offsets[index]));
        }
        writePdf(output, "trailer\n<< /Size " + offsets.length + " /Root 1 0 R >>\nstartxref\n"
                + xrefOffset + "\n%%EOF");
        return output.toByteArray();
    }

    private static byte[] pdfObject(String content) {
        return content.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static byte[] pdfStreamObject(byte[] content) {
        ByteArrayOutputStream object = new ByteArrayOutputStream();
        writePdf(object, "<< /Length " + content.length + " >>\nstream\n");
        object.writeBytes(content);
        writePdf(object, "endstream");
        return object.toByteArray();
    }

    private static void writePdf(ByteArrayOutputStream output, String value) {
        output.writeBytes(value.getBytes(StandardCharsets.ISO_8859_1));
    }

    private static String csvCell(Object value) {
        String text = value == null ? "" : value.toString();
        if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    public List<UserReport> getOpenReports(Long administratorId) {
        requireAdministrator(administratorId);
        EntityManager em = factory.createEntityManager();
        try {
            return em.createQuery(
                            "SELECT r FROM UserReport r JOIN FETCH r.reporter JOIN FETCH r.reportedUser " +
                                    "JOIN FETCH r.group WHERE r.status = 'OPEN' ORDER BY r.createdAt DESC",
                            UserReport.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public void resolveKyc(Long administratorId, Long userId, boolean approved) {
        requireAdministrator(administratorId);
        new AuthService().reviewKyc(administratorId, userId, approved);
    }

    public void strikeUser(Long administratorId, Long userId) {
        requireAdministrator(administratorId);
        updateUser(userId, user -> user.setStrikeCount(user.getStrikeCount() + 1));
    }

    public void disableUser(Long administratorId, Long userId) {
        requireAdministrator(administratorId);
        if (administratorId.equals(userId)) {
            throw new IllegalArgumentException("Não pode desativar a própria conta de administrador.");
        }
        updateUser(userId, user -> user.setActive(false));
    }

    public void reactivateUser(Long administratorId, Long userId) {
        requireAdministrator(administratorId);
        if (administratorId.equals(userId)) {
            throw new IllegalArgumentException("Não pode alterar o estado da própria conta de administrador.");
        }
        updateUser(userId, user -> user.setActive(true));
    }

    public void deleteUser(Long administratorId, Long userId) {
        requireAdministrator(administratorId);
        if (administratorId.equals(userId)) {
            throw new IllegalArgumentException("Não pode apagar a própria conta de administrador.");
        }
        EntityManager em = factory.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            User user = em.find(User.class, userId);
            if (user == null) {
                throw new IllegalArgumentException("Utilizador não encontrado.");
            }
            if (user.getUserRole() != null && user.getUserRole().isAdministrator()) {
                throw new IllegalArgumentException("Não é permitido apagar contas de administrador.");
            }
            long memberships = em.createQuery(
                            "SELECT COUNT(ug) FROM UserGroup ug WHERE ug.user.id = :userId", Long.class)
                    .setParameter("userId", userId).getSingleResult();
            long ownedGroups = em.createQuery(
                            "SELECT COUNT(g) FROM Group g WHERE g.createdBy.id = :userId", Long.class)
                    .setParameter("userId", userId).getSingleResult();
            if (memberships > 0 || ownedGroups > 0) {
                throw new IllegalStateException(
                        "Este utilizador está associado a grupos. Desative a conta para preservar os grupos e o histórico financeiro.");
            }
            em.createQuery("DELETE FROM UserReport r WHERE r.reporter.id = :userId OR r.reportedUser.id = :userId")
                    .setParameter("userId", userId).executeUpdate();
            em.createQuery("DELETE FROM MembershipInvitation i WHERE i.inviter.id = :userId OR i.invitee.id = :userId")
                    .setParameter("userId", userId).executeUpdate();
            em.createQuery("DELETE FROM LeadershipRequest r WHERE r.requester.id = :userId")
                    .setParameter("userId", userId).executeUpdate();
            em.remove(user);
            transaction.commit();
        } catch (RuntimeException exception) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw exception;
        } finally {
            em.close();
        }
    }

    public void resolveReport(Long administratorId, Long reportId) {
        requireAdministrator(administratorId);
        EntityManager em = factory.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            UserReport report = em.find(UserReport.class, reportId);
            if (report == null || !"OPEN".equals(report.getStatus())) {
                throw new IllegalArgumentException("A denúncia já foi resolvida ou não existe.");
            }
            report.setStatus("RESOLVED");
            transaction.commit();
        } catch (RuntimeException exception) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw exception;
        } finally {
            em.close();
        }
    }

    private void updateUser(Long userId, java.util.function.Consumer<User> update) {
        User user = userDao.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Utilizador não encontrado.");
        }
        update.accept(user);
        userDao.updateUser(user);
    }

    private void requireAdministrator(Long administratorId) {
        User administrator = userDao.findById(administratorId);
        if (administrator == null || administrator.getUserRole() == null
                || !administrator.getUserRole().isAdministrator()) {
            throw new SecurityException("Esta ação está disponível apenas para administradores.");
        }
    }
}
