package ge.comcom.anubis.edoc.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "vise")
@Getter
@Setter
public class EdocViseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "process_id", nullable = false)
    private EdocProcessEntity process;

    /**
     * Parent vise ID — stored as plain column (no JPA navigation to avoid ordering issues on delete).
     * Populated for child vises (ChildVises in eDocument API).
     */
    @Column(name = "parent_id")
    private Long parentId;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "author_id")
    private EdocEmployeeEntity author;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "creator_id")
    private EdocEmployeeEntity creator;

    @Column(name = "deadline")
    private OffsetDateTime deadline;

    @Column(name = "entry_date")
    private OffsetDateTime entryDate;

    @Column(name = "status")
    private String status;

    @Column(name = "status_change_date")
    private OffsetDateTime statusChangeDate;
}
