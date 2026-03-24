package ge.comcom.anubis.edoc.model;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class EdocCacheStatusDto {
    private UUID id;
    private boolean cached;
    private String documentStatus;
    private String documentType;
    private OffsetDateTime cachedAt;
    private int fetchCount;
    private OffsetDateTime lastFetchedAt;
}
