package dev.omercanbasboga.orgservice.reference;

import dev.omercanbasboga.orgservice.auth.CurrentUserProvider;
import dev.omercanbasboga.orgservice.common.ForbiddenException;
import dev.omercanbasboga.orgservice.common.NotFoundException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Categories and locations: small lookup lists only admins change. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Categories and locations")
public class ReferenceController {

    public record CategoryDto(UUID id, String name) {}
    public record CategoryRequest(@NotBlank String name) {}
    public record LocationDto(UUID id, String name, Integer capacity) {}
    public record LocationRequest(@NotBlank String name, @Min(1) Integer capacity) {}

    private final CategoryRepository categories;
    private final LocationRepository locations;
    private final CurrentUserProvider currentUser;

    public ReferenceController(CategoryRepository categories, LocationRepository locations,
                               CurrentUserProvider currentUser) {
        this.categories = categories;
        this.locations = locations;
        this.currentUser = currentUser;
    }

    @GetMapping("/categories")
    public List<CategoryDto> categories() {
        return categories.findAll(Sort.by("name")).stream().map(c -> new CategoryDto(c.getId(), c.getName())).toList();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createCategory(@Valid @RequestBody CategoryRequest req) {
        requireAdmin();
        if (categories.existsByNameIgnoreCase(req.name().trim())) {
            throw new IllegalArgumentException("A category with that name already exists.");
        }
        Category c = categories.save(new Category(req.name().trim()));
        return new CategoryDto(c.getId(), c.getName());
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable UUID id) {
        requireAdmin();
        if (!categories.existsById(id)) throw new NotFoundException("Category", id);
        try {
            categories.deleteById(id);
            categories.flush();
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Category is still used by an organization.");
        }
    }

    @GetMapping("/locations")
    public List<LocationDto> locations() {
        return locations.findAll(Sort.by("name")).stream()
                .map(l -> new LocationDto(l.getId(), l.getName(), l.getCapacity())).toList();
    }

    @PostMapping("/locations")
    @ResponseStatus(HttpStatus.CREATED)
    public LocationDto createLocation(@Valid @RequestBody LocationRequest req) {
        requireAdmin();
        Location l = locations.save(new Location(req.name().trim(), req.capacity()));
        return new LocationDto(l.getId(), l.getName(), l.getCapacity());
    }

    @DeleteMapping("/locations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLocation(@PathVariable UUID id) {
        requireAdmin();
        if (!locations.existsById(id)) throw new NotFoundException("Location", id);
        try {
            locations.deleteById(id);
            locations.flush();
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Location is still used by an organization or meeting.");
        }
    }

    private void requireAdmin() {
        if (!currentUser.get().admin()) throw new ForbiddenException("Only an admin can change this list.");
    }
}
