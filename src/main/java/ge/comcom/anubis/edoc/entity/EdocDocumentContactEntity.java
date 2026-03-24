package ge.comcom.anubis.edoc.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Contact participants for documents.
 * Role values: INCOMING_SENDER, OUTGOING_RECIPIENT, ORDER_OUTER_RECIPIENT
 */
@Entity
@Table(name = "edoc_document_contact")
@Getter
@Setter
public class EdocDocumentContactEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "document_id", nullable = false)
    private EdocCachedDocumentEntity document;

    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "contact_id", nullable = false)
    private EdocContactEntity contact;

    @Column(name = "role", nullable = false)
    private String role;
}
