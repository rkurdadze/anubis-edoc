package ge.comcom.anubis.edoc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "edoc")
public class EdocProperties {
    private boolean enabled = true;
    private String baseUrl = "https://edocument.ge/TEST/";
    private String exportServiceUrl = "https://edocument.ge/TEST/integrationservice/eDocumentExportService.svc";
    private String wsdlUrl = "https://edocument.ge/TEST/integrationservice/eDocumentExportService.svc?wsdl";
    private String clientAuthToken = "{BD081743-C0C4-43B6-A0C3-30914FC9888F}";
    private String serviceVersion = "1.0.0.0";
    private int connectTimeoutMillis = 10000;
    private int readTimeoutMillis = 60000;

    private String clientToken;

}
