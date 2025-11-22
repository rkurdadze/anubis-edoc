package ge.comcom.anubis.edoc.controller;

import ge.comcom.anubis.edoc.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler({EdocSecurityException.class})
    public ResponseEntity<Map<String, Object>> handleSecurity(EdocSecurityException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "EDOC_SECURITY", ex, request);
    }

    @ExceptionHandler({EdocValidationException.class, MethodArgumentNotValidException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> handleValidation(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "EDOC_VALIDATION", ex, request);
    }

    @ExceptionHandler({EdocOperationException.class})
    public ResponseEntity<Map<String, Object>> handleOperation(EdocOperationException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "EDOC_OPERATION", ex, request);
    }

    @ExceptionHandler({EdocInternalException.class})
    public ResponseEntity<Map<String, Object>> handleInternal(EdocInternalException ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "EDOC_INTERNAL", ex, request);
    }

    @ExceptionHandler({EdocRemoteException.class})
    public ResponseEntity<Map<String, Object>> handleRemote(EdocRemoteException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_GATEWAY, "EDOC_REMOTE", ex, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", ex, request);
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String code, Exception ex, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", OffsetDateTime.now());
        body.put("path", request.getRequestURI());
        body.put("errorCode", code);
        body.put("message", ex.getMessage());
        return ResponseEntity.status(status).body(body);
    }
}
