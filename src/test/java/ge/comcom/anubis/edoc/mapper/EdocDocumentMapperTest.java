package ge.comcom.anubis.edoc.mapper;

import ge.comcom.anubis.edoc.model.EdocDocumentDetailsDto;
import ge.comcom.anubis.edoc.model.EdocDocumentSummaryDto;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration_export.*;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration_datacontracts.DocumentStatuses;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration_datacontracts.DocumentTypes;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import javax.xml.datatype.DatatypeFactory;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EdocDocumentMapperTest {

    private final EdocDocumentMapper mapper = Mappers.getMapper(EdocDocumentMapper.class);

    @Test
    void mapsSummary() throws Exception {
        Document document = new Document();
        UUID id = UUID.randomUUID();
        document.setID(id.toString());
        document.setNumber("DOC-1");
        document.setCategory("General");
        document.setDocumentType(DocumentTypes.INCOMING);
        document.setDocumentStatus(DocumentStatuses.NEW);
        document.setRegistrationDate(DatatypeFactory.newInstance().newXMLGregorianCalendar("2024-01-01T00:00:00Z"));

        EdocDocumentSummaryDto dto = mapper.toSummary(document);
        assertEquals(id, dto.getId());
        assertEquals("DOC-1", dto.getNumber());
        assertEquals("INCOMING", dto.getDocumentType());
    }

    @Test
    void mapsDetailsWithFiles() throws Exception {
        DocumentData data = new DocumentData();
        data.setID(UUID.randomUUID().toString());
        data.setNumber("DOC-2");
        DocumentFile file = new DocumentFile();
        file.setName("file.txt");
        file.setContent("hello".getBytes());
        file.setFileType(org.datacontract.schemas._2004._07.fas_docmanagement_integration_datacontracts.DocumentFileTypes.MAIN);
        ArrayOfDocumentFile files = new ArrayOfDocumentFile();
        files.getDocumentFile().add(file);
        data.setFiles(files);

        RelatedDocument related = new RelatedDocument();
        DocumentInfo info = new DocumentInfo();
        info.setNumber("DOC-0");
        related.setDocument(info);
        ArrayOfRelatedDocument relatedArray = new ArrayOfRelatedDocument();
        relatedArray.getRelatedDocument().add(related);
        data.setRelatedDocuments(relatedArray);

        EdocDocumentDetailsDto dto = mapper.toDetails(data);
        assertEquals(1, dto.getFiles().size());
        assertEquals("file.txt", dto.getFiles().get(0).getName());
        assertEquals("DOC-0", dto.getRelatedDocuments().get(0).getDocumentNumber());
    }
}
