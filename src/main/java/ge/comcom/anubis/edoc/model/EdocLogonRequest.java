package ge.comcom.anubis.edoc.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Запрос на получение sessionId для eDocument Export Service")
public class EdocLogonRequest {
    @Schema(description = "Токен клиента eDocument", example = "{BD081743-C0C4-43B6-A0C3-30914FC9888F}")
    private String clientAuthenticationToken;

    @Schema(description = "Версия сервиса", example = "1.0.0.0")
    private String serviceVersion;
}
