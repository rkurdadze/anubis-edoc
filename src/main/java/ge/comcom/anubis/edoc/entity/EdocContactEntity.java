package ge.comcom.anubis.edoc.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "contact")
@Getter
@Setter
public class EdocContactEntity {

    @Id
    private UUID id;

    @Column(name = "contact_type", nullable = false)
    private String contactType;

    // PhysicalPerson
    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "personal_number")
    private String personalNumber;

    // Organization
    @Column(name = "identification_number")
    private String identificationNumber;

    @Column(name = "juridical_form")
    private String juridicalForm;

    // Organization / StateStructure
    @Column(name = "name")
    private String name;
}
