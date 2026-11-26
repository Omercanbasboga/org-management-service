package dev.omercanbasboga.orgservice.membership;

import dev.omercanbasboga.orgservice.approval.ApprovalStatus;
import dev.omercanbasboga.orgservice.common.BaseEntity;
import dev.omercanbasboga.orgservice.organization.Organization;
import dev.omercanbasboga.orgservice.person.Person;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * One person in one organization. A membership only counts once its approval
 * finishes; until then it sits in the queue with a pending status.
 */
@Entity
@Table(name = "membership", indexes = @Index(name = "idx_membership_org", columnList = "organization_id, status"))
public class Membership extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrgRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MembershipType type = MembershipType.FULL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ApprovalStatus status;

    /** Set when the approval completes. */
    @Column(name = "joined_at")
    private Instant joinedAt;

    /** Set when someone is taken off the roster; the row stays for the record. */
    @Column(name = "left_at")
    private Instant leftAt;

    protected Membership() {}

    public Membership(Organization organization, Person person, OrgRole role, MembershipType type, ApprovalStatus status) {
        this.organization = organization;
        this.person = person;
        this.role = role;
        this.type = type;
        this.status = status;
    }

    public boolean isCurrent() {
        return status == ApprovalStatus.APPROVED && leftAt == null;
    }

    public Organization getOrganization() { return organization; }
    public Person getPerson() { return person; }
    public OrgRole getRole() { return role; }
    public MembershipType getType() { return type; }
    public ApprovalStatus getStatus() { return status; }
    public Instant getJoinedAt() { return joinedAt; }
    public Instant getLeftAt() { return leftAt; }

    public void setStatus(ApprovalStatus status) {
        this.status = status;
        if (status == ApprovalStatus.APPROVED && joinedAt == null) joinedAt = Instant.now();
    }

    /** For seeding history; normal joins get their date from the approval. */
    public void setJoinedAt(Instant joinedAt) { this.joinedAt = joinedAt; }

    public void leave() {
        this.leftAt = Instant.now();
    }
}
