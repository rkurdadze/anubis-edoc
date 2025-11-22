package ge.comcom.anubis.edoc.mapper;

import ge.comcom.anubis.edoc.model.EdocOrganizationDto;
import ge.comcom.anubis.edoc.model.EdocPhysicalPersonDto;
import ge.comcom.anubis.edoc.model.EdocStateStructureDto;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration_export.Organization;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration_export.PhysicalPerson;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration_export.StateStructure;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EdocContactMapper {
    EdocPhysicalPersonDto toDto(PhysicalPerson person);

    List<EdocPhysicalPersonDto> toPhysicalList(List<PhysicalPerson> people);

    EdocOrganizationDto toDto(Organization org);

    List<EdocOrganizationDto> toOrganizationList(List<Organization> orgs);

    EdocStateStructureDto toDto(StateStructure structure);

    List<EdocStateStructureDto> toStructureList(List<StateStructure> structures);
}
