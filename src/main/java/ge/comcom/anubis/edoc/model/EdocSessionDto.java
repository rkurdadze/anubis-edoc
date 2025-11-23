package ge.comcom.anubis.edoc.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Ответ с sessionId от eDocument Export Service")
public class EdocSessionDto {
    @Schema(description = "Идентификатор активной сессии", example = "7e3c3bda-1f8a-4a8a-8fd8-12aa8b090c37")
    private String sessionId;
}
