package dev.omercanbasboga.orgservice.bylaws;

import dev.omercanbasboga.orgservice.approval.ApprovalKind;
import dev.omercanbasboga.orgservice.approval.ApprovalService;
import dev.omercanbasboga.orgservice.approval.ApprovalStatus;
import dev.omercanbasboga.orgservice.approval.ApprovalSubject;
import dev.omercanbasboga.orgservice.common.NotFoundException;
import dev.omercanbasboga.orgservice.membership.OrgAccess;
import dev.omercanbasboga.orgservice.membership.OrgRole;
import dev.omercanbasboga.orgservice.organization.Organization;
import dev.omercanbasboga.orgservice.organization.OrganizationService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class BylawsService implements ApprovalSubject {

    public record BylawsDto(UUID id, String fileName, long sizeBytes, Instant uploadedAt, ApprovalStatus status,
                            boolean approved) {
        static BylawsDto of(Bylaws b) {
            return new BylawsDto(b.getId(), b.getFileName(), b.getSizeBytes(), b.getCreatedAt(), b.getStatus(),
                    b.getStatus() == ApprovalStatus.APPROVED);
        }
    }

    /** Extension to the content type we store and serve it with. The client's own type is not trusted. */
    private static final Map<String, String> ALLOWED = Map.of(
            "pdf", "application/pdf",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "odt", "application/vnd.oasis.opendocument.text");

    private final BylawsRepository bylaws;
    private final FileStore files;
    private final OrganizationService orgs;
    private final OrgAccess access;
    private final ApprovalService approvals;

    public BylawsService(BylawsRepository bylaws, FileStore files, OrganizationService orgs, OrgAccess access,
                         @Lazy ApprovalService approvals) {
        this.bylaws = bylaws;
        this.files = files;
        this.orgs = orgs;
        this.access = access;
        this.approvals = approvals;
    }

    /** The newest upload, whatever its status, so a pending version shows up next to the old one's status. */
    @Transactional(readOnly = true)
    public Optional<BylawsDto> latest(UUID orgId) {
        orgs.require(orgId);
        access.requireMember(orgId, "see the bylaws");
        return bylaws.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream().findFirst().map(BylawsDto::of);
    }

    @Transactional(readOnly = true)
    public List<BylawsDto> history(UUID orgId) {
        orgs.require(orgId);
        access.requireMember(orgId, "see the bylaws");
        return bylaws.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream().map(BylawsDto::of).toList();
    }

    @Transactional
    public BylawsDto upload(UUID orgId, MultipartFile file) {
        Organization org = orgs.require(orgId);
        if (!org.isActive()) throw new IllegalStateException("This organization is closed.");
        String by = access.require(orgId, r -> r == OrgRole.PRESIDENT || r == OrgRole.SECRETARY,
                "upload bylaws").email();
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("The file is empty.");

        String original = displayName(file.getOriginalFilename());
        String ext = extension(original);
        String type = ALLOWED.get(ext);
        if (type == null) throw new IllegalArgumentException("Bylaws must be a PDF, DOCX or ODT file.");

        String stored = UUID.randomUUID() + "." + ext;
        try (InputStream in = file.getInputStream()) {
            files.save(stored, in);
        } catch (IOException e) {
            throw new UncheckedIOException("Couldn't store the file.", e);
        }
        // the file is on disk before the row is committed; take it back off if the transaction fails
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) files.delete(stored);
            }
        });

        Bylaws b = new Bylaws(org, original, stored, type, file.getSize());
        b.setStatus(ApprovalStatus.PENDING_ADMIN);
        bylaws.save(b);
        b.setStatus(approvals.open(ApprovalKind.BYLAWS, b.getId(), org, by));
        return BylawsDto.of(b);
    }

    /**
     * Members can download approved versions. Drafts and rejected uploads stay
     * with the people who handle them: officers, the advisor and admins.
     */
    @Transactional(readOnly = true)
    public Bylaws requireForDownload(UUID orgId, UUID id) {
        Bylaws b = bylaws.findById(id)
                .filter(x -> x.getOrganization().getId().equals(orgId))
                .orElseThrow(() -> new NotFoundException("Bylaws", id));
        if (b.getStatus() == ApprovalStatus.APPROVED) {
            access.requireMember(orgId, "download the bylaws");
        } else {
            access.require(orgId, r -> r.isOfficer() || r == OrgRole.ADVISOR, "download an unapproved version");
        }
        return b;
    }

    public Path pathOf(Bylaws b) {
        return files.resolve(b.getStoredName());
    }

    /** Keeps only the last path segment and drops control characters; used for display only. */
    static String displayName(String raw) {
        if (raw == null || raw.isBlank()) return "bylaws";
        String name = raw.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "").trim();
        if (name.length() > 200) name = name.substring(name.length() - 200);
        return name.isEmpty() ? "bylaws" : name;
    }

    static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    @Override
    public ApprovalKind kind() {
        return ApprovalKind.BYLAWS;
    }

    @Override
    public String describe(UUID subjectId) {
        return bylaws.findById(subjectId).map(Bylaws::getFileName).orElse("(missing file)");
    }

    @Override
    public void onStatusChange(UUID subjectId, ApprovalStatus status) {
        bylaws.findById(subjectId).ifPresent(b -> b.setStatus(status));
    }
}
