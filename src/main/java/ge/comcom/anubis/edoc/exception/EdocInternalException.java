package ge.comcom.anubis.edoc.exception;

public class EdocInternalException extends RuntimeException {
    public EdocInternalException(String message) {
        super(message);
    }

    public EdocInternalException(String message, Throwable cause) {
        super(message, cause);
    }
}
