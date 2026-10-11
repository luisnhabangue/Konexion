package exceptions;

/**
 * Exceção lançada quando se tenta realizar uma operação com uma conta de utilizador inativa.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public class InactiveUserException extends RuntimeException {

    /**
     * Construtor que aceita uma mensagem descritiva do erro.
     *
     * @param message Mensagem explicativa
     */
    public InactiveUserException(String message) {
        super(message);
    }
}