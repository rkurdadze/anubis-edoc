package ge.comcom.anubis.edoc.exception;

public class EdocOperationException extends RuntimeException {
    public EdocOperationException(String message) {
        super(message);
    }

    public EdocOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
