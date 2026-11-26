package dev.omercanbasboga.orgservice.approval;

import java.util.UUID;

/**
 * Implemented once per kind of thing that needs approval. The approval code
 * stays the same whether it is signing off a new member, a meeting or a
 * bylaws file; each subject only says how it looks in the queue and what
 * changes on its own row when the status moves.
 */
public interface ApprovalSubject {

    ApprovalKind kind();

    /** The line shown in the queue: a person's name, a meeting title, a file name. */
    String describe(UUID subjectId);

    void onStatusChange(UUID subjectId, ApprovalStatus status);
}
