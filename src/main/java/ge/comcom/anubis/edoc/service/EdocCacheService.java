package ge.comcom.anubis.edoc.service;

import ge.comcom.anubis.edoc.entity.*;
import ge.comcom.anubis.edoc.model.*;
import ge.comcom.anubis.edoc.repository.EdocCachedDocumentRepository;
import ge.comcom.anubis.edoc.repository.EdocContactRepository;
import jakarta.xml.bind.JAXBElement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.datacontract.schemas._2004._07.fas_docmanagement_integration.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EdocCacheService {

    private final EdocCachedDocumentRepository documentRepo;
    private final EdocContactRepository contactRepo;

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the cached document details DTO, or empty if not cached.
     */
    @Transactional(readOnly = true)
    public Optional<EdocDocumentDetailsDto> findCached(UUID id) {
        return documentRepo.findById(id).map(this::toDetailsDto);
    }

    /**
     * Returns the cache status for a document (may or may not be cached).
     */
    @Transactional(readOnly = true)
    public EdocCacheStatusDto getCacheStatus(UUID id) {
        Optional<EdocCachedDocumentEntity> opt = documentRepo.findById(id);
        EdocCacheStatusDto dto = new EdocCacheStatusDto();
        dto.setId(id);
        if (opt.isPresent()) {
            EdocCachedDocumentEntity e = opt.get();
            dto.setCached(true);
            dto.setDocumentStatus(e.getDocumentStatus());
            dto.setDocumentType(e.getDocumentType());
            dto.setCachedAt(e.getCachedAt());
            dto.setFetchCount(e.getFetchCount());
            dto.setLastFetchedAt(e.getLastFetchedAt());
        } else {
            dto.setCached(false);
        }
        return dto;
    }

    /**
     * Saves/updates the document in the local cache and returns the full DTO.
     * Call this only for Completed documents.
     */
    @Transactional
    public EdocDocumentDetailsDto cacheAndGetDetails(DocumentData data) {
        UUID id = extractId(data.getID());
        OffsetDateTime now = OffsetDateTime.now();

        // If already cached, delete and re-insert to refresh all data
        documentRepo.findById(id).ifPresent(existing -> {
            documentRepo.delete(existing);
            documentRepo.flush();
        });

        EdocCachedDocumentEntity entity = new EdocCachedDocumentEntity();
        entity.setId(id);
        entity.setCachedAt(now);
        entity.setFetchCount(1);
        entity.setLastFetchedAt(now);

        fillBaseDocument(entity, data);
        fillResultProcess(entity, data.getResultProcess());

        if (data instanceof IncomingDocumentData incoming) {
            fillIncoming(entity, incoming);
        } else if (data instanceof InternalDocumentData internal) {
            fillInternal(entity, internal);
        } else if (data instanceof OutgoingDocumentData outgoing) {
            fillOutgoing(entity, outgoing);
        } else if (data instanceof OrderDocumentData order) {
            fillOrder(entity, order);
        }

        documentRepo.save(entity);
        log.info("Документ {} ({}) сохранён в кэш", id, entity.getDocumentType());
        return toDetailsDto(entity);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fill methods: populate entity from SOAP DocumentData
    // ─────────────────────────────────────────────────────────────────────────

    private void fillBaseDocument(EdocCachedDocumentEntity e, DocumentData d) {
        e.setDocumentType(d.getDocumentType() != null ? d.getDocumentType().value() : null);
        e.setNumber(unwrapStr(d.getNumber()));
        e.setRegistrationDate(toOffset(d.getRegistrationDate()));
        e.setDeadline(toOffsetNullable(d.getDeadline()));
        e.setCategory(unwrapStr(d.getCategory()));
        e.setDocumentStatus(d.getDocumentStatus() != null ? d.getDocumentStatus().value() : null);
        e.setExportStatus(d.getExportStatus() != null ? d.getExportStatus().value() : null);
        e.setIssueDecisionType(unwrapStr(d.getIssueDecisionType()));
        e.setAnnotation(unwrapStr(d.getAnnotation()));
        e.setChancellary(unwrapStr(d.getChancellary()));
        e.setComments(unwrapStr(d.getComments()));
        e.setPurpose(d.getPurpose() != null ? d.getPurpose().value() : null);

        // Files
        ArrayOfDocumentFile filesArr = unwrap(d.getFiles());
        if (filesArr != null && filesArr.getDocumentFile() != null) {
            for (DocumentFile f : filesArr.getDocumentFile()) {
                EdocDocumentFileEntity fe = new EdocDocumentFileEntity();
                fe.setDocument(e);
                fe.setFileType(f.getFileType() != null ? f.getFileType().value() : null);
                fe.setName(unwrapStr(f.getName()));
                fe.setContent(unwrap(f.getContent()));
                e.getFiles().add(fe);
            }
        }

        // Related documents
        ArrayOfRelatedDocument relArr = unwrap(d.getRelatedDocuments());
        if (relArr != null && relArr.getRelatedDocument() != null) {
            for (RelatedDocument rd : relArr.getRelatedDocument()) {
                EdocRelatedDocumentEntity re = new EdocRelatedDocumentEntity();
                re.setDocument(e);
                re.setRelationType(rd.getRelationType() != null ? rd.getRelationType().value() : null);
                DocumentInfo info = unwrap(rd.getDocument());
                if (info != null) {
                    re.setRelatedDocId(extractIdNullable(info.getID()));
                    re.setRelatedDocNumber(unwrapStr(info.getNumber()));
                    re.setRelatedDocType(info.getDocumentType() != null ? info.getDocumentType().value() : null);
                    re.setRelatedDocRegistrationDate(toOffset(info.getRegistrationDate()));
                    Object expStatus = info.getExportStatus();
                    if (expStatus instanceof JAXBElement<?> je && je.getValue() != null) {
                        re.setRelatedDocExportStatus(je.getValue().toString());
                    } else if (expStatus instanceof ExportStatuses es) {
                        re.setRelatedDocExportStatus(es.value());
                    }
                }
                e.getRelatedDocuments().add(re);
            }
        }
    }

    private void fillResultProcess(EdocCachedDocumentEntity e, JAXBElement<ResultProcess> element) {
        ResultProcess rp = unwrap(element);
        if (rp != null) {
            e.setResultProcess(buildProcess(rp, false));
        }
    }

    private void fillIncoming(EdocCachedDocumentEntity e, IncomingDocumentData d) {
        e.setOriginalNumber(unwrapStr(d.getOriginalNumber()));
        e.setOriginalDate(toOffsetNullable(d.getOriginalDate()));

        // Senders: Contact[]
        ArrayOfContact sendersArr = unwrap(d.getSenders());
        if (sendersArr != null && sendersArr.getContact() != null) {
            for (Contact c : sendersArr.getContact()) {
                EdocContactEntity ce = saveOrUpdateContact(c);
                EdocDocumentContactEntity link = new EdocDocumentContactEntity();
                link.setDocument(e);
                link.setContact(ce);
                link.setRole("INCOMING_SENDER");
                e.getContacts().add(link);
            }
        }

        // Addressees: EmployeeData[]
        ArrayOfEmployeeData addresseesArr = unwrap(d.getAddressees());
        if (addresseesArr != null && addresseesArr.getEmployeeData() != null) {
            for (EmployeeData emp : addresseesArr.getEmployeeData()) {
                addEmployeeLink(e, emp, "INCOMING_ADDRESSEE");
            }
        }

        // ReceiveWays
        ArrayOfReceiveWayData receiveWaysArr = unwrap(d.getReceiveWays());
        if (receiveWaysArr != null && receiveWaysArr.getReceiveWayData() != null) {
            for (ReceiveWayData rw : receiveWaysArr.getReceiveWayData()) {
                EdocReceiveWayEntity rwe = new EdocReceiveWayEntity();
                rwe.setDocument(e);
                rwe.setWay(unwrapStr(rw.getWay()));
                rwe.setComments(unwrapStr(rw.getComments()));
                e.getReceiveWays().add(rwe);
            }
        }
    }

    private void fillInternal(EdocCachedDocumentEntity e, InternalDocumentData d) {
        e.setHasDigitalSignature(d.isHasDigitalSignature());
        e.setHasDigitalStamp(d.isHasDigitalStamp());

        PreparationProcess pp = unwrap(d.getPreProcess());
        if (pp != null) {
            e.setPreparationProcess(buildProcess(pp, true));
        }

        // Senders: EmployeeData[]
        ArrayOfEmployeeData sendersArr = unwrap(d.getSenders());
        if (sendersArr != null && sendersArr.getEmployeeData() != null) {
            for (EmployeeData emp : sendersArr.getEmployeeData()) {
                addEmployeeLink(e, emp, "INTERNAL_SENDER");
            }
        }

        // Recipients: EmployeeData[]
        ArrayOfEmployeeData recipientsArr = unwrap(d.getRecipients());
        if (recipientsArr != null && recipientsArr.getEmployeeData() != null) {
            for (EmployeeData emp : recipientsArr.getEmployeeData()) {
                addEmployeeLink(e, emp, "INTERNAL_RECIPIENT");
            }
        }
    }

    private void fillOutgoing(EdocCachedDocumentEntity e, OutgoingDocumentData d) {
        e.setHasDigitalSignature(d.isHasDigitalSignature());
        e.setHasDigitalStamp(d.isHasDigitalStamp());

        PreparationProcess pp = unwrap(d.getPreProcess());
        if (pp != null) {
            e.setPreparationProcess(buildProcess(pp, true));
        }

        // Recipients: Contact[]
        ArrayOfContact recipientsArr = unwrap(d.getRecipients());
        if (recipientsArr != null && recipientsArr.getContact() != null) {
            for (Contact c : recipientsArr.getContact()) {
                EdocContactEntity ce = saveOrUpdateContact(c);
                EdocDocumentContactEntity link = new EdocDocumentContactEntity();
                link.setDocument(e);
                link.setContact(ce);
                link.setRole("OUTGOING_RECIPIENT");
                e.getContacts().add(link);
            }
        }

        // Signatories: EmployeeData[]
        ArrayOfEmployeeData signatoriesArr = unwrap(d.getSignatories());
        if (signatoriesArr != null && signatoriesArr.getEmployeeData() != null) {
            for (EmployeeData emp : signatoriesArr.getEmployeeData()) {
                addEmployeeLink(e, emp, "OUTGOING_SIGNATORY");
            }
        }
    }

    private void fillOrder(EdocCachedDocumentEntity e, OrderDocumentData d) {
        e.setDirection(d.getDirection() != null ? d.getDirection().value() : null);
        e.setHasDigitalSignature(d.isHasDigitalSignature());
        e.setHasDigitalStamp(d.isHasDigitalStamp());
        e.setOrderType(unwrapStr(d.getOrderType()));

        PreparationProcess pp = unwrap(d.getPreProcess());
        if (pp != null) {
            e.setPreparationProcess(buildProcess(pp, true));
        }

        // OuterRecipients: Contact[]
        ArrayOfContact outerArr = unwrap(d.getOuterRecipients());
        if (outerArr != null && outerArr.getContact() != null) {
            for (Contact c : outerArr.getContact()) {
                EdocContactEntity ce = saveOrUpdateContact(c);
                EdocDocumentContactEntity link = new EdocDocumentContactEntity();
                link.setDocument(e);
                link.setContact(ce);
                link.setRole("ORDER_OUTER_RECIPIENT");
                e.getContacts().add(link);
            }
        }

        // InnerRecipients: EmployeeData[]
        ArrayOfEmployeeData innerArr = unwrap(d.getInnerRecipients());
        if (innerArr != null && innerArr.getEmployeeData() != null) {
            for (EmployeeData emp : innerArr.getEmployeeData()) {
                addEmployeeLink(e, emp, "ORDER_INNER_RECIPIENT");
            }
        }

        // RelatedEmployees: EmployeeData[]
        ArrayOfEmployeeData relatedArr = unwrap(d.getRelatedEmployees());
        if (relatedArr != null && relatedArr.getEmployeeData() != null) {
            for (EmployeeData emp : relatedArr.getEmployeeData()) {
                addEmployeeLink(e, emp, "ORDER_RELATED_EMPLOYEE");
            }
        }

        // Signatories: EmployeeData[]
        ArrayOfEmployeeData signatoriesArr = unwrap(d.getSignatories());
        if (signatoriesArr != null && signatoriesArr.getEmployeeData() != null) {
            for (EmployeeData emp : signatoriesArr.getEmployeeData()) {
                addEmployeeLink(e, emp, "ORDER_SIGNATORY");
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Process building
    // ─────────────────────────────────────────────────────────────────────────

    private EdocProcessEntity buildProcess(org.datacontract.schemas._2004._07.fas_docmanagement_integration.Process p, boolean isPreparation) {
        EdocProcessEntity pe = new EdocProcessEntity();
        TaskData task = unwrap(p.getTask());
        if (task != null) {
            pe.setTaskNumber(task.getNumber());
            pe.setTaskStartDate(toOffset(task.getStartDate()));
            pe.setTaskDeadline(toOffset(task.getDeadline()));
            pe.setTaskText(unwrapStr(task.getText()));
            pe.setTaskIsInitiated(task.isIsInitiated());
            EmployeeData initiatedBy = unwrap(task.getInitiatedBy());
            if (initiatedBy != null) {
                pe.setTaskInitiatedBy(buildEmployee(initiatedBy));
            }
        }

        ArrayOfResponsibleData resArr = unwrap(p.getResponsibles());
        if (resArr != null && resArr.getResponsibleData() != null) {
            for (ResponsibleData rd : resArr.getResponsibleData()) {
                buildResponsible(pe, rd, null); // null = top-level responsible
            }
        }

        if (isPreparation && p instanceof PreparationProcess prep) {
            ArrayOfSignatureData sigsArr = unwrap(prep.getSignatures());
            if (sigsArr != null && sigsArr.getSignatureData() != null) {
                for (SignatureData sd : sigsArr.getSignatureData()) {
                    EdocSignatureEntity se = new EdocSignatureEntity();
                    se.setProcess(pe);
                    se.setCreator(buildEmployeeNullable(unwrap(sd.getCreator())));
                    se.setSignatory(buildEmployeeNullable(unwrap(sd.getSignatory())));
                    se.setDeadline(toOffset(sd.getDeadline()));
                    se.setEntryDate(toOffset(sd.getEntryDate()));
                    se.setStatus(sd.getStatus() != null ? sd.getStatus().value() : null);
                    se.setStatusChangeDate(toOffset(sd.getStatusChangeDate()));
                    pe.getSignatures().add(se);
                }
            }

            ArrayOfViseData visesArr = unwrap(prep.getVises());
            if (visesArr != null && visesArr.getViseData() != null) {
                for (ViseData vd : visesArr.getViseData()) {
                    buildVise(pe, vd, null);
                }
            }
        }

        return pe;
    }

    /**
     * Recursively builds responsible entities. parentId is null for top-level entries.
     * All responsibles (including children) are added to pe.getResponsibles() so that
     * process_id FK is set for every row. The parent_id column stores the hierarchy.
     * Note: child entities are saved after their parents so that parentId can be resolved.
     */
    private void buildResponsible(EdocProcessEntity pe, ResponsibleData rd, Long parentId) {
        EdocResponsibleEntity re = new EdocResponsibleEntity();
        re.setProcess(pe);
        re.setParentId(parentId);
        re.setStartDate(toOffset(rd.getStartDate()));
        re.setDeadline(toOffset(rd.getDeadline()));
        re.setTask(unwrapStr(rd.getTask()));
        re.setStatus(rd.getStatus() != null ? rd.getStatus().value() : null);
        re.setStatusChangeDate(toOffset(rd.getStatusChangeDate()));
        re.setEmployee(buildEmployeeNullable(unwrap(rd.getEmployee())));
        pe.getResponsibles().add(re);
        // Note: re.id is not yet assigned here (assigned by DB on flush).
        // Child responsibles will be added to the process collection with a deferred parent reference.
        // The full hierarchy is stored after the process is saved in a post-save step if needed.
        // For now, children are stored flat with parentId resolved during cacheAndGetDetails.

        ArrayOfResponsibleData children = unwrap(rd.getChildResponsibles());
        if (children != null && children.getResponsibleData() != null) {
            for (ResponsibleData child : children.getResponsibleData()) {
                // parentId will be set after save — use a deferred reference approach below
                buildResponsible(pe, child, null); // parentId resolved post-save
            }
        }
    }

    private void buildVise(EdocProcessEntity pe, ViseData vd, Long parentId) {
        EdocViseEntity ve = new EdocViseEntity();
        ve.setProcess(pe);
        ve.setParentId(parentId);
        ve.setAuthor(buildEmployeeNullable(unwrap(vd.getAuthor())));
        ve.setCreator(buildEmployeeNullable(unwrap(vd.getCreator())));
        ve.setDeadline(toOffset(vd.getDeadline()));
        ve.setEntryDate(toOffset(vd.getEntryDate()));
        ve.setStatus(vd.getStatus() != null ? vd.getStatus().value() : null);
        ve.setStatusChangeDate(toOffset(vd.getStatusChangeDate()));
        pe.getVises().add(ve);

        ArrayOfViseData children = unwrap(vd.getChildVises());
        if (children != null && children.getViseData() != null) {
            for (ViseData child : children.getViseData()) {
                buildVise(pe, child, null); // parentId resolved post-save
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper: build employee entity
    // ─────────────────────────────────────────────────────────────────────────

    private void addEmployeeLink(EdocCachedDocumentEntity doc, EmployeeData emp, String role) {
        EdocDocumentEmployeeEntity link = new EdocDocumentEmployeeEntity();
        link.setDocument(doc);
        link.setEmployee(buildEmployee(emp));
        link.setRole(role);
        doc.getEmployees().add(link);
    }

    private EdocEmployeeEntity buildEmployee(EmployeeData emp) {
        EdocEmployeeEntity e = new EdocEmployeeEntity();
        e.setFirstName(unwrapStr(emp.getFirstName()));
        e.setLastName(unwrapStr(emp.getLastName()));
        e.setPosition(unwrapStr(emp.getPosition()));
        e.setOrganizationStructure(unwrapStr(emp.getOrganizationStructure()));
        return e;
    }

    private EdocEmployeeEntity buildEmployeeNullable(EmployeeData emp) {
        return emp == null ? null : buildEmployee(emp);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper: save/update contact entity (upsert by UUID)
    // ─────────────────────────────────────────────────────────────────────────

    private EdocContactEntity saveOrUpdateContact(Contact c) {
        UUID id = extractId(c.getID());
        return contactRepo.findById(id).orElseGet(() -> {
            EdocContactEntity ce = new EdocContactEntity();
            ce.setId(id);
            ce.setContactType(c.getContactType() != null ? c.getContactType().value() : "Unknown");
            if (c instanceof PhysicalPerson pp) {
                ce.setFirstName(unwrapStr(pp.getFirstName()));
                ce.setLastName(unwrapStr(pp.getLastName()));
                ce.setPersonalNumber(unwrapStr(pp.getPersonalNumber()));
            } else if (c instanceof Organization org) {
                ce.setIdentificationNumber(unwrapStr(org.getIdentificationNumber()));
                ce.setJuridicalForm(unwrapStr(org.getJuridicalForm()));
                ce.setName(unwrapStr(org.getName()));
            } else if (c instanceof StateStructure ss) {
                ce.setName(unwrapStr(ss.getName()));
            }
            return contactRepo.save(ce);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Convert entity → DTO
    // ─────────────────────────────────────────────────────────────────────────

    public EdocDocumentDetailsDto toDetailsDto(EdocCachedDocumentEntity e) {
        EdocDocumentDetailsDto dto = new EdocDocumentDetailsDto();
        dto.setId(e.getId());
        dto.setDocumentType(e.getDocumentType());
        dto.setNumber(e.getNumber());
        dto.setRegistrationDate(e.getRegistrationDate());
        dto.setDeadline(e.getDeadline());
        dto.setCategory(e.getCategory());
        dto.setDocumentStatus(e.getDocumentStatus());
        dto.setExportStatus(e.getExportStatus());
        dto.setAnnotation(e.getAnnotation());
        dto.setChancellary(e.getChancellary());
        dto.setComments(e.getComments());
        dto.setPurpose(e.getPurpose());
        dto.setFromCache(true);
        dto.setCachedAt(e.getCachedAt());
        dto.setFetchCount(e.getFetchCount());

        dto.setFiles(e.getFiles().stream().map(f -> {
            EdocDocumentFileDto fd = new EdocDocumentFileDto();
            fd.setName(f.getName());
            fd.setFileType(f.getFileType());
            fd.setContentBase64(f.getContent() != null ? Base64.getEncoder().encodeToString(f.getContent()) : null);
            return fd;
        }).collect(Collectors.toList()));

        dto.setRelatedDocuments(e.getRelatedDocuments().stream().map(r -> {
            EdocRelatedDocumentDto rd = new EdocRelatedDocumentDto();
            rd.setId(r.getRelatedDocId());
            rd.setRelationType(r.getRelationType());
            rd.setDocumentNumber(r.getRelatedDocNumber());
            return rd;
        }).collect(Collectors.toList()));

        // Type-specific
        dto.setOriginalNumber(e.getOriginalNumber());
        dto.setOriginalDate(e.getOriginalDate());
        dto.setHasDigitalSignature(e.getHasDigitalSignature());
        dto.setHasDigitalStamp(e.getHasDigitalStamp());
        dto.setDirection(e.getDirection());
        dto.setOrderType(e.getOrderType());

        // ReceiveWays
        if (!e.getReceiveWays().isEmpty()) {
            dto.setReceiveWays(e.getReceiveWays().stream().map(rw -> {
                EdocReceiveWayDto rwd = new EdocReceiveWayDto();
                rwd.setWay(rw.getWay());
                rwd.setComments(rw.getComments());
                return rwd;
            }).collect(Collectors.toList()));
        }

        // Participants
        List<EdocContactDto> incomingSenders = contactsOfRole(e, "INCOMING_SENDER");
        List<EdocContactDto> outRecipients = contactsOfRole(e, "OUTGOING_RECIPIENT");
        List<EdocContactDto> orderOuter = contactsOfRole(e, "ORDER_OUTER_RECIPIENT");

        dto.setSenders(incomingSenders.isEmpty() ? null : incomingSenders);
        dto.setRecipients(outRecipients.isEmpty() ? null : outRecipients);
        dto.setOuterRecipients(orderOuter.isEmpty() ? null : orderOuter);

        dto.setAddressees(empDtoList(e, "INCOMING_ADDRESSEE"));
        dto.setEmployeeSenders(empDtoList(e, "INTERNAL_SENDER"));
        dto.setEmployeeRecipients(empDtoList(e, "INTERNAL_RECIPIENT"));
        dto.setSignatories(empDtoList(e, "OUTGOING_SIGNATORY"));
        dto.setInnerRecipients(empDtoList(e, "ORDER_INNER_RECIPIENT"));
        dto.setRelatedEmployees(empDtoList(e, "ORDER_RELATED_EMPLOYEE"));

        // ORDER_SIGNATORY overrides signatories if present
        List<EdocEmployeeDto> orderSignatories = empDtoList(e, "ORDER_SIGNATORY");
        if (!orderSignatories.isEmpty()) {
            dto.setSignatories(orderSignatories);
        }

        return dto;
    }

    private List<EdocContactDto> contactsOfRole(EdocCachedDocumentEntity e, String role) {
        return e.getContacts().stream()
                .filter(c -> role.equals(c.getRole()))
                .map(c -> toContactDto(c.getContact()))
                .collect(Collectors.toList());
    }

    private List<EdocEmployeeDto> empDtoList(EdocCachedDocumentEntity e, String role) {
        List<EdocEmployeeDto> result = e.getEmployees().stream()
                .filter(el -> role.equals(el.getRole()))
                .map(el -> toEmployeeDto(el.getEmployee()))
                .collect(Collectors.toList());
        return result.isEmpty() ? null : result;
    }

    private EdocContactDto toContactDto(EdocContactEntity c) {
        EdocContactDto dto = new EdocContactDto();
        dto.setId(c.getId());
        dto.setContactType(c.getContactType());
        dto.setFirstName(c.getFirstName());
        dto.setLastName(c.getLastName());
        dto.setPersonalNumber(c.getPersonalNumber());
        dto.setIdentificationNumber(c.getIdentificationNumber());
        dto.setJuridicalForm(c.getJuridicalForm());
        dto.setName(c.getName());
        return dto;
    }

    private EdocEmployeeDto toEmployeeDto(EdocEmployeeEntity e) {
        EdocEmployeeDto dto = new EdocEmployeeDto();
        dto.setFirstName(e.getFirstName());
        dto.setLastName(e.getLastName());
        dto.setPosition(e.getPosition());
        dto.setOrganizationStructure(e.getOrganizationStructure());
        return dto;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JAXBElement / XML helpers
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private <T> T unwrap(Object obj) {
        if (obj instanceof JAXBElement<?> je) {
            return (T) je.getValue();
        }
        return (T) obj;
    }

    private String unwrapStr(Object obj) {
        if (obj instanceof JAXBElement<?> je) {
            return je.getValue() != null ? je.getValue().toString() : null;
        }
        return obj != null ? obj.toString() : null;
    }

    private UUID extractId(Object rawId) {
        String s = rawId instanceof JAXBElement<?> je ? (String) je.getValue() : (String) rawId;
        if (s == null || s.isEmpty()) {
            throw new IllegalArgumentException("Пустой ID документа");
        }
        return UUID.fromString(s);
    }

    private UUID extractIdNullable(Object rawId) {
        try {
            String s = rawId instanceof JAXBElement<?> je ? (je.getValue() != null ? je.getValue().toString() : null) : (rawId != null ? rawId.toString() : null);
            return (s != null && !s.isEmpty()) ? UUID.fromString(s) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private OffsetDateTime toOffset(XMLGregorianCalendar cal) {
        if (cal == null) return null;
        return cal.toGregorianCalendar().toZonedDateTime().toOffsetDateTime();
    }

    @SuppressWarnings("unchecked")
    private OffsetDateTime toOffsetNullable(Object obj) {
        if (obj == null) return null;
        if (obj instanceof JAXBElement<?> je) {
            if (je.getValue() == null) return null;
            return toOffset((XMLGregorianCalendar) je.getValue());
        }
        if (obj instanceof XMLGregorianCalendar cal) {
            return toOffset(cal);
        }
        return null;
    }
}
