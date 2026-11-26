package dev.omercanbasboga.orgservice.bylaws;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BylawsRepository extends JpaRepository<Bylaws, UUID> {
    List<Bylaws> findByOrganizationIdOrderByCreatedAtDesc(UUID orgId);
}
