package ge.comcom.anubis.edoc.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Employee participants for documents.
 * Role values: INCOMING_ADDRESSEE, INTERNAL_SENDER, INTERNAL_RECIPIENT,
 *              OUTGOING_SIGNATORY, ORDER_INNER_RECIPIENT, ORDER_RELATED_EMPLOYEE, ORDER_SIGNATORY
 */
@Entity
@Table(name = "document_employee")
@Getter
@Setter
public class EdocDocumentEmployeeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "document_id", nullable = false)
    private EdocCachedDocumentEntity document;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "employee_id", nullable = false)
    private EdocEmployeeEntity employee;

    @Column(name = "role", nullable = false)
    private String role;
}
