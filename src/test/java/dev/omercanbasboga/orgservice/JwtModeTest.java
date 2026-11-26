package dev.omercanbasboga.orgservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The production setup: no demo header, a JWT from the gateway on every call. */
@SpringBootTest(properties = {"app.security.demo=false", "spring.datasource.url=jdbc:h2:mem:jwt;DB_CLOSE_DELAY=-1"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class JwtModeTest {

    @Autowired MockMvc mvc;
    @MockitoBean JwtDecoder decoder;

    @Test
    void noTokenNoEntry() throws Exception {
        mvc.perform(get("/api/v1/organizations").header("X-Demo-User", "admin@example.org"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rolesComeFromTheToken() throws Exception {
        mvc.perform(get("/api/v1/approvals").with(jwt().jwt(j -> j
                        .claim("email", "someone@example.org").claim("roles", List.of("ORG-ADMIN")))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/organizations/00000000-0000-0000-0000-000000000000/finance/transactions")
                        .with(jwt().jwt(j -> j.claim("email", "someone@example.org").claim("roles", List.of()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void docsStayOpen() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }
}
