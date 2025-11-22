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

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class EdocClientConfig {

    private final EdocProperties properties;

    @Bean
    public IeDocumentExportService ieDocumentExportService() {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(IeDocumentExportService.class);
        factory.setAddress(properties.getExportServiceUrl());
        factory.setFeatures(List.of(new org.apache.cxf.ws.addressing.WSAddressingFeature()));
        IeDocumentExportService proxy = (IeDocumentExportService) factory.create();
        configureTimeouts(proxy);
        return proxy;
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
