package exceptions;

/**
 * Exceção lançada quando o nome de utilizador (username) viola as regras de formato do sistema.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public class InvalidUsernameException extends RuntimeException {

    /**
     * Construtor que aceita uma mensagem descritiva do erro.
     *
     * @param message Mensagem explicativa
     */
    public InvalidUsernameException(String message) {
        super(message);
    }
}