package dev.omercanbasboga.orgservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static dev.omercanbasboga.orgservice.auth.CurrentUserProvider.DEMO_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Runs against the demo data, switching personas with the demo header. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:${random.uuid};DB_CLOSE_DELAY=-1")
@ActiveProfiles("demo")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ApiFlowTest {

    private static final String ADMIN = "admin@example.org";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private JsonNode chess;

    @BeforeEach
    void findChessSociety() throws Exception {
        for (JsonNode o : get200("/api/v1/organizations", ADMIN)) {
            if (o.get("shortName").asText().equals("CHESS")) chess = o;
        }
    }

    private ResultActions call(MockHttpServletRequestBuilder req, String as) throws Exception {
        return mvc.perform(req.header(DEMO_HEADER, as));
    }

    private JsonNode get200(String url, String as) throws Exception {
        return json.readTree(call(get(url), as).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private String org() {
        return "/api/v1/organizations/" + chess.get("id").asText();
    }

    private String member(String role) throws Exception {
        for (JsonNode m : get200(org() + "/members", ADMIN)) {
            if (m.get("role").asText().equals(role)) return m.get("email").asText();
        }
        throw new AssertionError("no " + role);
    }

    private String outsider() throws Exception {
        List<String> inChess = new ArrayList<>();
        get200(org() + "/members", ADMIN).forEach(m -> inChess.add(m.get("personId").asText()));
        for (JsonNode p : get200("/api/v1/users", ADMIN)) {
            if (p.get("role").asText().equals("STUDENT") && !inChess.contains(p.get("id").asText())) return p.toString();
        }
        throw new AssertionError("everyone is in chess");
    }

    /** A student who holds no officer role in any club. */
    private String plainStudent() throws Exception {
        List<String> officers = new ArrayList<>();
        for (JsonNode o : get200("/api/v1/organizations", ADMIN)) {
            for (JsonNode m : get200("/api/v1/organizations/" + o.get("id").asText() + "/members", ADMIN)) {
                if (!m.get("role").asText().equals("MEMBER")) officers.add(m.get("email").asText());
            }
        }
        for (JsonNode p : get200("/api/v1/users", ADMIN)) {
            String e = p.get("email").asText();
            if (p.get("role").asText().equals("STUDENT") && !officers.contains(e)) return e;
        }
        throw new AssertionError("no plain student");
    }

    private JsonNode queueItem(String as, String list, String title) throws Exception {
        for (JsonNode i : get200("/api/v1/approvals", as).get(list)) {
            if (i.get("title").asText().equals(title)) return i;
        }
        return null;
    }

    @Test
    void joiningTakesTheAdvisorThenAnAdmin() throws Exception {
        JsonNode student = json.readTree(outsider());
        String email = student.get("email").asText();
        String name = student.get("firstName").asText() + " " + student.get("lastName").asText();
        String advisor = chess.get("advisor").get("email").asText();

        JsonNode created = json.readTree(call(post(org() + "/members").contentType(MediaType.APPLICATION_JSON)
                .content("{\"personId\":\"" + student.get("id").asText() + "\"}"), email)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        assertThat(created.get("status").asText()).isEqualTo("PENDING_ADVISOR");

        assertThat(queueItem(ADMIN, "membershipRequests", name)).as("admin waits for the advisor").isNull();
        JsonNode item = queueItem(advisor, "membershipRequests", name);
        assertThat(item).isNotNull();
        String approval = "/api/v1/approvals/" + item.get("id").asText();

        call(put(approval), email).andExpect(status().isForbidden());
        call(put(approval), ADMIN).andExpect(status().isForbidden());
        call(put(approval), advisor).andExpect(status().isOk());
        call(put(approval), advisor).andExpect(status().isForbidden());

        assertThat(queueItem(ADMIN, "membershipRequests", name)).isNotNull();
        call(put(approval), ADMIN).andExpect(status().isOk());
        call(put(approval), ADMIN).andExpect(status().isConflict());

        boolean listed = false;
        for (JsonNode m : get200(org() + "/members", ADMIN)) listed |= m.get("email").asText().equals(email);
        assertThat(listed).isTrue();

        call(post(org() + "/members").contentType(MediaType.APPLICATION_JSON)
                .content("{\"personId\":\"" + student.get("id").asText() + "\"}"), email)
                .andExpect(status().isConflict());
    }

    @Test
    void plainMembersCantPromoteOrManageMoney() throws Exception {
        String plain = member("MEMBER");
        String someone = json.readTree(outsider()).get("id").asText();
        call(post(org() + "/members").contentType(MediaType.APPLICATION_JSON)
                .content("{\"personId\":\"" + someone + "\",\"role\":\"TREASURER\"}"), plain)
                .andExpect(status().isForbidden());
        call(get(org() + "/finance/transactions"), plain).andExpect(status().isForbidden());
        call(post(org() + "/finance/transactions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"INCOME\",\"amount\":10,\"description\":\"x\"}"), member("SECRETARY"))
                .andExpect(status().isForbidden());
    }

    @Test
    void treasurerCantOverdraw() throws Exception {
        String treasurer = member("TREASURER");
        JsonNode before = get200(org() + "/finance/summary", treasurer);
        double balance = before.get("balance").asDouble();
        assertThat(balance).isEqualTo(before.get("income").asDouble() - before.get("expenses").asDouble());

        call(post(org() + "/finance/transactions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"EXPENSE\",\"amount\":" + (balance + 1) + ",\"description\":\"Boards and clocks\"}"),
                treasurer).andExpect(status().isConflict());
        call(post(org() + "/finance/transactions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"EXPENSE\",\"amount\":100.50,\"description\":\"Boards and clocks\"}"), treasurer)
                .andExpect(status().isCreated());
        assertThat(get200(org() + "/finance/summary", treasurer).get("balance").asDouble())
                .isEqualTo(balance - 100.50, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void bylawsUploadChecksTheFileType() throws Exception {
        String president = member("PRESIDENT");
        MockMultipartFile pdf = new MockMultipartFile("file", "chess-bylaws-2027.pdf", "application/pdf", "%PDF-1.4".getBytes());
        MockMultipartFile exe = new MockMultipartFile("file", "bylaws.exe", "application/pdf", new byte[]{1, 2});

        call(multipart(org() + "/bylaws").file(exe), president).andExpect(status().isBadRequest());
        call(multipart(org() + "/bylaws").file(pdf), member("MEMBER")).andExpect(status().isForbidden());
        JsonNode up = json.readTree(call(multipart(org() + "/bylaws").file(pdf), president)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        assertThat(up.get("approved").asBoolean()).isFalse();
        assertThat(get200(org() + "/bylaws", president).get("fileName").asText()).isEqualTo("chess-bylaws-2027.pdf");
    }

    @Test
    void meetingsRespectRoomSizeAndLeaveTheQueueWhenCancelled() throws Exception {
        String secretary = member("SECRETARY");
        String room = null;
        for (JsonNode l : get200("/api/v1/locations", ADMIN)) {
            if (l.get("capacity").asInt() == 12) room = l.get("id").asText();
        }
        String when = LocalDateTime.now().plusDays(20).withNano(0).toString();
        call(post(org() + "/meetings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Too big\",\"date\":\"" + when + "\",\"locationId\":\"" + room + "\",\"attendeeCount\":30}"),
                secretary).andExpect(status().isBadRequest());
        call(post(org() + "/meetings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Blitz night\",\"date\":\"" + when + "\"}"), member("MEMBER"))
                .andExpect(status().isForbidden());

        JsonNode m = json.readTree(call(post(org() + "/meetings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Blitz night\",\"date\":\"" + when + "\",\"locationId\":\"" + room + "\",\"attendeeCount\":10}"),
                secretary).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String advisor = chess.get("advisor").get("email").asText();
        assertThat(queueItem(advisor, "meetings", "Blitz night")).isNotNull();

        call(delete(org() + "/meetings/" + m.get("id").asText()), secretary).andExpect(status().isNoContent());
        assertThat(queueItem(advisor, "meetings", "Blitz night")).isNull();
    }

    @Test
    void outsidersDontSeeTheBooksOrTheDirectory() throws Exception {
        String student = json.readTree(outsider()).get("email").asText();
        call(get(org() + "/finance/summary"), student).andExpect(status().isForbidden());
        call(get(org() + "/bylaws"), student).andExpect(status().isForbidden());
        call(get("/api/v1/users"), plainStudent()).andExpect(status().isForbidden());
        call(get("/api/v1/users"), member("SECRETARY")).andExpect(status().isOk());
    }

    @Test
    void aPendingRequestCanBeWithdrawn() throws Exception {
        JsonNode student = json.readTree(outsider());
        String email = student.get("email").asText();
        String name = student.get("firstName").asText() + " " + student.get("lastName").asText();
        JsonNode created = json.readTree(call(post(org() + "/members").contentType(MediaType.APPLICATION_JSON)
                .content("{\"personId\":\"" + student.get("id").asText() + "\"}"), email)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String advisor = chess.get("advisor").get("email").asText();
        assertThat(queueItem(advisor, "membershipRequests", name)).isNotNull();

        call(delete(org() + "/members/" + created.get("id").asText()), email).andExpect(status().isNoContent());
        assertThat(queueItem(advisor, "membershipRequests", name)).isNull();
    }

    @Test
    void editingAfterTheAdvisorSignedStartsOver() throws Exception {
        String secretary = member("SECRETARY");
        String advisor = chess.get("advisor").get("email").asText();
        String when = LocalDateTime.now().plusDays(30).withNano(0).toString();
        JsonNode m = json.readTree(call(post(org() + "/meetings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Study session\",\"date\":\"" + when + "\"}"), secretary)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        call(put("/api/v1/approvals/" + queueItem(advisor, "meetings", "Study session").get("id").asText()), advisor)
                .andExpect(status().isOk());
        assertThat(queueItem(ADMIN, "meetings", "Study session")).isNotNull();

        call(put(org() + "/meetings/" + m.get("id").asText()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Concert\",\"date\":\"" + when + "\"}"), secretary)
                .andExpect(status().isOk());
        assertThat(queueItem(ADMIN, "meetings", "Concert")).as("admin must not get the unseen edit").isNull();
        assertThat(queueItem(advisor, "meetings", "Concert")).isNotNull();
    }

    @Test
    void requestsMoveOnWhenTheAdvisorLeaves() throws Exception {
        String advisor = chess.get("advisor").get("email").asText();
        int waiting = get200("/api/v1/approvals", advisor).get("membershipRequests").size();
        assertThat(waiting).isPositive();
        String advisorMembership = null;
        for (JsonNode m : get200(org() + "/members", ADMIN)) {
            if (m.get("role").asText().equals("ADVISOR")) advisorMembership = m.get("id").asText();
        }
        call(delete(org() + "/members/" + advisorMembership), ADMIN).andExpect(status().isNoContent());

        int adminSees = 0;
        for (JsonNode i : get200("/api/v1/approvals", ADMIN).get("membershipRequests")) {
            if (i.get("organizationId").asText().equals(chess.get("id").asText())) adminSees++;
        }
        assertThat(adminSees).isGreaterThanOrEqualTo(waiting);
    }
}
