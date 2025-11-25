package ge.comcom.anubis.edoc.service;

import ge.comcom.anubis.edoc.client.EdocExportClient;
import ge.comcom.anubis.edoc.config.EdocProperties;
import ge.comcom.anubis.edoc.exception.EdocRemoteException;
import ge.comcom.anubis.edoc.exception.EdocSecurityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EdocSessionServiceTest {

    private EdocExportClient client;
    private EdocProperties properties;
    private EdocSessionService service;

    @BeforeEach
    void setup() {
        client = mock(EdocExportClient.class);
        properties = new EdocProperties();
        properties.setClientAuthToken("token");
        properties.setServiceVersion("v1");
        service = new EdocSessionService(client, properties);
    }

    @Test
    void createsSessionOnce() {
        when(client.logOn(anyString(), anyString())).thenReturn("session-1");
        String first = service.currentSession();
        String second = service.currentSession();
        verify(client, times(1)).logOn(anyString(), anyString());
        assertEquals(first, second);
    }

    @Test
    void retriesOnSecurityFault() {
        when(client.logOn(anyString(), anyString())).thenReturn("session-1", "session-2");
        service.currentSession();
        assertThrows(EdocSecurityException.class, () -> {
            throw new EdocSecurityException("security");
        });
    }

    @Test
    void failsWithoutToken() {
        properties.setClientAuthToken("");
        assertThrows(EdocRemoteException.class, () -> new EdocSessionService(client, properties).currentSession());
    }
}
