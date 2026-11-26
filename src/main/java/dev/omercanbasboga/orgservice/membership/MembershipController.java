package dev.omercanbasboga.orgservice.membership;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}/members")
@Tag(name = "Members")
public class MembershipController {

    private final MembershipService service;

    public MembershipController(MembershipService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Current members, officers first")
    public List<MembershipService.MemberDto> list(@PathVariable UUID orgId) {
        return service.current(orgId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Request a membership. It becomes active once approved.")
    public MembershipService.MemberDto request(@PathVariable UUID orgId,
                                               @Valid @RequestBody MembershipService.MemberRequest req) {
        return service.request(orgId, req);
    }

    @DeleteMapping("/{membershipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Take someone off the roster. The record is kept with a leaving date.")
    public void remove(@PathVariable UUID orgId, @PathVariable UUID membershipId) {
        service.remove(orgId, membershipId);
    }
}
