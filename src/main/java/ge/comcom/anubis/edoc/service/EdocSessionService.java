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
            return login();
        });
    }

    private String login() {
        if (!StringUtils.hasText(properties.getClientAuthToken())) {
            throw new EdocRemoteException("Токен edoc.client-auth-token не задан");
        }
        log.info("Выполняется LogOn в eDocument Export Service, версия {}", properties.getServiceVersion());
        String sessionId = client.logOn(properties.getClientAuthToken(), properties.getServiceVersion());
        if (!StringUtils.hasText(sessionId)) {
            throw new EdocRemoteException("Пустой sessionID от LogOn");
        }
        log.info("Получена новая сессия eDocument: {}", sessionId);
        return sessionId;
    }

    public void logout() {
        String current = session.getAndSet(null);
        if (StringUtils.hasText(current)) {
            try {
                client.logOut(current);
                log.info("Выполнен LogOut для сессии {}", current);
            } catch (Exception ex) {
                log.warn("Ошибка при LogOut", ex);
            }
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
}
