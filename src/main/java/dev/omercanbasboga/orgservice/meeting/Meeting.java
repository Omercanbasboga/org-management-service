package dev.omercanbasboga.orgservice.meeting;

import dev.omercanbasboga.orgservice.approval.ApprovalStatus;
import dev.omercanbasboga.orgservice.common.BaseEntity;
import dev.omercanbasboga.orgservice.organization.Organization;
import dev.omercanbasboga.orgservice.reference.Location;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "meeting", indexes = @Index(name = "idx_meeting_org_date", columnList = "organization_id, starts_at"))
public class Meeting extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "expected_attendees")
    private Integer expectedAttendees;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ApprovalStatus status;

    protected Meeting() {}

    public Meeting(Organization organization) {
        this.organization = organization;
    }

    public Organization getOrganization() { return organization; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }

    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }

    public Integer getExpectedAttendees() { return expectedAttendees; }
    public void setExpectedAttendees(Integer expectedAttendees) { this.expectedAttendees = expectedAttendees; }

    public ApprovalStatus getStatus() { return status; }
    public void setStatus(ApprovalStatus status) { this.status = status; }
}
