# Dev Standup Automation Bot

## 1. Project Overview

The Dev Standup Automation Bot is a backend system that automates the daily standup process for software development teams.

A standup allows developers to report:

- What they completed yesterday
- What they plan to work on today
- Any blockers preventing them from continuing their work

The system collects these updates through REST APIs, stores them in a database, automatically checks submissions at the configured deadline, identifies missing and late submissions, monitors blockers, generates a readable daily digest, and sends the digest to a team's Slack channel through a webhook.

The system supports multiple independent teams, each with its own members, timezone, deadline, and Slack webhook configuration.

---

## 2. Main Goal

Automate the complete standup workflow:

Developer submits
        ↓
Backend stores submission
        ↓
Reminder before deadline
        ↓
Deadline reached
        ↓
Check submissions
        ↓
Identify missing members
        ↓
Generate daily digest
        ↓
Send digest to Slack
        ↓
Late submission received
        ↓
Update and resend digest

---

## 3. Core Features

### 3.1 Team Management

The system must allow teams to be created and managed.

Each team has:

- Team ID
- Team name
- Timezone
- Standup deadline
- Slack webhook destination
- Slack bot token (for personal reminders — see DATABASE_SCHEMA.md)

---

### 3.2 Team Member Management

Each team can have multiple members.

Members have:

- Member ID
- Name
- Email
- Slack user ID
- Team ID

A member belongs to a specific team.

---

### 3.3 Daily Standup Submission

A team member can submit one standup for a given working day.

A standup contains:

- Yesterday
- Today
- Blockers (optional)

Example:

{
  "yesterday": "Completed login API",
  "today": "Working on JWT authentication",
  "blockers": "Waiting for database credentials"
}

---

### 3.4 Configurable Deadline

Each team has a configurable standup deadline.

Default:

10:00 AM

The deadline is interpreted using the team's configured timezone.

Example:

Team A:
Timezone = Asia/Kolkata
Deadline = 10:00 AM

Team B:
Timezone = America/New_York
Deadline = 10:00 AM

---

### 3.5 Pre-Deadline Reminder

10 minutes before the team's configured deadline, the system checks which members have not submitted their standup.

Those members receive a personal Slack reminder.

Example:

"Your standup has not been submitted yet. The deadline is 10:00 AM."

Personal reminders are sent via the Slack Web API (`chat.postMessage`) using the team's bot token and the member's `slack_user_id` — not via the channel webhook. See ARCHITECTURE.md §10 and DATABASE_SCHEMA.md for details.

---

### 3.6 Scheduled Deadline Processing

At the configured deadline, the system automatically:

1. Gets all team members.
2. Gets the day's standup submissions.
3. Identifies submitted members.
4. Identifies missing members.
5. Determines on-time submissions.
6. Processes blockers.
7. Generates the daily digest.
8. Sends the digest through the configured webhook.

The job runs on weekdays.

---

### 3.7 Missing Member Detection

The system must explicitly identify members who have not submitted.

Example:

Team members = 5
Submitted = 3
Missing = 2

The digest must list the missing members.

---

### 3.8 Daily Digest

The system generates a readable digest containing:

- Team name
- Date
- On-time submissions
- Member updates
- Blockers
- Missing members
- Late submissions, when applicable

The digest must be human-readable and must not simply dump raw JSON.

---

### 3.9 Late Submission

Late submissions must be accepted.

If:

Deadline = 10:00 AM
Submission = 10:25 AM

The system marks the standup as:

LATE

The late standup is associated with the original working day.

---

### 3.10 Digest Update After Late Submission

If a member submits after the initial digest has already been sent:

1. Store the late standup.
2. Mark it as LATE.
3. Check DIGEST_LOG to confirm the initial digest was already sent (see DATABASE_SCHEMA.md).
4. Update that day's digest.
5. Send the updated digest to the team's Slack destination.

Multiple late submissions arriving close together should be debounced rather than triggering a separate Slack message per submission. See ARCHITECTURE.md §16b.

---

### 3.11 Standup History

Every standup must be stored.

History must be searchable by:

- Member
- Team
- Date
- Date range
- Submission status

---

### 3.12 Blocker Management

If a member reports a blocker:

- Store the blocker information.
- Highlight the blocker in the digest.
- Send a blocker alert through Slack when appropriate.

---

### 3.13 Persistent Blocker Detection

If the same blocker is reported by the same member for three consecutive standups, the system flags it as an unresolved blocker.

"Same blocker" is determined by an exact, case-insensitive, whitespace-trimmed string match against the immediately preceding standup's blocker text. No fuzzy or NLP matching in v1. A missed standup (no submission that day) breaks the streak rather than pausing it. See ARCHITECTURE.md §11a.

Example:

Monday:
Waiting for API credentials

Tuesday:
Waiting for API credentials

Wednesday:
Waiting for API credentials

Result:

UNRESOLVED BLOCKER

---

### 3.14 Multiple Teams

The application must support multiple teams.

Each team's data must remain isolated.

Each team can have:

- Different members
- Different deadline
- Different timezone
- Different Slack webhook
- Different standup history

---

## 4. Stretch Features

### 4.1 Weekly Summary

Every Friday, the system can generate a weekly summary containing:

- Participation rate
- On-time submission rate
- Late submission rate
- Common blockers
- Unresolved blockers
- Participation trends

---

## 5. Non-Goals

The first version will NOT include:

- AI-generated standup content
- AI-generated summaries
- Mobile application
- Complex frontend dashboard
- Microservices
- Real-time chat
- Advanced analytics
- Authentication / authorization — any client with the base URL can currently call any endpoint for any team. v1 assumes a trusted network / internal deployment. Adding API keys or OAuth is a fast-follow, not a launch blocker, but must be added before any public-facing or multi-organization deployment.

The system should first focus on reliable backend automation.

---

## 6. Technology Stack

### Backend

- Java
- Spring Boot
- Spring Web
- Spring Data JPA
- Hibernate
- Lombok
- Maven

### Database

- PostgreSQL

### Automation

- Spring Scheduler

### Communication

- Slack Webhooks (channel digests)
- Slack Web API / bot token (personal reminders)

### Testing

- JUnit
- Mockito
- Postman

### Deployment

- Docker
- AWS EC2
- AWS RDS PostgreSQL

### Version Control

- Git
- GitHub

---

## 7. Success Criteria

The project is considered successful when:

1. Multiple teams can be created.
2. Members can be added to teams.
3. Members can submit standups.
4. Standups are stored correctly.
5. The system handles team timezones.
6. Members receive personal reminders before the deadline.
7. Missing members are detected automatically.
8. A daily digest is generated automatically.
9. The digest is sent to Slack.
10. Late submissions are accepted.
11. Late submissions update the day's digest without duplicate/spammy sends.
12. Blockers are detected and alerted, including persistent blockers.
13. Standup history can be searched.
14. The system works correctly for multiple teams.
15. The application can run using Docker.
16. All contributors have acknowledged the lack of authentication before any deployment beyond local/internal use.