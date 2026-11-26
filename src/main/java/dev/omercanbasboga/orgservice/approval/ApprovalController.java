package dev.omercanbasboga.orgservice.approval;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/approvals")
@Tag(name = "Approvals", description = "Two-step sign-off: organization advisor first, then an admin")
public class ApprovalController {

    private final ApprovalService service;

    public ApprovalController(ApprovalService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Requests waiting for the caller's signature, grouped by kind")
    public ApprovalService.Queue queue() {
        return service.queue();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Sign at the current step. Returns the new status.")
    public Map<String, ApprovalStatus> approve(@PathVariable UUID id) {
        return Map.of("status", service.approve(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Turn the request down at the current step")
    public Map<String, ApprovalStatus> reject(@PathVariable UUID id, @RequestParam(required = false) String reason) {
        return Map.of("status", service.reject(id, reason));
    }
}
