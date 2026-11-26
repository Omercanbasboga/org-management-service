package dev.omercanbasboga.orgservice.person;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PersonRepository extends JpaRepository<Person, UUID> {
    Optional<Person> findByEmailIgnoreCase(String email);
}
