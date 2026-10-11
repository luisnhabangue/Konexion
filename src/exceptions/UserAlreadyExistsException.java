package exceptions;

/**
 * Exceção lançada ao tentar registar um utilizador com credenciais (username, e-mail ou telefone) já existentes.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public class UserAlreadyExistsException extends RuntimeException {

    /**
     * Construtor que aceita uma mensagem descritiva do erro.
     *
     * @param message Mensagem explicativa
     */
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}