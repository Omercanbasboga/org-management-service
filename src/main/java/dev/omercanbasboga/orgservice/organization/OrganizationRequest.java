package dev.omercanbasboga.orgservice.organization;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record OrganizationRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 32) String shortName,
        @Size(max = 2000) String description,
        @Email String email,
        String website,
        LocalDate foundingDate,
        UUID categoryId,
        UUID locationId) {
}
