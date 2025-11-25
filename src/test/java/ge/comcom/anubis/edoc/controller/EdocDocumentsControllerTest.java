package ge.comcom.anubis.edoc.controller;

import ge.comcom.anubis.edoc.model.EdocDocumentDetailsDto;
import ge.comcom.anubis.edoc.model.EdocDocumentSummaryDto;
import ge.comcom.anubis.edoc.service.EdocDocumentService;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.ContactTypes;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.DocumentTypes;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {EdocDocumentsController.class, EdocContactsController.class, RestExceptionHandler.class})
class EdocDocumentsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EdocDocumentService service;

    @Test
    void returnsDocuments() throws Exception {
        EdocDocumentSummaryDto dto = new EdocDocumentSummaryDto();
        dto.setNumber("DOC-1");
        Mockito.when(service.getDocuments(anyString(), any(DocumentTypes.class), any(), any(), any(ContactTypes.class), any()))
                .thenReturn(List.of(dto));

        String sessionId = UUID.randomUUID().toString();

        mockMvc.perform(get("/api/edoc/documents")
                        .param("sessionId", sessionId)
                        .param("type", DocumentTypes.INCOMING.value())
                        .param("from", LocalDate.of(2023, 1, 1).toString())
                        .param("to", LocalDate.of(2023, 12, 31).toString())
                        .param("contactType", ContactTypes.ORGANIZATION.value())
                        .param("contactId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value("DOC-1"));
    }

    @Test
    void returnsDocumentsWithoutContact() throws Exception {
        EdocDocumentSummaryDto dto = new EdocDocumentSummaryDto();
        dto.setNumber("DOC-3");
        Mockito.when(service.getDocuments(anyString(), any(DocumentTypes.class), any(), any(), isNull(), isNull()))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/edoc/documents")
                        .param("sessionId", UUID.randomUUID().toString())
                        .param("type", DocumentTypes.INTERNAL.value())
                        .param("from", LocalDate.of(2023, 1, 1).toString())
                        .param("to", LocalDate.of(2023, 2, 1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value("DOC-3"));
    }

    @Test
    void ignoresContactTypeWithoutId() throws Exception {
        EdocDocumentSummaryDto dto = new EdocDocumentSummaryDto();
        dto.setNumber("DOC-4");
        Mockito.when(service.getDocuments(anyString(), any(DocumentTypes.class), any(), any(), any(ContactTypes.class), isNull()))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/edoc/documents")
                        .param("sessionId", UUID.randomUUID().toString())
                        .param("type", DocumentTypes.ORDER.value())
                        .param("from", LocalDate.of(2024, 1, 1).toString())
                        .param("to", LocalDate.of(2024, 3, 1).toString())
                        .param("contactType", ContactTypes.ORGANIZATION.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value("DOC-4"));
    }

    @Test
    void returnsDocumentDetails() throws Exception {
        EdocDocumentDetailsDto dto = new EdocDocumentDetailsDto();
        dto.setNumber("DOC-2");
        UUID id = UUID.randomUUID();
        Mockito.when(service.getDocument(eq(id), anyBoolean())).thenReturn(dto);

        mockMvc.perform(get("/api/edoc/documents/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value("DOC-2"));
    }

    @Test
    void marksExported() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/api/edoc/documents/" + id + "/exported").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }
}
