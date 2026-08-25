# AI Development Rules

This file contains rules for using AI coding tools such as Cursor.

All team members must follow these rules.

---

# 1. Read Project Documentation First

Before making code changes, AI must be given context from:

- PROJECT_SPEC.md
- ARCHITECTURE.md
- DATABASE_SCHEMA.md
- API_CONTRACT.md
- SECURITY.md
- ERROR_HANDLING.md
- TESTING_STRATEGY.md
- DEPLOYMENT.md
- TEAM_TASKS.md

Do not implement features based only on a short prompt.

---

# 2. Do Not Redesign the Project

AI must not independently:

- Change the database architecture
- Change the API contract
- Introduce microservices
- Replace PostgreSQL
- Replace Spring Boot
- Add unnecessary frameworks
- Replace the webhook/bot-token Slack notification split (ARCHITECTURE.md §10) with a single mechanism
- Change the blocker matching rule (ARCHITECTURE.md §11a) to fuzzy/NLP matching

If a major architectural change seems necessary, explain it first.

---

# 3. Follow Layered Architecture

Use:

Controller
   ↓
Service
   ↓
Repository
   ↓
Database

Do not put business logic inside controllers.

---

# 4. Use DTOs

Do not expose JPA entities directly through REST APIs.

Use request/response DTOs.

`slackBotToken` and `webhookUrl` must never appear in response DTOs — write-only fields (see API_CONTRACT.md §10).

---

# 5. Follow Existing Database Schema

Do not create duplicate tables/entities if an existing one already represents the required data.

If a schema change is required:

1. Explain why.
2. Discuss with the team.
3. Update DATABASE_SCHEMA.md.
4. Then implement.

---

# 6. Follow API Contract

Do not change an existing endpoint's:

- URL
- Request format
- Response format

without team approval.

Update API_CONTRACT.md when a change is approved.

---

# 7. Respect Module Ownership

Each developer owns a specific module.

Do not modify another developer's module unnecessarily.

If integration requires a change:

- Inform the owner.
- Make the smallest required change.
- Explain the change in the pull request.

---

# 8. Avoid Unnecessary Dependencies

Do not add libraries simply because they make implementation easier.

Before adding a dependency:

- Check whether Spring Boot already provides the required functionality.
- Discuss significant dependencies with the team.

---

# 9. Write Tests

New business logic should have tests.

Important areas:

- Team validation
- Standup submission
- Missing-member detection
- Deadline logic
- Late submission
- Blocker detection (including exact-match rule and streak reset on missed day)
- Digest generation
- Digest log state transitions (initial send vs. update)
- Personal reminder delivery via bot token (not webhook)

---

# 10. Handle Errors Properly

Do not silently ignore exceptions.

Use appropriate exception handling.

Return meaningful API errors.

---

# 11. Do Not Hard-Code Configuration

Do not hard-code:

- Database credentials
- Slack webhook URLs
- Slack bot tokens
- Team deadlines
- Timezones
- API secrets

Use configuration/environment variables where appropriate.

---

# 12. Security

Never commit:

- Passwords
- API keys
- Slack webhook secrets
- Slack bot tokens
- Database credentials
- AWS credentials

Use environment variables or secure configuration.

Remember v1 has no API authentication (API_CONTRACT.md §1) — do not deploy publicly without addressing this first.

---

# 13. Code Quality

Prefer:

- Small methods
- Clear names
- Single responsibility
- Reusable services
- Meaningful comments only where necessary

Avoid:

- Huge methods
- Duplicate logic
- Dead code
- Unnecessary abstraction

---

# 14. AI Should Explain Important Changes

When AI makes a significant implementation decision, the developer should understand:

- What changed
- Why it changed
- Which files changed
- How the code works
- How it was tested

Do not blindly accept AI-generated code.

---

# 15. Git Rules

AI must not:

- Force push shared branches
- Delete branches without permission
- Rewrite shared history
- Commit secrets

Each feature should be developed on its assigned branch.

---

# 16. Final Rule

AI is a coding assistant.

The team owns:

- Architecture
- Design
- Database
- API contracts
- Security
- Final code

Never merge code that the developer does not understand.