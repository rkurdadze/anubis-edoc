package ge.comcom.anubis.edoc.model;

import lombok.Data;

import java.util.UUID;

@Data
public class EdocContactDto {
    private UUID id;
    private String contactType;
    // PhysicalPerson
    private String firstName;
    private String lastName;
    private String personalNumber;
    // Organization
    private String identificationNumber;
    private String juridicalForm;
    // Organization / StateStructure
    private String name;
}
