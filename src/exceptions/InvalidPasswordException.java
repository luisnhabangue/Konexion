package exceptions;

/**
 * Exceção lançada quando uma palavra-passe não cumpre os requisitos de validação ou segurança.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public class InvalidPasswordException extends RuntimeException {

    /**
     * Construtor da exceção com mensagem personalizada.
     * @param message Mensagem de erro explicativa
     */
    public InvalidPasswordException(String message) {
        super(message);
    }
}