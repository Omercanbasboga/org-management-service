package dev.omercanbasboga.orgservice.auth;

/** Who is calling. {@code admin} means the system-wide admin role, not a role inside one organization. */
public record CurrentUser(String email, boolean admin) {
}
