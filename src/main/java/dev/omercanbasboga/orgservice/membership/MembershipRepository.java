package dev.omercanbasboga.orgservice.membership;

import dev.omercanbasboga.orgservice.approval.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    @Query("select m from Membership m join fetch m.person where m.organization.id = :orgId "
            + "and m.status = dev.omercanbasboga.orgservice.approval.ApprovalStatus.APPROVED and m.leftAt is null "
            + "order by m.role, m.person.lastName")
    List<Membership> findCurrent(@Param("orgId") UUID orgId);

    @Query("select m from Membership m where m.organization.id = :orgId and lower(m.person.email) = lower(:email) "
            + "and m.status = dev.omercanbasboga.orgservice.approval.ApprovalStatus.APPROVED and m.leftAt is null")
    List<Membership> findCurrentByEmail(@Param("orgId") UUID orgId, @Param("email") String email);

    @Query("select m from Membership m join fetch m.organization where lower(m.person.email) = lower(:email) "
            + "and m.role = dev.omercanbasboga.orgservice.membership.OrgRole.ADVISOR "
            + "and m.status = dev.omercanbasboga.orgservice.approval.ApprovalStatus.APPROVED and m.leftAt is null")
    List<Membership> findAdvisorRoles(@Param("email") String email);

    @Query("select m.role from Membership m where lower(m.person.email) = lower(:email) "
            + "and m.status = dev.omercanbasboga.orgservice.approval.ApprovalStatus.APPROVED and m.leftAt is null")
    List<OrgRole> findCurrentRolesByEmail(@Param("email") String email);

    @Query("select m from Membership m join fetch m.person where m.organization.id in :orgIds "
            + "and m.status = dev.omercanbasboga.orgservice.approval.ApprovalStatus.APPROVED and m.leftAt is null")
    List<Membership> findCurrentIn(@Param("orgIds") java.util.Collection<UUID> orgIds);

    boolean existsByOrganizationIdAndPersonIdAndStatusInAndLeftAtIsNull(UUID orgId, UUID personId, List<ApprovalStatus> statuses);

    @Query("select count(m) > 0 from Membership m where m.organization.id = :orgId "
            + "and m.role = dev.omercanbasboga.orgservice.membership.OrgRole.ADVISOR "
            + "and m.status = dev.omercanbasboga.orgservice.approval.ApprovalStatus.APPROVED and m.leftAt is null")
    boolean hasAdvisor(@Param("orgId") UUID orgId);
}
