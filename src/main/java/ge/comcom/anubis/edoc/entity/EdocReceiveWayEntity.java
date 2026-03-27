package ge.comcom.anubis.edoc.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "receive_way")
@Getter
@Setter
public class EdocReceiveWayEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "document_id", nullable = false)
    private EdocCachedDocumentEntity document;

    @Column(name = "way")
    private String way;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;
}
