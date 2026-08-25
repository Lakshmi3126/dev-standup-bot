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