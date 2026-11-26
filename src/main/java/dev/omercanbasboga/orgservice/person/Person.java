package dev.omercanbasboga.orgservice.person;

import dev.omercanbasboga.orgservice.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "person", uniqueConstraints = @UniqueConstraint(name = "uq_person_email", columnNames = "email"))
public class Person extends BaseEntity {

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String email;

    /** Student or staff number. */
    @Column(name = "member_id")
    private String memberId;

    private String department;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_role", nullable = false, length = 16)
    private SystemRole systemRole = SystemRole.STUDENT;

    protected Person() {}

    public Person(String firstName, String lastName, String email, String memberId, String department, SystemRole role) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.memberId = memberId;
        this.department = department;
        this.systemRole = role;
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getMemberId() { return memberId; }
    public String getDepartment() { return department; }
    public SystemRole getSystemRole() { return systemRole; }
}
