package exceptions;

public class InvalidGroupNameException extends RuntimeException {
    public InvalidGroupNameException(String message) {
        super(message);
    }
}
