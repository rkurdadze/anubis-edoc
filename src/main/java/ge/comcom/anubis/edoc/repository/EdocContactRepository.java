package ge.comcom.anubis.edoc.repository;

import ge.comcom.anubis.edoc.entity.EdocContactEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EdocContactRepository extends JpaRepository<EdocContactEntity, UUID> {
}
