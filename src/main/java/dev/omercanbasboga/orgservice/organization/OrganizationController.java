package dev.omercanbasboga.orgservice.organization;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organizations")
public class OrganizationController {

    private final OrganizationService service;

    public OrganizationController(OrganizationService service) {
        this.service = service;
    }

    @GetMapping
    public List<OrganizationDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public OrganizationDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an organization (admin)")
    public OrganizationDto create(@Valid @RequestBody OrganizationRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit details (officers; category and location are admin only)")
    public OrganizationDto update(@PathVariable UUID id, @Valid @RequestBody OrganizationRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Close the organization (admin). History is kept.")
    public void close(@PathVariable UUID id) {
        service.close(id);
    }
}
