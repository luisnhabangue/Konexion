package exceptions;

/**
 * Exceção lançada quando o endereço de e-mail fornecido não cumpre o formato válido.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public class InvalidEmailException extends RuntimeException {

    /**
     * Construtor que aceita uma mensagem descritiva do erro.
     *
     * @param message Mensagem explicativa
     */
    public InvalidEmailException(String message) {
        super(message);
    }
}