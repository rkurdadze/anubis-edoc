package ge.comcom.anubis.edoc.model;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class EdocDocumentSummaryDto {
    private UUID id;
    private String number;
    private String category;
    private String documentType;
    private String documentStatus;
    private String exportStatus;
    private OffsetDateTime registrationDate;
    private OffsetDateTime deadline;
}
