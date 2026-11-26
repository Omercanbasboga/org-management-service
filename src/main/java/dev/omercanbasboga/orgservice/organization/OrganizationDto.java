package dev.omercanbasboga.orgservice.organization;

import java.time.LocalDate;
import java.util.UUID;

/** The shape org-management-console reads; president and advisor come from current memberships. */
public record OrganizationDto(
        UUID id,
        String name,
        String shortName,
        String description,
        String email,
        String website,
        LocalDate foundingDate,
        UUID categoryId,
        String category,
        UUID locationId,
        String location,
        boolean isActive,
        Officer president,
        Officer advisor,
        int memberCount) {

    public record Officer(UUID personId, String name, String memberId, String email, String department) {}
}
