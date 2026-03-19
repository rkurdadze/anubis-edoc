package ge.comcom.anubis.edoc.model;

import lombok.Data;
import java.util.UUID;

@Data
public class EdocRelatedDocumentDto {
    private UUID id;
    private String relationType;
    private String documentNumber;
}
