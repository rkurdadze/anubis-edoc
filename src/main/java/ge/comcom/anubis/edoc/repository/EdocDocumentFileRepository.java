package ge.comcom.anubis.edoc.repository;

import ge.comcom.anubis.edoc.entity.EdocDocumentFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EdocDocumentFileRepository extends JpaRepository<EdocDocumentFileEntity, Long> {
    Optional<EdocDocumentFileEntity> findByIdAndDocumentId(Long id, UUID documentId);
}
