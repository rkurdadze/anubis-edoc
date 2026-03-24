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
    // Cache metadata — populated only by EdocCacheService, not from SOAP directly
    @Mapping(target = "fromCache", ignore = true)
    @Mapping(target = "cachedAt", ignore = true)
    @Mapping(target = "fetchCount", ignore = true)
    // Type-specific fields — populated only by EdocCacheService from typed subclasses
    @Mapping(target = "originalNumber", ignore = true)
    @Mapping(target = "originalDate", ignore = true)
    @Mapping(target = "senders", ignore = true)
    @Mapping(target = "addressees", ignore = true)
    @Mapping(target = "receiveWays", ignore = true)
    @Mapping(target = "employeeSenders", ignore = true)
    @Mapping(target = "employeeRecipients", ignore = true)
    @Mapping(target = "hasDigitalSignature", ignore = true)
    @Mapping(target = "hasDigitalStamp", ignore = true)
    @Mapping(target = "recipients", ignore = true)
    @Mapping(target = "signatories", ignore = true)
    @Mapping(target = "direction", ignore = true)
    @Mapping(target = "orderType", ignore = true)
    @Mapping(target = "outerRecipients", ignore = true)
    @Mapping(target = "innerRecipients", ignore = true)
    @Mapping(target = "relatedEmployees", ignore = true)
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
            if (info != null) {
                dto.setDocumentNumber(unwrap(info.getNumber()));
                
                // Extra safety for ID extraction in related documents
                Object rawId = info.getID();
                String idStr = null;
                if (rawId instanceof JAXBElement) {
                    idStr = (String) ((JAXBElement<?>) rawId).getValue();
                } else if (rawId instanceof String) {
                    idStr = (String) rawId;
                }
                
                if (idStr != null && !idStr.isEmpty()) {
                    dto.setId(toUuid(idStr));
                }
            }
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
        if (guid == null || guid.isEmpty() || guid.equals("null")) {
            return null;
        }
        return UUID.fromString(guid);
    }
}
