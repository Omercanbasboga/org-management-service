package dev.omercanbasboga.orgservice.approval;

import dev.omercanbasboga.orgservice.common.BaseEntity;
import dev.omercanbasboga.orgservice.organization.Organization;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * One request waiting for sign-off. It points at its subject (a membership,
 * meeting or bylaws upload) by kind and id, and each subject package knows
 * how to describe and update its own rows (see {@link ApprovalSubject}).
 */
@Entity
@Table(name = "approval", indexes = @Index(name = "idx_approval_status", columnList = "status, organization_id"))
public class Approval extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ApprovalKind kind;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ApprovalStatus status;

    @Column(name = "requested_by")
    private String requestedBy;

    /** Who signed last, and when. */
    @Column(name = "decided_by")
    private String decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    protected Approval() {}

    public Approval(ApprovalKind kind, UUID subjectId, Organization organization, ApprovalStatus status,
                    String requestedBy) {
        this.kind = kind;
        this.subjectId = subjectId;
        this.organization = organization;
        this.status = status;
        this.requestedBy = requestedBy;
    }

    void record(ApprovalStatus next, String by, String reason) {
        this.status = next;
        this.decidedBy = by;
        this.decidedAt = Instant.now();
        if (reason != null) this.rejectReason = reason;
    }

    public ApprovalKind getKind() { return kind; }
    public UUID getSubjectId() { return subjectId; }
    public Organization getOrganization() { return organization; }
    public ApprovalStatus getStatus() { return status; }
    public String getRequestedBy() { return requestedBy; }
    public String getDecidedBy() { return decidedBy; }
    public Instant getDecidedAt() { return decidedAt; }
    public String getRejectReason() { return rejectReason; }
}
