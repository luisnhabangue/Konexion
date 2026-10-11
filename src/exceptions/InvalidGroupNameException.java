package exceptions;

/**
 * Exceção lançada quando o nome atribuído a um grupo de Xitique é inválido.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public class InvalidGroupNameException extends RuntimeException {

    /**
     * Construtor da exceção com mensagem personalizada.
     * @param message Mensagem de erro explicativa
     */
    public InvalidGroupNameException(String message) {
        super(message);
    }
}
