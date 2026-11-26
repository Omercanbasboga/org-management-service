package dev.omercanbasboga.orgservice.membership;

import dev.omercanbasboga.orgservice.approval.ApprovalKind;
import dev.omercanbasboga.orgservice.approval.ApprovalService;
import dev.omercanbasboga.orgservice.approval.ApprovalStatus;
import dev.omercanbasboga.orgservice.approval.ApprovalSubject;
import dev.omercanbasboga.orgservice.auth.CurrentUser;
import dev.omercanbasboga.orgservice.auth.CurrentUserProvider;
import dev.omercanbasboga.orgservice.common.NotFoundException;
import dev.omercanbasboga.orgservice.organization.Organization;
import dev.omercanbasboga.orgservice.organization.OrganizationService;
import dev.omercanbasboga.orgservice.person.Person;
import dev.omercanbasboga.orgservice.person.PersonRepository;
import dev.omercanbasboga.orgservice.person.SystemRole;
import jakarta.validation.constraints.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MembershipService implements ApprovalSubject {

    public record MemberDto(UUID id, UUID personId, String name, String memberId, String email, String department,
                            OrgRole role, MembershipType type, ApprovalStatus status, Instant joinedAt) {
        static MemberDto of(Membership m) {
            Person p = m.getPerson();
            return new MemberDto(m.getId(), p.getId(), p.fullName(), p.getMemberId(), p.getEmail(),
                    p.getDepartment(), m.getRole(), m.getType(), m.getStatus(), m.getJoinedAt());
        }
    }

    public record MemberRequest(@NotNull UUID personId, OrgRole role, MembershipType type) {}

    private static final List<ApprovalStatus> OPEN = List.of(
            ApprovalStatus.PENDING_ADVISOR, ApprovalStatus.PENDING_ADMIN, ApprovalStatus.APPROVED);

    private final MembershipRepository memberships;
    private final PersonRepository people;
    private final OrganizationService orgs;
    private final OrgAccess access;
    private final CurrentUserProvider currentUser;
    private final ApprovalService approvals;

    public MembershipService(MembershipRepository memberships, PersonRepository people, OrganizationService orgs,
                             OrgAccess access, CurrentUserProvider currentUser, @Lazy ApprovalService approvals) {
        this.memberships = memberships;
        this.people = people;
        this.orgs = orgs;
        this.access = access;
        this.currentUser = currentUser;
        this.approvals = approvals;
    }

    @Transactional(readOnly = true)
    public List<MemberDto> current(UUID orgId) {
        orgs.require(orgId);
        return memberships.findCurrent(orgId).stream().map(MemberDto::of).toList();
    }

    /**
     * Anyone can ask to join; officers can add someone else. Either way it is a
     * request that goes through approval. Advisors are appointed by an admin and
     * skip the queue, since they are the ones who would sign it.
     */
    @Transactional
    public MemberDto request(UUID orgId, MemberRequest req) {
        Organization org = orgs.require(orgId);
        if (!org.isActive()) throw new IllegalStateException("This organization is closed.");
        Person person = people.findById(req.personId()).orElseThrow(() -> new NotFoundException("Person", req.personId()));
        OrgRole role = req.role() == null ? OrgRole.MEMBER : req.role();
        MembershipType type = req.type() == null ? MembershipType.FULL : req.type();

        CurrentUser user = currentUser.get();
        boolean self = person.getEmail().equalsIgnoreCase(user.email());
        if (role == OrgRole.ADVISOR) {
            access.requireAdmin("appoint an advisor");
            if (person.getSystemRole() != SystemRole.STAFF) {
                throw new IllegalArgumentException("An advisor has to be a member of staff.");
            }
        } else if (!(self && role == OrgRole.MEMBER)) {
            access.require(orgId, OrgRole::canManageMembers, "add members or give out roles");
        }
        if (memberships.existsByOrganizationIdAndPersonIdAndStatusInAndLeftAtIsNull(orgId, person.getId(), OPEN)) {
            throw new IllegalStateException(person.fullName() + " is already a member or has a request open.");
        }

        Membership m = memberships.save(new Membership(org, person, role, type, ApprovalStatus.PENDING_ADMIN));
        if (role == OrgRole.ADVISOR) {
            m.setStatus(ApprovalStatus.APPROVED);
        } else {
            m.setStatus(approvals.open(ApprovalKind.MEMBERSHIP, m.getId(), org, user.email()));
        }
        return MemberDto.of(m);
    }

    @Transactional
    public void remove(UUID orgId, UUID membershipId) {
        Membership m = memberships.findById(membershipId)
                .filter(x -> x.getOrganization().getId().equals(orgId))
                .orElseThrow(() -> new NotFoundException("Membership", membershipId));
        CurrentUser user = currentUser.get();
        boolean self = m.getPerson().getEmail().equalsIgnoreCase(user.email());
        if (m.getRole() == OrgRole.ADVISOR) {
            access.requireAdmin("remove an advisor");
        } else if (!self) {
            access.require(orgId, OrgRole::canManageMembers, "remove members");
        }
        if (m.getStatus().isPending()) {
            // a request nobody has finished signing: withdraw it instead of leaving it in the queue
            approvals.withdraw(ApprovalKind.MEMBERSHIP, m.getId(), user.email());
            m.setStatus(ApprovalStatus.REJECTED);
            return;
        }
        if (!m.isCurrent()) throw new IllegalStateException("That membership isn't active.");
        m.leave();
        if (m.getRole() == OrgRole.ADVISOR) approvals.advisorLeft(orgId);
    }

    // ---- approval subject ---------------------------------------------

    @Override
    public ApprovalKind kind() {
        return ApprovalKind.MEMBERSHIP;
    }

    @Override
    public String describe(UUID subjectId) {
        return memberships.findById(subjectId)
                .map(m -> m.getPerson().fullName() + (m.getRole() == OrgRole.MEMBER ? "" : " as " + label(m.getRole())))
                .orElse("(removed membership)");
    }

    @Override
    public void onStatusChange(UUID subjectId, ApprovalStatus status) {
        memberships.findById(subjectId).ifPresent(m -> m.setStatus(status));
    }

    private static String label(OrgRole r) {
        return r.name().toLowerCase().replace('_', ' ');
    }

}
