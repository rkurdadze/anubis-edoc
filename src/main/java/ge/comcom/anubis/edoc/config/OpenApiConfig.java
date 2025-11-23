package ge.comcom.anubis.edoc.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Anubis eDocument Export REST API",
                version = "1.0.0",
                description = "REST-обёртка над eDocument Export Service с полным соответствием методам LogOn/LogOut, " +
                        "GetDocuments, GetDocument, SetDocumentExported и поисковым операциям по физическим лицам, " +
                        "организациям и государственным структурам.",
                contact = @Contact(name = "Anubis Team"),
                license = @License(name = "Proprietary")
        ),
        servers = {
                @Server(description = "Local", url = "http://localhost:8080")
        }
)
public class OpenApiConfig {

    @Bean
    public OpenAPI edocOpenApi(@Value("${edoc.base-url:http://localhost:8080}") String baseUrl,
                               @Value("${edoc.client-auth-token:{BD081743-C0C4-43B6-A0C3-30914FC9888F}}") String token) {
        return new OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("Anubis eDocument Export REST API")
                        .version("1.0.0")
                        .description("REST-обёртка над eDocument Export Service. " +
                                "Для тестов используйте дефолтный токен " + token + ", " +
                                "а также набор сценариев Postman из репозитория.")
                        .license(new io.swagger.v3.oas.models.info.License().name("Proprietary")))
                .addServersItem(new io.swagger.v3.oas.models.servers.Server().url(baseUrl).description("Configured base URL"));
    }
}
