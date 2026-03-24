-- edoc_employee: employee data (no natural ID from eDocument, bigserial PK)
CREATE TABLE edoc_employee (
    id                   BIGSERIAL PRIMARY KEY,
    first_name           VARCHAR(255),
    last_name            VARCHAR(255),
    position             VARCHAR(500),
    organization_structure VARCHAR(500)
);

-- edoc_contact: contact data (PhysicalPerson / Organization / StateStructure)
-- UUID PK comes directly from eDocument
CREATE TABLE edoc_contact (
    id                   UUID PRIMARY KEY,
    contact_type         VARCHAR(50)  NOT NULL,
    -- PhysicalPerson
    first_name           VARCHAR(255),
    last_name            VARCHAR(255),
    personal_number      VARCHAR(50),
    -- Organization
    identification_number VARCHAR(50),
    juridical_form       VARCHAR(500),
    -- Organization / StateStructure
    name                 VARCHAR(500)
);

-- edoc_process: base for both ResultProcess and PreparationProcess
CREATE TABLE edoc_process (
    id                        BIGSERIAL PRIMARY KEY,
    task_number               INT,
    task_start_date           TIMESTAMPTZ,
    task_deadline             TIMESTAMPTZ,
    task_text                 TEXT,
    task_is_initiated         BOOLEAN,
    task_initiated_by_id      BIGINT REFERENCES edoc_employee(id)
);

-- edoc_responsible: part of a Process (ResultProcess or PreparationProcess)
-- Supports hierarchy via parent_id (ChildResponsibles)
CREATE TABLE edoc_responsible (
    id                   BIGSERIAL PRIMARY KEY,
    process_id           BIGINT     NOT NULL REFERENCES edoc_process(id) ON DELETE CASCADE,
    parent_id            BIGINT     REFERENCES edoc_responsible(id) ON DELETE SET NULL,
    start_date           TIMESTAMPTZ,
    deadline             TIMESTAMPTZ,
    task                 TEXT,
    status               VARCHAR(50),
    status_change_date   TIMESTAMPTZ,
    employee_id          BIGINT     REFERENCES edoc_employee(id)
);

-- edoc_signature: part of PreparationProcess
CREATE TABLE edoc_signature (
    id                   BIGSERIAL PRIMARY KEY,
    process_id           BIGINT     NOT NULL REFERENCES edoc_process(id) ON DELETE CASCADE,
    creator_id           BIGINT     REFERENCES edoc_employee(id),
    deadline             TIMESTAMPTZ,
    entry_date           TIMESTAMPTZ,
    signatory_id         BIGINT     REFERENCES edoc_employee(id),
    status               VARCHAR(50),
    status_change_date   TIMESTAMPTZ
);

-- edoc_vise: part of PreparationProcess, supports hierarchy via parent_id (ChildVises)
CREATE TABLE edoc_vise (
    id                   BIGSERIAL PRIMARY KEY,
    process_id           BIGINT     NOT NULL REFERENCES edoc_process(id) ON DELETE CASCADE,
    parent_id            BIGINT     REFERENCES edoc_vise(id) ON DELETE SET NULL,
    author_id            BIGINT     REFERENCES edoc_employee(id),
    creator_id           BIGINT     REFERENCES edoc_employee(id),
    deadline             TIMESTAMPTZ,
    entry_date           TIMESTAMPTZ,
    status               VARCHAR(50),
    status_change_date   TIMESTAMPTZ
);

-- edoc_document: merged base table for ALL document types
-- Nullable columns are type-specific and only populated for the relevant DocumentType
CREATE TABLE edoc_document (
    id                   UUID        PRIMARY KEY,
    -- Document (base)
    document_type        VARCHAR(50) NOT NULL,
    number               VARCHAR(255),
    registration_date    TIMESTAMPTZ NOT NULL,
    deadline             TIMESTAMPTZ,
    category             VARCHAR(500),
    document_status      VARCHAR(50),
    export_status        VARCHAR(50),
    issue_decision_type  VARCHAR(500),
    -- DocumentData
    annotation           TEXT,
    chancellary          VARCHAR(500),
    comments             TEXT,
    purpose              VARCHAR(50),
    result_process_id    BIGINT      REFERENCES edoc_process(id),
    -- IncomingDocumentData
    original_number      VARCHAR(255),
    original_date        TIMESTAMPTZ,
    -- InternalDocumentData / OutgoingDocumentData / OrderDocumentData
    has_digital_signature BOOLEAN,
    has_digital_stamp     BOOLEAN,
    preparation_process_id BIGINT    REFERENCES edoc_process(id),
    -- OrderDocumentData
    direction            VARCHAR(50),
    order_type           VARCHAR(500),
    -- Cache metadata
    cached_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fetch_count          INT         NOT NULL DEFAULT 1,
    last_fetched_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- edoc_document_file: file attachments for a document
CREATE TABLE edoc_document_file (
    id           BIGSERIAL PRIMARY KEY,
    document_id  UUID        NOT NULL REFERENCES edoc_document(id) ON DELETE CASCADE,
    file_type    VARCHAR(50),
    name         VARCHAR(500),
    content      BYTEA
);

-- edoc_related_document: related document references
CREATE TABLE edoc_related_document (
    id                              BIGSERIAL PRIMARY KEY,
    document_id                     UUID        NOT NULL REFERENCES edoc_document(id) ON DELETE CASCADE,
    relation_type                   VARCHAR(50),
    related_doc_id                  UUID,
    related_doc_number              VARCHAR(255),
    related_doc_type                VARCHAR(50),
    related_doc_registration_date   TIMESTAMPTZ,
    related_doc_export_status       VARCHAR(50)
);

-- edoc_receive_way: receive ways for IncomingDocumentData
CREATE TABLE edoc_receive_way (
    id           BIGSERIAL PRIMARY KEY,
    document_id  UUID        NOT NULL REFERENCES edoc_document(id) ON DELETE CASCADE,
    way          VARCHAR(500),
    comments     TEXT
);

-- edoc_document_employee: employee participants with role discriminator
-- Roles: INCOMING_ADDRESSEE, INTERNAL_SENDER, INTERNAL_RECIPIENT,
--        OUTGOING_SIGNATORY, ORDER_INNER_RECIPIENT, ORDER_RELATED_EMPLOYEE, ORDER_SIGNATORY
CREATE TABLE edoc_document_employee (
    id           BIGSERIAL PRIMARY KEY,
    document_id  UUID        NOT NULL REFERENCES edoc_document(id) ON DELETE CASCADE,
    employee_id  BIGINT      NOT NULL REFERENCES edoc_employee(id),
    role         VARCHAR(50) NOT NULL
);

-- edoc_document_contact: contact participants with role discriminator
-- Roles: INCOMING_SENDER, OUTGOING_RECIPIENT, ORDER_OUTER_RECIPIENT
CREATE TABLE edoc_document_contact (
    id           BIGSERIAL PRIMARY KEY,
    document_id  UUID        NOT NULL REFERENCES edoc_document(id) ON DELETE CASCADE,
    contact_id   UUID        NOT NULL REFERENCES edoc_contact(id),
    role         VARCHAR(50) NOT NULL
);

-- Indexes
CREATE INDEX idx_edoc_document_status    ON edoc_document(document_status);
CREATE INDEX idx_edoc_document_type      ON edoc_document(document_type);
CREATE INDEX idx_edoc_file_doc_id        ON edoc_document_file(document_id);
CREATE INDEX idx_edoc_related_doc_id     ON edoc_related_document(document_id);
CREATE INDEX idx_edoc_receive_way_doc_id ON edoc_receive_way(document_id);
CREATE INDEX idx_edoc_doc_emp_doc_id     ON edoc_document_employee(document_id);
CREATE INDEX idx_edoc_doc_con_doc_id     ON edoc_document_contact(document_id);
CREATE INDEX idx_edoc_responsible_proc   ON edoc_responsible(process_id);
CREATE INDEX idx_edoc_signature_proc     ON edoc_signature(process_id);
CREATE INDEX idx_edoc_vise_proc          ON edoc_vise(process_id);
