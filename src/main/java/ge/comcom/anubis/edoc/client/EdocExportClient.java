package ge.comcom.anubis.edoc.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.DatePeriod;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration_export.Contact;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration_export.Document;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration_export.DocumentData;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration_export.ArrayOfDocument;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration_export.ArrayOfOrganization;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration_export.ArrayOfPhysicalPerson;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration_export.ArrayOfStateStructure;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration_export.Organization;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration_export.PhysicalPerson;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration_export.StateStructure;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration_datacontracts.DocumentTypes;
import org.tempuri.IeDocumentExportService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class EdocExportClient {

    private final IeDocumentExportService service;

    public String logOn(String token, String version) {
        return service.logOn(token, version);
    }

    public void logOut(String sessionId) {
        service.logOut(sessionId);
    }

    public List<Document> getDocuments(String sessionId, DocumentTypes type, DatePeriod period, Contact contact) {
        ArrayOfDocument response = service.getDocuments(sessionId, type, period, contact);
        return Optional.ofNullable(response).map(ArrayOfDocument::getDocument).orElse(Collections.emptyList());
    }

    public DocumentData getDocument(String sessionId, String documentId, boolean full) {
        return service.getDocument(sessionId, documentId, full);
    }

    public void setDocumentExported(String sessionId, String documentId) {
        service.setDocumentExported(sessionId, documentId);
    }

    public List<PhysicalPerson> getPhysicalPersonsByPersonalNumber(String sessionId, String personalNumber) {
        ArrayOfPhysicalPerson response = service.getPhysicalPersonsByPersonalNumber(sessionId, personalNumber);
        return Optional.ofNullable(response).map(ArrayOfPhysicalPerson::getPhysicalPerson).orElse(Collections.emptyList());
    }

    public List<PhysicalPerson> getPhysicalPersonsByName(String sessionId, String lastName, String firstName) {
        ArrayOfPhysicalPerson response = service.getPhysicalPersonsByName(sessionId, lastName, firstName);
        return Optional.ofNullable(response).map(ArrayOfPhysicalPerson::getPhysicalPerson).orElse(Collections.emptyList());
    }

    public List<Organization> getOrganizationsByIdentificationNumber(String sessionId, String identificationNumber) {
        ArrayOfOrganization response = service.getOrganizationsByIdentificationNumber(sessionId, identificationNumber);
        return Optional.ofNullable(response).map(ArrayOfOrganization::getOrganization).orElse(Collections.emptyList());
    }

    public List<Organization> getOrganizationsByName(String sessionId, String name) {
        ArrayOfOrganization response = service.getOrganizationsByName(sessionId, name);
        return Optional.ofNullable(response).map(ArrayOfOrganization::getOrganization).orElse(Collections.emptyList());
    }

    public List<StateStructure> getStateStructures(String sessionId, String name) {
        ArrayOfStateStructure response = service.getStateStructures(sessionId, name);
        return Optional.ofNullable(response).map(ArrayOfStateStructure::getStateStructure).orElse(Collections.emptyList());
    }
}
