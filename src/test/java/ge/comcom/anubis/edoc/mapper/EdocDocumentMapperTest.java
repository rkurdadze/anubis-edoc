package ge.comcom.anubis.edoc.mapper;

import ge.comcom.anubis.edoc.model.EdocDocumentDetailsDto;
import ge.comcom.anubis.edoc.model.EdocDocumentSummaryDto;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.*;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.DocumentStatuses;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.DocumentTypes;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.ObjectFactory;
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
        ObjectFactory factory = new ObjectFactory();
        Document document = new Document();
        UUID id = UUID.randomUUID();
        document.setID(id.toString());
        document.setNumber(factory.createDocumentNumber("DOC-1"));
        document.setCategory(factory.createDocumentCategory("General"));
        document.setDocumentType(DocumentTypes.INCOMING);
        document.setDocumentStatus(DocumentStatuses.DRAFT);
        document.setRegistrationDate(DatatypeFactory.newInstance().newXMLGregorianCalendar("2024-01-01T00:00:00Z"));

        EdocDocumentSummaryDto dto = mapper.toSummary(document);
        assertEquals(id, dto.getId());
        assertEquals("DOC-1", dto.getNumber());
        assertEquals("Incoming", dto.getDocumentType());
    }

    @Test
    void mapsDetailsWithFiles() throws Exception {
        ObjectFactory factory = new ObjectFactory();
        DocumentData data = new DocumentData();
        data.setID(UUID.randomUUID().toString());
        data.setNumber(factory.createDocumentNumber("DOC-2"));
        DocumentFile file = new DocumentFile();
        file.setName(factory.createDocumentFileName("file.txt"));
        file.setContent(factory.createDocumentFileContent("hello".getBytes()));
        file.setFileType(org.datacontract.schemas._2004._07.fas_docmanagement_integration.DocumentFileTypes.MAIN);
        ArrayOfDocumentFile files = new ArrayOfDocumentFile();
        files.getDocumentFile().add(file);
        data.setFiles(factory.createDocumentDataFiles(files));

        RelatedDocument related = new RelatedDocument();
        DocumentInfo info = new DocumentInfo();
        info.setNumber(factory.createDocumentInfoNumber("DOC-0"));
        related.setDocument(factory.createRelatedDocumentDocument(info));
        ArrayOfRelatedDocument relatedArray = new ArrayOfRelatedDocument();
        relatedArray.getRelatedDocument().add(related);
        data.setRelatedDocuments(factory.createDocumentDataRelatedDocuments(relatedArray));

        EdocDocumentDetailsDto dto = mapper.toDetails(data);
        assertEquals(1, dto.getFiles().size());
        assertEquals("file.txt", dto.getFiles().get(0).getName());
        assertEquals("DOC-0", dto.getRelatedDocuments().get(0).getDocumentNumber());
    }
}
