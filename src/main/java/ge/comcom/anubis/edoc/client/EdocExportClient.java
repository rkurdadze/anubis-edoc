package ge.comcom.anubis.edoc.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.datacontract.schemas._2004._07.fas_docmanagement.DatePeriod;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.ArrayOfDocument;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.ArrayOfOrganization;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.ArrayOfPhysicalPerson;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.ArrayOfStateStructure;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.Contact;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.Document;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.DocumentData;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.DocumentTypes;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.Organization;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.PhysicalPerson;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.StateStructure;
import org.tempuri.IeDocumentExportService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class EdocExportClient {

    private final IeDocumentExportService service;

    public String logOn(String token, String version) {
        try {
            return service.logOn(token, version);
        } catch (Exception ex) {
            throw new IllegalStateException("Ошибка вызова logOn", ex);
        }
    }

    public void logOut(String sessionId) {
        try {
            service.logOut(sessionId);
        } catch (Exception ex) {
            throw new IllegalStateException("Ошибка вызова logOut", ex);
        }
    }

    public List<Document> getDocuments(String sessionId, DocumentTypes type, DatePeriod period, Contact contact) {
        try {
            ArrayOfDocument response = service.getDocuments(sessionId, type, period, contact);
            return Optional.ofNullable(response).map(ArrayOfDocument::getDocument).orElse(Collections.emptyList());
        } catch (Exception ex) {
            throw new IllegalStateException("Ошибка вызова getDocuments", ex);
        }
    }

    public DocumentData getDocument(String sessionId, String documentId, boolean full) {
        try {
            return service.getDocument(sessionId, documentId, full);
        } catch (Exception ex) {
            throw new IllegalStateException("Ошибка вызова getDocument", ex);
        }
    }

    public void setDocumentExported(String sessionId, String documentId) {
        try {
            service.setDocumentExported(sessionId, documentId);
        } catch (Exception ex) {
            throw new IllegalStateException("Ошибка вызова setDocumentExported", ex);
        }
    }

    public List<PhysicalPerson> getPhysicalPersonsByPersonalNumber(String sessionId, String personalNumber) {
        try {
            ArrayOfPhysicalPerson response = service.getPhysicalPersonsByPersonalNumber(sessionId, personalNumber);
            return Optional.ofNullable(response).map(ArrayOfPhysicalPerson::getPhysicalPerson).orElse(Collections.emptyList());
        } catch (Exception ex) {
            throw new IllegalStateException("Ошибка вызова getPhysicalPersonsByPersonalNumber", ex);
        }
    }

    public List<PhysicalPerson> getPhysicalPersonsByName(String sessionId, String lastName, String firstName) {
        try {
            ArrayOfPhysicalPerson response = service.getPhysicalPersonsByName(sessionId, lastName, firstName);
            return Optional.ofNullable(response).map(ArrayOfPhysicalPerson::getPhysicalPerson).orElse(Collections.emptyList());
        } catch (Exception ex) {
            throw new IllegalStateException("Ошибка вызова getPhysicalPersonsByName", ex);
        }
    }

    public List<Organization> getOrganizationsByIdentificationNumber(String sessionId, String identificationNumber) {
        try {
            ArrayOfOrganization response = service.getOrganizationsByIdentificationNumber(sessionId, identificationNumber);
            return Optional.ofNullable(response).map(ArrayOfOrganization::getOrganization).orElse(Collections.emptyList());
        } catch (Exception ex) {
            throw new IllegalStateException("Ошибка вызова getOrganizationsByIdentificationNumber", ex);
        }
    }

    public List<Organization> getOrganizationsByName(String sessionId, String name) {
        try {
            ArrayOfOrganization response = service.getOrganizationsByName(sessionId, name);
            return Optional.ofNullable(response).map(ArrayOfOrganization::getOrganization).orElse(Collections.emptyList());
        } catch (Exception ex) {
            throw new IllegalStateException("Ошибка вызова getOrganizationsByName", ex);
        }
    }

    public List<StateStructure> getStateStructures(String sessionId, String name) {
        try {
            ArrayOfStateStructure response = service.getStateStructures(sessionId, name);
            return Optional.ofNullable(response).map(ArrayOfStateStructure::getStateStructure).orElse(Collections.emptyList());
        } catch (Exception ex) {
            throw new IllegalStateException("Ошибка вызова getStateStructures", ex);
        }
    }
}
