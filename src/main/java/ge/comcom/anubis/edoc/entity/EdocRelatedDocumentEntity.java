package ge.comcom.anubis.edoc.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "edoc_related_document")
@Getter
@Setter
public class EdocRelatedDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "document_id", nullable = false)
    private EdocCachedDocumentEntity document;

    @Column(name = "relation_type")
    private String relationType;

    @Column(name = "related_doc_id")
    private UUID relatedDocId;

    @Column(name = "related_doc_number")
    private String relatedDocNumber;

    @Column(name = "related_doc_type")
    private String relatedDocType;

    @Column(name = "related_doc_registration_date")
    private OffsetDateTime relatedDocRegistrationDate;

    @Column(name = "related_doc_export_status")
    private String relatedDocExportStatus;
}
