package ge.comcom.anubis.edoc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "edoc")
public class EdocProperties {
    private boolean enabled;
    private String baseUrl;
    private String exportServiceUrl;
    private String wsdlUrl;
    private String clientAuthToken;
    private String serviceVersion;
    private int connectTimeoutMillis;
    private int readTimeoutMillis;
    /**
     * Maximum allowed XML text node length for CXF/Woodstox unmarshalling.
     * Needed for large base64 payloads returned by GetDocument(full=true).
     */
    private int maxTextLength = 536_870_912; // 512 MiB
}
