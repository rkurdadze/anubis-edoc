package ge.comcom.anubis.edoc.mapper;

import ge.comcom.anubis.edoc.model.EdocDocumentDetailsDto;
import ge.comcom.anubis.edoc.model.EdocDocumentFileDto;
import ge.comcom.anubis.edoc.model.EdocDocumentSummaryDto;
import ge.comcom.anubis.edoc.model.EdocRelatedDocumentDto;
import jakarta.xml.bind.JAXBElement;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.ArrayOfDocumentFile;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.ArrayOfRelatedDocument;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.Document;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.DocumentData;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.DocumentFile;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.DocumentInfo;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.RelatedDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface EdocDocumentMapper {

    @Mapping(target = "id", source = "ID", qualifiedByName = "toUuid")
    @Mapping(target = "registrationDate", source = "registrationDate", qualifiedByName = "toOffset")
    @Mapping(target = "deadline", source = "deadline", qualifiedByName = "toOffset")
    @Mapping(target = "number", source = "number", qualifiedByName = "unwrapString")
    @Mapping(target = "category", source = "category", qualifiedByName = "unwrapString")
    @Mapping(target = "documentType", expression = "java(document.getDocumentType() != null ? document.getDocumentType().value() : null)")
    @Mapping(target = "documentStatus", expression = "java(document.getDocumentStatus() != null ? document.getDocumentStatus().value() : null)")
    @Mapping(target = "exportStatus", expression = "java(document.getExportStatus() != null ? document.getExportStatus().value() : null)")
    EdocDocumentSummaryDto toSummary(Document document);

    List<EdocDocumentSummaryDto> toSummaryList(List<Document> documents);

    @Mapping(target = "id", source = "ID", qualifiedByName = "toUuid")
    @Mapping(target = "registrationDate", source = "registrationDate", qualifiedByName = "toOffset")
    @Mapping(target = "deadline", source = "deadline", qualifiedByName = "toOffset")
    @Mapping(target = "files", expression = "java(mapFiles(data.getFiles()))")
    @Mapping(target = "relatedDocuments", expression = "java(mapRelated(data.getRelatedDocuments()))")
    @Mapping(target = "number", source = "number", qualifiedByName = "unwrapString")
    @Mapping(target = "category", source = "category", qualifiedByName = "unwrapString")
    @Mapping(target = "documentType", expression = "java(data.getDocumentType() != null ? data.getDocumentType().value() : null)")
    @Mapping(target = "documentStatus", expression = "java(data.getDocumentStatus() != null ? data.getDocumentStatus().value() : null)")
    @Mapping(target = "exportStatus", expression = "java(data.getExportStatus() != null ? data.getExportStatus().value() : null)")
    @Mapping(target = "annotation", source = "annotation", qualifiedByName = "unwrapString")
    @Mapping(target = "comments", source = "comments", qualifiedByName = "unwrapString")
    @Mapping(target = "chancellary", source = "chancellary", qualifiedByName = "unwrapString")
    @Mapping(target = "purpose", expression = "java(data.getPurpose() != null ? data.getPurpose().value() : null)")
    EdocDocumentDetailsDto toDetails(DocumentData data);

    default List<EdocDocumentFileDto> mapFiles(JAXBElement<ArrayOfDocumentFile> filesElement) {
        ArrayOfDocumentFile files = unwrap(filesElement);
        if (files == null || files.getDocumentFile() == null) {
            return List.of();
        }
        return files.getDocumentFile().stream().map(file -> {
            EdocDocumentFileDto dto = new EdocDocumentFileDto();
            dto.setName(unwrap(file.getName()));
            dto.setFileType(file.getFileType() != null ? file.getFileType().value() : null);
            byte[] content = unwrap(file.getContent());
            dto.setContentBase64(content != null ? java.util.Base64.getEncoder().encodeToString(content) : null);
            return dto;
        }).collect(Collectors.toList());
    }

    default List<EdocRelatedDocumentDto> mapRelated(JAXBElement<ArrayOfRelatedDocument> relatedElement) {
        ArrayOfRelatedDocument related = unwrap(relatedElement);
        if (related == null || related.getRelatedDocument() == null) {
            return List.of();
        }
        return related.getRelatedDocument().stream().map(item -> {
            EdocRelatedDocumentDto dto = new EdocRelatedDocumentDto();
            dto.setRelationType(item.getRelationType() != null ? item.getRelationType().value() : null);
            DocumentInfo info = unwrap(item.getDocument());
            dto.setDocumentNumber(info != null ? unwrap(info.getNumber()) : null);
            return dto;
        }).collect(Collectors.toList());
    }

    default <T> T unwrap(JAXBElement<T> element) {
        return element != null ? element.getValue() : null;
    }

    @Named("unwrapString")
    default String unwrapString(JAXBElement<String> element) {
        return unwrap(element);
    }

    @Named("toOffset")
    default OffsetDateTime toOffset(XMLGregorianCalendar calendar) {
        if (calendar == null) {
            return null;
        }
        return calendar.toGregorianCalendar().toZonedDateTime().toOffsetDateTime();
    }

    @Named("toOffset")
    default OffsetDateTime toOffset(JAXBElement<XMLGregorianCalendar> calendarElement) {
        if (calendarElement == null || calendarElement.getValue() == null) {
            return null;
        }
        return toOffset(calendarElement.getValue());
    }

    @Named("toUuid")
    default UUID toUuid(String guid) {
        if (guid == null) {
            return null;
        }
        return UUID.fromString(guid);
    }
}
