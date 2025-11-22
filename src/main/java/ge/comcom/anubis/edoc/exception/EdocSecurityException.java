package ge.comcom.anubis.edoc.exception;

public class EdocSecurityException extends RuntimeException {
    public EdocSecurityException(String message) {
        super(message);
    }

    public EdocSecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}
