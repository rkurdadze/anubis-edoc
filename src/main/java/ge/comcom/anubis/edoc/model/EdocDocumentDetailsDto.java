package ge.comcom.anubis.edoc.model;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class EdocDocumentDetailsDto {
    private UUID id;
    private String number;
    private String category;
    private String documentType;
    private String documentStatus;
    private String exportStatus;
    private OffsetDateTime registrationDate;
    private OffsetDateTime deadline;
    private String annotation;
    private String comments;
    private String chancellary;
    private String purpose;
    private String task;
    private List<EdocDocumentFileDto> files;
    private List<EdocRelatedDocumentDto> relatedDocuments;

    // Cache metadata (populated when served from local DB)
    private Boolean fromCache;
    private OffsetDateTime cachedAt;
    private Integer fetchCount;

    // IncomingDocumentData
    private String originalNumber;
    private OffsetDateTime originalDate;
    private List<EdocContactDto> senders;           // Contact[]
    private List<EdocEmployeeDto> addressees;        // EmployeeData[]
    private List<EdocEmployeeDto> responsibles;      // ResultProcess.Responsibles (flattened)
    private EdocEmployeeDto taskInitiatedBy;         // ResultProcess/PreProcess.Task.InitiatedBy
    private List<EdocReceiveWayDto> receiveWays;

    // InternalDocumentData
    private List<EdocEmployeeDto> employeeSenders;   // EmployeeData[]
    private List<EdocEmployeeDto> employeeRecipients; // EmployeeData[]

    // InternalDocumentData / OutgoingDocumentData / OrderDocumentData
    private Boolean hasDigitalSignature;
    private Boolean hasDigitalStamp;

    // OutgoingDocumentData
    private List<EdocContactDto> recipients;         // Contact[]
    private List<EdocEmployeeDto> signatories;       // EmployeeData[]
    private List<EdocSignatureDto> signatures;       // PreparationProcess.Signatures

    // OrderDocumentData
    private String direction;
    private String orderType;
    private List<EdocContactDto> outerRecipients;    // Contact[]
    private List<EdocEmployeeDto> innerRecipients;   // EmployeeData[]
    private List<EdocEmployeeDto> relatedEmployees;  // EmployeeData[]
    private List<EdocViseDto> vises;                 // PreparationProcess.Vises
}
