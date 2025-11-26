package ge.comcom.anubis.edoc.controller;

import ge.comcom.anubis.edoc.service.EdocSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/edoc/session")
@RequiredArgsConstructor
@Tag(name = "Session", description = "Управление сессией eDocument Export Service")
@Validated
public class EdocSessionController {

    private final EdocSessionService sessionService;

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Завершение сессии", description = "Закрывает активную сессию, идентификатор берётся автоматически",
            responses = {@ApiResponse(responseCode = "204", description = "Сессия закрыта")})
    public void logOut() {
        sessionService.logout();
    }
}
