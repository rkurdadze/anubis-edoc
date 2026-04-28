package ge.comcom.anubis.edoc.repository;

import ge.comcom.anubis.edoc.entity.EdocEmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EdocEmployeeRepository extends JpaRepository<EdocEmployeeEntity, Long> {

    @Query(value = """
            select *
            from employee e
            where coalesce(lower(trim(e.first_name)), '') = coalesce(lower(trim(:firstName)), '')
              and coalesce(lower(trim(e.last_name)), '') = coalesce(lower(trim(:lastName)), '')
              and coalesce(lower(trim(e.position)), '') = coalesce(lower(trim(:position)), '')
              and coalesce(lower(trim(e.organization_structure)), '') = coalesce(lower(trim(:organizationStructure)), '')
            order by e.id
            limit 1
            """, nativeQuery = true)
    Optional<EdocEmployeeEntity> findCanonical(
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("position") String position,
            @Param("organizationStructure") String organizationStructure
    );
}

