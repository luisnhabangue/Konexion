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

public class AuthService {

    private final UserDao userDao;

    public AuthService() {
        this.userDao = new UserDao(
                EntityManagerFactoryUtil.getEntityManagerFactory()
        );
    }

    public void register(User user, String confirmPassword) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "Os dados do utilizador são obrigatórios."
            );
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

        UserValidator.validatePasswordConfirmation(
                user.getPassword(),
                confirmPassword
        );


        String username = user.getUsername().trim();
        String email = user.getEmail().trim();
        String phoneNumber = normalizePhoneNumber(
                user.getPhoneNumber()
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

        user.setUsername(username);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);

        String hashPassword = PasswordUtil.createHashPassword(
                user.getPassword()
        );

        user.setPassword(hashPassword);


        userDao.createUser(user);
    }


    public User login(String identifier, String password) {

        UserValidator.validateRequiredFields(identifier, password);

        String normalizedIdentifier = identifier.trim();

        User user;


        if (normalizedIdentifier.contains("@")) {

            user = userDao.findByEmail(
                    normalizedIdentifier
            );

        } else if (normalizedIdentifier.matches(
                "^(?:\\+258)?(?:82|83|84|85|86|87)[0-9]{7}$")) {

            user = userDao.findByPhone(
                    normalizePhoneNumber(normalizedIdentifier)
            );

        } else {

            try {
                UserValidator.validateUsername(normalizedIdentifier);
            } catch (InvalidUsernameException e) {
                throw new UserNotFoundException(
                        "Utilizador ou password inválidos!"
                );
            }

            user = userDao.findByUsername(
                    normalizedIdentifier
            );
        }


        if (user == null) {
            throw new UserNotFoundException(
                    "Utilizador ou password inválidos!"
            );
        }


        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new UserNotFoundException(
                    "Utilizador ou password inválidos!"
            );
        }

        return user;
    }

    public User findById(Long id) {
      return userDao.findById(id);
    }

    private String normalizePhoneNumber(String phoneNumber) {

        return phoneNumber.replaceAll("\\s+", "");
    }

    public boolean thisUsernameExists(String username) {

        return userDao.findByUsername(username) != null;
    }

    public boolean thisEmailExists(String email) {

        return userDao.findByEmail(email) != null;
    }

    public boolean thisPhoneNumberExists(String phoneNumber) {

        return userDao.findByPhone(
                normalizePhoneNumber(phoneNumber)
        ) != null;
    }
}