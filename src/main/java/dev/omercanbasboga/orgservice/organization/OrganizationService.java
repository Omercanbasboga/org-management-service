package dev.omercanbasboga.orgservice.organization;

import dev.omercanbasboga.orgservice.common.BaseEntity;
import dev.omercanbasboga.orgservice.common.ForbiddenException;
import dev.omercanbasboga.orgservice.common.NotFoundException;
import dev.omercanbasboga.orgservice.membership.Membership;
import dev.omercanbasboga.orgservice.membership.MembershipRepository;
import dev.omercanbasboga.orgservice.membership.OrgAccess;
import dev.omercanbasboga.orgservice.membership.OrgRole;
import dev.omercanbasboga.orgservice.person.Person;
import dev.omercanbasboga.orgservice.reference.CategoryRepository;
import dev.omercanbasboga.orgservice.reference.LocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class OrganizationService {

    private final OrganizationRepository orgs;
    private final MembershipRepository memberships;
    private final CategoryRepository categories;
    private final LocationRepository locations;
    private final OrgAccess access;

    public OrganizationService(OrganizationRepository orgs, MembershipRepository memberships,
                               CategoryRepository categories, LocationRepository locations, OrgAccess access) {
        this.orgs = orgs;
        this.memberships = memberships;
        this.categories = categories;
        this.locations = locations;
        this.access = access;
    }

    /** Three queries whatever the number of organizations: orgs with their references, then all current members. */
    @Transactional(readOnly = true)
    public List<OrganizationDto> list() {
        List<Organization> all = orgs.findAllWithReferences();
        Map<UUID, List<Membership>> byOrg = memberships.findCurrentIn(all.stream().map(Organization::getId).toList())
                .stream().collect(Collectors.groupingBy(m -> m.getOrganization().getId()));
        return all.stream().map(o -> toDto(o, byOrg.getOrDefault(o.getId(), List.of()))).toList();
    }

    @Transactional(readOnly = true)
    public OrganizationDto get(UUID id) {
        return toDto(require(id));
    }

    @Transactional
    public OrganizationDto create(OrganizationRequest req) {
        access.requireAdmin("create an organization");
        Organization o = new Organization();
        apply(o, req);
        return toDto(orgs.save(o));
    }

    /** Officers can edit their own organization's details; category and location stay with admins. */
    @Transactional
    public OrganizationDto update(UUID id, OrganizationRequest req) {
        Organization o = require(id);
        boolean admin = access.require(id, OrgRole::isOfficer, "edit the details").admin();
        if (!admin && (changed(req.categoryId(), o.getCategory()) || changed(req.locationId(), o.getLocation()))) {
            throw new ForbiddenException(
                    "Only an admin can change the category or the usual location.");
        }
        apply(o, req);
        return toDto(o);
    }

    /** Closes the organization. Its members, meetings and accounts stay readable. */
    @Transactional
    public void close(UUID id) {
        access.requireAdmin("close an organization");
        require(id).setActive(false);
    }

    public Organization requireForUpdate(UUID id) {
        return orgs.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Organization", id));
    }

    public Organization require(UUID id) {
        return orgs.findById(id).orElseThrow(() -> new NotFoundException("Organization", id));
    }

    private void apply(Organization o, OrganizationRequest req) {
        o.setName(req.name().trim());
        o.setShortName(req.shortName());
        o.setDescription(req.description());
        o.setEmail(req.email());
        o.setWebsite(req.website());
        o.setFoundingDate(req.foundingDate());
        o.setCategory(req.categoryId() == null ? null : categories.findById(req.categoryId())
                .orElseThrow(() -> new NotFoundException("Category", req.categoryId())));
        o.setLocation(req.locationId() == null ? null : locations.findById(req.locationId())
                .orElseThrow(() -> new NotFoundException("Location", req.locationId())));
    }

    private static boolean changed(UUID requested, BaseEntity current) {
        UUID now = current == null ? null : current.getId();
        return requested == null ? now != null : !requested.equals(now);
    }

    private OrganizationDto toDto(Organization o) {
        return toDto(o, memberships.findCurrent(o.getId()));
    }

    private OrganizationDto toDto(Organization o, List<Membership> current) {
        return new OrganizationDto(
                o.getId(), o.getName(), o.getShortName(), o.getDescription(), o.getEmail(), o.getWebsite(),
                o.getFoundingDate(),
                o.getCategory() == null ? null : o.getCategory().getId(),
                o.getCategory() == null ? null : o.getCategory().getName(),
                o.getLocation() == null ? null : o.getLocation().getId(),
                o.getLocation() == null ? null : o.getLocation().getName(),
                o.isActive(),
                officer(current, OrgRole.PRESIDENT).orElse(null),
                officer(current, OrgRole.ADVISOR).orElse(null),
                (int) current.stream().filter(m -> m.getRole() != OrgRole.ADVISOR).count());
    }

    private static Optional<OrganizationDto.Officer> officer(List<Membership> current, OrgRole role) {
        return current.stream().filter(m -> m.getRole() == role).findFirst().map(m -> {
            Person p = m.getPerson();
            return new OrganizationDto.Officer(p.getId(), p.fullName(), p.getMemberId(), p.getEmail(), p.getDepartment());
        });
    }
}
