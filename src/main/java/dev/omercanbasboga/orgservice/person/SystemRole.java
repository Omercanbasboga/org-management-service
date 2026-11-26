package dev.omercanbasboga.orgservice.person;

/**
 * Role across the whole system. Roles inside one organization (president,
 * treasurer, advisor...) live on the membership instead, so the same student
 * can lead one club and be a plain member of another.
 */
public enum SystemRole {
    STUDENT,
    STAFF,
    ADMIN
}
