# org-management-service

[![CI](https://github.com/Omercanbasboga/org-management-service/actions/workflows/ci.yml/badge.svg)](https://github.com/Omercanbasboga/org-management-service/actions/workflows/ci.yml)

The backend for [org-management-console](https://github.com/Omercanbasboga/org-management-console): student clubs, their members, meetings, books and bylaws, and the approval queue that most changes pass through. Spring Boot 3.5, Java 21, JPA, Spring Security as an OAuth2 resource server.

**See it working:** the [console's live demo](https://omercanbasboga.github.io/org-management-console/) runs an in-browser copy of these rules. Sign a request as a club's advisor, switch to the admin and approve it.

## Where it comes from

I wrote the original service for a university's student club system. That code, its data and its servers stay with the university. This repository keeps the design and the rules and swaps everything specific for a made-up campus: the package names, the Turkish domain names (the original called approvals *onay* and bylaws *tüzük*), the student import job that read from the university's own systems, and the data.

## The approval flow

New members, meetings and bylaws uploads don't take effect when they're created. Each opens an approval that needs two signatures: first the club's staff advisor, then a system admin.

```
request ──> PENDING_ADVISOR ──advisor──> PENDING_ADMIN ──admin──> APPROVED
                  │                            │
                  └──────── either ────────────┴──> REJECTED
```

- Each signature counts only at its own step. The advisor can't approve on the admin's behalf, and the admin can't skip the advisor. A club with no advisor starts its requests at the admin step, so nothing waits on a signature nobody can give.
- `ApprovalWorkflow` holds these rules as a pure function with its own tests. `ApprovalService` loads the request, works out who the caller is for this club, and asks the workflow for the next status.
- The approval code doesn't know what it is approving. Memberships, meetings and bylaws each implement `ApprovalSubject` (how the item looks in the queue, and what changes on its row when the status moves), so adding a fourth kind means one new class.
- `GET /api/v1/approvals` returns only what the caller can sign right now: an advisor sees their own clubs at the advisor step, an admin sees everything at the admin step.
- A cancelled meeting withdraws its open approval so it leaves the queue.

## Who can do what

Roles inside a club live on the membership (`OrgRole`), so one student can be president of one club and a plain member of another. The system-wide admin role comes from the token.

| Action | Who |
| --- | --- |
| Ask to join | anyone, for themselves |
| Add someone, give a role, remove a member | president, vice president, secretary |
| Schedule or cancel a meeting | the four officers |
| See the ledger | officers and the advisor |
| Record income or an expense | president, vice president, treasurer |
| Upload bylaws | president, secretary |
| Appoint or remove an advisor, create or close a club, edit categories and locations | admin |

A few rules the service checks so the client doesn't have to:

- an expense that would take the balance below zero is refused, and amounts are always positive with the type giving the direction
- a meeting can't book more people than its room holds, and it can be edited only until it is approved
- bylaws must be PDF, DOCX or ODT, up to 5 MB. The stored file gets a generated name; the uploaded name is kept for display only, with path segments and control characters stripped. Downloads go out with the content type we chose, not the client's.
- removing a member or closing a club keeps the row with a date, so history stays readable

## Running it

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

The `demo` profile gives you in-memory H2 on `:8082`, six clubs with members, books and pending requests, and Swagger UI at `http://localhost:8082/swagger-ui.html`. There is no identity provider in the demo. Pick a persona with the `X-Demo-User` header:

```bash
curl -H 'X-Demo-User: admin@example.org'    localhost:8082/api/v1/approvals
curl -H 'X-Demo-User: advisor3@example.org' localhost:8082/api/v1/approvals
```

The president of each club is printed in the startup log.

The `prod` profile uses Postgres and expects a JWT on every call, from the gateway in front of the service ([sso-gateway-bff](https://github.com/Omercanbasboga/sso-gateway-bff)). The token must carry an `email` claim and a `roles` list; `ORG-ADMIN` in that list makes the caller an admin (configurable with `ADMIN_ROLE`).

```bash
cp .env.example .env    # set the password and your issuer
docker compose up --build
```

Tests: `./mvnw test`. `ApiFlowTest` boots the demo and walks a membership through both signatures, checks who gets 403 for what, tries to overdraw the books, uploads a good and a bad file, and cancels a meeting out of the queue. `JwtModeTest` checks that the production setup ignores the demo header and reads roles from the token.

## API

All under `/api/v1`.

- `organizations`: list, get, create, update, close
- `organizations/{id}/members`: current members, request, remove
- `organizations/{id}/meetings`: list, schedule, edit, cancel
- `organizations/{id}/finance/summary`, `organizations/{id}/finance/transactions`
- `organizations/{id}/bylaws`: newest version, `history`, upload, `{bylawsId}/file`
- `approvals`: the caller's queue; `PUT approvals/{id}` signs, `DELETE approvals/{id}?reason=` turns it down
- `users`, `categories`, `locations`

## Notes

- Schema changes are left to Hibernate (`ddl-auto=update`). A real deployment should add Flyway first.
- Uploaded files go to local disk (`UPLOADS_DIR`). In a cluster they belong in object storage.
- All names in the demo data are generated.
