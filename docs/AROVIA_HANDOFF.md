# Arovia — Development Handoff

**Last updated:** 24 November 2026  
**Repository:** `techfirm123/arovia`  
**Active branch:** `develop`  
**Latest merged milestone:** AROVIA-QA-005 — End-to-End Testing Foundation  
**Latest verified develop commit:** `7b089dc17a91ef3bac7cfc9a057a83a27452cd49`

---

## 1. Product in one paragraph

Arovia is an AI-powered, composable business operating platform. Its first real-world validation domain is computer sales and service, while the underlying capabilities are intended to be reusable across industries. The platform is designed around a shared core, composable business capabilities and industry-specific compositions. AI remains an optional capability: essential business operations must work without depending on AI.

## 2. Current implementation

### Platform foundation

- **Identity:** registration, email verification, Argon2id password hashing, login/logout, server-side sessions, CSRF protection, password recovery and security audit events.
- **Organization:** organization provisioning, profiles, locations, membership and roles, active-organization selection, and trusted organization context resolution.
- **Tenant boundary:** Organization is the canonical tenant boundary. Backend authorization and organization-scoped persistence—not browser-supplied organization IDs—determine access.

### First business capability: Customers

Customer Management is the first reusable business module. It supports individual and business customers, organization-scoped customer numbers, an active directory with search and pagination, customer details, create/update and archive workflows.

Key API operations:

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/v1/customers` | Create customer |
| GET | `/api/v1/customers?page=0&size=20&search=...` | Search/list active customers |
| GET | `/api/v1/customers/{customerId}` | Retrieve customer |
| PUT | `/api/v1/customers/{customerId}` | Update customer |
| POST | `/api/v1/customers/{customerId}/archive` | Archive customer |

The customer workspace is available at `/app/customers`. Archiving preserves history; hard deletion is not part of the current capability.

### Frontend

Public routes include `/`, `/signup`, `/login`, `/forgot-password`, `/reset-password` and `/verify-email`. The authenticated workspace starts at `/app`, with Customer Management at `/app/customers`.

The workspace navigation also presents other business areas, but not all of those areas should be assumed to be implemented. The Overview dashboard still contains illustrative sample data, not persistent business information.

## 3. Architecture and engineering guardrails

- Keep the backend as a **modular monolith** until implementation evidence justifies another deployment boundary.
- Maintain clear platform, business-capability and industry-composition boundaries.
- Identity is global; organization membership and active tenant context are separate concerns.
- Every tenant-owned operation must enforce organization scope in the backend.
- Business modules own their domain, application contracts, persistence and rules.
- Avoid dependencies from domain/application layers toward web or infrastructure implementations.
- Do not introduce generic frameworks or abstractions ahead of a real use case.
- AI integrations must use controlled application contracts/tools, respect user permissions and organization context, and never access business tables directly.
- Consequential AI actions require explicit authorization, approval policy where appropriate, and auditability.
- Essential business workflows must remain usable when AI is disabled or unavailable.

See the reference documents in section 9 for detailed product, architecture and identity decisions.

## 4. Technology stack

| Area | Technology |
|---|---|
| Backend | Java 21, Spring Boot 4.1.2, Gradle Wrapper 9.9.0 |
| Frontend | Next.js App Router, React 19, TypeScript |
| Workspace tooling | pnpm 11.22.0 |
| Database | PostgreSQL, Flyway |
| Session management | Spring Session JDBC |
| Backend shape | Modular monolith |
| Browser testing | Playwright with Chromium |
| Frontend component testing | Vitest, React Testing Library, jsdom |

## 5. Quality and CI status

The GitHub Actions workflow is `.github/workflows/ci.yml`. It runs three jobs for pull requests targeting `develop`:

| Job | Scope | Latest result |
|---|---|---|
| Backend verification | Unit/architecture tests and PostgreSQL integration tests | Passed |
| Frontend verification | Lint/architecture, TypeScript, component tests and production build | Passed |
| Browser end-to-end tests | Isolated PostgreSQL, API startup/readiness and Playwright browser suite | Passed |

**Latest verification:** workflow run #10, triggered for the merged PR #9 head, completed successfully with all three jobs green. [View workflow run #10](https://github.com/techfirm123/arovia/actions/runs/45412709114).

QA-005's browser suite contains five tests across authentication/protected routes, organization context and the customer lifecycle. The original feature validation reported `5 passed (13.6s)` in workflow run #9. The subsequent workflow run #10 also passed the E2E job.

## 6. Known limitations and follow-ups

- Browser coverage currently focuses on the five QA-005 tests; it is not exhaustive application-wide coverage.
- Identity, Organization and Customer would benefit from a consolidated automated coverage review as more workflows are added.
- The role-based authorization policy for future business capabilities still needs to be defined and implemented as those capabilities arrive.
- Revoked/stale membership behavior should receive dedicated HTTP-level regression coverage.
- Production CORS, secure-cookie settings, SMTP delivery, secret provisioning/rotation and deployment configuration require environment-specific validation.
- The Overview dashboard still uses illustrative sample data.
- No production deployment or production-readiness sign-off is implied by the passing CI pipeline.

## 7. Development workflow

Build in small, complete vertical slices:

1. Define the user/business outcome and workflow.
2. Design the UX and domain behavior together.
3. Define API, persistence, authorization and integration changes.
4. Implement the complete slice across the required layers.
5. Validate with unit, integration, component and browser tests appropriate to the change.
6. Update the architecture/reference documents only when a decision or durable design changes; update this handoff with the current state and next checkpoint.

Use one feature branch per slice. Keep `develop` stable and validated. Do not merge until the relevant checks pass. Prefer complete, copy-paste-ready file contents with exact repository paths when sharing implementation changes.

## 8. Current milestone and next checkpoint

### Completed: AROVIA-QA-005

Playwright is configured and integrated into CI. The five selected browser tests validate login/logout and protected routes, active organization selection/persistence, and customer create/edit/archive. The E2E job uses a disposable PostgreSQL 18 `arovia_test` database, provisions its test identity and organization through the real API flow, waits for the API readiness endpoint, and runs Chromium tests. Email verification is bypassed only in that disposable test database; production identity/security behavior is not weakened.

PR #9 is merged to `develop`: [PR #9](https://github.com/techfirm123/arovia/pull/9).  
Merge commit: `7b089dc17a91ef3bac7cfc9a057a83a27452cd49`.

### Next checkpoint

The next implementation milestone has not yet been selected. Choose the next complete operational workflow based on the pilot business's highest-value need and the product roadmap. Avoid starting speculative platform abstractions before a concrete workflow requires them.

## 9. Reference documents

These documents retain the deeper context; this handoff should not duplicate them:

- [`AROVIA-001_DISCOVERY_BASELINE.md`](AROVIA-001_DISCOVERY_BASELINE.md) — product discovery, target users, pilot assumptions, initial workflows and domain baseline.
- [`AROVIA-002_IDENTITY_AND_ORGANIZATION_DESIGN.md`](AROVIA-002_IDENTITY_AND_ORGANIZATION_DESIGN.md) — detailed identity, organization, membership, API, persistence and tenant-isolation design history.
- [`AROVIA_ARCHITECTURE_DECISIONS.md`](AROVIA_ARCHITECTURE_DECISIONS.md) — durable architecture and product-architecture decisions.
- [`AROVIA_PRODUCT_ROADMAP.md`](AROVIA_PRODUCT_ROADMAP.md) — product evolution, optional AI direction, architecture guardrails and roadmap.
- [`AROVIA_QUALITY_HISTORY.md`](AROVIA_QUALITY_HISTORY.md) — quality milestones QA-001 through QA-005, validation scope and CI evidence.

---

**Handoff maintenance rule:** Keep this document focused on the current truth: what exists, what is verified, what remains open and what should happen next. Put detailed historical or design material in the appropriate reference document instead of repeatedly expanding this file.
