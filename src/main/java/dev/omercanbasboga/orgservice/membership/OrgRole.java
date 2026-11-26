package dev.omercanbasboga.orgservice.membership;

/**
 * A person's role inside one organization.
 *
 * The advisor is a staff member who signs off requests before an admin sees
 * them. The four officer roles run the organization day to day.
 */
public enum OrgRole {
    PRESIDENT,
    VICE_PRESIDENT,
    SECRETARY,
    TREASURER,
    MEMBER,
    ADVISOR;

    public boolean isOfficer() {
        return this == PRESIDENT || this == VICE_PRESIDENT || this == SECRETARY || this == TREASURER;
    }

    public boolean canManageMembers() {
        return this == PRESIDENT || this == VICE_PRESIDENT || this == SECRETARY;
    }

    public boolean canScheduleMeetings() {
        return isOfficer();
    }

    /** The secretary takes minutes but doesn't touch money. */
    public boolean canManageFinances() {
        return this == PRESIDENT || this == VICE_PRESIDENT || this == TREASURER;
    }
}
