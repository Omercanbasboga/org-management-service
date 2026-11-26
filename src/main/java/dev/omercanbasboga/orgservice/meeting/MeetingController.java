package dev.omercanbasboga.orgservice.meeting;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}/meetings")
@Tag(name = "Meetings")
public class MeetingController {

    private final MeetingService service;

    public MeetingController(MeetingService service) {
        this.service = service;
    }

    @GetMapping
    public List<MeetingService.MeetingDto> list(@PathVariable UUID orgId) {
        return service.list(orgId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Schedule a meeting (officers). It goes to the approval queue.")
    public MeetingService.MeetingDto schedule(@PathVariable UUID orgId,
                                              @Valid @RequestBody MeetingService.MeetingRequest req) {
        return service.schedule(orgId, req);
    }

    @PutMapping("/{meetingId}")
    @Operation(summary = "Edit a meeting that hasn't been approved yet")
    public MeetingService.MeetingDto update(@PathVariable UUID orgId, @PathVariable UUID meetingId,
                                            @Valid @RequestBody MeetingService.MeetingRequest req) {
        return service.update(orgId, meetingId, req);
    }

    @DeleteMapping("/{meetingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID orgId, @PathVariable UUID meetingId) {
        service.cancel(orgId, meetingId);
    }
}
