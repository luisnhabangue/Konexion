package validation;

import exceptions.InvalidEmailException;
import exceptions.InvalidPasswordException;
import exceptions.InvalidPhoneNumberException;
import exceptions.InvalidUsernameException;
import java.time.LocalDate;
import java.time.Period;

/**
 * Classe utilitária e final para validações de formato dos dados do utilizador.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public final class UserValidator {

    private UserValidator() {
    }

    /**
     * Valida se os campos obrigatórios em formato varargs não são nulos nem estão vazios.
     * @param fields Campos de texto a validar
     * @throws IllegalArgumentException Se algum campo for nulo ou em branco
     */
    public static void validateRequiredFields(String... fields) {
        for (String field : fields) {
            if (field == null || field.isBlank()) {
                throw new IllegalArgumentException("Preencha todos os campos obrigatórios.");
            }
        }
    }

    public static void validateBirthDate(LocalDate birthDate) {
        if (birthDate == null) {
            throw new IllegalArgumentException("Informe a data de nascimento.");
        }
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        if (age < 15 || age > 90) {
            throw new IllegalArgumentException("A idade deve estar entre 15 e 90 anos.");
        }
    }

    /**
     * Valida se o formato do nome de utilizador cumpre a expressão regular estipulada (3-20 caracteres).
     * @param username Nome de utilizador
     * @throws InvalidUsernameException Se o formato for inválido
     */
    public static void validateUsername(String username) {
        if (username == null || !username.matches("[a-z0-9_.-]{3,20}")) {
            throw new InvalidUsernameException(
                    "O nome de utilizador deve ter entre 3 e 20 caracteres e conter apenas letras minúsculas, números, '.', '-' ou '_'."
            );
        }
    }

    /**
     * Valida o formato do e-mail.
     * @param email Endereço de e-mail
     * @throws InvalidEmailException Se o e-mail for nulo, em branco ou fora do padrão
     */
    public static void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidEmailException("O e-mail é obrigatório.");
        }

        email = email.trim();

        if (email.length() > 254
                || !email.matches("(?=.{1,254}$)[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9-]+(?:\\.[a-z0-9-]+)+")) {
            throw new InvalidEmailException("Introduza um endereço de e-mail válido.");
        }
    }

    /**
     * Valida se o número de telefone pertence ao formato válido de Moçambique (+258 / 82, 83, 84, 85, 86, 87).
     * @param phoneNumber Número de telefone
     * @throws InvalidPhoneNumberException Se o número for inválido
     */
    public static void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new InvalidPhoneNumberException("O número de telefone é obrigatório.");
        }

        String normalized = phoneNumber.replaceAll("\\s+", "");

        if (!normalized.matches("^(?:\\+258)?(?:82|83|84|85|86|87)[0-9]{7}$")) {
            throw new InvalidPhoneNumberException("Introduza um número de telefone moçambicano válido.");
        }
    }

    /**
     * Valida os requisitos mínimos e máximos de tamanho da palavra-passe (8 a 72 caracteres).
     * @param password Palavra-passe a validar
     * @throws InvalidPasswordException Se a password não respeitar o tamanho
     */
    public static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new InvalidPasswordException("A password é obrigatória.");
        }

        if (password.length() < 8) {
            throw new InvalidPasswordException("A password deve ter pelo menos 8 caracteres.");
        }

        if (password.length() > 72) {
            throw new InvalidPasswordException("A password não pode ultrapassar 72 caracteres.");
        }
    }

    /**
     * Valida se a palavra-passe e a respetiva confirmação coincidem.
     * @param password Palavra-passe
     * @param confirmation Confirmação da palavra-passe
     * @throws InvalidPasswordException Se as passwords forem divergentes
     */
    public static void validatePasswordConfirmation(String password, String confirmation) {
        if (confirmation == null || confirmation.isBlank()) {
            throw new InvalidPasswordException("Confirme a sua password.");
        }

        if (!password.equals(confirmation)) {
            throw new InvalidPasswordException("As passwords não coincidem.");
        }
    }
}