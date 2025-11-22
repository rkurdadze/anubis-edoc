package ge.comcom.anubis.edoc.controller;

import ge.comcom.anubis.edoc.model.EdocDocumentDetailsDto;
import ge.comcom.anubis.edoc.model.EdocDocumentSummaryDto;
import ge.comcom.anubis.edoc.service.EdocDocumentService;
import lombok.RequiredArgsConstructor;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.DocumentTypes;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/edoc/documents")
@RequiredArgsConstructor
public class EdocDocumentsController {

    private final EdocDocumentService documentService;

    @GetMapping
    public List<EdocDocumentSummaryDto> getDocuments(
            @RequestParam(name = "type", defaultValue = "NONE") DocumentTypes type,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return documentService.getDocuments(type, from, to);
    }

    @GetMapping("/{id}")
    public EdocDocumentDetailsDto getDocument(@PathVariable("id") UUID id,
                                              @RequestParam(name = "full", defaultValue = "true") boolean full) {
        return documentService.getDocument(id, full);
    }

    @PostMapping("/{id}/exported")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setExported(@PathVariable("id") UUID id) {
        documentService.setDocumentExported(id);
    }
}
