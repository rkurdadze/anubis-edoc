package ge.comcom.anubis.edoc.controller;

import ge.comcom.anubis.edoc.model.EdocLogonRequest;
import ge.comcom.anubis.edoc.model.EdocSessionDto;
import ge.comcom.anubis.edoc.service.EdocSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/edoc/session")
@RequiredArgsConstructor
@Tag(name = "Session", description = "Методы авторизации LogOn/LogOut для eDocument Export Service")
@Validated
public class EdocSessionController {

    private final EdocSessionService sessionService;

    @PostMapping("/logon")
    @Operation(
            summary = "Авторизация в eDocument Export Service",
            description = "Возвращает sessionId по токену клиента и версии сервиса. Если значения не переданы, используются"
                    + " дефолты из настроек приложения.",
            requestBody = @RequestBody(required = false, description = "Параметры авторизации",
                    content = @Content(schema = @Schema(implementation = EdocLogonRequest.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешный вход",
                            content = @Content(schema = @Schema(implementation = EdocSessionDto.class)))
            }
    )
    public EdocSessionDto logOn(@org.springframework.web.bind.annotation.RequestBody(required = false) EdocLogonRequest request) {
        String sessionId = sessionService.logOn(
                request != null ? request.getClientAuthenticationToken() : null,
                request != null ? request.getServiceVersion() : null
        );
        EdocSessionDto response = new EdocSessionDto();
        response.setSessionId(sessionId);
        return response;
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Завершение сессии", description = "Закрывает сессию по sessionId. Если не передан, используется"
            + " текущий активный sessionId из сервиса.",
            requestBody = @RequestBody(required = false, description = "Сессия для закрытия",
                    content = @Content(schema = @Schema(implementation = EdocSessionDto.class))),
            responses = {@ApiResponse(responseCode = "204", description = "Сессия закрыта")})
    public void logOut(@org.springframework.web.bind.annotation.RequestBody(required = false) EdocSessionDto request) {
        String sessionId = request != null && StringUtils.hasText(request.getSessionId())
                ? request.getSessionId()
                : null;
        sessionService.logOut(sessionId);
    }
}
