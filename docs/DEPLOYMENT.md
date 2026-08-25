# Dev Standup Automation Bot — Deployment

This file defines how the application is packaged, deployed, and configured across environments. It consolidates deployment-related items from TEAM_TASKS.md, SECURITY.md, and PROJECT_SPEC.md into a single actionable reference.

---

## 1. Environments

| Environment | Purpose | Database | Notes |
|---|---|---|---|
| Local | Individual development | Local PostgreSQL or Dockerized PostgreSQL | Each developer runs independently |
| Integration | Shared testing before deploy | Dockerized PostgreSQL (shared or per-dev) | Used to validate cross-module integration |
| Production | Live deployment | AWS RDS PostgreSQL | Requires full pre-deployment checklist (§6) |

v1 targets a single production environment on AWS. No staging environment is defined for v1 — if the team wants one, document it here before adding it.

---

## 2. Containerization

### 2.1 Dockerfile

- Multi-stage build: build the Spring Boot JAR in a Maven build stage, then copy only the built artifact into a slim JRE runtime image.
- Do not bake secrets (DB credentials, Slack tokens) into the image at build time — they are injected at runtime via environment variables (see §3).

### 2.2 Local Docker Compose (recommended)

For local/integration environments, a `docker-compose.yml` should define:

- `app` service (the Spring Boot application)
- `db` service (PostgreSQL, with a named volume for persistence)

This lets any of the four contributors spin up a consistent environment with one command, avoiding "works on my machine" drift.

---

## 3. Configuration & Environment Variables

All configuration must be externalized — nothing environment-specific hard-coded (AI_RULES.md §11).

Required environment variables:

| Variable | Purpose |
|---|---|
| `DB_URL` | PostgreSQL JDBC connection string |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `SPRING_PROFILES_ACTIVE` | `local` / `integration` / `production` |
| `SERVER_PORT` | Application port (default 8080 unless overridden) |

Note: `slack_bot_token` and `webhook_url` are **not** global environment variables — they are per-team values stored in the database (see DATABASE_SCHEMA.md §3), entered via the Team API. Only database and infrastructure credentials are environment-level.

- Local: use a `.env` file, excluded from Git via `.gitignore` (see SECURITY.md §2.2).
- Production: use AWS-native secret storage (e.g., environment variables injected via ECS/EC2 instance configuration, or AWS Secrets Manager) rather than a committed file.

---

## 4. Database Migrations

- Use a migration tool (e.g., Flyway or Liquibase) rather than relying on Hibernate `ddl-auto` in production. `ddl-auto=update` is acceptable for local development only.
- Every schema change discussed in DATABASE_SCHEMA.md must correspond to a versioned migration script, checked into the repo.
- Migrations run automatically on application startup in all environments, or as an explicit pre-deploy step — decide and document which, before the first production deploy (open decision, see §7).

---

## 5. Deployment Steps (AWS)

1. Provision AWS RDS PostgreSQL instance (per TEAM_TASKS.md → Deployment).
2. Provision AWS EC2 instance (or equivalent compute) for the application container.
3. Configure security groups: EC2 → RDS on the PostgreSQL port only; restrict inbound access to the application port as appropriate for the trusted-network assumption in SECURITY.md §1.
4. Set environment variables on the EC2 instance / container runtime (§3) — do not SSH in and hand-edit config files with secrets in plaintext history.
5. Build and push the Docker image (or build directly on the instance for v1 simplicity — document which approach is chosen).
6. Run database migrations against RDS.
7. Start the application container.
8. Verify health via a basic endpoint check (e.g., `GET /api/teams` returns 200).
9. Create the first real team via the API and confirm a test digest reaches Slack end-to-end before considering the deployment complete.

---

## 6. Pre-Deployment Checklist

Do not deploy to a shared or public-facing environment until every item is checked:

- [ ] SECURITY.md §1 (no-auth limitation) explicitly acknowledged by all four contributors
- [ ] SECURITY.md §7 security review checklist completed
- [ ] All required environment variables set (§3) — no secrets in image or repo
- [ ] Database migrations applied and verified against RDS
- [ ] Security groups restrict access appropriately (§5.3)
- [ ] End-to-end scenario tests (TESTING_STRATEGY.md §2.3) passing against the integration environment
- [ ] Slack bot token and webhook URL tested against a real (non-production-critical) Slack channel before rollout to the team's actual channel
- [ ] Rollback plan agreed (§7 — currently open)

---

## 7. Open Decisions (resolve as a team before first production deploy)

- [ ] Migration execution: automatic on startup vs. explicit pre-deploy step.
- [ ] Image build location: on EC2 directly vs. built in CI and pushed to a registry (e.g., ECR). ECR is preferable long-term but may be more setup than needed for v1.
- [ ] Rollback strategy if a deployment breaks production (e.g., keep previous image tagged and ready to redeploy).
- [ ] Whether a staging environment is worth adding before v1 ships, given the team is four people on one shared RDS instance.

---

## 8. Ownership

Deployment is a shared task (see TEAM_TASKS.md → Shared Team Tasks → Deployment), not owned by a single module. Whoever performs a deployment should confirm the checklist in §6 with the rest of the team beforehand, not deploy unilaterally.