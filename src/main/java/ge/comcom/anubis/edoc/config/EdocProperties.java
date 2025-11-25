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

    private String clientToken;

}
