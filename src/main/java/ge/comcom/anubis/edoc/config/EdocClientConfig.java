package ge.comcom.anubis.edoc.config;

import ge.comcom.anubis.edoc.client.EdocExportClient;
import lombok.RequiredArgsConstructor;
import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.SoapVersionFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tempuri.IeDocumentExportService;

import javax.xml.namespace.QName;

@Configuration
@RequiredArgsConstructor
public class EdocClientConfig {

    private final EdocProperties properties;

    private static final String NS = "http://tempuri.org/";

    @Bean
    public IeDocumentExportService ieDocumentExportService() {

        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(IeDocumentExportService.class);

        // URL только базовый (без ?wsdl!)
        factory.setAddress(properties.getExportServiceUrl());

        // SERVICE + PORT строго из WSDL
        factory.setServiceName(new QName(NS, "eDocumentExportService"));
        factory.setEndpointName(new QName(NS, "CustomBinding_IeDocumentExportService"));

        factory.setBindingId("http://schemas.xmlsoap.org/wsdl/soap/");

        // WS-Addressing включен
        factory.getFeatures().add(new WSAddressingFeature());

        IeDocumentExportService proxy = (IeDocumentExportService) factory.create();

        configureTimeouts(proxy);

        return proxy;
    }


    /** Установить WS-Addressing Action вручную */
    private void configureWSAAction(IeDocumentExportService proxy) {
        Client client = ClientProxy.getClient(proxy);
        client.getRequestContext().put(
                SoapBindingConstants.SOAP_ACTION,
                "http://tempuri.org/IeDocumentExportService/LogOn"
        );
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
