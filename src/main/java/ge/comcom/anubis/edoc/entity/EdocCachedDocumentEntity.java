package ge.comcom.anubis.edoc.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Merged entity for ALL document types (Incoming/Internal/Outgoing/Order).
 * Type-specific columns are nullable and populated only for the relevant DocumentType.
 */
@Entity
@Table(name = "edoc_document")
@Getter
@Setter
public class EdocCachedDocumentEntity {

    @Id
    private UUID id;

    // --- Document (base) ---
    @Column(name = "document_type", nullable = false)
    private String documentType;

    @Column(name = "number")
    private String number;

    @Column(name = "registration_date", nullable = false)
    private OffsetDateTime registrationDate;

    @Column(name = "deadline")
    private OffsetDateTime deadline;

    @Column(name = "category")
    private String category;

    @Column(name = "document_status")
    private String documentStatus;

    @Column(name = "export_status")
    private String exportStatus;

    @Column(name = "issue_decision_type")
    private String issueDecisionType;

    // --- DocumentData ---
    @Column(name = "annotation", columnDefinition = "TEXT")
    private String annotation;

    @Column(name = "chancellary")
    private String chancellary;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "purpose")
    private String purpose;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "result_process_id")
    private EdocProcessEntity resultProcess;

    // --- IncomingDocumentData ---
    @Column(name = "original_number")
    private String originalNumber;

    @Column(name = "original_date")
    private OffsetDateTime originalDate;

    // --- Internal / Outgoing / Order ---
    @Column(name = "has_digital_signature")
    private Boolean hasDigitalSignature;

    @Column(name = "has_digital_stamp")
    private Boolean hasDigitalStamp;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "preparation_process_id")
    private EdocProcessEntity preparationProcess;

    // --- OrderDocumentData ---
    @Column(name = "direction")
    private String direction;

    @Column(name = "order_type")
    private String orderType;

    // --- Cache metadata ---
    @Column(name = "cached_at", nullable = false)
    private OffsetDateTime cachedAt;

    @Column(name = "fetch_count", nullable = false)
    private int fetchCount;

    @Column(name = "last_fetched_at", nullable = false)
    private OffsetDateTime lastFetchedAt;

    // --- Relations ---
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EdocDocumentFileEntity> files = new ArrayList<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EdocRelatedDocumentEntity> relatedDocuments = new ArrayList<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EdocReceiveWayEntity> receiveWays = new ArrayList<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EdocDocumentEmployeeEntity> employees = new ArrayList<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EdocDocumentContactEntity> contacts = new ArrayList<>();
}
