package dev.omercanbasboga.orgservice.auth;

import dev.omercanbasboga.orgservice.common.ForbiddenException;
import dev.omercanbasboga.orgservice.person.PersonRepository;
import dev.omercanbasboga.orgservice.person.SystemRole;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * Resolves the caller.
 *
 * With a gateway in front (the normal setup, see sso-gateway-bff) every request
 * carries a JWT, and the email and roles come from its claims. In demo mode
 * there is no identity provider, so the caller picks a persona with the
 * {@code X-Demo-User} header and the role comes from that person's record.
 */
@Component
public class CurrentUserProvider {

    public static final String DEMO_HEADER = "X-Demo-User";

    private final PersonRepository people;
    private final boolean demo;
    private final String demoDefault;
    private final String adminRole;

    public CurrentUserProvider(PersonRepository people,
                               @Value("${app.security.demo:false}") boolean demo,
                               @Value("${app.security.demo-default-user:admin@example.org}") String demoDefault,
                               @Value("${app.security.admin-role:ORG-ADMIN}") String adminRole) {
        this.people = people;
        this.demo = demo;
        this.demoDefault = demoDefault;
        this.adminRole = adminRole;
    }

    public CurrentUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            String email = jwt.getToken().getClaimAsString("email");
            List<String> roles = jwt.getToken().getClaimAsStringList("roles");
            if (email == null) throw new ForbiddenException("Token has no email claim.");
            return new CurrentUser(email, roles != null && roles.contains(adminRole));
        }
        if (demo) {
            String email = demoHeader();
            boolean admin = people.findByEmailIgnoreCase(email)
                    .map(p -> p.getSystemRole() == SystemRole.ADMIN)
                    .orElse(false);
            return new CurrentUser(email, admin);
        }
        throw new ForbiddenException("Not signed in.");
    }

    private String demoHeader() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest req = attrs.getRequest();
            String h = req.getHeader(DEMO_HEADER);
            if (h != null && !h.isBlank()) return h.trim();
        }
        return demoDefault;
    }
}
