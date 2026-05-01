package ge.comcom.anubis.edoc.repository;

import ge.comcom.anubis.edoc.entity.EdocCachedDocumentEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface EdocCachedDocumentRepository extends JpaRepository<EdocCachedDocumentEntity, UUID> {
    @EntityGraph(attributePaths = {"contacts", "contacts.contact"})
    List<EdocCachedDocumentEntity> findByDocumentTypeAndRegistrationDateBetween(
            String documentType,
            OffsetDateTime from,
            OffsetDateTime to
    );
}
