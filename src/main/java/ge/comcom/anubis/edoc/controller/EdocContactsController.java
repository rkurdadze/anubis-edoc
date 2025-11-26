package ge.comcom.anubis.edoc.controller;

import ge.comcom.anubis.edoc.model.EdocOrganizationDto;
import ge.comcom.anubis.edoc.model.EdocPhysicalPersonDto;
import ge.comcom.anubis.edoc.model.EdocStateStructureDto;
import ge.comcom.anubis.edoc.service.EdocDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/edoc/contacts")
@RequiredArgsConstructor
@Tag(name = "Contacts", description = "Поиск физических лиц, организаций и госструктур")
@Validated
public class EdocContactsController {

    private final EdocDocumentService documentService;

    @GetMapping("/physical/by-personalNumber")
    @Operation(summary = "Поиск физлиц по личному номеру",
            responses = @ApiResponse(responseCode = "200", description = "Список граждан",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = EdocPhysicalPersonDto.class)))))
    public List<EdocPhysicalPersonDto> getPhysicalByPersonalNumber(@Parameter(description = "11-значный персональный номер", example = "12345678901") @RequestParam @NotBlank String personalNumber,
                                                                  @Parameter(description = "sessionId, если уже получен методом LogOn", example = "f6c06169-2991-47db-b024-771031f3197b")
                                                                  @RequestParam(name = "sessionId", required = false) String sessionId) {
        return documentService.getPhysicalByPersonalNumber(personalNumber, sessionId);
    }

    @GetMapping("/physical/by-name")
    @Operation(summary = "Поиск физлиц по ФИО",
            responses = @ApiResponse(responseCode = "200", description = "Список граждан",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = EdocPhysicalPersonDto.class)))))
    public List<EdocPhysicalPersonDto> getPhysicalByName(@Parameter(description = "Фамилия", example = "Иванов") @RequestParam @NotBlank String lastName,
                                                         @Parameter(description = "Имя", example = "Иван") @RequestParam(required = false) String firstName,
                                                         @Parameter(description = "sessionId, если уже получен методом LogOn", example = "f6c06169-2991-47db-b024-771031f3197b")
                                                         @RequestParam(name = "sessionId", required = false) String sessionId) {
        return documentService.getPhysicalByName(lastName, firstName, sessionId);
    }

    @GetMapping("/organizations/by-identificationNumber")
    @Operation(summary = "Поиск организаций по идентификатору",
            responses = @ApiResponse(responseCode = "200", description = "Список организаций",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = EdocOrganizationDto.class)))))
    public List<EdocOrganizationDto> getOrganizationsByIdentification(@Parameter(description = "Идентификационный номер", example = "123456789") @RequestParam @NotBlank String identificationNumber,
                                                                      @Parameter(description = "sessionId, если уже получен методом LogOn", example = "f6c06169-2991-47db-b024-771031f3197b")
                                                                      @RequestParam(name = "sessionId", required = false) String sessionId) {
        return documentService.getOrganizationsByIdentificationNumber(identificationNumber, sessionId);
    }

    @GetMapping("/organizations/by-name")
    @Operation(summary = "Поиск организаций по названию",
            responses = @ApiResponse(responseCode = "200", description = "Список организаций",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = EdocOrganizationDto.class)))))
    public List<EdocOrganizationDto> getOrganizationsByName(@Parameter(description = "Название организации") @RequestParam @NotBlank String name,
                                                            @Parameter(description = "sessionId, если уже получен методом LogOn", example = "f6c06169-2991-47db-b024-771031f3197b")
                                                            @RequestParam(name = "sessionId", required = false) String sessionId) {
        return documentService.getOrganizationsByName(name, sessionId);
    }

    @GetMapping("/stateStructures")
    @Operation(summary = "Поиск государственных структур",
            responses = @ApiResponse(responseCode = "200", description = "Список госструктур",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = EdocStateStructureDto.class)))))
    public List<EdocStateStructureDto> getStateStructures(@Parameter(description = "Название госструктуры") @RequestParam(required = false) String name,
                                                         @Parameter(description = "sessionId, если уже получен методом LogOn", example = "f6c06169-2991-47db-b024-771031f3197b")
                                                         @RequestParam(name = "sessionId", required = false) String sessionId) {
        return documentService.getStateStructures(name, sessionId);
    }
}
