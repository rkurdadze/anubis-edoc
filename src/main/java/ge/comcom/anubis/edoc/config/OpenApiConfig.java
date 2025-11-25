package ge.comcom.anubis.edoc.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI edocOpenApi(@Value("${edoc.base-url:}") String baseUrl,
                               @Value("${edoc.client-auth-token:{BD081743-C0C4-43B6-A0C3-30914FC9888F}}") String token) {
        OpenAPI api = new OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("Anubis eDocument Export REST API")
                        .version("1.0.0")
                        .description("REST-обёртка над eDocument Export Service. " +
                                "Для тестов используйте дефолтный токен " + token + ", " +
                                "а также набор сценариев Postman из репозитория.")
                        .license(new io.swagger.v3.oas.models.info.License().name("Proprietary")))
                .addServersItem(new Server().url("/").description("Текущий хост (без смены схемы для Swagger UI)"));

        if (StringUtils.hasText(baseUrl)) {
            api.addServersItem(new Server().url(baseUrl).description("Базовый URL из настроек"));
        }

        return api;
    }
}
