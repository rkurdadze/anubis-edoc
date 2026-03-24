package ge.comcom.anubis.edoc.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "edoc_process")
@Getter
@Setter
public class EdocProcessEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_number")
    private Integer taskNumber;

    @Column(name = "task_start_date")
    private OffsetDateTime taskStartDate;

    @Column(name = "task_deadline")
    private OffsetDateTime taskDeadline;

    @Column(name = "task_text", columnDefinition = "TEXT")
    private String taskText;

    @Column(name = "task_is_initiated")
    private Boolean taskIsInitiated;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "task_initiated_by_id")
    private EdocEmployeeEntity taskInitiatedBy;

    @OneToMany(mappedBy = "process", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EdocResponsibleEntity> responsibles = new ArrayList<>();

    // PreparationProcess only
    @OneToMany(mappedBy = "process", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EdocSignatureEntity> signatures = new ArrayList<>();

    @OneToMany(mappedBy = "process", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EdocViseEntity> vises = new ArrayList<>();
}
