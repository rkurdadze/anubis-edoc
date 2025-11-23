package ge.comcom.anubis.edoc.service;

import ge.comcom.anubis.edoc.client.EdocExportClient;
import ge.comcom.anubis.edoc.config.EdocProperties;
import ge.comcom.anubis.edoc.exception.EdocRemoteException;
import ge.comcom.anubis.edoc.exception.EdocSecurityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class EdocSessionService {

    private final EdocExportClient client;
    private final EdocProperties properties;
    private final AtomicReference<String> session = new AtomicReference<>();

    public String currentSession() {
        return session.updateAndGet(existing -> {
            if (StringUtils.hasText(existing)) {
                return existing;
            }
            return createSession(properties.getClientAuthToken(), properties.getServiceVersion());
        });
    }

    public String logOn(String token, String version) {
        String sessionId = createSession(token, version);
        session.set(sessionId);
        return sessionId;
    }

    public void logout() {
        logOut(null);
    }

    public void logOut(String sessionId) {
        String target = StringUtils.hasText(sessionId) ? sessionId : session.get();
        if (!StringUtils.hasText(target)) {
            return;
        }
        try {
            client.logOut(target);
            log.info("Выполнен LogOut для сессии {}", target);
        } catch (Exception ex) {
            log.warn("Ошибка при LogOut", ex);
        } finally {
            session.compareAndSet(target, null);
        }
    }

    public <T> T withSession(Function<String, T> callback) {
        try {
            return callback.apply(currentSession());
        } catch (EdocSecurityException ex) {
            log.warn("Сессия eDocument недействительна, выполняется повторный вход", ex);
            session.set(null);
            return callback.apply(currentSession());
        }
    }

    private String createSession(String token, String version) {
        String resolvedToken = StringUtils.hasText(token) ? token : properties.getClientAuthToken();
        String resolvedVersion = StringUtils.hasText(version) ? version : properties.getServiceVersion();
        if (!StringUtils.hasText(resolvedToken)) {
            throw new EdocRemoteException("Токен edoc.client-auth-token не задан");
        }
        if (!StringUtils.hasText(resolvedVersion)) {
            throw new EdocRemoteException("Версия сервиса edoc.service-version не задана");
        }
        log.info("Выполняется LogOn в eDocument Export Service, версия {}", resolvedVersion);
        String sessionId = client.logOn(resolvedToken, resolvedVersion);
        if (!StringUtils.hasText(sessionId)) {
            throw new EdocRemoteException("Пустой sessionID от LogOn");
        }
        log.info("Получена новая сессия eDocument: {}", sessionId);
        return sessionId;
    }
}
