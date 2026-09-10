# Dev Standup Automation Bot — Team Tasks

## Team Structure

The project is divided into four major modules.

Person 1:
Team & Member Management

Person 2:
Standup Management

Person 3:
Scheduler & Automation

Person 4:
Notifications & Blocker Management

---

# SHARED CONTRACTS & CROSS-CUTTING RESPONSIBILITIES

These items affect multiple modules. They are not owned exclusively by one feature module, but one person is assigned as the primary driver to ensure the shared piece is implemented consistently.

## 1. DigestLog — Primary Driver: PERSON 3

Person 3 owns:

- [ ] DigestLog entity
- [ ] DigestLog repository
- [ ] DigestLog status model
- [ ] PENDING state
- [ ] SENT state
- [ ] FAILED state
- [ ] Initial digest record creation
- [ ] Digest delivery tracking

Person 4 consumes this contract when implementing Slack delivery.

### Agreed interaction

Person 3 provides the notification layer with the digest record.

Conceptually:

    DigestLog
        ↓
    NotificationService
        ↓
    Slack
        ↓
    success → markSent()
    failure → markFailed()

The exact service/repository method signatures must be agreed before Person 3 and Person 4 implement their modules independently.

---

## 2. Global Exception Handling — Primary Driver: PERSON 1

Person 1 owns the initial global exception-handling infrastructure.

Responsibilities:

- [ ] Create @RestControllerAdvice
- [ ] Implement global exception handler
- [ ] Implement standard error response
- [ ] Implement agreed exception/status mappings
- [ ] Create base/custom exception structure defined in ERROR_HANDLING.md

Other contributors must use the shared exception-handling mechanism.

They should NOT create separate global exception handlers.

---

## 3. Secure Slack Credential Handling — Primary Driver: PERSON 1

Person 1 owns the secure representation of team Slack credentials inside the Team entity and API layer.

Responsibilities:

- [ ] Store webhook URL/token according to DATABASE_SCHEMA.md
- [ ] Ensure sensitive fields are not returned in API responses
- [ ] Use DTOs that exclude sensitive fields from responses
- [ ] Ensure sensitive fields are not accidentally serialized
- [ ] Ensure credentials are not logged

Person 4 consumes these credentials through the appropriate service without exposing them through APIs or logs.

Person 4 must not create alternative credential storage.

---

## 4. Working-Day Derivation — Primary Driver: PERSON 2

Person 2 owns the logic for determining the standup working date.

Responsibilities:

- [ ] Derive standup_date according to ARCHITECTURE.md
- [ ] Use the team's timezone
- [ ] Apply the defined working-day rules
- [ ] Ensure late submissions are associated with the correct standup day
- [ ] Test date-boundary cases

Person 3 must use this existing logic rather than creating a second working-day calculation.

---

## 5. Digest Debounce — Primary Driver: PERSON 3

Person 3 owns the debounce behavior for late-submission digest updates.

Responsibilities:

- [ ] Implement the configured debounce window
- [ ] Prevent multiple late submissions from generating unnecessary Slack messages
- [ ] Ensure the final updated digest contains all late submissions received within the debounce window
- [ ] Coordinate with NotificationService

Person 4 is responsible for sending the resulting notification, not implementing a separate debounce mechanism.

---

## Initial Project Infrastructure

Although these are shared team responsibilities, the following drivers will coordinate the initial setup.

### Database Migrations — Driver: PERSON 1

- [ ] Set up database migration tool
- [ ] Create initial migration
- [ ] Verify schema matches DATABASE_SCHEMA.md
- [ ] Document migration process

All contributors must use migrations for schema changes.

### Docker / Docker Compose — Driver: PERSON 3

- [ ] Create Dockerfile
- [ ] Create docker-compose configuration
- [ ] Configure application container
- [ ] Configure PostgreSQL container
- [ ] Verify application can run through Docker

All contributors should test their modules in the shared Docker environment.

### Testcontainers — Driver: PERSON 2

- [ ] Configure Testcontainers
- [ ] Configure PostgreSQL test container
- [ ] Document integration-test setup
- [ ] Provide shared test configuration

All contributors should use the shared integration-test setup.

---

## Shared Contract Rule

Before Person 3 and Person 4 begin implementing the scheduler, digest, and notification integration, the team must agree on:

- DigestLog schema
- DigestLog status values
- DigestLog service interaction
- NotificationService contract
- Working-day calculation
- Debounce behavior

These decisions must be documented before implementation.

No contributor should independently create a competing implementation of a shared contract.

---

# PERSON 1 — TEAM & MEMBER MANAGEMENT

## Responsibilities

### Team Management

- [ ] Create Team entity
- [ ] Create Team repository
- [ ] Create Team service
- [ ] Create Team controller
- [ ] Create Team DTOs
- [ ] Create Team APIs
- [ ] Add team validation
- [ ] Configure timezone
- [ ] Configure deadline
- [ ] Configure webhook
- [ ] Configure Slack bot token (store securely, write-only in DTOs)

### Member Management

- [ ] Create Member entity
- [ ] Create Member repository
- [ ] Create Member service
- [ ] Create Member controller
- [ ] Create Member DTOs
- [ ] Add member API
- [ ] Update member API
- [ ] Remove member API
- [ ] Validate team membership
- [ ] Validate slack_user_id presence when reminders are enabled

### Testing

- [ ] Team service tests
- [ ] Member service tests
- [ ] Controller tests
- [ ] Validation tests
- [ ] Test that bot token / webhook URL never leak into response DTOs

### Agreed Team Decisions

- `deadline` is required during team creation.
- `webhookUrl` is required during team creation.
- `slackBotToken` is required during team creation.
- `slackBotToken` cannot be updated through PUT.
- Team deletion returns 409 if members exist.
- Team deletion does not cascade to members.
- `slack_workspace_id` is stored internally but not exposed through API responses.
- Team responses never expose Slack secrets.
- `slackUserId` is optional for members.
- Members without `slackUserId` are skipped/logged for personal reminders.

---

# PERSON 2 — STANDUP MANAGEMENT

## Responsibilities

### Standup

- [ ] Create Standup entity
- [ ] Create Standup repository
- [ ] Create Standup service
- [ ] Create Standup controller
- [ ] Create Standup DTOs
- [ ] Implement submission API
- [ ] Validate member/team relationship
- [ ] Prevent duplicate daily submissions
- [ ] Record submission timestamp
- [ ] Determine ON_TIME/LATE status

### History

- [ ] Member standup history
- [ ] Date-range search
- [ ] Team standup search
- [ ] Today's standups endpoint
- [ ] Filtering
- [ ] Pagination if required

### Testing

- [ ] Submission tests
- [ ] Validation tests
- [ ] History tests
- [ ] Late submission tests

---

# PERSON 3 — SCHEDULER & AUTOMATION

## Responsibilities

### Scheduler

- [ ] Configure Spring Scheduler
- [ ] Design timezone-aware scheduling
- [ ] Implement weekday processing
- [ ] Identify teams whose deadlines are approaching
- [ ] Implement 10-minute reminder trigger

### Missing Detection

- [ ] Get team members
- [ ] Get today's standups
- [ ] Compare members vs submissions
- [ ] Identify missing members

### Daily Digest

- [ ] Build digest data
- [ ] Include submitted members
- [ ] Include missing members
- [ ] Include late submissions
- [ ] Integrate with NotificationService
- [ ] Write DigestLog row on initial send (ARCHITECTURE.md §15)
- [ ] Check DigestLog before resending on late submission (ARCHITECTURE.md §16a)
- [ ] Implement debounce window for clustered late submissions (ARCHITECTURE.md §16b)

### Late Workflow

- [ ] Detect late submissions
- [ ] Update daily digest
- [ ] Trigger updated notification
- [ ] Update DigestLog (last_updated_at, update_count)

### Testing

- [ ] Scheduler tests
- [ ] Missing-member tests
- [ ] Timezone tests
- [ ] Deadline tests
- [ ] Late submission workflow tests
- [ ] DigestLog state transition tests

---

# PERSON 4 — NOTIFICATIONS & BLOCKERS

## Responsibilities

### Slack

- [ ] Design notification interface (webhook path + bot-token path, ARCHITECTURE.md §10)
- [ ] Implement webhook service (channel digests)
- [ ] Implement Slack Web API bot-token service (personal reminders — NOT via webhook)
- [ ] Implement Slack message formatting
- [ ] Send daily digest
- [ ] Send updated digest
- [ ] Send personal reminders via chat.postMessage using slack_user_id

### Blockers

- [ ] Detect blockers in standups
- [ ] Store blocker records
- [ ] Implement blocker status
- [ ] Implement blocker alerts
- [ ] Track repeated blockers using exact-match rule (ARCHITECTURE.md §11a)
- [ ] Detect 3 consecutive occurrences
- [ ] Reset streak on missed standup day
- [ ] Generate unresolved blocker alert

### Weekly Summary

Stretch:

- [ ] Weekly participation calculation
- [ ] On-time/late statistics
- [ ] Common blockers
- [ ] Unresolved blockers
- [ ] Weekly Slack report

### Testing

- [ ] Webhook tests
- [ ] Bot-token / personal reminder tests
- [ ] Message formatting tests
- [ ] Reminder tests
- [ ] Blocker detection tests
- [ ] Persistent blocker tests (including streak reset on missed day and on wording change)

---

# SHARED TEAM TASKS

These tasks must be done together.

## Initial Setup

- [ ] Create GitHub repository
- [ ] Add all team members
- [ ] Create develop branch
- [ ] Set up Spring Boot
- [ ] Set up Maven
- [ ] Set up PostgreSQL
- [ ] Configure database credentials through environment variables
- [ ] Configure application environment variables (port, active profile, etc.)
- [ ] Configure Slack credentials securely according to SECURITY.md — these are per-team values entered through the Team API and stored in the database, NOT global environment variables (see DEPLOYMENT.md §3)
- [ ] Create initial project structure
- [ ] Create docs folder

## Architecture

- [ ] Finalize database schema (including digest_log table)
- [ ] Finalize API contract
- [ ] Finalize package structure
- [ ] Agree on naming conventions
- [ ] Agree on Git workflow
- [ ] Acknowledge no-auth decision for v1 (PROJECT_SPEC.md §5)

## Integration

- [ ] Integrate Team module
- [ ] Integrate Member module
- [ ] Integrate Standup module
- [ ] Integrate Scheduler
- [ ] Integrate Digest generation
- [ ] Integrate Slack (both webhook and bot-token paths)
- [ ] Integrate Blocker management

## Testing

- [ ] End-to-end submission test
- [ ] Missing-member scenario
- [ ] 10-minute reminder scenario (personal DM via bot token)
- [ ] On-time submission scenario
- [ ] Late submission scenario
- [ ] Updated digest scenario (with debounce)
- [ ] Blocker alert scenario
- [ ] Three-day blocker scenario (including streak reset cases)
- [ ] Multiple-team scenario
- [ ] Multiple-timezone scenario

## Deployment

- [ ] Create Dockerfile
- [ ] Build Docker image
- [ ] Run application in Docker
- [ ] Configure PostgreSQL container/local environment
- [ ] Prepare AWS EC2
- [ ] Prepare AWS RDS PostgreSQL
- [ ] Confirm no-auth risk is acceptable for deployment target, or add auth first
- [ ] Deploy application
- [ ] Test deployed application

## Documentation

- [ ] Complete README
- [ ] Document setup instructions
- [ ] Document API endpoints
- [ ] Document database schema
- [ ] Add Postman collection
- [ ] Add architecture diagram
- [ ] Add demo instructions

---

# DEVELOPMENT STATUS

## Person 1

Status: NOT STARTED

## Person 2

Status: NOT STARTED

## Person 3

Status: NOT STARTED

## Person 4

Status: NOT STARTED

---

# Git Branches

main
develop

feature/team-member-management
feature/standup-management
feature/scheduler
feature/notifications-blockers

---

# Definition of Done

A feature is considered complete only when:

- [ ] Code implemented
- [ ] Validation added
- [ ] Error handling added
- [ ] Tests added
- [ ] Documentation updated if necessary
- [ ] Code reviewed
- [ ] Pull request approved
- [ ] Integrated into develop