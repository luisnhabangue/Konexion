package exceptions;

/**
 * Exceção lançada quando o número de telefone fornecido não cumpre os requisitos de formato.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public class InvalidPhoneNumberException extends RuntimeException {

    /**
     * Construtor que aceita uma mensagem descritiva do erro.
     *
     * @param message Mensagem explicativa
     */
    public InvalidPhoneNumberException(String message) {
        super(message);
    }
}