package ge.comcom.anubis.edoc.controller;

import ge.comcom.anubis.edoc.model.EdocCacheStatusDto;
import ge.comcom.anubis.edoc.model.EdocDataSource;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
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
    @Operation(
            summary = "Получить список документов",
            description = "Возвращает документы по типу и периоду регистрации. " +
                    "Запрос всегда идёт на удалённый сервис — счётчик чтения НЕ расходуется.",
            responses = @ApiResponse(responseCode = "200", description = "Список документов",
                    content = @Content(schema = @Schema(implementation = EdocDocumentSummaryDto.class))))
    public List<EdocDocumentSummaryDto> getDocuments(
            @Parameter(description = "Тип документа",
                    schema = @Schema(allowableValues = {"INCOMING", "OUTGOING", "INTERNAL", "ORDER"}))
            @RequestParam(name = "type") DocumentTypes type,
            @Parameter(description = "Дата с", example = "2025-01-01")
            @RequestParam(name = "from", defaultValue = "2025-01-01")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Дата по", example = "2025-12-31")
            @RequestParam(name = "to", defaultValue = "2025-12-31")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Источник данных: local (только кэш) или remote")
            @RequestParam(name = "source", defaultValue = "remote") EdocDataSource source,
            @Parameter(description = "Тип связанного контакта")
            @RequestParam(name = "contactType", required = false) ContactTypes contactType,
            @Parameter(description = "ID связанного контакта (GUID)")
            @RequestParam(name = "contactId", required = false) String contactId) {
        if (source == EdocDataSource.local) {
            return documentService.getDocumentsFromCache(type, from, to, contactType, contactId);
        }
        ContactTypes normalizedContactType = contactType;
        UUID parsedContactId = null;
        if (contactType != null && contactId != null && !contactId.isBlank()) {
            try {
                parsedContactId = UUID.fromString(contactId.trim());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contactId must be UUID for remote source");
            }
        } else {
            normalizedContactType = null;
        }
        return documentService.getDocumentsRemote(type, from, to, normalizedContactType, parsedContactId);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Получить детали документа из локального кэша",
            description = "Возвращает детали документа ТОЛЬКО из локальной базы данных. " +
                    "Кэшируются исключительно документы со статусом Completed. " +
                    "Если документ отсутствует в кэше — 404. " +
                    "Для принудительной загрузки с удалённого сервера используйте POST /{id}/fetch.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Детали документа из кэша",
                            content = @Content(schema = @Schema(implementation = EdocDocumentDetailsDto.class))),
                    @ApiResponse(responseCode = "404", description = "Документ не найден в кэше")
            })
    public EdocDocumentDetailsDto getDocument(
            @Parameter(description = "Идентификатор документа") @PathVariable("id") UUID id) {
        return documentService.getDocument(id);
    }

    @PostMapping("/{id}/fetch")
    @Operation(
            summary = "Загрузить документ с удалённого сервера (ручное подтверждение)",
            description = "Отправляет запрос GetDocument на удалённый eDocument сервис. " +
                    "РАСХОДУЕТ один цикл чтения (лимит 3 на документ). " +
                    "Должен вызываться ТОЛЬКО после явного подтверждения пользователя в интерфейсе. " +
                    "Если статус документа Completed — результат сохраняется в локальном кэше. " +
                    "Последующие вызовы GET /{id} будут возвращать данные из кэша без расхода цикла.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Детали документа"),
                    @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
                    @ApiResponse(responseCode = "401", description = "Ошибка аутентификации"),
                    @ApiResponse(responseCode = "502", description = "Ошибка связи с удалённым сервисом")
            })
    public EdocDocumentDetailsDto fetchDocument(
            @Parameter(description = "Идентификатор документа") @PathVariable("id") UUID id) {
        return documentService.fetchDocument(id);
    }

    @GetMapping("/{id}/files/{fileId}/content")
    @Operation(summary = "Скачать содержимое файла документа из кэша")
    public ResponseEntity<byte[]> getFileContent(
            @PathVariable("id") UUID id,
            @PathVariable("fileId") Long fileId) {
        byte[] content = documentService.getFileContent(id, fileId);
        String filename = documentService.getFileName(id, fileId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, buildAttachmentDisposition(filename));
        return ResponseEntity.ok().headers(headers).body(content);
    }

    private String buildAttachmentDisposition(String filename) {
        String safeName = (filename == null || filename.isBlank()) ? "file" : filename;
        String asciiFallback = toAsciiFilename(safeName);
        String encodedUtf8 = UriUtils.encode(safeName, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return "attachment; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + encodedUtf8;
    }

    private String toAsciiFilename(String filename) {
        String ascii = filename.replaceAll("[^\\x20-\\x7E]", "_")
                .replace("\"", "")
                .replace("\\\\", "_");
        if (ascii.isBlank()) {
            return "file";
        }
        return ascii;
    }

    @PostMapping("/{id}/exported")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Пометить документ как экспортированный",
            description = "Обновляет статус экспорта документа на 'Exported' в локальном кэше. " +
                    "Не обращается к удалённому сервису eDocument. " +
                    "Если документ не закэширован — операция игнорируется.")
    public void markExported(
            @Parameter(description = "Идентификатор документа") @PathVariable("id") UUID id) {
        documentService.markExported(id);
    }

    @GetMapping("/{id}/cache-status")
    @Operation(
            summary = "Проверить статус кэша документа",
            description = "Возвращает информацию о том, закэширован ли документ локально, " +
                    "и метаданные кэша (дата, количество загрузок). Не обращается к удалённому сервису.",
            responses = @ApiResponse(responseCode = "200", description = "Статус кэша"))
    public EdocCacheStatusDto getCacheStatus(
            @Parameter(description = "Идентификатор документа") @PathVariable("id") UUID id) {
        return documentService.getCacheStatus(id);
    }
}
