package ge.comcom.anubis.edoc.controller;

import ge.comcom.anubis.edoc.model.EdocDocumentDetailsDto;
import ge.comcom.anubis.edoc.model.EdocDocumentSummaryDto;
import ge.comcom.anubis.edoc.service.EdocDocumentService;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration_datacontracts.DocumentTypes;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
        Mockito.when(service.getDocuments(any(DocumentTypes.class), any(), any())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/edoc/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value("DOC-1"));
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
