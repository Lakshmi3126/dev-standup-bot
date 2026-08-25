# Dev Standup Automation Bot — Error Handling

This file defines how errors are detected, classified, thrown, caught, and returned across the application. All four modules must follow this consistently, or the API becomes unpredictable for clients (including Postman collections and future frontends).

---

## 1. Principles

1. Do not silently swallow exceptions (see AI_RULES.md §10).
2. Fail fast and explicitly — validate early, at the controller/DTO boundary where possible.
3. Never leak internal details (stack traces, SQL, class names) in API responses.
4. Every error response must follow the structure defined in API_CONTRACT.md §8.
5. Log the full exception server-side; return a clean, minimal message to the client.

---

## 2. Standard Error Response Shape

Defined in API_CONTRACT.md §8 — repeated here for convenience:

```json
{
  "timestamp": "2026-08-25T10:00:00Z",
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Team not found",
  "path": "/api/teams/99"
}
```

- `error` is a short, stable machine-readable code (UPPER_SNAKE_CASE) — clients may branch on this.
- `message` is human-readable and safe to display; it must not contain raw exception text or stack traces.
- `path` is the request path that triggered the error.

Use a single `@ControllerAdvice` / `@RestControllerAdvice` class to produce this shape everywhere. Do not build ad hoc error JSON in individual controllers.

---

## 3. Exception Hierarchy

Define custom exceptions per domain, all extending a common base:

```
StandupBotException (abstract base, extends RuntimeException)
├── ResourceNotFoundException        → 404 NOT_FOUND
├── DuplicateSubmissionException     → 409 CONFLICT
├── InvalidTimezoneException         → 400 BAD_REQUEST
├── TeamMemberMismatchException      → 400 BAD_REQUEST   (member doesn't belong to team)
├── SlackDeliveryException           → 502 BAD_GATEWAY    (Slack API/webhook call failed)
└── ValidationException              → 400 BAD_REQUEST   (generic request validation failure)
```

Rules:

- Every custom exception carries an `errorCode` (matches the `error` field above) and a client-safe `message`.
- Do not reuse `RuntimeException` or generic Java exceptions directly in service code — always throw a named exception so the `@ControllerAdvice` can map it deterministically.
- Add new exception types to this file before introducing them in code (same discipline as API_CONTRACT.md §10 — document before implement).

---

## 4. HTTP Status Mapping

| Status | Meaning | Example trigger |
|---|---|---|
| 400 BAD_REQUEST | Malformed or invalid input | Invalid timezone, missing required field |
| 404 NOT_FOUND | Referenced entity doesn't exist | Unknown `teamId`, `memberId` |
| 409 CONFLICT | Request conflicts with current state | Duplicate standup for the same day |
| 502 BAD_GATEWAY | Downstream Slack call failed | Webhook or bot-token API unreachable/erroring |
| 500 INTERNAL_SERVER_ERROR | Unexpected/unhandled exception | Bug, unmapped exception |

Every unmapped exception must still be caught by a catch-all handler and returned as a generic 500 with a safe message ("An unexpected error occurred") — never let a raw exception reach the client unformatted.

---

## 5. Module-Specific Error Handling

### 5.1 Team & Member (Person 1)

- Creating a team with an invalid IANA timezone → `InvalidTimezoneException` (400).
- Adding a member to a nonexistent team → `ResourceNotFoundException` (404).
- Deleting a team that still has members/standups: decide and document — either cascade delete or block with 409. (Open decision — see §7.)

### 5.2 Standup (Person 2)

- Duplicate standup submission for the same member/team/day → `DuplicateSubmissionException` (409), not a silent overwrite, unless editing is explicitly supported (see DATABASE_SCHEMA.md §9).
- Standup submitted for a `memberId` not belonging to the `teamId` in the URL → `TeamMemberMismatchException` (400).
- Empty `yesterday`/`today` fields → `ValidationException` (400); `blockers` remains optional and may be empty.

### 5.3 Scheduler & Automation (Person 3)

- Scheduler failures must not crash the whole job for all teams — wrap per-team processing in a try/catch so one team's failure (e.g., malformed timezone data) doesn't block digest generation for other teams. Log and continue.
- DigestLog write failures should be retried once, then logged as an alert-worthy failure (this is an internal job failure, not something a client sees — no HTTP response involved here).

### 5.4 Notifications & Blockers (Person 4)

- Slack webhook or bot-token call failures → `SlackDeliveryException`, logged with the team ID and digest date. Do not let a Slack failure roll back the already-stored standup/digest data — the data is durable in Postgres regardless of whether Slack delivery succeeded.
- Consider a retry-with-backoff (e.g., 3 attempts) for transient Slack failures before giving up and logging.
- If personal reminder delivery fails for one member (e.g., missing `slack_user_id`, see DATABASE_SCHEMA.md §4), skip that member and continue the batch — do not fail the entire reminder job.

---

## 6. Logging Standards

- Log full exception details (stack trace, relevant IDs) server-side at `ERROR` level for 5xx-mapped exceptions, `WARN` for 4xx-mapped exceptions.
- Never log secrets (see SECURITY.md §2.5).
- Include `teamId`/`memberId`/`standupDate` in log context where relevant, to make debugging a specific team's issue tractable.

---

## 7. Open Decisions (resolve as a team before implementing)

- [ ] Team deletion with existing members/standups: cascade vs. block.
- [ ] Should `SlackDeliveryException` retries be synchronous (blocking the scheduler) or async/queued? Recommend starting synchronous with a short timeout for v1 simplicity, revisit if it delays other teams' processing.

---

## 8. Testing Requirements

Every custom exception should have at least one test asserting:

1. It's thrown under the correct condition.
2. The `@ControllerAdvice` maps it to the correct HTTP status and `error` code.
3. The response body matches the standard shape (§2) and contains no leaked internals.

See TESTING_STRATEGY.md for how these fit into the overall test plan.