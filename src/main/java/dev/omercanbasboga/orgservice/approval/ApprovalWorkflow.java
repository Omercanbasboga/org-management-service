package dev.omercanbasboga.orgservice.approval;

import dev.omercanbasboga.orgservice.common.ForbiddenException;

/**
 * The two-step sign-off, with no database in sight so it can be tested on its own.
 *
 * A request starts with the organization's advisor, then goes to an admin.
 * Each signature only counts at its own step: the advisor can't approve on the
 * admin's behalf, and the admin can't skip the advisor. An organization without
 * an advisor starts its requests at the admin step instead (see {@link #firstStep}),
 * so nothing gets stuck waiting for a signature nobody can give.
 */
public final class ApprovalWorkflow {

    private ApprovalWorkflow() {}

    public static ApprovalStatus firstStep(boolean organizationHasAdvisor) {
        return organizationHasAdvisor ? ApprovalStatus.PENDING_ADVISOR : ApprovalStatus.PENDING_ADMIN;
    }

    public static ApprovalStatus approve(ApprovalStatus current, Approver who) {
        requireTurn(current, who);
        return current == ApprovalStatus.PENDING_ADVISOR ? ApprovalStatus.PENDING_ADMIN : ApprovalStatus.APPROVED;
    }

    /** Either signer can turn a request down at their own step; that ends it. */
    public static ApprovalStatus reject(ApprovalStatus current, Approver who) {
        requireTurn(current, who);
        return ApprovalStatus.REJECTED;
    }

    public static boolean isTurnOf(ApprovalStatus current, Approver who) {
        return (current == ApprovalStatus.PENDING_ADVISOR && who == Approver.ADVISOR)
                || (current == ApprovalStatus.PENDING_ADMIN && who == Approver.ADMIN);
    }

    private static void requireTurn(ApprovalStatus current, Approver who) {
        if (!current.isPending()) {
            throw new IllegalStateException("This request was already " + current.name().toLowerCase() + ".");
        }
        if (!isTurnOf(current, who)) {
            throw new ForbiddenException(current == ApprovalStatus.PENDING_ADVISOR
                    ? "This request is waiting for the organization's advisor."
                    : "This request is waiting for an admin.");
        }
    }
}
