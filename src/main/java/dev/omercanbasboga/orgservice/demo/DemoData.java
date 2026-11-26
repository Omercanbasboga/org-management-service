package dev.omercanbasboga.orgservice.demo;

import dev.omercanbasboga.orgservice.approval.Approval;
import dev.omercanbasboga.orgservice.approval.ApprovalKind;
import dev.omercanbasboga.orgservice.approval.ApprovalRepository;
import dev.omercanbasboga.orgservice.approval.ApprovalStatus;
import dev.omercanbasboga.orgservice.bylaws.Bylaws;
import dev.omercanbasboga.orgservice.bylaws.BylawsRepository;
import dev.omercanbasboga.orgservice.bylaws.FileStore;
import dev.omercanbasboga.orgservice.finance.Transaction;
import dev.omercanbasboga.orgservice.finance.TransactionRepository;
import dev.omercanbasboga.orgservice.finance.TransactionType;
import dev.omercanbasboga.orgservice.meeting.Meeting;
import dev.omercanbasboga.orgservice.meeting.MeetingRepository;
import dev.omercanbasboga.orgservice.membership.Membership;
import dev.omercanbasboga.orgservice.membership.MembershipRepository;
import dev.omercanbasboga.orgservice.membership.MembershipType;
import dev.omercanbasboga.orgservice.membership.OrgRole;
import dev.omercanbasboga.orgservice.organization.Organization;
import dev.omercanbasboga.orgservice.organization.OrganizationRepository;
import dev.omercanbasboga.orgservice.person.Person;
import dev.omercanbasboga.orgservice.person.PersonRepository;
import dev.omercanbasboga.orgservice.person.SystemRole;
import dev.omercanbasboga.orgservice.reference.Category;
import dev.omercanbasboga.orgservice.reference.CategoryRepository;
import dev.omercanbasboga.orgservice.reference.Location;
import dev.omercanbasboga.orgservice.reference.LocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Sample data for the demo profile: six student clubs at a made-up university,
 * with officers, advisors, a few months of meetings and books, and some
 * requests waiting in the approval queue. Every name is generated.
 *
 * Personas for the X-Demo-User header:
 * admin@example.org (admin), the advisor and president addresses printed at startup.
 */
@Component
@Profile("demo")
public class DemoData implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoData.class);

    private static final String[] FIRST = {"Ada", "Bora", "Cem", "Defne", "Ece", "Emir", "Deniz", "Ilgaz", "Kerem",
            "Lale", "Mert", "Nil", "Onur", "Pelin", "Rüzgar", "Selin", "Tuna", "Umut", "Yağmur", "Zeynep", "Arda",
            "Beren", "Can", "Duru", "Eren", "Gizem", "Hakan", "İpek", "Kaan", "Mina"};
    private static final String[] LAST = {"Aksoy", "Balcı", "Çetin", "Demir", "Erdem", "Gök", "Kaya", "Kılıç",
            "Korkmaz", "Öztürk", "Polat", "Sarı", "Şahin", "Tekin", "Uslu", "Yalçın", "Yıldız", "Zengin"};
    private static final String[] DEPTS = {"Computer Engineering", "Physics", "History", "Economics",
            "Molecular Biology", "Civil Engineering", "Philosophy", "Mathematics", "Psychology", "Architecture"};

    private record Club(String name, String shortName, String category, String description, int members,
                        int opening) {}

    private static final List<Club> CLUBS = List.of(
            new Club("Photography Club", "PHOTO", "Arts",
                    "Weekly photo walks, a darkroom on the third floor and a show every spring.", 18, 6200),
            new Club("Mountaineering Club", "MOUNT", "Sports",
                    "Day hikes most weekends, two winter climbs a year and a gear library.", 22, 14800),
            new Club("Chess Society", "CHESS", "Games",
                    "Open play on Tuesdays, a rated league and the inter-university cup.", 12, 2100),
            new Club("Robotics Team", "ROBO", "Science and technology",
                    "Builds one competition robot a year and runs workshops for first-years.", 16, 23500),
            new Club("Film Society", "FILM", "Arts",
                    "Screens a film every Thursday and makes one short of its own each term.", 14, 4300),
            new Club("Debate Union", "DEBATE", "Academic",
                    "British Parliamentary debate practice twice a week and two tournaments a term.", 10, 3000));

    private final PersonRepository people;
    private final CategoryRepository categories;
    private final LocationRepository locations;
    private final OrganizationRepository orgs;
    private final MembershipRepository memberships;
    private final MeetingRepository meetings;
    private final TransactionRepository transactions;
    private final BylawsRepository bylaws;
    private final ApprovalRepository approvals;
    private final FileStore files;
    private final Random rnd = new Random(11);

    public DemoData(PersonRepository people, CategoryRepository categories, LocationRepository locations,
                    OrganizationRepository orgs, MembershipRepository memberships, MeetingRepository meetings,
                    TransactionRepository transactions, BylawsRepository bylaws, ApprovalRepository approvals,
                    FileStore files) {
        this.people = people;
        this.categories = categories;
        this.locations = locations;
        this.orgs = orgs;
        this.memberships = memberships;
        this.meetings = meetings;
        this.transactions = transactions;
        this.bylaws = bylaws;
        this.approvals = approvals;
        this.files = files;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        if (orgs.count() > 0) return;

        people.save(new Person("Demo", "Admin", "admin@example.org", "S-0001", "Student Affairs", SystemRole.ADMIN));
        List<Location> rooms = List.of(
                locations.save(new Location("Hall A, Main Building", 120)),
                locations.save(new Location("Seminar Room 2", 40)),
                locations.save(new Location("Student Centre, Room 3", 25)),
                locations.save(new Location("Sports Hall", 200)),
                locations.save(new Location("Library Group Room", 12)));

        List<Person> students = new ArrayList<>();
        for (int i = 0; i < 90; i++) {
            String first = FIRST[i % FIRST.length];
            String last = LAST[(i * 7 + i / FIRST.length) % LAST.length];
            students.add(people.save(new Person(first, last, email(first, last, i), "2024" + String.format("%04d", 1000 + i),
                    DEPTS[i % DEPTS.length], SystemRole.STUDENT)));
        }

        int s = 0;
        for (int c = 0; c < CLUBS.size(); c++) {
            Club club = CLUBS.get(c);
            Organization org = new Organization();
            org.setName(club.name());
            org.setShortName(club.shortName());
            org.setDescription(club.description());
            org.setEmail(club.shortName().toLowerCase() + "@clubs.example.org");
            org.setFoundingDate(LocalDate.of(1985 + c * 6, 1 + c, 10 + c));
            org.setCategory(categories.findAll().stream().filter(x -> x.getName().equals(club.category())).findFirst()
                    .orElseGet(() -> categories.save(new Category(club.category()))));
            org.setLocation(rooms.get(c % rooms.size()));
            orgs.save(org);

            Person advisor = people.save(new Person(FIRST[(c * 5 + 3) % FIRST.length], LAST[(c * 3 + 5) % LAST.length],
                    "advisor" + (c + 1) + "@example.org", "P-" + (300 + c), DEPTS[(c + 4) % DEPTS.length], SystemRole.STAFF));
            join(org, advisor, OrgRole.ADVISOR, MembershipType.FULL, 900);

            OrgRole[] officers = {OrgRole.PRESIDENT, OrgRole.VICE_PRESIDENT, OrgRole.SECRETARY, OrgRole.TREASURER};
            for (int k = 0; k < club.members(); k++) {
                Person p = students.get(s++ % students.size());
                OrgRole role = k < officers.length ? officers[k] : OrgRole.MEMBER;
                MembershipType type = k % 9 == 8 ? MembershipType.RESERVE : MembershipType.FULL;
                join(org, p, role, type, 30 + rnd.nextInt(700));
                if (k == 0) log.info("{}: president {}, advisor {}", club.name(), p.getEmail(), advisor.getEmail());
            }

            // two people waiting to join: one at the advisor, one already signed by the advisor
            Membership m1 = memberships.save(new Membership(org, students.get((s + 3) % students.size()), OrgRole.MEMBER,
                    MembershipType.FULL, ApprovalStatus.PENDING_ADVISOR));
            approvals.save(new Approval(ApprovalKind.MEMBERSHIP, m1.getId(), org, ApprovalStatus.PENDING_ADVISOR,
                    students.get((s + 3) % students.size()).getEmail()));
            if (c % 2 == 0) {
                Membership m2 = memberships.save(new Membership(org, students.get((s + 7) % students.size()),
                        OrgRole.MEMBER, MembershipType.FULL, ApprovalStatus.PENDING_ADMIN));
                approvals.save(new Approval(ApprovalKind.MEMBERSHIP, m2.getId(), org, ApprovalStatus.PENDING_ADMIN,
                        students.get((s + 7) % students.size()).getEmail()));
            }

            seedMeetings(org, rooms, c);
            seedBooks(org, club.opening(), c);
            seedBylaws(org, c);
        }
        log.info("Demo data ready: {} organizations, {} people, {} open approvals",
                orgs.count(), people.count(), approvals.count());
    }

    private Membership join(Organization org, Person p, OrgRole role, MembershipType type, int daysAgo) {
        Membership m = new Membership(org, p, role, type, ApprovalStatus.APPROVED);
        m.setJoinedAt(Instant.now().minus(daysAgo, ChronoUnit.DAYS));
        return memberships.save(m);
    }

    private void seedMeetings(Organization org, List<Location> rooms, int c) {
        String[][] titles = {
                {"Term planning", "Budget review", "Spring show selection", "Darkroom induction"},
                {"Winter climb briefing", "Gear check", "First aid refresher", "Route planning"},
                {"League round 4", "Cup team selection", "Openings workshop", "Simul night"},
                {"Design review", "Motor driver test", "Sponsor call", "Workshop for first-years"},
                {"Screening schedule", "Short film pitch night", "Editing workshop", "Festival trip planning"},
                {"Motion practice", "Tournament prep", "Judging workshop", "Novice night"}};
        LocalDateTime base = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).withHour(18);
        for (int i = 0; i < 4; i++) {
            Meeting m = new Meeting(org);
            m.setTitle(titles[c][i]);
            m.setDescription("Regular " + org.getName().toLowerCase() + " meeting.");
            m.setStartsAt(base.minusDays(70 - i * 21L));
            m.setLocation(rooms.get((c + i) % rooms.size()));
            m.setExpectedAttendees(8 + rnd.nextInt(10));
            m.setStatus(ApprovalStatus.APPROVED);
            meetings.save(m);
        }
        Meeting next = new Meeting(org);
        next.setTitle(c % 2 == 0 ? "End of term meeting" : "Election of officers");
        next.setDescription("Needs a room booking once approved.");
        next.setStartsAt(base.plusDays(9 + c));
        next.setLocation(rooms.get(c % rooms.size()));
        next.setExpectedAttendees(12 + c);
        ApprovalStatus st = c % 3 == 0 ? ApprovalStatus.PENDING_ADMIN : ApprovalStatus.PENDING_ADVISOR;
        next.setStatus(st);
        meetings.save(next);
        approvals.save(new Approval(ApprovalKind.MEETING, next.getId(), org, st, org.getEmail()));
    }

    private void seedBooks(Organization org, int opening, int c) {
        LocalDate start = LocalDate.now().minusDays(150);
        transactions.save(new Transaction(org, TransactionType.INCOME, money(opening), "TRY", "Grant",
                "Faculty grant for the year", start, "demo"));
        String[] income = {"Membership fees", "Sponsor", "Event tickets"};
        String[] expense = {"Equipment", "Travel", "Printing", "Venue", "Food"};
        for (int i = 0; i < 14; i++) {
            LocalDate d = start.plusDays(8 + i * 10L);
            boolean in = i % 3 == 0;
            int amount = in ? 300 + rnd.nextInt(1500) : 120 + rnd.nextInt(Math.max(200, opening / 14));
            transactions.save(new Transaction(org, in ? TransactionType.INCOME : TransactionType.EXPENSE, money(amount),
                    "TRY", in ? income[i % income.length] : expense[(i + c) % expense.length],
                    (in ? income[i % income.length] : expense[(i + c) % expense.length]) + ", " + d.getMonth()
                            .toString().toLowerCase(), d, "demo"));
        }
    }

    private void seedBylaws(Organization org, int c) throws IOException {
        Bylaws approved = saveFile(org, org.getShortName().toLowerCase() + "-bylaws-2023.pdf");
        approved.setStatus(ApprovalStatus.APPROVED);
        if (c == 1 || c == 3) {
            Bylaws draft = saveFile(org, org.getShortName().toLowerCase() + "-bylaws-2026-draft.pdf");
            draft.setStatus(ApprovalStatus.PENDING_ADMIN);
            approvals.save(new Approval(ApprovalKind.BYLAWS, draft.getId(), org, ApprovalStatus.PENDING_ADMIN,
                    org.getEmail()));
        }
    }

    private Bylaws saveFile(Organization org, String name) throws IOException {
        String stored = UUID.randomUUID() + ".pdf";
        byte[] pdf = tinyPdf(org.getName() + " bylaws");
        files.save(stored, new ByteArrayInputStream(pdf));
        Bylaws b = new Bylaws(org, name, stored, "application/pdf", pdf.length);
        b.setStatus(ApprovalStatus.APPROVED);
        return bylaws.save(b);
    }

    /** A valid one-page PDF with a single line of text, so downloads open in a viewer. */
    private static byte[] tinyPdf(String text) {
        String content = "BT /F1 18 Tf 72 720 Td (" + text.replace("(", "").replace(")", "") + ") Tj ET";
        String[] objs = {
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>",
                "<< /Length " + content.length() + " >>\nstream\n" + content + "\nendstream",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"};
        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        int[] offsets = new int[objs.length];
        for (int i = 0; i < objs.length; i++) {
            offsets[i] = pdf.length();
            pdf.append(i + 1).append(" 0 obj\n").append(objs[i]).append("\nendobj\n");
        }
        int xref = pdf.length();
        pdf.append("xref\n0 ").append(objs.length + 1).append("\n0000000000 65535 f \n");
        for (int off : offsets) pdf.append(String.format("%010d 00000 n \n", off));
        pdf.append("trailer\n<< /Size ").append(objs.length + 1).append(" /Root 1 0 R >>\nstartxref\n")
                .append(xref).append("\n%%EOF\n");
        return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private static BigDecimal money(int whole) {
        return BigDecimal.valueOf(whole).setScale(2);
    }

    private static String email(String first, String last, int i) {
        String clean = (first + "." + last).toLowerCase()
                .replace('ı', 'i').replace('ş', 's').replace('ğ', 'g').replace('ç', 'c').replace('ö', 'o')
                .replace('ü', 'u').replace("i\u0307", "i");
        return clean + (i >= FIRST.length ? String.valueOf(i / FIRST.length) : "") + "@students.example.org";
    }
}
