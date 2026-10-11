package validation;

import exceptions.InvalidEmailException;
import exceptions.InvalidPasswordException;
import exceptions.InvalidPhoneNumberException;
import exceptions.InvalidUsernameException;

public final class UserValidator {

    public static void validateRequiredFields(String... fields) {

        for (String field : fields) {
            if (field == null || field.isBlank()) {
                throw new IllegalArgumentException(
                        "Preencha todos os campos obrigatórios."
                );
            }
        }
    }

    public static void validateUsername(String username) {

        if (username == null
                || !username.matches("[a-z0-9_.-]{3,20}")) {

            throw new InvalidUsernameException(
                    "O nome de utilizador deve ter entre 3 e 20 caracteres "
                            + "e conter apenas letras minúsculas, números, "
                            + "'.', '-' ou '_'."
            );
        }
    }

    public static void validateEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new InvalidEmailException(
                    "O e-mail é obrigatório."
            );
        }

        email = email.trim();

        if (email.length() > 254
                || !email.matches(
                "(?=.{1,254}$)[a-z0-9.!#$%&'*+/=?^_`{|}~-]+"
                        + "@[a-z0-9-]+(?:\\.[a-z0-9-]+)+"
        )) {

            throw new InvalidEmailException(
                    "Introduza um endereço de e-mail válido."
            );
        }
    }

    // 4. Validar telefone moçambicano
    public static void validatePhoneNumber(String phoneNumber) {

        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new InvalidPhoneNumberException(
                    "O número de telefone é obrigatório."
            );
        }

        String normalized = phoneNumber.replaceAll("\\s+", "");

        if (!normalized.matches(
                "^(?:\\+258)?(?:82|83|84|85|86|87)[0-9]{7}$")) {

            throw new InvalidPhoneNumberException(
                    "Introduza um número de telefone moçambicano válido."
            );
        }
    }

    public static void validatePassword(String password) {

        if (password == null || password.isBlank()) {
            throw new InvalidPasswordException(
                    "A password é obrigatória."
            );
        }

        if (password.length() < 8) {
            throw new InvalidPasswordException(
                    "A password deve ter pelo menos 8 caracteres."
            );
        }

        if (password.length() > 72) {
            throw new InvalidPasswordException(
                    "A password não pode ultrapassar 72 caracteres."
            );
        }
    }

    public static void validatePasswordConfirmation(
            String password,
            String confirmation) {

        if (confirmation == null || confirmation.isBlank()) {
            throw new InvalidPasswordException(
                    "Confirme a sua password."
            );
        }

        if (!password.equals(confirmation)) {
            throw new InvalidPasswordException(
                    "As passwords não coincidem."
            );
        }
    }
}



