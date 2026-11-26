package dev.omercanbasboga.orgservice.approval;

/** Shared by approvals and the things they approve (memberships, meetings, bylaws). */
public enum ApprovalStatus {
    PENDING_ADVISOR,
    PENDING_ADMIN,
    APPROVED,
    REJECTED;

    public boolean isPending() {
        return this == PENDING_ADVISOR || this == PENDING_ADMIN;
    }
}
