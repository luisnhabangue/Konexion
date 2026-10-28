package service;

import dao.UserDao;
import exceptions.InvalidUsernameException;
import exceptions.UserAlreadyExistsException;
import exceptions.UserNotFoundException;
import model.User;
import model.enums.UserRole;
import org.mindrot.jbcrypt.BCrypt;
import util.EntityManagerFactoryUtil;
import util.PasswordUtil;
import validation.UserValidator;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Serviço responsável pelo registo, autenticação e gestão
 * dos dados pessoais dos utilizadores do sistema iXitique.
 *
 * Aplica as regras de negócio relacionadas com contas,
 * credenciais e validação dos dados.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public class AuthService {

    private final UserDao userDao;

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-z][a-z0-9._]{2,18}[a-z0-9_]$");

    private static final Pattern MOZAMBICAN_PHONE_PATTERN =
            Pattern.compile("^(?:\\+258)?(?:82|83|84|85|86|87)[0-9]{7}$");

    private static final Pattern BI_PATTERN =
            Pattern.compile("^[0-9]{12}[A-Z]$");

    private static final Pattern NUIT_PATTERN =
            Pattern.compile("^[0-9]{9}$");

    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[\\p{L}]+(?:[ '-][\\p{L}]+)*$");

    private static final Pattern PASSWORD_UPPERCASE =
            Pattern.compile(".*[A-Z].*");

    private static final Pattern PASSWORD_LOWERCASE =
            Pattern.compile(".*[a-z].*");

    private static final Pattern PASSWORD_DIGIT =
            Pattern.compile(".*[0-9].*");

    private static final Pattern PASSWORD_SYMBOL =
            Pattern.compile(".*[^a-zA-Z0-9].*");

    /**
     * Inicializa a DAO responsável pela persistência dos utilizadores.
     */
    public AuthService() {
        this.userDao = new UserDao(
                EntityManagerFactoryUtil.getEntityManagerFactory()
        );
    }

    /**
     * Regista um novo utilizador após validar os dados e verificar
     * a existência de registos duplicados.
     *
     * A conta é criada ativa e com o papel base.
     *
     * @param user Dados do novo utilizador
     * @param confirmPassword Confirmação da palavra-passe
     * @param acceptedTerms Confirmação de aceitação dos Termos e Condições
     */
    public void register(User user, String confirmPassword, boolean acceptedTerms) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "Os dados do utilizador são obrigatórios."
            );
        }
        if (!acceptedTerms) {
            throw new IllegalArgumentException(
                    "É necessário aceitar os Termos e Condições para criar uma conta."
            );
        }

        validateName(user.getFirstName(), "primeiro nome");
        validateName(user.getLastName(), "apelido");

        if (user.getGender() == null) {
            throw new IllegalArgumentException("Selecione o género.");
        }

        UserValidator.validateRequiredFields(
                user.getUsername(), user.getEmail(), user.getPhoneNumber(),
                user.getPassword(), confirmPassword
        );

        String username = normalizeUsername(user.getUsername());
        String email = normalizeEmail(user.getEmail());
        String phoneNumber = normalizePhoneNumber(user.getPhoneNumber());
        String documentId = user.getDocumentId() == null || user.getDocumentId().isBlank()
                ? null : normalizeDocumentId(user.getDocumentId());

        validateUsername(username);
        UserValidator.validateEmail(email);
        validatePhoneNumber(phoneNumber);
        if (documentId != null) {
            validateDocumentId(documentId);
        }
        validateStrongPassword(user.getPassword());

        UserValidator.validatePasswordConfirmation(
                user.getPassword(),
                confirmPassword
        );

        if (userDao.findByUsername(username) != null) {
            throw new UserAlreadyExistsException(
                    "Este nome de utilizador já está registado."
            );
        }

        if (userDao.findByEmail(email) != null) {
            throw new UserAlreadyExistsException(
                    "Este e-mail já está registado."
            );
        }

        if (userDao.findByPhone(phoneNumber) != null) {
            throw new UserAlreadyExistsException(
                    "Este número de telefone já está registado."
            );
        }

        if (documentId != null && userDao.findByDocumentId(documentId) != null) {
            throw new UserAlreadyExistsException(
                    "Este documento já está registado."
            );
        }

        user.setFirstName(user.getFirstName().trim());
        user.setLastName(user.getLastName().trim());
        user.setDocumentId(documentId);
        user.setUsername(username);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);

        // A palavra-passe nunca deve ser guardada em texto simples.
        user.setPassword(
                PasswordUtil.createHashPassword(user.getPassword())
        );

        // O registo não permite criar uma conta de administrador.
        user.setUserRole(UserRole.USER);

        // Accounts are active for participation but require administrator KYC approval to create groups.
        user.setActive(true);
        user.setVerified(false);

        userDao.createUser(user);
    }

    /**
     * Autentica um utilizador através do username, e-mail ou telefone.
     *
     * @param identifier Username, e-mail ou telefone
     * @param password Palavra-passe em texto simples
     * @return Utilizador autenticado
     */
    public User login(String identifier, String password) {

        if (identifier == null || identifier.isBlank()
                || password == null || password.isBlank()) {
            throw invalidCredentials();
        }

        String normalizedIdentifier = identifier.trim();
        User user;

        if (normalizedIdentifier.contains("@")) {

            user = userDao.findByEmail(
                    normalizeEmail(normalizedIdentifier)
            );

        } else if (MOZAMBICAN_PHONE_PATTERN.matcher(
                normalizedIdentifier.replaceAll("\\s+", "")
        ).matches()) {

            user = userDao.findByPhone(
                    normalizePhoneNumber(normalizedIdentifier)
            );

        } else {

            String username = normalizeUsername(normalizedIdentifier);

            try {
                validateUsername(username);
            } catch (IllegalArgumentException
                     | InvalidUsernameException exception) {
                throw invalidCredentials();
            }

            user = userDao.findByUsername(username);
        }

        // A mensagem não revela se o utilizador existe ou não.
        if (user == null || !user.isActive()) {
            throw invalidCredentials();
        }

        if (user.getPassword() == null
                || !BCrypt.checkpw(password, user.getPassword())) {
            throw invalidCredentials();
        }

        return user;
    }

    /**
     * Procura um utilizador pelo identificador.
     *
     * @param id Identificador do utilizador
     * @return Utilizador encontrado ou null
     */
    public User findById(Long id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID de utilizador inválido.");
        }

        return userDao.findById(id);
    }

    public void submitKyc(Long userId, String documentId, byte[] documentImage, byte[] selfieImage) {
        if (userId == null) {
            throw new IllegalArgumentException("A conta a verificar é inválida.");
        }
        String normalizedDocument = normalizeDocumentId(documentId == null ? "" : documentId);
        validateDocumentId(normalizedDocument);
        if (documentImage == null || documentImage.length == 0
                || selfieImage == null || selfieImage.length == 0) {
            throw new IllegalArgumentException("Anexe uma foto do BI/NUIT e uma fotografia de confirmação.");
        }
        if (documentImage.length > 8 * 1024 * 1024 || selfieImage.length > 8 * 1024 * 1024) {
            throw new IllegalArgumentException("Cada fotografia deve ter no máximo 8 MB.");
        }
        User storedUser = userDao.findById(userId);
        if (storedUser == null) {
            throw new UserNotFoundException("Não foi possível encontrar a conta.");
        }
        User documentOwner = userDao.findByDocumentId(normalizedDocument);
        if (documentOwner != null && !documentOwner.getId().equals(userId)) {
            throw new UserAlreadyExistsException("Este documento já está associado a outra conta.");
        }
        storedUser.setDocumentId(normalizedDocument);
        storedUser.setIdentityDocumentImage(documentImage);
        storedUser.setIdentitySelfieImage(selfieImage);
        storedUser.setVerified(false);
        storedUser.setKycStatus("PENDING");
        userDao.updateUser(storedUser);
    }

    public void reviewKyc(Long administratorId, Long userId, boolean approved) {
        User administrator = userDao.findById(administratorId);
        User reviewedUser = userDao.findById(userId);
        if (administrator == null || administrator.getUserRole() == null
                || !administrator.getUserRole().isAdministrator()) {
            throw new SecurityException("Apenas um administrador pode rever a verificação KYC.");
        }
        if (reviewedUser == null || reviewedUser.getIdentityDocumentImage() == null
                || reviewedUser.getIdentitySelfieImage() == null) {
            throw new IllegalArgumentException("O utilizador ainda não submeteu os documentos KYC.");
        }
        reviewedUser.setVerified(approved);
        reviewedUser.setKycStatus(approved ? "VERIFIED" : "REJECTED");
        userDao.updateUser(reviewedUser);
    }

    /**
     * Verifica se o username já está registado.
     */
    public boolean thisUsernameExists(String username) {

        if (username == null || username.isBlank()) {
            return false;
        }

        return userDao.findByUsername(
                normalizeUsername(username)
        ) != null;
    }

    /**
     * Verifica se o e-mail já está registado.
     */
    public boolean thisEmailExists(String email) {

        if (email == null || email.isBlank()) {
            return false;
        }

        return userDao.findByEmail(
                normalizeEmail(email)
        ) != null;
    }

    /**
     * Verifica se o telefone já está registado.
     */
    public boolean thisPhoneNumberExists(String phoneNumber) {

        if (phoneNumber == null || phoneNumber.isBlank()) {
            return false;
        }

        return userDao.findByPhone(
                normalizePhoneNumber(phoneNumber)
        ) != null;
    }

    /**
     * Atualiza os dados pessoais do utilizador.
     *
     * O username, o papel e a palavra-passe não são alterados
     * por este método.
     *
     * A alteração do e-mail fica bloqueada até existir o fluxo
     * de confirmação do novo endereço.
     */
    public void updateProfile(
            User user,
            String firstName,
            String lastName,
            String email,
            String phoneNumber
    ) {

        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException(
                    "A conta a atualizar é inválida."
            );
        }

        validateName(firstName, "primeiro nome");
        validateName(lastName, "apelido");

        UserValidator.validateRequiredFields(email, phoneNumber);
        UserValidator.validateEmail(email);

        String normalizedEmail = normalizeEmail(email);
        String normalizedPhone = normalizePhoneNumber(phoneNumber);

        validatePhoneNumber(normalizedPhone);

        // Recarrega a conta para não depender apenas do objeto recebido.
        User storedUser = userDao.findById(user.getId());

        if (storedUser == null) {
            throw new UserNotFoundException(
                    "Não foi possível encontrar a conta."
            );
        }

        // O novo e-mail só poderá substituir o atual depois
        // de implementarmos a confirmação por token.
        if (!normalizedEmail.equals(normalizeEmail(storedUser.getEmail()))) {
            throw new IllegalStateException(
                    "A alteração do e-mail requer confirmação. "
                            + "Este processo ainda precisa de ser implementado."
            );
        }

        User phoneOwner = userDao.findByPhone(normalizedPhone);

        if (phoneOwner != null
                && !phoneOwner.getId().equals(storedUser.getId())) {
            throw new UserAlreadyExistsException(
                    "Este número de telefone já está registado."
            );
        }

        storedUser.setFirstName(firstName.trim());
        storedUser.setLastName(lastName.trim());
        storedUser.setPhoneNumber(normalizedPhone);

        userDao.updateUser(storedUser);

        user.setFirstName(storedUser.getFirstName());
        user.setLastName(storedUser.getLastName());
        user.setEmail(storedUser.getEmail());
        user.setPhoneNumber(storedUser.getPhoneNumber());
    }

    /**
     * Valida o formato do username.
     *
     * Regras:
     * - Entre 4 e 20 caracteres;
     * - Começa por uma letra;
     * - Aceita letras, números, pontos e underscores;
     * - Não termina com ponto;
     * - Não aceita pontos consecutivos.
     */
    private void validateUsername(String username) {

        UserValidator.validateUsername(username);

        if (username == null
                || username.length() < 4
                || username.length() > 20
                || !USERNAME_PATTERN.matcher(username).matches()
                || username.contains("..")) {

            throw new InvalidUsernameException(
                    "O username deve ter 4 a 20 caracteres, "
                            + "começar por uma letra e usar apenas letras, "
                            + "números, pontos ou underscores."
            );
        }
    }

    /**
     * Valida o nome ou apelido.
     */
    private void validateName(String name, String fieldName) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "O campo " + fieldName + " é obrigatório."
            );
        }

        String normalizedName = name.trim();

        if (normalizedName.length() < 2
                || normalizedName.length() > 60
                || !NAME_PATTERN.matcher(normalizedName).matches()) {

            throw new IllegalArgumentException(
                    "O campo " + fieldName
                            + " deve ter entre 2 e 60 caracteres "
                            + "e conter apenas letras, espaços, apóstrofos "
                            + "ou hífenes."
            );
        }
    }

    /**
     * Valida BI ou NUIT.
     *
     * BI: 12 dígitos seguidos de uma letra.
     * NUIT: exatamente 9 dígitos.
     */
    private void validateDocumentId(String documentId) {

        if (!BI_PATTERN.matcher(documentId).matches()
                && !NUIT_PATTERN.matcher(documentId).matches()) {

            throw new IllegalArgumentException(
                    "Documento inválido. Introduza um BI com "
                            + "12 dígitos e uma letra, ou um NUIT com 9 dígitos."
            );
        }
    }

    /**
     * Valida o formato do telefone móvel moçambicano.
     */
    private void validatePhoneNumber(String phoneNumber) {

        if (!MOZAMBICAN_PHONE_PATTERN.matcher(phoneNumber).matches()) {
            throw new IllegalArgumentException(
                    "Introduza um número móvel moçambicano válido."
            );
        }
    }

    /**
     * Aplica a política de segurança da palavra-passe.
     */
    private void validateStrongPassword(String password) {

        if (password == null
                || password.length() < 8
                || password.length() > 64
                || !PASSWORD_UPPERCASE.matcher(password).matches()
                || !PASSWORD_LOWERCASE.matcher(password).matches()
                || !PASSWORD_DIGIT.matcher(password).matches()
                || !PASSWORD_SYMBOL.matcher(password).matches()) {

            throw new IllegalArgumentException(
                    "A palavra-passe deve ter entre 8 e 64 caracteres "
                            + "e incluir uma letra maiúscula, uma minúscula, "
                            + "um número e um símbolo."
            );
        }
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeDocumentId(String documentId) {
        return documentId.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Remove espaços e guarda o telefone num formato único.
     * Números locais são convertidos para o formato +258.
     */
    private String normalizePhoneNumber(String phoneNumber) {

        String normalized = phoneNumber.replaceAll("\\s+", "");

        if (!normalized.startsWith("+258")) {
            normalized = "+258" + normalized;
        }

        return normalized;
    }

    private UserNotFoundException invalidCredentials() {
        return new UserNotFoundException(
                "Utilizador ou palavra-passe inválidos."
        );
    }
}
