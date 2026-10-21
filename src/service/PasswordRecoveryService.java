package service;

import dao.UserDao;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import model.User;
import org.mindrot.jbcrypt.BCrypt;
import util.EntityManagerFactoryUtil;
import util.PasswordUtil;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/** Recuperação simples por código, válida apenas enquanto esta aplicação estiver aberta. */
public class PasswordRecoveryService {
    private record RecoveryCode(String code, Instant expiresAt) { }

    private static final Map<String, RecoveryCode> CODES = new ConcurrentHashMap<>();
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserDao userDao = new UserDao(EntityManagerFactoryUtil.getEntityManagerFactory());

    public void sendCode(String email) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Informe o email da conta.");
        String normalized = email.trim().toLowerCase();
        User user = userDao.findByEmail(normalized);
        if (user == null) throw new IllegalArgumentException("Não existe uma conta com esse email.");
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        sendEmail(normalized, code);
        CODES.put(normalized, new RecoveryCode(BCrypt.hashpw(code, BCrypt.gensalt()), Instant.now().plusSeconds(600)));
    }

    public void resetPassword(String email, String code, String newPassword) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        RecoveryCode saved = CODES.get(normalized);
        if (saved == null || Instant.now().isAfter(saved.expiresAt()) || code == null
                || !BCrypt.checkpw(code.trim(), saved.code())) {
            throw new IllegalArgumentException("Código inválido ou expirado. Peça um novo código.");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("A nova senha deve ter pelo menos 8 caracteres.");
        }
        User user = userDao.findByEmail(normalized);
        if (user == null) throw new IllegalArgumentException("Conta não encontrada.");
        user.setPassword(PasswordUtil.createHashPassword(newPassword));
        userDao.updateUser(user);
        CODES.remove(normalized);

    }

    private void sendEmail(String recipient, String code) {

        String host = requiredEnv("IXITIQUE_SMTP_HOST");
        String username = requiredEnv("IXITIQUE_SMTP_USER");
        String password = requiredEnv("IXITIQUE_SMTP_PASSWORD");

        String from = System.getenv()
                .getOrDefault("IXITIQUE_SMTP_FROM", username);

        String port = System.getenv()
                .getOrDefault("IXITIQUE_SMTP_PORT", "587");
//        String host = "smtp.gmail.com";
//        String username = "luisaurelionhabangue42@gmail.com";
//        String password = "";
//        String from = username;
//        String port = "587";


        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        Session session = Session.getInstance(props);
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject("Código para recuperar a senha - iXitique");
            message.setText("O seu código de recuperação é: " + code + "\n\nEste código expira em 10 minutos.");
            Transport.send(message, username, password);
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível enviar o email. Verifique a configuração SMTP.", exception);
        }
    }

    private String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Configure a variável de ambiente " + name + ".");
        return value;
    }
}
