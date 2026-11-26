package dev.omercanbasboga.orgservice.bylaws;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}/bylaws")
@Tag(name = "Bylaws")
public class BylawsController {

    private final BylawsService service;

    public BylawsController(BylawsService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "The newest upload; 204 if there is none")
    public ResponseEntity<BylawsService.BylawsDto> latest(@PathVariable UUID orgId) {
        return service.latest(orgId).map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/history")
    public List<BylawsService.BylawsDto> history(@PathVariable UUID orgId) {
        return service.history(orgId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a new version (president or secretary; PDF, DOCX or ODT up to 5 MB)")
    public BylawsService.BylawsDto upload(@PathVariable UUID orgId, @RequestParam("file") MultipartFile file) {
        return service.upload(orgId, file);
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> download(@PathVariable UUID orgId, @PathVariable UUID id) {
        Bylaws b = service.requireForDownload(orgId, id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(b.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(b.getFileName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(service.pathOf(b)));
    }
}
