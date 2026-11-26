package dev.omercanbasboga.orgservice.meeting;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MeetingRepository extends JpaRepository<Meeting, UUID> {
    List<Meeting> findByOrganizationIdOrderByStartsAtDesc(UUID orgId);
}
