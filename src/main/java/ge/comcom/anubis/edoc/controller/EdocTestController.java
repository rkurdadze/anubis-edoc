package ge.comcom.anubis.edoc.controller;

import ge.comcom.anubis.edoc.client.EdocExportClient;
import ge.comcom.anubis.edoc.config.EdocProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/edoc/test")
@RequiredArgsConstructor
public class EdocTestController {

    private final EdocExportClient client;
    private final EdocProperties properties;

    @PostMapping("/logon")
    public ResponseEntity<?> testLogOn() {
        log.info("➡ Calling eDocument LogOn test endpoint...");

        try {
            String sessionId = client.logOn(
                    properties.getClientToken(),
                    properties.getServiceVersion()
            );

            log.info("✔ SUCCESS: Session ID = {}", sessionId);

            return ResponseEntity.ok(sessionId);

        } catch (Exception ex) {
            log.error("❌ ERROR calling eDocument LogOn", ex);
            return ResponseEntity.internalServerError().body(ex.getMessage());
        }
    }
}
