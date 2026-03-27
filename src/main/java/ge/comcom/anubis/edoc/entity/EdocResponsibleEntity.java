package ge.comcom.anubis.edoc.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "responsible")
@Getter
@Setter
public class EdocResponsibleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "process_id", nullable = false)
    private EdocProcessEntity process;

    /**
     * Parent responsible ID — stored as plain column (no JPA navigation to avoid ordering issues on delete).
     * Populated for child responsibles (ChildResponsibles in eDocument API).
     */
    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "start_date")
    private OffsetDateTime startDate;

    @Column(name = "deadline")
    private OffsetDateTime deadline;

    @Column(name = "task", columnDefinition = "TEXT")
    private String task;

    @Column(name = "status")
    private String status;

    @Column(name = "status_change_date")
    private OffsetDateTime statusChangeDate;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "employee_id")
    private EdocEmployeeEntity employee;
}
