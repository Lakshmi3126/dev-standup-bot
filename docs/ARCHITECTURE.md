# Dev Standup Automation Bot — Architecture

## 1. Architecture Overview

The application follows a layered Spring Boot architecture.

Client
  ↓
REST Controller
  ↓
Service Layer
  ↓
Repository Layer
  ↓
PostgreSQL

Automation:

Spring Scheduler
  ↓
Service Layer
  ↓
Standup/Team/Member Data
  ↓
Digest Generator
  ↓
Notification Service
  ↓
Slack Webhook / Slack Web API

---

## 2. High-Level Architecture

                    Client
                      |
                      v
              REST Controllers
                      |
                      v
                Service Layer
                      |
             +--------+--------+
             |                 |
             v                 v
       Repository Layer   External Services
             |                 |
             v                 v
         PostgreSQL       Slack Webhook / Slack Web API


             Spring Scheduler
                    |
                    v
             Automation Service
                    |
        +-----------+-----------+
        |           |           |
        v           v           v
     Missing      Digest      Blocker
     Detection   Generation   Processing
                    |
                    v
             Notification Service
                    |
                    v
                  Slack

---

## 3. Package Structure

Recommended structure:

src/main/java/com/example/standupbot/

├── controller/
├── service/
├── repository/
├── entity/
├── dto/
├── scheduler/
├── notification/
├── blocker/
├── exception/
├── config/
└── util/

---

## 4. Controller Layer

Responsible for handling HTTP requests.

Examples:

TeamController
MemberController
StandupController

Controllers should:

- Receive requests
- Validate request data
- Call services
- Return responses

Controllers should NOT contain business logic.

---

## 5. Service Layer

Contains business logic.

Examples:

TeamService
MemberService
StandupService
DigestService
ReminderService
BlockerService

Business decisions should happen here.

---

## 6. Repository Layer

Responsible for database access.

Use:

Spring Data JPA

Examples:

TeamRepository
MemberRepository
StandupRepository
BlockerRepository
DigestLogRepository

Repositories should not contain business logic.

---

## 7. Entity Layer

Contains JPA entities:

Team
Member
Standup
Blocker
DigestLog

---

## 8. DTO Layer

API requests and responses should use DTOs instead of exposing JPA entities directly.

Examples:

CreateTeamRequest
UpdateTeamRequest
CreateMemberRequest
SubmitStandupRequest
StandupResponse
TeamResponse

---

## 9. Scheduler

The scheduler is responsible for triggering automation.

Responsibilities:

- Pre-deadline reminders
- Deadline processing
- Daily digest generation
- Weekly summary

The scheduler should trigger services instead of containing all business logic itself.

Example:

Scheduler
  ↓
AutomationService
  ↓
DigestService
  ↓
NotificationService

---

## 10. Notification Architecture

Use a notification abstraction so Slack is not hard-coded everywhere.

Example concept:

NotificationService
        |
        +--------------------+
        v                    v
WebhookNotificationService   BotNotificationService
        |                    |
        v                    v
Slack Incoming Webhook       Slack Web API (chat.postMessage)
(channel digests)            (personal DMs / reminders)

This allows another webhook destination to be added later.

**Important:** Incoming webhooks can only post to a fixed channel — they cannot DM an individual. Personal pre-deadline reminders must go through the Slack Web API (`chat.postMessage`), authenticated with the team's bot token (`slack_bot_token`), targeting the member's `slack_user_id`. Do not attempt to implement personal reminders through the webhook path.

---

## 11. Blocker Architecture

Standup submission
        ↓
Blocker detected
        ↓
BlockerService
        ↓
Store / analyze blocker
        ↓
Alert if necessary

Persistent blocker:

Blocker history
        ↓
Check consecutive occurrences
        ↓
3 consecutive reports
        ↓
Unresolved blocker
        ↓
Alert

### 11a. Blocker Match Rule (v1)

"Same blocker" is determined by exact string match (case-insensitive, trimmed whitespace) between a member's current `blockers` text and their immediately preceding standup's `blockers` text.

- No fuzzy matching, no NLP similarity in v1 — explicitly a non-goal.
- If the wording changes even slightly between days, the consecutive-day streak resets to 1 rather than continuing.
- If a member misses a standup entirely (no submission that day), the streak resets — a gap breaks continuity; it does not pause and resume.

This is a deliberate MVP simplification. If it proves too strict in practice, revisit as a documented schema/logic change per AI_RULES.md §5, not an ad hoc fix inside BlockerService.

---

## 12. Timezone Handling

Team timezone must be stored using a valid IANA timezone.

Examples:

Asia/Kolkata
America/New_York
Europe/London

Do not assume all teams use the server timezone.

The system should use timezone-aware date/time types.

Preferred Java types:

ZonedDateTime
LocalDate
LocalTime

---

## 13. Digest Generation

DigestService receives:

- Team
- Date
- Submitted standups
- Missing members
- Late submissions
- Relevant blockers

It generates a structured digest.

NotificationService then converts the digest into a Slack-compatible message.

---

## 14. Data Flow — Normal Submission

Developer
  ↓
POST /standups
  ↓
StandupController
  ↓
StandupService
  ↓
Validate
  ↓
Determine status
  ↓
StandupRepository
  ↓
PostgreSQL

---

## 15. Data Flow — Deadline

Scheduler
  ↓
AutomationService
  ↓
Find teams whose deadline is reached
  ↓
Find members
  ↓
Find today's standups
  ↓
Find missing members
  ↓
DigestService
  ↓
NotificationService
  ↓
Slack
  ↓
Write DigestLog (initial_sent_at)

---

## 16. Data Flow — Late Submission

Late developer
  ↓
Standup API
  ↓
StandupService
  ↓
Determine LATE status
  ↓
Store standup
  ↓
Regenerate/update digest
  ↓
NotificationService
  ↓
Slack

### 16a. Digest Update Decision Logic

Late developer
  ↓
Standup API
  ↓
StandupService marks LATE
  ↓
Check DigestLog for (team_id, today)
  ↓
  ├─ No row found → do nothing yet (deadline job hasn't run)
  ├─ Row found, no prior update → generate updated digest, send, increment update_count, set last_updated_at
  └─ Row found, prior update exists → same as above (repeat sends are allowed, but see 16b for debouncing)

### 16b. Debounce Recommendation

If multiple members submit late within a short window (e.g., a few minutes of each other), avoid sending a separate Slack message per submission. Recommended approach: batch late submissions with a short delay (e.g., 2–5 minutes) before regenerating and sending the updated digest, rather than firing on every single late POST. This is a v1 decision to make explicitly, not something to leave to whichever module happens to implement it first.

---

## 17. Data Flow — Blocker

Standup submission
  ↓
Blocker detected
  ↓
BlockerService
  ↓
Store blocker
  ↓
Check previous blockers (exact-match rule, see §11a)
  ↓
If persistent (3 consecutive):
    Alert
  ↓
Slack

---

## 18. Architectural Rules

1. Controllers should be thin.
2. Business logic belongs in services.
3. Database access belongs in repositories.
4. Do not expose JPA entities directly through APIs.
5. Use DTOs.
6. Keep modules loosely coupled.
7. Scheduler should call services.
8. Slack communication should be isolated in notification classes, with the webhook path (channel digests) and bot-token path (personal DMs) kept as separate implementations behind the same NotificationService interface.
9. Timezone must come from the team configuration.
10. Do not hard-code team-specific values.
11. Digest send/update state must be tracked (DigestLog) rather than inferred implicitly, so late-submission handling is deterministic.
12. Blocker matching uses the documented exact-match rule (§11a) unless the team formally agrees to change it.