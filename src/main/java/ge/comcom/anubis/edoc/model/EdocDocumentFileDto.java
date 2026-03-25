package ge.comcom.anubis.edoc.model;

import lombok.Data;

@Data
public class EdocDocumentFileDto {
    /** DB row id — used to fetch content via /files/{fileId}/content */
    private Long id;
    private String name;
    private String fileType;
    /** Size in bytes (filled from DB, null for SOAP-direct responses) */
    private Long size;
}
