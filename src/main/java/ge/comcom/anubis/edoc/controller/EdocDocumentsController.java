package ge.comcom.anubis.edoc.controller;

import ge.comcom.anubis.edoc.model.EdocDocumentDetailsDto;
import ge.comcom.anubis.edoc.model.EdocDocumentSummaryDto;
import ge.comcom.anubis.edoc.service.EdocDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.ContactTypes;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.DocumentTypes;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/edoc/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Методы экспорта документов eDocument")
@Validated
public class EdocDocumentsController {

    private final EdocDocumentService documentService;

    @GetMapping
    @Operation(summary = "Получить список документов",
            description = "Возвращает документы по типу и периоду регистрации",
            responses = @ApiResponse(responseCode = "200", description = "Список документов",
                    content = @Content(schema = @Schema(implementation = EdocDocumentSummaryDto.class))))
    public List<EdocDocumentSummaryDto> getDocuments(
            @Parameter(description = "Тип документа", example = "Incoming")
            @RequestParam(name = "type", defaultValue = "NONE") DocumentTypes type,
            @Parameter(description = "Дата с", example = "2023-01-01")
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Дата по", example = "2023-12-31")
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Тип связанного контакта: PhysicalPerson/Organization/StateStructure", example = "Organization")
            @RequestParam(name = "contactType", required = false) ContactTypes contactType,
            @Parameter(description = "ID связанного контакта (GUID)", example = "15f5b4a7-7ad6-4de6-9a8f-fb39c5ba6c44")
            @RequestParam(name = "contactId", required = false) UUID contactId) {
        return documentService.getDocuments(type, from, to, contactType, contactId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить детальную информацию по документу",
            description = "Возвращает DocumentData/Incoming/Outgoing/Internal/Order в зависимости от типа",
            responses = @ApiResponse(responseCode = "200", description = "Детали документа",
                    content = @Content(schema = @Schema(implementation = EdocDocumentDetailsDto.class))))
    public EdocDocumentDetailsDto getDocument(@Parameter(description = "Идентификатор документа") @PathVariable("id") UUID id,
                                              @Parameter(description = "Вернуть полные данные связанных процессов", example = "true")
                                              @RequestParam(name = "full", defaultValue = "true") boolean full) {
        return documentService.getDocument(id, full);
    }

    @PostMapping("/{id}/exported")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Подтвердить успешный экспорт", responses = {
            @ApiResponse(responseCode = "204", description = "Статус изменён")
    })
    public void setExported(@Parameter(description = "Идентификатор документа") @PathVariable("id") UUID id) {
        documentService.setDocumentExported(id);
    }
}
