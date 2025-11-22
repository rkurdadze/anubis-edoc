package ge.comcom.anubis.edoc.exception;

public class EdocRemoteException extends RuntimeException {
    public EdocRemoteException(String message) {
        super(message);
    }

    public EdocRemoteException(String message, Throwable cause) {
        super(message, cause);
    }
}
