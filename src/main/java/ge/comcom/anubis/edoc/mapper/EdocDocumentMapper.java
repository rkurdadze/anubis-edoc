package ge.comcom.anubis.edoc.mapper;

import ge.comcom.anubis.edoc.model.EdocDocumentDetailsDto;
import ge.comcom.anubis.edoc.model.EdocDocumentFileDto;
import ge.comcom.anubis.edoc.model.EdocDocumentSummaryDto;
import ge.comcom.anubis.edoc.model.EdocRelatedDocumentDto;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration_export.Document;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration_export.DocumentData;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration_export.DocumentFile;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration_export.RelatedDocument;
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

    @Mapping(target = "id", source = "id", qualifiedByName = "toUuid")
    @Mapping(target = "registrationDate", source = "registrationDate", qualifiedByName = "toOffset")
    @Mapping(target = "deadline", source = "deadline", qualifiedByName = "toOffset")
    EdocDocumentSummaryDto toSummary(Document document);

    List<EdocDocumentSummaryDto> toSummaryList(List<Document> documents);

    @Mapping(target = "id", source = "id", qualifiedByName = "toUuid")
    @Mapping(target = "registrationDate", source = "registrationDate", qualifiedByName = "toOffset")
    @Mapping(target = "deadline", source = "deadline", qualifiedByName = "toOffset")
    @Mapping(target = "files", expression = "java(mapFiles(data.getFiles() != null ? data.getFiles().getDocumentFile() : null))")
    @Mapping(target = "relatedDocuments", expression = "java(mapRelated(data.getRelatedDocuments() != null ? data.getRelatedDocuments().getRelatedDocument() : null))")
    EdocDocumentDetailsDto toDetails(DocumentData data);

    default List<EdocDocumentFileDto> mapFiles(List<DocumentFile> files) {
        if (files == null) {
            return List.of();
        }
        return files.stream().map(file -> {
            EdocDocumentFileDto dto = new EdocDocumentFileDto();
            dto.setName(file.getName());
            dto.setFileType(file.getFileType() != null ? file.getFileType().value() : null);
            dto.setContentBase64(file.getContent() != null ? java.util.Base64.getEncoder().encodeToString(file.getContent()) : null);
            return dto;
        }).collect(Collectors.toList());
    }

    default List<EdocRelatedDocumentDto> mapRelated(List<RelatedDocument> related) {
        if (related == null) {
            return List.of();
        }
        return related.stream().map(item -> {
            EdocRelatedDocumentDto dto = new EdocRelatedDocumentDto();
            dto.setRelationType(item.getRelationType() != null ? item.getRelationType().value() : null);
            dto.setDocumentNumber(item.getDocument() != null ? item.getDocument().getNumber() : null);
            return dto;
        }).collect(Collectors.toList());
    }

    @Named("toOffset")
    default OffsetDateTime toOffset(XMLGregorianCalendar calendar) {
        if (calendar == null) {
            return null;
        }
        return calendar.toGregorianCalendar().toZonedDateTime().toOffsetDateTime();
    }

    @Named("toUuid")
    default UUID toUuid(String guid) {
        if (guid == null) {
            return null;
        }
        return UUID.fromString(guid);
    }
}
