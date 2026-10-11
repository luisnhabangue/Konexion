package exceptions;

/**
 * Exceção lançada quando uma pesquisa por utilizador não produz resultados ou durante falhas de autenticação.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public class UserNotFoundException extends RuntimeException {

    /**
     * Construtor que aceita uma mensagem descritiva do erro.
     *
     * @param message Mensagem explicativa
     */
    public UserNotFoundException(String message) {
        super(message);
    }
}