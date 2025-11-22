package ge.comcom.anubis.edoc.controller;

import ge.comcom.anubis.edoc.model.EdocOrganizationDto;
import ge.comcom.anubis.edoc.model.EdocPhysicalPersonDto;
import ge.comcom.anubis.edoc.model.EdocStateStructureDto;
import ge.comcom.anubis.edoc.service.EdocDocumentService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/edoc/contacts")
@RequiredArgsConstructor
public class EdocContactsController {

    private final EdocDocumentService documentService;

    @GetMapping("/physical/by-personalNumber")
    public List<EdocPhysicalPersonDto> getPhysicalByPersonalNumber(@RequestParam @NotBlank String personalNumber) {
        return documentService.getPhysicalByPersonalNumber(personalNumber);
    }

    @GetMapping("/physical/by-name")
    public List<EdocPhysicalPersonDto> getPhysicalByName(@RequestParam @NotBlank String lastName,
                                                         @RequestParam(required = false) String firstName) {
        return documentService.getPhysicalByName(lastName, firstName);
    }

    @GetMapping("/organizations/by-identificationNumber")
    public List<EdocOrganizationDto> getOrganizationsByIdentification(@RequestParam @NotBlank String identificationNumber) {
        return documentService.getOrganizationsByIdentificationNumber(identificationNumber);
    }

    @GetMapping("/organizations/by-name")
    public List<EdocOrganizationDto> getOrganizationsByName(@RequestParam @NotBlank String name) {
        return documentService.getOrganizationsByName(name);
    }

    @GetMapping("/stateStructures")
    public List<EdocStateStructureDto> getStateStructures(@RequestParam(required = false) String name) {
        return documentService.getStateStructures(name);
    }
}
