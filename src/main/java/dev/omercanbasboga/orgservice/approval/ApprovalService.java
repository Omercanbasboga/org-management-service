package dev.omercanbasboga.orgservice.approval;

import dev.omercanbasboga.orgservice.auth.CurrentUser;
import dev.omercanbasboga.orgservice.auth.CurrentUserProvider;
import dev.omercanbasboga.orgservice.common.NotFoundException;
import dev.omercanbasboga.orgservice.membership.Membership;
import dev.omercanbasboga.orgservice.membership.MembershipRepository;
import dev.omercanbasboga.orgservice.membership.OrgRole;
import dev.omercanbasboga.orgservice.organization.Organization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ApprovalService {

    public record QueueItem(UUID id, ApprovalKind kind, String title, UUID organizationId,
                            String organizationName, ApprovalStatus status, String requestedBy, Instant requestedAt) {}

    /** Field names match what org-management-console reads. */
    public record Queue(List<QueueItem> membershipRequests, List<QueueItem> meetings, List<QueueItem> bylawsUploads) {}

    private final ApprovalRepository approvals;
    private final MembershipRepository memberships;
    private final CurrentUserProvider currentUser;
    private final Map<ApprovalKind, ApprovalSubject> subjects;

    public ApprovalService(ApprovalRepository approvals, MembershipRepository memberships,
                           CurrentUserProvider currentUser, List<ApprovalSubject> subjects) {
        this.approvals = approvals;
        this.memberships = memberships;
        this.currentUser = currentUser;
        this.subjects = subjects.stream().collect(Collectors.toMap(
                ApprovalSubject::kind, Function.identity(), (a, b) -> a, () -> new EnumMap<>(ApprovalKind.class)));
    }

    /** Opens a request. Returns the status the subject should start in. */
    @Transactional
    public ApprovalStatus open(ApprovalKind kind, UUID subjectId, Organization org, String requestedBy) {
        ApprovalStatus first = ApprovalWorkflow.firstStep(memberships.hasAdvisor(org.getId()));
        approvals.save(new Approval(kind, subjectId, org, first, requestedBy));
        return first;
    }

    /**
     * What the caller can act on right now: admins see everything at the admin
     * step, advisors see their own organizations' requests at the advisor step.
     */
    @Transactional(readOnly = true)
    public Queue queueFor(CurrentUser user) {
        List<Approval> mine = new ArrayList<>();
        if (user.admin()) mine.addAll(approvals.findByStatus(ApprovalStatus.PENDING_ADMIN));
        Set<UUID> advisedOrgs = memberships.findAdvisorRoles(user.email()).stream()
                .map(m -> m.getOrganization().getId())
                .collect(Collectors.toSet());
        if (!advisedOrgs.isEmpty()) {
            mine.addAll(approvals.findByStatusInOrganizations(ApprovalStatus.PENDING_ADVISOR, advisedOrgs));
        }

        Map<ApprovalKind, List<QueueItem>> byKind = new EnumMap<>(ApprovalKind.class);
        for (ApprovalKind k : ApprovalKind.values()) byKind.put(k, new ArrayList<>());
        for (Approval a : mine) {
            byKind.get(a.getKind()).add(new QueueItem(a.getId(), a.getKind(), subject(a.getKind()).describe(a.getSubjectId()),
                    a.getOrganization().getId(), a.getOrganization().getName(), a.getStatus(),
                    a.getRequestedBy(), a.getCreatedAt()));
        }
        return new Queue(byKind.get(ApprovalKind.MEMBERSHIP), byKind.get(ApprovalKind.MEETING),
                byKind.get(ApprovalKind.BYLAWS));
    }

    @Transactional(readOnly = true)
    public Queue queue() {
        return queueFor(currentUser.get());
    }

    @Transactional
    public ApprovalStatus approve(UUID approvalId) {
        return decide(approvalId, true, null);
    }

    @Transactional
    public ApprovalStatus reject(UUID approvalId, String reason) {
        return decide(approvalId, false, reason);
    }

    /**
     * The requester called it off (a cancelled meeting, say). Closes any open
     * approval for the subject so it leaves the queue; nobody's signature needed.
     */
    @Transactional
    public void withdraw(ApprovalKind kind, UUID subjectId, String by) {
        approvals.findFirstByKindAndSubjectIdOrderByCreatedAtDesc(kind, subjectId)
                .filter(a -> a.getStatus().isPending())
                .ifPresent(a -> a.record(ApprovalStatus.REJECTED, by, "Withdrawn."));
    }

    /**
     * The organization lost its last advisor: requests waiting for an advisor
     * signature move on to the admin step instead of waiting forever.
     */
    @Transactional
    public void advisorLeft(UUID orgId) {
        if (memberships.hasAdvisor(orgId)) return;
        approvals.findByStatusInOrganizations(ApprovalStatus.PENDING_ADVISOR, Set.of(orgId)).forEach(a -> {
            a.record(ApprovalStatus.PENDING_ADMIN, "system", null);
            subject(a.getKind()).onStatusChange(a.getSubjectId(), ApprovalStatus.PENDING_ADMIN);
        });
    }

    /**
     * The requester changed the subject after someone had signed it. Signatures
     * given to the old content don't carry over, so the request starts again.
     */
    @Transactional
    public ApprovalStatus restart(ApprovalKind kind, UUID subjectId, Organization org, String by) {
        withdraw(kind, subjectId, by);
        return open(kind, subjectId, org, by);
    }

    private ApprovalStatus decide(UUID approvalId, boolean approve, String reason) {
        Approval a = approvals.findById(approvalId).orElseThrow(() -> new NotFoundException("Approval", approvalId));
        if (!a.getOrganization().isActive() && approve) {
            throw new IllegalStateException(a.getOrganization().getName() + " is closed; its requests can only be rejected.");
        }
        CurrentUser user = currentUser.get();
        Approver who = capacity(a, user);
        ApprovalStatus next = approve
                ? ApprovalWorkflow.approve(a.getStatus(), who)
                : ApprovalWorkflow.reject(a.getStatus(), who);
        a.record(next, user.email(), approve ? null : reason);
        subject(a.getKind()).onStatusChange(a.getSubjectId(), next);
        return next;
    }

    /**
     * Someone who is both an admin and this organization's advisor acts as
     * whichever signature the request is waiting for. That's still one
     * signature per step: they would have to approve twice to finish it.
     */
    private Approver capacity(Approval a, CurrentUser user) {
        boolean advisor = memberships.findCurrentByEmail(a.getOrganization().getId(), user.email()).stream()
                .map(Membership::getRole)
                .anyMatch(r -> r == OrgRole.ADVISOR);
        if (a.getStatus() == ApprovalStatus.PENDING_ADVISOR && advisor) return Approver.ADVISOR;
        if (user.admin()) return Approver.ADMIN;
        return advisor ? Approver.ADVISOR : Approver.NOBODY;
    }

    private ApprovalSubject subject(ApprovalKind kind) {
        ApprovalSubject s = subjects.get(kind);
        if (s == null) throw new IllegalStateException("No handler for " + kind);
        return s;
    }
}
