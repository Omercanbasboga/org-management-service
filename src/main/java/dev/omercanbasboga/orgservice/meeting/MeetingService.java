package dev.omercanbasboga.orgservice.meeting;

import dev.omercanbasboga.orgservice.approval.ApprovalKind;
import dev.omercanbasboga.orgservice.approval.ApprovalService;
import dev.omercanbasboga.orgservice.approval.ApprovalStatus;
import dev.omercanbasboga.orgservice.approval.ApprovalSubject;
import dev.omercanbasboga.orgservice.common.NotFoundException;
import dev.omercanbasboga.orgservice.membership.OrgAccess;
import dev.omercanbasboga.orgservice.membership.OrgRole;
import dev.omercanbasboga.orgservice.organization.Organization;
import dev.omercanbasboga.orgservice.organization.OrganizationService;
import dev.omercanbasboga.orgservice.reference.Location;
import dev.omercanbasboga.orgservice.reference.LocationRepository;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MeetingService implements ApprovalSubject {

    public record MeetingDto(UUID id, String title, String description, LocalDateTime date, UUID locationId,
                             String location, Integer attendeeCount, ApprovalStatus status, boolean approved) {
        static MeetingDto of(Meeting m) {
            Location l = m.getLocation();
            return new MeetingDto(m.getId(), m.getTitle(), m.getDescription(), m.getStartsAt(),
                    l == null ? null : l.getId(), l == null ? null : l.getName(), m.getExpectedAttendees(),
                    m.getStatus(), m.getStatus() == ApprovalStatus.APPROVED);
        }
    }

    public record MeetingRequest(@NotBlank @Size(max = 200) String title, @Size(max = 2000) String description,
                                 @NotNull LocalDateTime date, UUID locationId, @Min(1) Integer attendeeCount) {}

    private final MeetingRepository meetings;
    private final LocationRepository locations;
    private final OrganizationService orgs;
    private final OrgAccess access;
    private final ApprovalService approvals;

    public MeetingService(MeetingRepository meetings, LocationRepository locations, OrganizationService orgs,
                          OrgAccess access, @Lazy ApprovalService approvals) {
        this.meetings = meetings;
        this.locations = locations;
        this.orgs = orgs;
        this.access = access;
        this.approvals = approvals;
    }

    @Transactional(readOnly = true)
    public List<MeetingDto> list(UUID orgId) {
        orgs.require(orgId);
        return meetings.findByOrganizationIdOrderByStartsAtDesc(orgId).stream().map(MeetingDto::of).toList();
    }

    @Transactional
    public MeetingDto schedule(UUID orgId, MeetingRequest req) {
        Organization org = orgs.require(orgId);
        if (!org.isActive()) throw new IllegalStateException("This organization is closed.");
        String by = access.require(orgId, OrgRole::canScheduleMeetings, "schedule meetings").email();
        Meeting m = new Meeting(org);
        apply(m, req);
        m.setStatus(ApprovalStatus.PENDING_ADMIN);
        meetings.save(m);
        m.setStatus(approvals.open(ApprovalKind.MEETING, m.getId(), org, by));
        return MeetingDto.of(m);
    }

    /**
     * A meeting can be edited until it is approved; after that, cancel and
     * schedule a new one. An edit after the advisor signed starts the approval over.
     */
    @Transactional
    public MeetingDto update(UUID orgId, UUID meetingId, MeetingRequest req) {
        Meeting m = require(orgId, meetingId);
        String by = access.require(orgId, OrgRole::canScheduleMeetings, "edit meetings").email();
        if (!m.getStatus().isPending()) {
            throw new IllegalStateException("Only meetings still waiting for approval can be edited.");
        }
        apply(m, req);
        // the advisor may already have signed the old version; the new one needs both signatures again
        if (m.getStatus() == ApprovalStatus.PENDING_ADMIN) {
            m.setStatus(approvals.restart(ApprovalKind.MEETING, m.getId(), m.getOrganization(), by));
        }
        return MeetingDto.of(m);
    }

    @Transactional
    public void cancel(UUID orgId, UUID meetingId) {
        Meeting m = require(orgId, meetingId);
        String by = access.require(orgId, OrgRole::canScheduleMeetings, "cancel meetings").email();
        approvals.withdraw(ApprovalKind.MEETING, m.getId(), by);
        meetings.delete(m);
    }

    private void apply(Meeting m, MeetingRequest req) {
        m.setTitle(req.title().trim());
        m.setDescription(req.description());
        m.setStartsAt(req.date());
        m.setExpectedAttendees(req.attendeeCount());
        m.setLocation(req.locationId() == null ? null : locations.findById(req.locationId())
                .orElseThrow(() -> new NotFoundException("Location", req.locationId())));
        if (m.getLocation() != null && m.getLocation().getCapacity() != null && req.attendeeCount() != null
                && req.attendeeCount() > m.getLocation().getCapacity()) {
            throw new IllegalArgumentException(m.getLocation().getName() + " holds "
                    + m.getLocation().getCapacity() + " people.");
        }
    }

    private Meeting require(UUID orgId, UUID meetingId) {
        return meetings.findById(meetingId)
                .filter(m -> m.getOrganization().getId().equals(orgId))
                .orElseThrow(() -> new NotFoundException("Meeting", meetingId));
    }

    @Override
    public ApprovalKind kind() {
        return ApprovalKind.MEETING;
    }

    @Override
    public String describe(UUID subjectId) {
        return meetings.findById(subjectId).map(Meeting::getTitle).orElse("(cancelled meeting)");
    }

    @Override
    public void onStatusChange(UUID subjectId, ApprovalStatus status) {
        meetings.findById(subjectId).ifPresent(m -> m.setStatus(status));
    }
}
