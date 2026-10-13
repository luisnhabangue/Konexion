package service;

import dao.UserDao;
import exceptions.InvalidUsernameException;
import exceptions.UserAlreadyExistsException;
import exceptions.UserNotFoundException;
import model.User;
import org.mindrot.jbcrypt.BCrypt;
import util.EntityManagerFactoryUtil;
import util.PasswordUtil;
import validation.UserValidator;

/**
 * Serviço responsável pela autenticação e gestão de registo de utilizadores.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public class AuthService {

    private final UserDao userDao;

    /**
     * Construtor padrão que inicializa a DAO de utilizador com o Entity Manager singleton.
     */
    public AuthService() {
        this.userDao = new UserDao(
                EntityManagerFactoryUtil.getEntityManagerFactory()
        );
    }

    /**
     * Regista um novo utilizador no sistema efetuando validações prévias e encriptação da palavra-passe.
     *
     * @param user Dados do novo utilizador
     * @param confirmPassword Confirmação da palavra-passe
     * @throws IllegalArgumentException Se os dados forem nulos
     * @throws UserAlreadyExistsException Se o username, e-mail ou telefone já existirem
     */
    public void register(User user, String confirmPassword) {
        if (user == null) {
            throw new IllegalArgumentException("Os dados do utilizador são obrigatórios.");
        }
        UserValidator.validateRequiredFields(
                user.getUsername(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getPassword()
        );

        UserValidator.validateUsername(user.getUsername());
        UserValidator.validateEmail(user.getEmail());
        UserValidator.validatePhoneNumber(user.getPhoneNumber());
        UserValidator.validatePassword(user.getPassword());
        UserValidator.validatePasswordConfirmation(user.getPassword(), confirmPassword);

        String username = user.getUsername().trim();
        String email = user.getEmail().trim();
        String phoneNumber = normalizePhoneNumber(user.getPhoneNumber());

        if (userDao.findByUsername(username) != null) {
            throw new UserAlreadyExistsException("Este nome de utilizador já está registado.");
        }

        if (userDao.findByEmail(email) != null) {
            throw new UserAlreadyExistsException("Este e-mail já está registado.");
        }

        if (userDao.findByPhone(phoneNumber) != null) {
            throw new UserAlreadyExistsException("Este número de telefone já está registado.");
        }

        user.setUsername(username);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);

        String hashPassword = PasswordUtil.createHashPassword(user.getPassword());
        user.setPassword(hashPassword);

        userDao.createUser(user);
    }

    /**
     * Realiza a autenticação (login) de um utilizador aceitando username, e-mail ou número de telefone.
     *
     * @param identifier Identificador (username, e-mail ou telefone)
     * @param password Palavra-passe em texto limpo
     * @return O utilizador autenticado {@link User}
     * @throws UserNotFoundException Se as credenciais forem inválidas
     */
    public User login(String identifier, String password) {
        UserValidator.validateRequiredFields(identifier, password);

        String normalizedIdentifier = identifier.trim();
        User user;

        if (normalizedIdentifier.contains("@")) {
            user = userDao.findByEmail(normalizedIdentifier);
        } else if (normalizedIdentifier.matches("^(?:\\+258)?(?:82|83|84|85|86|87)[0-9]{7}$")) {
            user = userDao.findByPhone(normalizePhoneNumber(normalizedIdentifier));
        } else {
            try {
                UserValidator.validateUsername(normalizedIdentifier);
            } catch (InvalidUsernameException e) {
                throw new UserNotFoundException("Utilizador ou password inválidos!");
            }
            user = userDao.findByUsername(normalizedIdentifier);
        }

        if (user == null) {
            throw new UserNotFoundException("Utilizador ou password inválidos!");
        }

        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new UserNotFoundException("Utilizador ou password inválidos!");
        }

        return user;
    }

    /**
     * Procura um utilizador pelo seu ID.
     *
     * @param id Identificador do utilizador
     * @return Objeto {@link User} encontrado
     */
    public User findById(Long id) {
        return userDao.findById(id);
    }

    /**
     * Remove os espaços em branco de um número de telefone.
     *
     * @param phoneNumber Número de telefone com possível formatação
     * @return Número de telefone normalizado
     */
    private String normalizePhoneNumber(String phoneNumber) {
        return phoneNumber.replaceAll("\\s+", "");
    }

    /**
     * Verifica a existência prévia de um username.
     * @param username Nome de utilizador
     * @return {@code true} se já existir; {@code false} caso contrário
     */
    public boolean thisUsernameExists(String username) {
        return userDao.findByUsername(username) != null;
    }

    /**
     * Verifica a existência prévia de um e-mail.
     * @param email Endereço de e-mail
     * @return {@code true} se já existir; {@code false} caso contrário
     */
    public boolean thisEmailExists(String email) {
        return userDao.findByEmail(email) != null;
    }
    /**
     * Verifica a existência prévia de um número de telefone.
     * @param phoneNumber Número de telefone
     * @return {@code true} se já existir; {@code false} caso contrário
     */
    public boolean thisPhoneNumberExists(String phoneNumber) {
        return userDao.findByPhone(normalizePhoneNumber(phoneNumber)) != null;
    }
}