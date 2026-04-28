package ge.comcom.anubis.edoc.model;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class EdocSignatureDto {
    private EdocEmployeeDto creator;
    private EdocEmployeeDto signatory;
    private OffsetDateTime deadline;
    private OffsetDateTime entryDate;
    private String status;
    private OffsetDateTime statusChangeDate;
}

