# Dev Standup Automation Bot — Security

This file consolidates all security-relevant decisions scattered across the other docs. If a security question comes up during implementation, check here first; if the answer isn't here, it needs a team discussion before being decided ad hoc.

---

## 1. Authentication & Authorization (v1 Status)

**No authentication or authorization is implemented in v1.**

Any client with network access to the API can:

- Call any endpoint
- Read or write any team's data
- Submit standups as any `memberId`

This is a deliberate, explicit non-goal for v1 (see PROJECT_SPEC.md §5), not an oversight. It assumes deployment on a trusted internal network, not the public internet.

### Before any public-facing or multi-organization deployment:

- [ ] Add API key or OAuth-based authentication
- [ ] Add per-team authorization (a caller should only access their own team's data)
- [ ] Rate-limit the standup submission endpoint
- [ ] Revisit this file and update this section before removing the warning

All four contributors must explicitly acknowledge this limitation before deploying to AWS EC2 (see DEPLOYMENT.md).

---

## 2. Secrets Management

### 2.1 What counts as a secret

- Database credentials (PostgreSQL username/password)
- `slack_bot_token` (per team)
- `webhook_url` (per team — treat as sensitive; it allows posting to the team's Slack channel)
- AWS credentials
- Any API keys added later (e.g., for authentication in a future version)

### 2.2 Rules

1. **Never commit secrets to Git.** Not in code, not in `application.properties`/`application.yml`, not in test fixtures, not in commit messages.
2. Use environment variables or a secrets manager (e.g., AWS Secrets Manager, or `.env` files excluded via `.gitignore`) for local and deployed configuration.
3. `slack_bot_token` and `webhook_url` are stored per-team in the database (see DATABASE_SCHEMA.md §3). Store them encrypted at rest where the deployment target supports it (e.g., RDS encryption); at minimum, do not log them.
4. `slack_bot_token` and `webhook_url` must be **write-only** in the API — never returned in any response body (see API_CONTRACT.md §10). Enforce this in DTOs, not just by convention.
5. Do not print secrets in application logs, stack traces, or error responses.
6. If a secret is accidentally committed, treat it as compromised: rotate it immediately (regenerate the Slack token/webhook, change the DB password) rather than just removing it from a later commit.

---

## 3. Data Isolation Between Teams

Team data must remain logically isolated (see PROJECT_SPEC.md §3.14). In v1, this isolation is enforced only at the query level (every repository query for standups/members/blockers must filter by `team_id`), not by authentication — so it protects against bugs, not malicious access. Do not treat query-level filtering as a substitute for authorization; it is a data-integrity measure, not a security boundary, until auth is added.

---

## 4. Input Validation

- Validate all request bodies at the controller/DTO layer before they reach services (see ARCHITECTURE.md §4, §8).
- Reject standup submissions referencing a `memberId` that doesn't belong to the `teamId` in the URL (see DATABASE_SCHEMA.md §9).
- Validate `timezone` values against the IANA timezone database rather than accepting arbitrary strings.
- Validate `webhookUrl` is a well-formed HTTPS URL before storing it.

---

## 5. Slack Credential Scope

- The bot token should be scoped to the minimum required permission: `chat:write` only. Do not request broader scopes (e.g., channel management, user reads) than the reminder feature needs.
- Incoming webhooks are inherently scoped to a single channel by Slack's design — no additional restriction needed beyond storing the URL securely.

---

## 6. Dependency Security

- Avoid adding dependencies casually (see AI_RULES.md §8). Each new dependency is a potential vulnerability surface.
- Keep Spring Boot and its starters on a maintained version; check for known CVEs before finalizing the dependency list at project setup.

---

## 7. Security Review Checklist (pre-deployment)

- [ ] No secrets in Git history
- [ ] `slackBotToken` / `webhookUrl` never appear in API responses
- [ ] All queries scoped by `team_id`
- [ ] Timezone and webhook URL inputs validated
- [ ] Team explicitly acknowledges no-auth status (or auth has been added)
- [ ] Logs reviewed for accidental secret leakage
- [ ] `.env` / credential files present in `.gitignore`

---

## 8. Ownership

Security is a shared responsibility, not a single person's module (see AI_RULES.md §16). Any contributor can flag a security concern; it should be resolved before merging to `develop`, not deferred.