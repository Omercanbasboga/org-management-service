package dev.omercanbasboga.orgservice.person;

import dev.omercanbasboga.orgservice.membership.OrgAccess;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users")
public class PersonController {

    public record PersonDto(UUID id, String firstName, String lastName, String email, String memberId,
                            String department, SystemRole role) {
        static PersonDto of(Person p) {
            return new PersonDto(p.getId(), p.getFirstName(), p.getLastName(), p.getEmail(), p.getMemberId(),
                    p.getDepartment(), p.getSystemRole());
        }
    }

    private final PersonRepository people;
    private final OrgAccess access;

    public PersonController(PersonRepository people, OrgAccess access) {
        this.people = people;
        this.access = access;
    }

    /** The directory is personal data: admins, officers and advisors only, for adding people. */
    @GetMapping
    public List<PersonDto> list() {
        access.requireOfficerSomewhere("browse the directory");
        return people.findAll(Sort.by("lastName", "firstName")).stream().map(PersonDto::of).toList();
    }
}
