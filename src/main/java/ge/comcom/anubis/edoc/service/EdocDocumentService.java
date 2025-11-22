package ge.comcom.anubis.edoc.service;

import ge.comcom.anubis.edoc.client.EdocExportClient;
import ge.comcom.anubis.edoc.mapper.EdocContactMapper;
import ge.comcom.anubis.edoc.mapper.EdocDocumentMapper;
import ge.comcom.anubis.edoc.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.DatePeriod;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration_datacontracts.DocumentTypes;
import org.springframework.stereotype.Service;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.time.ZoneOffset;
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

    public List<EdocDocumentSummaryDto> getDocuments(DocumentTypes type, LocalDate from, LocalDate to) {
        DatePeriod period = buildPeriod(from, to);
        List<EdocDocumentSummaryDto> result = sessionService.withSession(sessionId ->
                documentMapper.toSummaryList(client.getDocuments(sessionId, type, period, null)));
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
}
