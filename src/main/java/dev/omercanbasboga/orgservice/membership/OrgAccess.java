package dev.omercanbasboga.orgservice.membership;

import dev.omercanbasboga.orgservice.auth.CurrentUser;
import dev.omercanbasboga.orgservice.auth.CurrentUserProvider;
import dev.omercanbasboga.orgservice.common.ForbiddenException;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Permission checks inside one organization. Admins pass every check; anyone
 * else needs a current membership whose role passes the test.
 */
@Component
public class OrgAccess {

    private final CurrentUserProvider currentUser;
    private final MembershipRepository memberships;

    public OrgAccess(CurrentUserProvider currentUser, MembershipRepository memberships) {
        this.currentUser = currentUser;
        this.memberships = memberships;
    }

    public CurrentUser require(UUID orgId, Predicate<OrgRole> allowed, String action) {
        CurrentUser user = currentUser.get();
        if (user.admin()) return user;
        if (rolesIn(orgId, user).stream().anyMatch(allowed)) return user;
        throw new ForbiddenException("You can't " + action + " in this organization.");
    }

    /** Any current member, the advisor included, or an admin. */
    public CurrentUser requireMember(UUID orgId, String action) {
        return require(orgId, r -> true, action);
    }

    /** Officers and advisors anywhere: the people who need to look others up to add them. */
    public CurrentUser requireOfficerSomewhere(String action) {
        CurrentUser user = currentUser.get();
        if (user.admin()) return user;
        boolean any = memberships.findCurrentRolesByEmail(user.email()).stream()
                .anyMatch(r -> r.isOfficer() || r == OrgRole.ADVISOR);
        if (!any) throw new ForbiddenException("You can't " + action + ".");
        return user;
    }

    public CurrentUser requireAdmin(String action) {
        CurrentUser user = currentUser.get();
        if (!user.admin()) throw new ForbiddenException("Only an admin can " + action + ".");
        return user;
    }

    public Set<OrgRole> rolesIn(UUID orgId, CurrentUser user) {
        return memberships.findCurrentByEmail(orgId, user.email()).stream()
                .map(Membership::getRole)
                .collect(Collectors.toSet());
    }
}
