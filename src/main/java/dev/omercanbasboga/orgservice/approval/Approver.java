package dev.omercanbasboga.orgservice.approval;

/** The capacity someone acts in on one approval. */
public enum Approver {
    /** The organization's staff advisor: the first signature. */
    ADVISOR,
    /** A system admin: the final signature. */
    ADMIN,
    NOBODY
}
