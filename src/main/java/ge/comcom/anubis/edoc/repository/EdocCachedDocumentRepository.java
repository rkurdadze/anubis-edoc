package ge.comcom.anubis.edoc.repository;

import ge.comcom.anubis.edoc.entity.EdocCachedDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EdocCachedDocumentRepository extends JpaRepository<EdocCachedDocumentEntity, UUID> {
}
