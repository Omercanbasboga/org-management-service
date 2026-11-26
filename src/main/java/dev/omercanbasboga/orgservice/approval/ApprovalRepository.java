package dev.omercanbasboga.orgservice.approval;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalRepository extends JpaRepository<Approval, UUID> {

    @Query("select a from Approval a join fetch a.organization where a.status = :status order by a.createdAt")
    List<Approval> findByStatus(@Param("status") ApprovalStatus status);

    @Query("select a from Approval a join fetch a.organization where a.status = :status "
            + "and a.organization.id in :orgIds order by a.createdAt")
    List<Approval> findByStatusInOrganizations(@Param("status") ApprovalStatus status,
                                               @Param("orgIds") Collection<UUID> orgIds);

    Optional<Approval> findFirstByKindAndSubjectIdOrderByCreatedAtDesc(ApprovalKind kind, UUID subjectId);
}
