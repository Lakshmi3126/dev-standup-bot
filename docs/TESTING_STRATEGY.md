# Dev Standup Automation Bot — Testing Strategy

This file consolidates testing requirements scattered across PROJECT_SPEC.md, ARCHITECTURE.md, and TEAM_TASKS.md into one coherent strategy, so all four modules are tested to a consistent standard.

---

## 1. Testing Principles

1. New business logic must have tests (AI_RULES.md §9) — no feature is "done" without them (see TEAM_TASKS.md, Definition of Done).
2. Tests belong to the module owner unless integration-testing crosses module boundaries.
3. Prefer testing behavior through the service layer, not by asserting on internal implementation details.
4. Mock external dependencies (Slack, the database where appropriate) — tests should not require live network access or a live Slack workspace to run in CI.

---

## 2. Test Levels

### 2.1 Unit Tests

Scope: a single class/service in isolation, with dependencies mocked (JUnit + Mockito).

Applies to all business logic, especially:

- Timezone/deadline calculations
- ON_TIME/LATE status determination
- Missing-member detection
- Blocker exact-match logic and streak reset (ARCHITECTURE.md §11a)
- DigestLog state transitions (ARCHITECTURE.md §16a)
- Debounce window logic for late submissions (ARCHITECTURE.md §16b)
- Exception mapping (ERROR_HANDLING.md §3)

### 2.2 Integration Tests

Scope: multiple layers together (controller → service → repository → test database), verifying wiring is correct.

- Use an in-memory or containerized test PostgreSQL instance (e.g., Testcontainers) rather than mocking the repository layer entirely, since schema constraints (foreign keys, unique constraints) matter here.
- Slack calls should still be mocked/stubbed at the HTTP client level — never hit real Slack in automated tests.

### 2.3 End-to-End / Scenario Tests

Scope: full request-to-Slack-message flow for the key scenarios listed in TEAM_TASKS.md ("Shared Team Tasks → Testing"). These are owned jointly, run during integration, not by a single person's module.

Required scenarios (from TEAM_TASKS.md, restated here as the authoritative list):

- [ ] End-to-end submission (submit → stored → appears in today's standups)
- [ ] Missing-member scenario (deadline passes with unsubmitted members)
- [ ] 10-minute reminder scenario — personal DM via bot token, not webhook
- [ ] On-time submission scenario
- [ ] Late submission scenario
- [ ] Updated digest scenario, including debounce behavior with multiple late submissions close together
- [ ] Blocker alert scenario (immediate alert on blocker report)
- [ ] Three-day blocker scenario, including: exact-match streak continuing, streak resetting on wording change, streak resetting on a missed day
- [ ] Multiple-team scenario (data isolation — Team A's digest never includes Team B's data)
- [ ] Multiple-timezone scenario (deadlines fire correctly per team's IANA timezone)

---

## 3. Per-Module Test Requirements

### 3.1 Person 1 — Team & Member Management

- Team service tests (CRUD, timezone validation, deadline validation)
- Member service tests (CRUD, team-membership validation)
- Controller tests (request validation, correct DTOs returned)
- **Security-specific:** test that `slackBotToken` and `webhookUrl` never appear in any response payload (see SECURITY.md §2.4, API_CONTRACT.md §10)

### 3.2 Person 2 — Standup Management

- Submission tests (valid submission stored correctly)
- Validation tests (missing required fields, mismatched member/team)
- Duplicate submission tests (409 on same-day resubmission, unless editing is supported)
- History tests (date range search, per-member search, per-team search)
- Late submission tests (correct status determination against team deadline + timezone)

### 3.3 Person 3 — Scheduler & Automation

- Scheduler trigger tests (fires at correct time per team timezone)
- Missing-member detection tests
- Timezone tests (teams in different zones processed independently and correctly)
- Deadline tests (deadline boundary conditions — exactly on time vs. one second late)
- Late submission workflow tests (DigestLog correctly consulted and updated, per ARCHITECTURE.md §16a)
- Per-team isolation in scheduler failures (one team's error doesn't block another's processing — see ERROR_HANDLING.md §5.3)

### 3.4 Person 4 — Notifications & Blockers

- Webhook delivery tests (correct payload format sent to channel)
- Bot-token / personal reminder tests (correct `slack_user_id` targeted, not sent via webhook)
- Message formatting tests (digest renders human-readable, not raw JSON — see PROJECT_SPEC.md §3.8)
- Blocker detection tests (exact-match rule, case-insensitivity, whitespace trimming)
- Persistent blocker tests (3-consecutive-day detection, streak reset on wording change, streak reset on missed day)
- Slack failure handling tests (`SlackDeliveryException` doesn't roll back already-stored data — see ERROR_HANDLING.md §5.4)

---

## 4. What NOT to Test (avoid over-engineering for v1)

- Do not write tests against real Slack workspaces or real webhook URLs.
- Do not test framework internals (e.g., that Spring Data JPA itself works) — trust the framework, test your logic.
- Do not chase 100% coverage as a target; prioritize the scenarios in §2.3 and the business-logic-heavy areas in §2.1 over trivial getters/setters.

---

## 5. Tooling

- JUnit 5 for test structure and assertions.
- Mockito for mocking dependencies (Slack client, repositories in unit tests).
- Postman collection (see TEAM_TASKS.md → Documentation) for manual/exploratory API testing — not a substitute for automated tests, but useful during integration.
- Consider Testcontainers for integration tests requiring a real PostgreSQL instance, to catch schema-constraint issues that pure mocking would miss.

---

## 6. CI Expectations

- All unit and integration tests must pass before a pull request is merged into `develop` (see TEAM_TASKS.md, Definition of Done).
- Tests must not depend on wall-clock time in a way that makes them flaky (e.g., "deadline in 10 minutes" tests should inject a fixed clock rather than relying on `System.now()`).
- Tests must not require internet access or live credentials to run.