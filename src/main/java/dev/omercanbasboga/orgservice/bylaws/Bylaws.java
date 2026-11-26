package dev.omercanbasboga.orgservice.bylaws;

import dev.omercanbasboga.orgservice.approval.ApprovalStatus;
import dev.omercanbasboga.orgservice.common.BaseEntity;
import dev.omercanbasboga.orgservice.organization.Organization;
import jakarta.persistence.*;

/** One uploaded version of an organization's bylaws. Older versions are kept. */
@Entity
@Table(name = "bylaws", indexes = @Index(name = "idx_bylaws_org", columnList = "organization_id, created_at"))
public class Bylaws extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /** The name the file was uploaded with, for display only. */
    @Column(name = "file_name", nullable = false)
    private String fileName;

    /** The name on disk: generated, never taken from the client. */
    @Column(name = "stored_name", nullable = false, unique = true)
    private String storedName;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "size_bytes")
    private long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ApprovalStatus status;

    protected Bylaws() {}

    public Bylaws(Organization organization, String fileName, String storedName, String contentType, long sizeBytes) {
        this.organization = organization;
        this.fileName = fileName;
        this.storedName = storedName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    public Organization getOrganization() { return organization; }
    public String getFileName() { return fileName; }
    public String getStoredName() { return storedName; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public ApprovalStatus getStatus() { return status; }
    public void setStatus(ApprovalStatus status) { this.status = status; }
}
