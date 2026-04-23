package ge.comcom.anubis.edoc.config;

import ge.comcom.anubis.edoc.client.EdocExportClient;
import lombok.RequiredArgsConstructor;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tempuri.IeDocumentExportService;

import jakarta.xml.ws.BindingProvider;
import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class EdocClientConfig {

    private final EdocProperties properties;

    private static final String NS = "http://tempuri.org/";

    @Bean
    public IeDocumentExportService ieDocumentExportService() {

        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(IeDocumentExportService.class);

        // Allow large text nodes in SOAP payload (e.g. base64 file content in GetDocument).
        Map<String, Object> factoryProps = new HashMap<>();
        factoryProps.put("org.apache.cxf.stax.maxTextLength", properties.getMaxTextLength());
        factory.setProperties(factoryProps);

        // URL только базовый (без ?wsdl!)
        factory.setAddress(properties.getExportServiceUrl());

        // SERVICE + PORT строго из WSDL
        factory.setServiceName(new QName(NS, "eDocumentExportService"));
        factory.setEndpointName(new QName(NS, "BasicHttpBinding_IeDocumentExportService"));

        IeDocumentExportService proxy = (IeDocumentExportService) factory.create();

        configureStaxLimits(proxy);
        configureTimeouts(proxy);

        return proxy;
    }

    private void configureStaxLimits(IeDocumentExportService proxy) {
        String maxTextLen = Integer.toString(properties.getMaxTextLength());

        // CXF checks this key for StAX limits.
        System.setProperty("org.apache.cxf.stax.maxTextLength", maxTextLen);

        // Woodstox text-length limit key (effective for underlying parser instances).
        System.setProperty("com.ctc.wstx.maxTextLength", maxTextLen);

        // Also push to request context for this proxy explicitly.
        BindingProvider bp = (BindingProvider) proxy;
        Map<String, Object> ctx = bp.getRequestContext();
        ctx.put("org.apache.cxf.stax.maxTextLength", properties.getMaxTextLength());
    }
    private void configureTimeouts(IeDocumentExportService proxy) {
        Client client = ClientProxy.getClient(proxy);
        HTTPConduit conduit = (HTTPConduit) client.getConduit();

        HTTPClientPolicy policy = new HTTPClientPolicy();
        policy.setConnectionTimeout(properties.getConnectTimeoutMillis());
        policy.setReceiveTimeout(properties.getReadTimeoutMillis());

        conduit.setClient(policy);
    }

    @Bean
    public EdocExportClient edocExportClient(IeDocumentExportService service) {
        return new EdocExportClient(service);
    }
}
