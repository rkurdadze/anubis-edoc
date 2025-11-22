package ge.comcom.anubis.edoc.model;

import lombok.Data;

@Data
public class EdocDocumentFileDto {
    private String name;
    private String fileType;
    private String contentBase64;
}
