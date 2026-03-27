package ge.comcom.anubis.edoc.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "signature")
@Getter
@Setter
public class EdocSignatureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "process_id", nullable = false)
    private EdocProcessEntity process;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "creator_id")
    private EdocEmployeeEntity creator;

    @Column(name = "deadline")
    private OffsetDateTime deadline;

    @Column(name = "entry_date")
    private OffsetDateTime entryDate;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "signatory_id")
    private EdocEmployeeEntity signatory;

    @Column(name = "status")
    private String status;

    @Column(name = "status_change_date")
    private OffsetDateTime statusChangeDate;
}
