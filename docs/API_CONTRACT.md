# Dev Standup Automation Bot — API Contract

## 1. API Principles

Base URL:

/api

All APIs should:

- Return JSON.
- Use meaningful HTTP status codes.
- Validate request data.
- Return consistent error responses.

## Security Note

No authentication is implemented in v1. All endpoints are open to any caller with network access to the API. Do not deploy this publicly without adding auth (see PROJECT_SPEC.md §5, Non-Goals). This must be explicitly acknowledged by all four contributors before AWS deployment.

---

# 2. TEAM APIs

## Create Team

POST /api/teams

Request:

{
  "name": "Backend Team",
  "timezone": "Asia/Kolkata",
  "deadline": "10:00",
  "webhookUrl": "https://example.com/webhook",
  "slackBotToken": "xoxb-..."
}

Response:

{
  "id": 1,
  "name": "Backend Team",
  "timezone": "Asia/Kolkata",
  "deadline": "10:00"
}

Note: `slackBotToken` and `webhookUrl` are write-only — neither must ever be echoed back in any response, including this one. Omit both entirely from response DTOs. (An earlier version of this example incorrectly included `webhookUrl` in the response — this was a documentation bug, not an intended exception. See SECURITY.md §2.4.)

---

## Get All Teams

GET /api/teams

---

## Get Team

GET /api/teams/{teamId}

---

## Update Team

PUT /api/teams/{teamId}

Request:

{
  "name": "Backend Team",
  "timezone": "Asia/Kolkata",
  "deadline": "10:30",
  "webhookUrl": "https://example.com/webhook"
}

---

### Team API Decisions

- `deadline` is required when creating a team.
- `webhookUrl` is required when creating a team.
- `slackBotToken` is required when creating a team.
- `slackBotToken` is accepted during team creation only and cannot be updated through PUT.
- Team responses contain only:
  - `id`
  - `name`
  - `timezone`
  - `deadline`
- `webhookUrl` and `slackBotToken` must never appear in team responses.
- `slack_workspace_id` is stored internally but is not exposed through the API.

### Member API Decisions

- `slackUserId` is optional.
- Member responses contain:
  - `id`
  - `teamId`
  - `name`
  - `email`
  - `slackUserId`
    
## Delete Team

DELETE /api/teams/{teamId}

---

# 3. MEMBER APIs

## Add Member

POST /api/teams/{teamId}/members

Request:

{
  "name": "Lavanya",
  "email": "lavanya@example.com",
  "slackUserId": "U123456"
}

---

## Get Team Members

GET /api/teams/{teamId}/members

---

## Get Member

GET /api/teams/{teamId}/members/{memberId}

---

## Update Member

PUT /api/teams/{teamId}/members/{memberId}

---

## Remove Member

DELETE /api/teams/{teamId}/members/{memberId}

---

# 4. STANDUP APIs

## Submit Standup

POST /api/teams/{teamId}/standups

`standupDate` is never accepted in the request — it is always derived server-side from the team's local calendar date at submission time (see ARCHITECTURE.md §12a). This is intentional, not an omission; do not add a `standupDate` field to the request DTO.

Request:

{
  "memberId": 1,
  "yesterday": "Completed login API",
  "today": "Working on JWT authentication",
  "blockers": "Waiting for database credentials"
}

Response:

{
  "id": 101,
  "teamId": 1,
  "memberId": 1,
  "standupDate": "2026-08-25",
  "yesterday": "Completed login API",
  "today": "Working on JWT authentication",
  "blockers": "Waiting for database credentials",
  "submittedAt": "2026-08-25T09:40:00+05:30",
  "status": "ON_TIME"
}

---

# 5. STANDUP HISTORY

## Get Member Standups

GET /api/teams/{teamId}/members/{memberId}/standups

---

## Get Standups by Date Range

GET /api/teams/{teamId}/standups?from=2026-08-01&to=2026-08-25

---

## Get Today's Standups

GET /api/teams/{teamId}/standups/today

---

# 6. DIGEST API

## Get Today's Digest

GET /api/teams/{teamId}/digest/today

Example response:

{
  "teamName": "Backend Team",
  "date": "2026-08-25",
  "submitted": 3,
  "missing": 2,
  "late": 0,
  "standups": [],
  "missingMembers": [],
  "digestSentAt": "2026-08-25T10:00:05+05:30",
  "digestUpdateCount": 0
}

`digestSentAt` and `digestUpdateCount` are sourced from DIGEST_LOG (see DATABASE_SCHEMA.md §7).

---

# 7. BLOCKER APIs

## Get Active Blockers

GET /api/teams/{teamId}/blockers

---

## Get Member Blockers

GET /api/teams/{teamId}/members/{memberId}/blockers

---

# 8. ERROR RESPONSE

Use a consistent structure.

Example:

{
  "timestamp": "2026-08-25T10:00:00Z",
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Team not found",
  "path": "/api/teams/99"
}

---

# 9. HTTP STATUS CODES

200 OK
201 CREATED
204 NO_CONTENT
400 BAD_REQUEST
404 NOT_FOUND
409 CONFLICT
500 INTERNAL_SERVER_ERROR

---

# 10. API RULES

1. Do not change endpoint names without team discussion.
2. Do not change request/response structures without updating this file.
3. Use DTOs.
4. Validate all user input.
5. Use appropriate HTTP status codes.
6. Document new endpoints here before implementation.
7. Never return `slackBotToken` or `webhookUrl` in any response body — write-only fields.
8. No auth is enforced in v1 (see §1 Security Note); do not assume caller identity in business logic.