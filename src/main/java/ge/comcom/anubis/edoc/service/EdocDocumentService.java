package ge.comcom.anubis.edoc.service;

import ge.comcom.anubis.edoc.client.EdocExportClient;
import ge.comcom.anubis.edoc.exception.EdocNotCachedException;
import ge.comcom.anubis.edoc.mapper.EdocContactMapper;
import ge.comcom.anubis.edoc.mapper.EdocDocumentMapper;
import ge.comcom.anubis.edoc.model.*;
import ge.comcom.anubis.edoc.repository.EdocDocumentFileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.datacontract.schemas._2004._07.fas_docmanagement.DatePeriod;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.Contact;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.ContactTypes;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.DocumentData;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.DocumentTypes;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
    private final EdocCacheService cacheService;
    private final EdocDocumentFileRepository fileRepository;

    /**
     * Returns document list from eDocument (always calls remote — no counter increment).
     */
    public List<EdocDocumentSummaryDto> getDocumentsRemote(DocumentTypes type, LocalDate from, LocalDate to,
                                                           ContactTypes contactType, UUID contactId) {
        ContactTypes normalizedContactType = (contactType != null && contactId != null) ? contactType : null;
        UUID normalizedContactId = (contactType != null && contactId != null) ? contactId : null;

        validateParameters(type, from, to, normalizedContactType, normalizedContactId);
        DatePeriod period = buildPeriod(from, to);
        log.info("Запрос документов eDocument: type={}, from={}, to={}, periodFrom={}, periodTo={}", 
                type, from, to, period.getFrom(), period.getTo());
        Contact contact = buildContact(normalizedContactType, normalizedContactId);
        List<EdocDocumentSummaryDto> result = sessionService.withSession(sid ->
                documentMapper.toSummaryList(client.getDocuments(sid, type, period, contact)));
        log.debug("Получено {} документов", result.size());
        return result;
    }

    public List<EdocDocumentSummaryDto> getDocumentsFromCache(DocumentTypes type, LocalDate from, LocalDate to,
                                                              ContactTypes contactType, String contactId) {
        validateParameters(type, from, to, null, null);
        String normalizedContactId = StringUtils.hasText(contactId) ? contactId.trim() : null;
        ContactTypes normalizedContactType = (contactType != null && normalizedContactId != null) ? contactType : null;
        return cacheService.findCachedSummaries(type.value(), from, to, normalizedContactType, normalizedContactId);
    }

    /**
     * Returns document details from LOCAL CACHE ONLY.
     * Only Completed documents are cached — any other status will yield 404.
     * Does NOT call the remote eDocument service.
     */
    public EdocDocumentDetailsDto getDocument(UUID id) {
        return cacheService.findCached(id)
                .orElseThrow(() -> new EdocNotCachedException(id));
    }

    /**
     * Returns cache status for a given document ID (does not call remote).
     */
    public EdocCacheStatusDto getCacheStatus(UUID id) {
        return cacheService.getCacheStatus(id);
    }

    /**
     * Manually fetches the document from the remote eDocument service (consumes one read cycle).
     * If the document status is Completed, stores it in the local cache.
     * Returns full document details regardless of status.
     *
     * This method MUST be called only after explicit user confirmation in the frontend.
     */
    public EdocDocumentDetailsDto fetchDocument(UUID id) {
        log.info("Ручная загрузка документа {} с удалённого сервера (потребляет цикл чтения)", id);
        DocumentData data = sessionService.withSession(sid ->
                client.getDocument(sid, id.toString(), true));

        // Cache ALL explicitly fetched documents regardless of status.
        // This ensures every manual fetch costs at most 1 read cycle — subsequent views come from cache.
        // Non-Completed documents can be re-fetched later if the user wants fresh data.
        String status = data.getDocumentStatus() != null ? data.getDocumentStatus().value() : "null";
        try {
            EdocDocumentDetailsDto dto = cacheService.cacheAndGetDetails(data);
            log.info("Документ {} (статус: {}) сохранён в кэш", id, status);
            return dto;
        } catch (Exception cacheEx) {
            log.error("Не удалось сохранить документ {} в кэш (статус: {}): {}", id, status, cacheEx.getMessage(), cacheEx);
            // Return SOAP data without caching so the user still sees the document
            return documentMapper.toDetails(data);
        }
    }

    /**
     * Marks the document as exported in the local cache only.
     * Does NOT call the remote eDocument service.
     */
    public void markExported(UUID id) {
        cacheService.markExported(id);
    }

    public byte[] getFileContent(UUID docId, Long fileId) {
        var file = fileRepository.findByIdAndDocumentId(fileId, docId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        if (file.getContent() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File content not available");
        }
        return file.getContent();
    }

    public String getFileName(UUID docId, Long fileId) {
        return fileRepository.findByIdAndDocumentId(fileId, docId)
                .map(f -> f.getName() != null ? f.getName() : "file")
                .orElse("file");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Contacts (unchanged)
    // ─────────────────────────────────────────────────────────────────────────

    public List<EdocPhysicalPersonDto> getPhysicalByPersonalNumber(String personalNumber) {
        return sessionService.withSession(sid -> contactMapper.toPhysicalList(client.getPhysicalPersonsByPersonalNumber(sid, personalNumber)));
    }

    public List<EdocPhysicalPersonDto> getPhysicalByName(String lastName, String firstName) {
        return sessionService.withSession(sid -> contactMapper.toPhysicalList(client.getPhysicalPersonsByName(sid, lastName, firstName)));
    }

    public List<EdocOrganizationDto> getOrganizationsByIdentificationNumber(String identificationNumber) {
        return sessionService.withSession(sid -> contactMapper.toOrganizationList(client.getOrganizationsByIdentificationNumber(sid, identificationNumber)));
    }

    public List<EdocOrganizationDto> getOrganizationsByName(String name) {
        return sessionService.withSession(sid -> contactMapper.toOrganizationList(client.getOrganizationsByName(sid, name)));
    }

    public List<EdocStateStructureDto> getStateStructures(String name) {
        return sessionService.withSession(sid -> contactMapper.toStructureList(client.getStateStructures(sid, name)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void validateParameters(DocumentTypes type, LocalDate from, LocalDate to,
                                    ContactTypes contactType, UUID contactId) {
        if (type == null) {
            throw new IllegalArgumentException("Не указан обязательный параметр type");
        }
        if (DocumentTypes.NONE.equals(type)) {
            throw new IllegalArgumentException("Необходимо указать один из типов: INCOMING, OUTGOING, INTERNAL, ORDER");
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
    }

    private DatePeriod buildPeriod(LocalDate from, LocalDate to) {
        try {
            DatatypeFactory factory = DatatypeFactory.newInstance();
            DatePeriod period = new DatePeriod();
            period.setFrom(toCalendar(factory, from, 0, 0, 0));
            period.setTo(toCalendar(factory, to, 23, 59, 59));
            return period;
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("Ошибка создания даты", e);
        }
    }

    private XMLGregorianCalendar toCalendar(DatatypeFactory factory, LocalDate date, int hour, int minute, int second) {
        XMLGregorianCalendar calendar = factory.newXMLGregorianCalendar();
        calendar.setYear(date.getYear());
        calendar.setMonth(date.getMonthValue());
        calendar.setDay(date.getDayOfMonth());
        calendar.setTime(hour, minute, second);
        return calendar;
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
