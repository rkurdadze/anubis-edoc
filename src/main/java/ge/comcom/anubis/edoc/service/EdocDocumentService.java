package ge.comcom.anubis.edoc.service;

import ge.comcom.anubis.edoc.client.EdocExportClient;
import ge.comcom.anubis.edoc.mapper.EdocContactMapper;
import ge.comcom.anubis.edoc.mapper.EdocDocumentMapper;
import ge.comcom.anubis.edoc.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.datacontract.schemas._2004._07.fas_docmanagement.DatePeriod;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.Contact;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.ContactTypes;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.DocumentTypes;
import org.springframework.stereotype.Service;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EdocDocumentService {

    private final EdocExportClient client;
    private final EdocSessionService sessionService;
    private final EdocDocumentMapper documentMapper;
    private final EdocContactMapper contactMapper;

    public List<EdocDocumentSummaryDto> getDocuments(String sessionId, DocumentTypes type, LocalDate from, LocalDate to,
                                                    ContactTypes contactType, UUID contactId) {
        ContactTypes normalizedContactType = contactType;
        UUID normalizedContactId = contactId;

        if (normalizedContactType == null || normalizedContactId == null) {
            normalizedContactType = null;
            normalizedContactId = null;
        }

        validateParameters(sessionId, type, from, to, normalizedContactType, normalizedContactId);
        DatePeriod period = buildPeriod(from, to);
        Contact contact = buildContact(normalizedContactType, normalizedContactId);
        List<EdocDocumentSummaryDto> result = sessionService.withProvidedSession(sessionId, sid ->
                documentMapper.toSummaryList(client.getDocuments(sid, type, period, contact)));
        log.debug("Получено {} документов", result.size());
        return result;
    }

    public EdocDocumentDetailsDto getDocument(UUID id, boolean full) {
        return sessionService.withSession(sessionId -> documentMapper.toDetails(client.getDocument(sessionId, id.toString(), full)));
    }

    public void setDocumentExported(UUID id) {
        sessionService.withSession(sessionId -> {
            client.setDocumentExported(sessionId, id.toString());
            return null;
        });
    }

    public List<EdocPhysicalPersonDto> getPhysicalByPersonalNumber(String personalNumber) {
        return sessionService.withSession(sessionId -> contactMapper.toPhysicalList(client.getPhysicalPersonsByPersonalNumber(sessionId, personalNumber)));
    }

    public List<EdocPhysicalPersonDto> getPhysicalByName(String lastName, String firstName) {
        return sessionService.withSession(sessionId -> contactMapper.toPhysicalList(client.getPhysicalPersonsByName(sessionId, lastName, firstName)));
    }

    public List<EdocOrganizationDto> getOrganizationsByIdentificationNumber(String identificationNumber) {
        return sessionService.withSession(sessionId -> contactMapper.toOrganizationList(client.getOrganizationsByIdentificationNumber(sessionId, identificationNumber)));
    }

    public List<EdocOrganizationDto> getOrganizationsByName(String name) {
        return sessionService.withSession(sessionId -> contactMapper.toOrganizationList(client.getOrganizationsByName(sessionId, name)));
    }

    public List<EdocStateStructureDto> getStateStructures(String name) {
        return sessionService.withSession(sessionId -> contactMapper.toStructureList(client.getStateStructures(sessionId, name)));
    }

    private void validateParameters(String sessionId, DocumentTypes type, LocalDate from, LocalDate to, ContactTypes contactType, UUID contactId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Не указан обязательный параметр sessionId");
        }
        if (type == null) {
            throw new IllegalArgumentException("Не указан обязательный параметр type");
        }
        if (from == null || to == null) {
            throw new IllegalArgumentException("Необходимо указать даты from и to для формирования периода");
        }
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("Дата 'to' не может быть раньше 'from'");
        }
        if (ChronoUnit.DAYS.between(from, to) > 365) {
            throw new IllegalArgumentException("Длительность периода не должна превышать 365 дней");
        }
        if (contactType != null && contactId == null) {
            throw new IllegalArgumentException("Необходимо указать contactId при выборе contactType");
        }
    }

    private DatePeriod buildPeriod(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return null;
        }
        try {
            DatatypeFactory factory = DatatypeFactory.newInstance();
            DatePeriod period = new DatePeriod();
            if (from != null) {
                period.setFrom(toCalendar(factory, from));
            }
            if (to != null) {
                period.setTo(toCalendar(factory, to));
            }
            return period;
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("Ошибка создания даты", e);
        }
    }

    private XMLGregorianCalendar toCalendar(DatatypeFactory factory, LocalDate date) {
        GregorianCalendar calendar = GregorianCalendar.from(date.atStartOfDay(ZoneOffset.UTC));
        return factory.newXMLGregorianCalendar(calendar);
    }

    private Contact buildContact(ContactTypes contactType, UUID contactId) {
        if (contactType == null || contactId == null) {
            return null;
        }
        Contact contact = new Contact();
        contact.setContactType(contactType);
        contact.setID(contactId.toString());
        return contact;
    }
}
