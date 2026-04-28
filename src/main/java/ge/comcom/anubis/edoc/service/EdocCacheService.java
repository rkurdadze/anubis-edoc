package ge.comcom.anubis.edoc.service;

import ge.comcom.anubis.edoc.entity.*;
import ge.comcom.anubis.edoc.model.*;
import ge.comcom.anubis.edoc.repository.EdocCachedDocumentRepository;
import ge.comcom.anubis.edoc.repository.EdocContactRepository;
import ge.comcom.anubis.edoc.repository.EdocEmployeeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
    private final EdocEmployeeRepository employeeRepo;

    @PersistenceContext
    private EntityManager entityManager;

    private record PendingResponsibleParent(EdocResponsibleEntity child, EdocResponsibleEntity parent) {}
    private record PendingViseParent(EdocViseEntity child, EdocViseEntity parent) {}

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
     * Updates exportStatus to "Exported" in the local cache.
     * Does NOT call the remote eDocument service.
     * If the document is not cached, this is a no-op.
     */
    @Transactional
    public void markExported(UUID id) {
        documentRepo.findById(id).ifPresent(e -> {
            e.setExportStatus("Exported");
            documentRepo.save(e);
            log.info("Документ {} помечен как Exported в локальном кэше", id);
        });
    }

    /**
     * Saves/updates the document in the local cache and returns the full DTO.
     */
    @Transactional
    public EdocDocumentDetailsDto cacheAndGetDetails(DocumentData data) {
        UUID id = extractId(data.getID());
        OffsetDateTime now = OffsetDateTime.now();
        int nextFetchCount = documentRepo.findById(id)
                .map(existing -> Math.max(existing.getFetchCount(), 0) + 1)
                .orElse(1);

        List<PendingResponsibleParent> pendingResponsibleParents = new ArrayList<>();
        List<PendingViseParent> pendingViseParents = new ArrayList<>();

        // If already cached — delete and evict from 1L cache before re-inserting.
        // Using deleteById + flush + clear avoids Hibernate treating the same UUID as
        // REMOVED-state in the session, which would break the subsequent persist().
        if (documentRepo.existsById(id)) {
            documentRepo.deleteById(id);
            documentRepo.flush();
            entityManager.clear();
        }

        EdocCachedDocumentEntity entity = new EdocCachedDocumentEntity();
        entity.setId(id);
        entity.setCachedAt(now);
        entity.setFetchCount(nextFetchCount);
        entity.setLastFetchedAt(now);

        fillBaseDocument(entity, data);
        fillResultProcess(entity, data.getResultProcess(), pendingResponsibleParents, pendingViseParents);

        if (data instanceof IncomingDocumentData incoming) {
            fillIncoming(entity, incoming);
        } else if (data instanceof InternalDocumentData internal) {
            fillInternal(entity, internal, pendingResponsibleParents, pendingViseParents);
        } else if (data instanceof OutgoingDocumentData outgoing) {
            fillOutgoing(entity, outgoing, pendingResponsibleParents, pendingViseParents);
        } else if (data instanceof OrderDocumentData order) {
            fillOrder(entity, order, pendingResponsibleParents, pendingViseParents);
        }

        // persist() (not merge) so that IDs from IDENTITY strategy are assigned
        // directly on the entity objects inside entity.getFiles(), not on copies.
        entityManager.persist(entity);
        entityManager.flush();
        resolveDeferredHierarchyLinks(pendingResponsibleParents, pendingViseParents);
        log.info("Документ {} ({}) сохранён в кэш, файлов: {}", id, entity.getDocumentType(), entity.getFiles().size());
        entity.getFiles().forEach(f -> log.debug("  Файл id={} name={}", f.getId(), f.getName()));
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

    private void fillResultProcess(EdocCachedDocumentEntity e,
                                   JAXBElement<ResultProcess> element,
                                   List<PendingResponsibleParent> pendingResponsibleParents,
                                   List<PendingViseParent> pendingViseParents) {
        ResultProcess rp = unwrap(element);
        if (rp != null) {
            e.setResultProcess(buildProcess(rp, false, pendingResponsibleParents, pendingViseParents));
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

    private void fillInternal(EdocCachedDocumentEntity e,
                              InternalDocumentData d,
                              List<PendingResponsibleParent> pendingResponsibleParents,
                              List<PendingViseParent> pendingViseParents) {
        e.setHasDigitalSignature(d.isHasDigitalSignature());
        e.setHasDigitalStamp(d.isHasDigitalStamp());

        PreparationProcess pp = unwrap(d.getPreProcess());
        if (pp != null) {
            e.setPreparationProcess(buildProcess(pp, true, pendingResponsibleParents, pendingViseParents));
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

    private void fillOutgoing(EdocCachedDocumentEntity e,
                              OutgoingDocumentData d,
                              List<PendingResponsibleParent> pendingResponsibleParents,
                              List<PendingViseParent> pendingViseParents) {
        e.setHasDigitalSignature(d.isHasDigitalSignature());
        e.setHasDigitalStamp(d.isHasDigitalStamp());

        PreparationProcess pp = unwrap(d.getPreProcess());
        if (pp != null) {
            e.setPreparationProcess(buildProcess(pp, true, pendingResponsibleParents, pendingViseParents));
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

    private void fillOrder(EdocCachedDocumentEntity e,
                           OrderDocumentData d,
                           List<PendingResponsibleParent> pendingResponsibleParents,
                           List<PendingViseParent> pendingViseParents) {
        e.setDirection(d.getDirection() != null ? d.getDirection().value() : null);
        e.setHasDigitalSignature(d.isHasDigitalSignature());
        e.setHasDigitalStamp(d.isHasDigitalStamp());
        e.setOrderType(unwrapStr(d.getOrderType()));

        PreparationProcess pp = unwrap(d.getPreProcess());
        if (pp != null) {
            e.setPreparationProcess(buildProcess(pp, true, pendingResponsibleParents, pendingViseParents));
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

    private EdocProcessEntity buildProcess(org.datacontract.schemas._2004._07.fas_docmanagement_integration.Process p,
                                           boolean isPreparation,
                                           List<PendingResponsibleParent> pendingResponsibleParents,
                                           List<PendingViseParent> pendingViseParents) {
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
                buildResponsible(pe, rd, null, pendingResponsibleParents);
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
                    buildVise(pe, vd, null, pendingViseParents);
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
    private void buildResponsible(EdocProcessEntity pe,
                                  ResponsibleData rd,
                                  EdocResponsibleEntity parent,
                                  List<PendingResponsibleParent> pendingResponsibleParents) {
        EdocResponsibleEntity re = new EdocResponsibleEntity();
        re.setProcess(pe);
        re.setParentId(parent != null ? parent.getId() : null);
        re.setStartDate(toOffset(rd.getStartDate()));
        re.setDeadline(toOffset(rd.getDeadline()));
        re.setTask(unwrapStr(rd.getTask()));
        re.setStatus(rd.getStatus() != null ? rd.getStatus().value() : null);
        re.setStatusChangeDate(toOffset(rd.getStatusChangeDate()));
        re.setEmployee(buildEmployeeNullable(unwrap(rd.getEmployee())));
        pe.getResponsibles().add(re);
        if (parent != null) {
            pendingResponsibleParents.add(new PendingResponsibleParent(re, parent));
        }

        ArrayOfResponsibleData children = unwrap(rd.getChildResponsibles());
        if (children != null && children.getResponsibleData() != null) {
            for (ResponsibleData child : children.getResponsibleData()) {
                buildResponsible(pe, child, re, pendingResponsibleParents);
            }
        }
    }

    private void buildVise(EdocProcessEntity pe,
                           ViseData vd,
                           EdocViseEntity parent,
                           List<PendingViseParent> pendingViseParents) {
        EdocViseEntity ve = new EdocViseEntity();
        ve.setProcess(pe);
        ve.setParentId(parent != null ? parent.getId() : null);
        ve.setAuthor(buildEmployeeNullable(unwrap(vd.getAuthor())));
        ve.setCreator(buildEmployeeNullable(unwrap(vd.getCreator())));
        ve.setDeadline(toOffset(vd.getDeadline()));
        ve.setEntryDate(toOffset(vd.getEntryDate()));
        ve.setStatus(vd.getStatus() != null ? vd.getStatus().value() : null);
        ve.setStatusChangeDate(toOffset(vd.getStatusChangeDate()));
        pe.getVises().add(ve);
        if (parent != null) {
            pendingViseParents.add(new PendingViseParent(ve, parent));
        }

        ArrayOfViseData children = unwrap(vd.getChildVises());
        if (children != null && children.getViseData() != null) {
            for (ViseData child : children.getViseData()) {
                buildVise(pe, child, ve, pendingViseParents);
            }
        }
    }

    private void resolveDeferredHierarchyLinks(List<PendingResponsibleParent> pendingResponsibleParents,
                                               List<PendingViseParent> pendingViseParents) {
        boolean changed = false;

        for (PendingResponsibleParent pending : pendingResponsibleParents) {
            EdocResponsibleEntity child = pending.child();
            EdocResponsibleEntity parent = pending.parent();
            if (child != null && parent != null && child.getParentId() == null && parent.getId() != null) {
                child.setParentId(parent.getId());
                changed = true;
            }
        }

        for (PendingViseParent pending : pendingViseParents) {
            EdocViseEntity child = pending.child();
            EdocViseEntity parent = pending.parent();
            if (child != null && parent != null && child.getParentId() == null && parent.getId() != null) {
                child.setParentId(parent.getId());
                changed = true;
            }
        }

        if (changed) {
            entityManager.flush();
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
        if (emp == null) {
            return null;
        }

        String firstName = normalizeNullable(unwrapStr(emp.getFirstName()));
        String lastName = normalizeNullable(unwrapStr(emp.getLastName()));
        String position = normalizeNullable(unwrapStr(emp.getPosition()));
        String organizationStructure = normalizeNullable(unwrapStr(emp.getOrganizationStructure()));

        Optional<EdocEmployeeEntity> existing = employeeRepo.findCanonical(firstName, lastName, position, organizationStructure);
        if (existing.isPresent()) {
            return existing.get();
        }

        EdocEmployeeEntity e = new EdocEmployeeEntity();
        e.setFirstName(firstName);
        e.setLastName(lastName);
        e.setPosition(position);
        e.setOrganizationStructure(organizationStructure);
        return employeeRepo.save(e);
    }

    private EdocEmployeeEntity buildEmployeeNullable(EmployeeData emp) {
        return emp == null ? null : buildEmployee(emp);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper: save/update contact entity (upsert by UUID)
    // ─────────────────────────────────────────────────────────────────────────

    private EdocContactEntity saveOrUpdateContact(Contact c) {
        UUID id = extractId(c.getID());
        EdocContactEntity ce = contactRepo.findById(id).orElseGet(() -> {
            EdocContactEntity created = new EdocContactEntity();
            created.setId(id);
            return created;
        });

        ce.setContactType(c.getContactType() != null ? c.getContactType().value() : "Unknown");
        ce.setFirstName(null);
        ce.setLastName(null);
        ce.setPersonalNumber(null);
        ce.setIdentificationNumber(null);
        ce.setJuridicalForm(null);
        ce.setName(null);

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
        dto.setTask(resolveDocumentTask(e));
        dto.setFromCache(true);
        dto.setCachedAt(e.getCachedAt());
        dto.setFetchCount(e.getFetchCount());

        dto.setFiles(e.getFiles().stream().map(f -> {
            EdocDocumentFileDto fd = new EdocDocumentFileDto();
            fd.setId(f.getId());
            fd.setName(f.getName());
            fd.setFileType(f.getFileType());
            fd.setSize(f.getContent() != null ? (long) f.getContent().length : null);
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

        dto.setAddressees(resolveDocumentAddressees(e));
        dto.setResponsibles(resolveDocumentResponsibles(e));
        dto.setEmployeeSenders(empDtoList(e, "INTERNAL_SENDER"));
        dto.setEmployeeRecipients(empDtoList(e, "INTERNAL_RECIPIENT"));
        dto.setSignatories(resolveDocumentSignatories(e));
        dto.setInnerRecipients(empDtoList(e, "ORDER_INNER_RECIPIENT"));
        dto.setRelatedEmployees(empDtoList(e, "ORDER_RELATED_EMPLOYEE"));
        dto.setSignatures(signatureDtoList(e));
        dto.setVises(viseDtoList(e));

        return dto;
    }

    private String resolveDocumentTask(EdocCachedDocumentEntity e) {
        String task = normalizeNullable(e != null && e.getResultProcess() != null ? e.getResultProcess().getTaskText() : null);
        if (task != null) {
            return task;
        }

        task = normalizeNullable(e != null && e.getPreparationProcess() != null ? e.getPreparationProcess().getTaskText() : null);
        if (task != null) {
            return task;
        }

        task = firstResponsibleTask(e != null ? e.getResultProcess() : null);
        if (task != null) {
            return task;
        }

        return firstResponsibleTask(e != null ? e.getPreparationProcess() : null);
    }

    private String firstResponsibleTask(EdocProcessEntity process) {
        if (process == null || process.getResponsibles() == null) {
            return null;
        }
        for (EdocResponsibleEntity responsible : process.getResponsibles()) {
            if (responsible == null) {
                continue;
            }
            String task = normalizeNullable(responsible.getTask());
            if (task != null) {
                return task;
            }
        }
        return null;
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

    private List<EdocEmployeeDto> resolveDocumentAddressees(EdocCachedDocumentEntity e) {
        List<EdocEmployeeDto> incoming = empDtoList(e, "INCOMING_ADDRESSEE");
        if (incoming != null && !incoming.isEmpty()) {
            return incoming;
        }

        // For ORDER documents eDoc often doesn't provide explicit Addressees.
        // Keep historical behavior used by existing objects:
        // top-level ResultProcess.Responsibles only.
        if (e != null && "Order".equalsIgnoreCase(e.getDocumentType())) {
            List<EdocEmployeeDto> topLevelResult = resolveOrderTopLevelResultAddressees(e);
            if (topLevelResult != null && !topLevelResult.isEmpty()) return topLevelResult;
        }
        return null;
    }

    private List<EdocEmployeeDto> resolveOrderTopLevelResultAddressees(EdocCachedDocumentEntity e) {
        if (e == null || e.getResultProcess() == null || e.getResultProcess().getResponsibles() == null) {
            return null;
        }
        LinkedHashMap<String, EdocEmployeeDto> unique = new LinkedHashMap<>();
        for (EdocResponsibleEntity responsible : e.getResultProcess().getResponsibles()) {
            if (responsible == null || responsible.getEmployee() == null) {
                continue;
            }
            EdocEmployeeDto dto = toEmployeeDto(responsible.getEmployee());
            String key = String.join("|",
                    normalizePart(dto.getFirstName()),
                    normalizePart(dto.getLastName()),
                    normalizePart(dto.getPosition()),
                    normalizePart(dto.getOrganizationStructure())
            );
            unique.putIfAbsent(key, dto);
        }
        return unique.isEmpty() ? null : new ArrayList<>(unique.values());
    }

    private List<EdocEmployeeDto> resolveDocumentSignatories(EdocCachedDocumentEntity e) {
        LinkedHashMap<String, EdocEmployeeDto> unique = new LinkedHashMap<>();

        if (e != null && "Order".equalsIgnoreCase(e.getDocumentType())
                && e.getPreparationProcess() != null
                && e.getPreparationProcess().getSignatures() != null) {
            for (EdocSignatureEntity signature : e.getPreparationProcess().getSignatures()) {
                if (signature == null || signature.getSignatory() == null) {
                    continue;
                }
                EdocEmployeeDto dto = toEmployeeDto(signature.getSignatory());
                String key = String.join("|",
                        normalizePart(dto.getFirstName()),
                        normalizePart(dto.getLastName()),
                        normalizePart(dto.getPosition()),
                        normalizePart(dto.getOrganizationStructure())
                );
                unique.putIfAbsent(key, dto);
            }
            if (!unique.isEmpty()) {
                return new ArrayList<>(unique.values());
            }
        }

        List<EdocEmployeeDto> orderSignatories = empDtoList(e, "ORDER_SIGNATORY");
        if (orderSignatories != null && !orderSignatories.isEmpty()) {
            return orderSignatories;
        }
        return empDtoList(e, "OUTGOING_SIGNATORY");
    }

    private List<EdocSignatureDto> signatureDtoList(EdocCachedDocumentEntity e) {
        if (e.getPreparationProcess() == null || e.getPreparationProcess().getSignatures() == null) {
            return null;
        }
        List<EdocSignatureDto> signatures = e.getPreparationProcess().getSignatures().stream()
                .map(this::toSignatureDto)
                .toList();
        return signatures.isEmpty() ? null : signatures;
    }

    private List<EdocViseDto> viseDtoList(EdocCachedDocumentEntity e) {
        if (e.getPreparationProcess() == null || e.getPreparationProcess().getVises() == null) {
            return null;
        }
        List<EdocViseDto> vises = e.getPreparationProcess().getVises().stream()
                .sorted(Comparator.comparing(EdocViseEntity::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toViseDto)
                .toList();
        return vises.isEmpty() ? null : vises;
    }

    private List<EdocEmployeeDto> resolveDocumentResponsibles(EdocCachedDocumentEntity e) {
        LinkedHashMap<String, EdocEmployeeDto> unique = new LinkedHashMap<>();
        if (e != null && "Order".equalsIgnoreCase(e.getDocumentType())) {
            collectResponsiblesFromProcess(e.getPreparationProcess(), unique);
            if (unique.isEmpty()) {
                collectResponsiblesFromProcess(e.getResultProcess(), unique);
            }
        } else {
            collectResponsiblesFromProcess(e != null ? e.getResultProcess() : null, unique);
            if (unique.isEmpty()) {
                collectResponsiblesFromProcess(e != null ? e.getPreparationProcess() : null, unique);
            }
        }
        if (unique.isEmpty()) {
            return null;
        }
        return new ArrayList<>(unique.values());
    }

    private void collectResponsiblesFromProcess(EdocProcessEntity process, LinkedHashMap<String, EdocEmployeeDto> unique) {
        if (process == null || process.getResponsibles() == null) {
            return;
        }
        for (EdocResponsibleEntity responsible : process.getResponsibles()) {
            if (responsible == null || responsible.getEmployee() == null) {
                continue;
            }
            EdocEmployeeDto dto = toEmployeeDto(responsible.getEmployee());
            String key = String.join("|",
                    normalizePart(dto.getFirstName()),
                    normalizePart(dto.getLastName()),
                    normalizePart(dto.getPosition()),
                    normalizePart(dto.getOrganizationStructure())
            );
            unique.putIfAbsent(key, dto);
        }
    }

    private String normalizePart(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

    private EdocSignatureDto toSignatureDto(EdocSignatureEntity signature) {
        EdocSignatureDto dto = new EdocSignatureDto();
        dto.setCreator(toEmployeeDtoNullable(signature.getCreator()));
        dto.setSignatory(toEmployeeDtoNullable(signature.getSignatory()));
        dto.setDeadline(signature.getDeadline());
        dto.setEntryDate(signature.getEntryDate());
        dto.setStatus(signature.getStatus());
        dto.setStatusChangeDate(signature.getStatusChangeDate());
        return dto;
    }

    private EdocViseDto toViseDto(EdocViseEntity vise) {
        EdocViseDto dto = new EdocViseDto();
        dto.setParentId(vise.getParentId());
        dto.setAuthor(toEmployeeDtoNullable(vise.getAuthor()));
        dto.setCreator(toEmployeeDtoNullable(vise.getCreator()));
        dto.setDeadline(vise.getDeadline());
        dto.setEntryDate(vise.getEntryDate());
        dto.setStatus(vise.getStatus());
        dto.setStatusChangeDate(vise.getStatusChangeDate());
        return dto;
    }

    private EdocEmployeeDto toEmployeeDtoNullable(EdocEmployeeEntity employee) {
        return employee != null ? toEmployeeDto(employee) : null;
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
