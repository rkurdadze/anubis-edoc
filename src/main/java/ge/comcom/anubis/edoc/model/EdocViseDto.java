package ge.comcom.anubis.edoc.model;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class EdocViseDto {
    private Long parentId;
    private EdocEmployeeDto author;
    private EdocEmployeeDto creator;
    private OffsetDateTime deadline;
    private OffsetDateTime entryDate;
    private String status;
    private OffsetDateTime statusChangeDate;
}

