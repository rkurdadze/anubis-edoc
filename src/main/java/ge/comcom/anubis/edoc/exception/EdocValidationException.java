package ge.comcom.anubis.edoc.exception;

public class EdocValidationException extends RuntimeException {
    public EdocValidationException(String message) {
        super(message);
    }

    public EdocValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
