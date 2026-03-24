package ge.comcom.anubis.edoc.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "edoc_document_file")
@Getter
@Setter
public class EdocDocumentFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "document_id", nullable = false)
    private EdocCachedDocumentEntity document;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "name")
    private String name;

    @Column(name = "content", columnDefinition = "BYTEA")
    private byte[] content;
}
