# Dev Standup Automation Bot — Database Schema

## 1. Database

Database:

PostgreSQL

ORM:

Spring Data JPA / Hibernate

---

# 2. Tables

The core database contains:

1. team
2. member
3. standup
4. blocker
5. digest_log

---

# 3. TEAM

### Team Constraints

- `deadline` is required.
- `webhook_url` is required.
- `slack_bot_token` is required.
- `slack_workspace_id` is stored but is not exposed through the API.
- Do not add additional uniqueness constraints beyond those explicitly defined in this schema.

Stores team configuration.

### Fields

| Field | Type | Description |
|---|---|---|
| id | BIGINT | Primary key |
| name | VARCHAR | Team name |
| timezone | VARCHAR | IANA timezone |
| deadline | TIME | Daily standup deadline |
| webhook_url | VARCHAR | Slack incoming webhook destination (channel digests only) |
| slack_bot_token | VARCHAR | Bot token (xoxb-...) with chat:write scope, used for personal DMs. Store encrypted or via env-var reference, never plaintext. |
| slack_workspace_id | VARCHAR | Optional — workspace identifier if supporting multiple workspaces later. |
| created_at | TIMESTAMP | Creation time |
| updated_at | TIMESTAMP | Last update |

**Why two Slack credentials:** Incoming webhooks (`webhook_url`) can only post to a fixed channel — they cannot DM an individual member. Personal pre-deadline reminders require the Slack Web API (`chat.postMessage`) authenticated with a bot token, targeting the member's `slack_user_id`. Both channel digests and personal reminders are needed, so both credentials must be stored per team.

Example:

Team:
Backend Team

Timezone:
Asia/Kolkata

Deadline:
10:00

---

# 4. MEMBER

Stores team members.

### Fields

| Field | Type | Description |
|---|---|---|
| id | BIGINT | Primary key |
| team_id | BIGINT | Foreign key to team |
| name | VARCHAR | Member name |
| email | VARCHAR | Member email |
| slack_user_id | VARCHAR | Slack user identifier |
| created_at | TIMESTAMP | Creation time |

Relationship:

One Team
  ↓
Many Members

### Note on slack_user_id

`slack_user_id` is optional.

Members without a `slack_user_id` are skipped and logged when personal Slack reminders are sent. 

---

# 5. STANDUP

Stores daily standup submissions.

### Fields

| Field | Type | Description |
|---|---|---|
| id | BIGINT | Primary key |
| team_id | BIGINT | Foreign key |
| member_id | BIGINT | Foreign key |
| standup_date | DATE | Working day |
| yesterday | TEXT | Yesterday's work |
| today | TEXT | Today's work |
| blockers | TEXT | Blockers |
| submitted_at | TIMESTAMP | Actual submission time |
| status | VARCHAR | ON_TIME / LATE |
| created_at | TIMESTAMP | Record creation |

Relationship:

One Member
  ↓
Many Standups

### Constraint

```sql
UNIQUE(team_id, member_id, standup_date)
```

This is enforced at the database level, not just in service-layer validation. Relying only on an application-layer "check then insert" is a race condition: two near-simultaneous requests can both pass the check before either has inserted, resulting in two rows for the same member/day. The unique constraint makes this impossible regardless of request timing, and the service layer catches the resulting constraint-violation exception and maps it to `DuplicateSubmissionException` → 409 (see ERROR_HANDLING.md §3, §5.2).

---

# 6. BLOCKER

Stores blocker information for tracking.

### Fields

| Field | Type | Description |
|---|---|---|
| id | BIGINT | Primary key |
| member_id | BIGINT | Member who reported it |
| standup_id | BIGINT | Related standup |
| description | TEXT | Blocker description |
| first_reported_at | TIMESTAMP | First occurrence |
| last_reported_at | TIMESTAMP | Most recent occurrence |
| consecutive_days | INTEGER | Consecutive occurrences |
| status | VARCHAR | ACTIVE / RESOLVED / UNRESOLVED |
| created_at | TIMESTAMP | Creation time |
| updated_at | TIMESTAMP | Last update |

Matching rule for "same blocker" (exact, case-insensitive, trimmed string match against the immediately preceding standup) is documented in ARCHITECTURE.md §11a — do not implement fuzzy matching without updating that section first.

---

# 7. DIGEST_LOG

Tracks whether a digest has already been sent for a given team/day, so late submissions know whether to trigger a fresh send or an update.

### Fields

| Field | Type | Description |
|---|---|---|
| id | BIGINT | Primary key |
| team_id | BIGINT | Foreign key to team |
| digest_date | DATE | The working day this digest covers |
| status | VARCHAR | PENDING / SENT / FAILED (see §7a) |
| initial_sent_at | TIMESTAMP | When the first digest was successfully sent (nullable until status = SENT) |
| last_updated_at | TIMESTAMP | When the digest was last resent (nullable until first update) |
| update_count | INTEGER | Number of times the digest has been resent due to late submissions |
| slack_message_ts | VARCHAR | Slack message timestamp/ID of the sent digest, if using an API that supports editing messages in place instead of resending |

Constraint: one row per (team_id, digest_date).

### 7a. Status Lifecycle

```
PENDING → SENT      (Slack call succeeded)
PENDING → FAILED    (Slack call failed after retries)
FAILED  → SENT      (a later retry or resend succeeds)
```

The row is created in `PENDING` status *before* the Slack call is attempted, not after. This ensures that even if the application crashes between the Slack call and updating the row, the next scheduler run sees a `PENDING`/`FAILED` row rather than no row at all, and knows the digest state needs to be resolved rather than blindly resending or blindly skipping. See ARCHITECTURE.md §15 and §16a for the full sequencing.

**Why:** Without this, the scheduler can't distinguish "first digest for today" from "resend after late submission," risking either duplicate initial sends or a late submission that never triggers an update. See ARCHITECTURE.md §16a for the decision logic that uses this table.

---

# 8. Relationships

TEAM
  |
  | 1:N
  ↓
MEMBER
  |
  | 1:N
  ↓
STANDUP
  |
  | 1:N
  ↓
BLOCKER

TEAM
  |
  | 1:N
  ↓
DIGEST_LOG

A Team has many Members.

A Member has many Standups.

A Standup may contain one or more tracked Blockers.

A Team has one DigestLog row per day.

---

# 9. Important Constraints

## Member

A member must belong to an existing team.

## Standup

A standup must reference:

- Existing team
- Existing member

The member must belong to the specified team.

## Duplicate Standup

A member has only one standup per team per working day, enforced by the `UNIQUE(team_id, member_id, standup_date)` constraint (see §5).

If editing is supported, update the existing standup instead of creating another one. A second POST for the same day must fail with 409 (via the constraint violation), not silently succeed or overwrite.

## Digest Log

One row per (team_id, digest_date). The scheduler creates the row on initial send; late-submission handling updates the existing row rather than inserting a new one.

---

# 10. Standup Status

Allowed values:

ON_TIME
LATE

Status is determined using:

submission timestamp
+
team deadline
+
team timezone

---

# 11. Blocker Status

Allowed values:

ACTIVE
RESOLVED
UNRESOLVED

---

# 12. Indexing

Indexes should be considered for frequently searched fields.

Important candidates:

standup.member_id
standup.team_id
standup.standup_date
standup.status
digest_log.team_id
digest_log.digest_date

Potential composite indexes:

(team_id, standup_date) on standup
(team_id, digest_date) on digest_log — this should also be a unique constraint per §9

This will help daily standup queries and prevent duplicate digest_log rows.

---

# 13. Database Rules

1. Use foreign keys.
2. Do not duplicate team/member information unnecessarily.
3. Use UTC for stored timestamps where appropriate.
4. Use the team's timezone when interpreting deadlines.
5. Do not change the schema without informing the team.
6. Schema changes must be documented.
7. Never store `slack_bot_token` or `webhook_url` in plaintext in version control — use encrypted columns or environment-variable references (see AI_RULES.md §12).