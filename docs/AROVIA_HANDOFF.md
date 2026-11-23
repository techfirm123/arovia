
# Arovia — Development Handoff

**Current milestone:** AROVIA-QA-002 — Shared PostgreSQL Integration-Test Infrastructure  
**Implementation status:** Complete locally — backend unit/architecture tests and PostgreSQL integration tests passed, as reported by the developer.  
**Git status:** AROVIA-003 is present on `develop`; AROVIA-QA-001 is merged via PR #5; AROVIA-QA-002 is implemented and validated on the local feature branch, with GitHub PR/merge pending.  
**Last updated:** 23 November 2026

---

## 1. Product direction

Arovia is an AI-powered, composable business operating platform.

The initial real-world validation domain is computer sales and service. The broader cross-industry direction is an architectural north star, not a commitment to implement every industry immediately.

The platform consists of:

1. Platform Core — identity, organizations, permissions, notifications, audit and shared foundations.
2. Reusable Business Capabilities — customers, products, inventory, sales, purchasing, finance and reporting.
3. Industry Compositions — industry-specific capabilities and workflows. Computer sales and service is the first composition.

Favor reusable capabilities and explicit module boundaries over a rigid industry-specific application. AI is important, but core business operations must remain usable without depending on AI.

The pilot business is a design partner and validation environment, not a process blueprint. Define optimized workflows from first principles, then validate usability, exceptions, operational fit and outcomes with the pilot business.

## 2. Development methodology

Build and design proceed together through complete vertical slices:

1. Define the business capability and desired outcome.
2. Design the workflow and UX.
3. Define domain, API, persistence, permissions and integration changes.
4. Implement the complete slice.
5. Test the implementation and walk through realistic business scenarios.
6. Refine the design and update architecture and handoff documentation.

Use one feature branch per implementation slice. Keep the stable branch validated and merge only after local validation.

Provide complete, copy-paste-ready file contents with exact repository paths. Update this handoff after each completed milestone.

Do not commit, push or merge unless explicitly requested.

## 3. Technology stack

- Java 21
- Spring Boot 4.1.2
- Gradle Wrapper 9.9.0
- Next.js App Router
- React 19
- TypeScript
- pnpm workspace
- PostgreSQL
- Flyway database migrations
- Spring Session JDBC
- Modular monolith backend

The repository Gradle wrapper is pinned to 9.9.0.

## 4. Architecture principles

- Keep a modular monolith; do not split deployable services prematurely.
- Platform concerns and business-domain capabilities must have clear boundaries.
- Identity is platform-level. Organization membership and tenant context are separate concerns.
- Organization is the canonical tenant boundary; Workspace is its user-facing representation.
- UserAccount remains global and must not acquire an organization ID.
- Business capabilities own their domain, application contracts, persistence and rules.
- Modules communicate through explicit application contracts and domain events where appropriate.
- Common business capabilities must not depend on industry-specific modules.
- Tenant-aware operations must enforce organization scope in the backend.
- Capability activation and authorization must be backend-enforced.
- AI workers call application use cases through controlled tools; they do not access the database directly.
- Consequential actions use backend-enforced permissions, approval policies and audit records.
- Avoid premature generic frameworks and abstractions.
- Introduce persistent schema when the corresponding workflow has been designed.

## 5. Current implementation

### 5.1 Frontend — public website and workspace

Public routes:

- `/` — public marketing homepage
- `/signup` — registration
- `/login`
- `/forgot-password`
- `/reset-password`
- `/verify-email`

Authenticated workspace:

- `/app` — workspace dashboard
- `/app/customers` — customer management

The frontend uses an identity boundary to protect authenticated workspace routes.

The workspace navigation includes Overview, Customers, Sales & Billing, Service Desk, Products & Inventory, Purchasing, Finance, Communications, AI Workforce, Reports and Settings.

The Overview dashboard still contains illustrative sample data. It should not be treated as persistent business information.

### 5.2 Backend — Identity

The Identity module provides:

- User registration and email verification.
- Argon2id password hashing.
- Login and logout.
- Current authenticated identity.
- Server-side sessions through Spring Session JDBC.
- CSRF protection.
- Session fixation protection.
- Absolute session lifetime.
- Login rate limiting.
- Password recovery and reset.
- Authentication and relevant security audit events.

Password-reset tokens are stored as hashes, expire after 30 minutes and can be consumed only once. Reset messages use the encrypted notification outbox.

Password changes invalidate existing server-side sessions.

### 5.3 Backend — Organization

The Organization module provides the business organization and workspace foundation.

Flyway V4 introduced:

- `organization`
- `organization_profile`
- `organization_location`
- `organization_membership`
- `organization_membership_role`
- `organization_audit_event`

Organization APIs:

- `POST /api/v1/organizations`
- `GET /api/v1/organizations`
- `GET /api/v1/organizations/active`
- `PUT /api/v1/organizations/active`
- `DELETE /api/v1/organizations/active`

The active organization ID is stored in the server-side session.

The backend resolves organization context using the authenticated user and active membership. Client-supplied organization IDs are not trusted for tenant authorization.

Important context components:

- `OrganizationContext`
- `OrganizationContextResolver`
- `OrganizationScoped`
- `OrganizationContextInterceptor`
- `OrganizationWebConfiguration`
- `OrganizationSessionAttributes`

The immutable `OrganizationContext` contains user account ID, organization ID, organization name, membership ID and roles.

The interceptor attaches the context to the request. It does not use a ThreadLocal.

Expected context behavior:

- Unauthenticated request: 401.
- Missing active organization context: 409.
- Revoked or stale membership: clear selected organization and return 403.

**Membership lifecycle decision:** Suspension preserves membership history and does not set `ended_at`. Removal ends the membership and sets `ended_at`.

### 5.4 Backend — Customer Management

**Milestone:** AROVIA-003  
**Status:** Implementation and manual acceptance complete. Present on `develop`.

Customer Management is the first reusable, industry-neutral business capability.

Customer types:

- `INDIVIDUAL`
- `BUSINESS`

Customer statuses:

- `ACTIVE`
- `ARCHIVED`

Customer information includes:

- Backend-generated customer number.
- Customer type.
- Display name.
- Optional legal name.
- Optional email.
- Optional phone.
- Optional notes.
- Status.
- Creator and timestamps.
- Version.

Contact information is not globally unique. The same email or phone may be associated with multiple customers.

Customer numbers are unique within an organization, not globally.

The number sequence is database-backed and concurrency-safe.

The generated format is:

`CUS-000001`, `CUS-000002`, and so on.

### 5.5 Customer module boundaries

The Customer module owns:

- Domain entities and enums.
- Application services and command/query contracts.
- JDBC persistence adapter.
- REST controllers and request/response DTOs.
- Customer-specific exception handling.

The Customer application layer does not depend on Organization infrastructure or web implementation classes.

The web controller obtains the trusted organization context and passes the organization ID to the application layer.

Every customer persistence query, update and archive operation is scoped by organization ID.

The customer module does not trust organization IDs supplied by the browser.

### 5.6 Customer APIs

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/v1/customers` | Create customer |
| GET | `/api/v1/customers?page=0&size=20&search=...` | Paginated active directory |
| GET | `/api/v1/customers/{customerId}` | Retrieve customer |
| PUT | `/api/v1/customers/{customerId}` | Update customer |
| POST | `/api/v1/customers/{customerId}/archive` | Archive customer |

The active directory supports search by:

- Customer number.
- Display name.
- Email.
- Phone.

Pagination is supported.

Archiving preserves customer history. Hard deletion is not part of this capability.

### 5.7 Customer frontend

Implemented frontend functionality:

- Customer directory.
- Search and pagination.
- Customer details.
- Customer creation.
- Customer editing.
- Customer archiving.
- Archive confirmation dialog.
- API error handling.
- CSRF-protected write requests.

The customer workspace is available at `/app/customers`.

The creation workflow closes the dialog after successful creation and refreshes the directory.

The archive workflow uses an in-app confirmation dialog rather than the browser's native confirmation prompt.

The customer workflow was manually verified, including creation, persistence after refresh, editing and archiving.

### 5.8 Quality automation and CI

#### AROVIA-QA-001 — CI workflow

The GitHub Actions workflow is present at:

`.github/workflows/ci.yml`

It was merged to `develop` through PR #5.

The workflow runs for pull requests targeting `develop` and supports manual `workflow_dispatch`.

**Backend verification:**

- Uses Java 21 and the repository Gradle wrapper.
- Starts a PostgreSQL 18 service with the dedicated `arovia_test` database.
- Runs `./gradlew --no-daemon clean test`.
- Runs `./gradlew --no-daemon integrationTest`.
- Uploads backend test reports as workflow artifacts.

**Frontend verification:**

- Uses Node.js 24 and pnpm 11.22.0.
- Installs dependencies with `pnpm install --frozen-lockfile`.
- Runs `pnpm lint:web`.
- Runs `pnpm typecheck:web`.
- Runs `pnpm build:web`.

The workflow uses read-only repository permissions and concurrency cancellation for obsolete runs.

PR #5 (`feature/AROVIA-QA-001-testing-foundation` → `develop`) was merged. Both GitHub Actions jobs — Backend verification and Frontend verification — passed.

#### AROVIA-QA-002 — Shared PostgreSQL Integration-Test Infrastructure

**Status:** Implementation and local validation complete. GitHub PR/merge pending.

Implementation branch:

`feature/AROVIA-QA-002-backend-integration-support`

A shared test support class centralizes the PostgreSQL integration-test configuration:

`apps/api/src/test/java/com/arovia/api/testing/PostgresIntegrationTestSupport.java`

It centralizes:

- `AROVIA_TEST_DB_URL` validation, restricting integration tests to the `arovia_test` database.
- Test database username and password resolution.
- Test outbox encryption key and key-version configuration.
- Test rate-limit HMAC key configuration.
- Disabled scheduling and non-web application test properties.

The following integration-test classes use the shared support class:

- `apps/api/src/test/java/com/arovia/api/identity/IdentityPersistenceIntegrationTest.java`
- `apps/api/src/test/java/com/arovia/api/organization/OrganizationProvisioningIntegrationTest.java`
- `apps/api/src/test/java/com/arovia/api/notifications/infrastructure/EmailOutboxOperationalSnapshotIntegrationTest.java`

The refactor preserves the existing test cases, per-suite encryption key versions and cleanup behavior.

The three suites contain 11 integration tests in total:

- 7 Identity integration tests.
- 3 Organization integration tests.
- 1 Email Outbox operational snapshot integration test.

The developer confirmed that both local backend validation commands passed after the refactor.

The shared support class and three refactored test classes have not yet been pushed or merged to `develop`.

## 6. Database migrations

Flyway migration history relevant to the current implementation:

| Migration | Purpose |
|---|---|
| V1–V3 | Identity, verification, notifications, sessions and password recovery |
| V4 | Organization provisioning and membership foundation |
| V5 | Organization membership lifecycle constraint correction |
| V6 | Customer Management schema |

### V5 — Organization membership lifecycle correction

The lifecycle constraint was corrected to reflect the agreed membership semantics:

- ACTIVE: `ended_at IS NULL`
- SUSPENDED: `ended_at IS NULL`
- REMOVED: `ended_at IS NOT NULL`

The already-applied V4 migration must not be edited. The correction is delivered through a new migration.

### V6 — Customer Management

Creates:

- `customer_number_sequence`
- `customer`

The customer table includes organization ownership, customer number, type, contact information, status, creator, timestamps and version.

Important constraints and indexes:

- Unique `(organization_id, customer_number)`.
- Customer type check.
- Customer status check.
- Nonblank display name.
- Organization-scoped directory and search indexes.
- Organization and creator foreign keys.

Customer number generation uses a PostgreSQL atomic upsert with `RETURNING` to support concurrent creation safely.

## 7. Security and tenant isolation

Security requirements for tenant-owned business capabilities:

1. Every tenant-aware request must have a trusted organization context.
2. The backend must resolve the organization from the authenticated session and active membership.
3. Client-supplied organization IDs must never determine data ownership.
4. Every tenant-owned persistence operation must include organization scope.
5. Cross-organization reads and mutations must not expose another organization's records.
6. CSRF protection must apply to state-changing browser requests.
7. Revoked membership must not retain access through a stale session context.

The Customer controller is protected by organization context resolution and authenticated endpoint configuration.

The security configuration uses explicit endpoint authorization and a deny-by-default fallback.

## 8. Architecture tests

The existing `PlatformArchitectureTest` includes rules for:

- Customer domain must not depend on outer layers.
- Customer application must not depend on web or infrastructure.
- Organization must not depend on Customer.
- Top-level module dependency cycles must be prohibited.

These rules preserve the intended dependency direction as new capabilities are introduced.

## 9. Validation status

The following results were reported by the developer. Local commands were run on 23 November 2026. GitHub Actions results refer to PR #5.

| Area | Command / check | Result |
|---|---|---|
| Backend unit and architecture tests | `cd apps/api && ./gradlew clean test` | Passed after QA-002 refactor |
| Backend PostgreSQL integration tests | `cd apps/api && ./gradlew integrationTest` | Passed after QA-002 refactor |
| Identity integration tests | 7 tests in `IdentityPersistenceIntegrationTest` | Passed |
| Organization integration tests | 3 tests in `OrganizationProvisioningIntegrationTest` | Passed |
| Email outbox integration test | 1 test in `EmailOutboxOperationalSnapshotIntegrationTest` | Passed |
| Frontend lint | `pnpm lint:web` | Passed for AROVIA-003 |
| Frontend typecheck | `pnpm typecheck:web` | Passed for AROVIA-003 |
| Frontend production build | `pnpm build:web` | Passed for AROVIA-003 |
| GitHub Actions — Backend verification | PR #5 | Passed |
| GitHub Actions — Frontend verification | PR #5 | Passed |
| Customer creation | Manual workflow | Passed |
| Customer persistence after refresh | Manual workflow | Passed |
| Customer directory and details | Manual workflow | Passed |
| Customer editing | Manual workflow | Passed |
| Customer archiving | Manual workflow | Passed |
| Archive confirmation UI | Visual/manual validation | Passed |

The developer confirmed that both local backend commands — `./gradlew clean test` and `./gradlew integrationTest` — passed after the QA-002 refactor.

The developer also confirmed that both GitHub Actions jobs on PR #5 passed before the PR was merged.

The customer workflow was manually validated using the local application, including creation, persistence after refresh, editing and archiving.

**Coverage boundary:** The shared PostgreSQL support refactors existing integration-test configuration; it does not itself add HTTP-level authorization, CSRF or cross-organization isolation tests. Automated frontend component tests and end-to-end Playwright coverage have not yet been established.

## 10. Current limitations and follow-ups

- Standardized frontend component test infrastructure (for example, Vitest and React Testing Library) has not yet been established.
- Customer frontend component tests are pending.
- HTTP-level security coverage for authentication, active organization context, CSRF and cross-organization isolation should be reviewed and strengthened.
- Playwright end-to-end coverage for critical business workflows has not yet been established.
- Identity, Organization and Customer need a consolidated automated coverage review.
- Role-based business capability authorization policy has not yet been fully established.
- Production CORS, cookie security, SMTP delivery, encryption-key provisioning and rotation require deployment-specific validation.
- No production deployment is implied by local validation.

## 11. Git and milestone status

Repository:

`https://github.com/techfirm123/arovia`

Default development branch:

`develop`

Latest verified `develop` commit:

`7a363a6d4397b54facaafdb3887439f5b5793619`

Commit message:

`Merge pull request #5 from techfirm123/feature/AROVIA-QA-001-testing-foundation`

### Merged and present on `develop`

- AROVIA-002B — Organization Provisioning, Workspace and Context Foundation; PR #4 merged; merge commit `c09ec3f60ed819860977de61a4d3479aef0c81ae`.
- AROVIA-QA-001 — CI workflow; PR #5 merged; merge commit `7a363a6d4397b54facaafdb3887439f5b5793619`.

### Present on `develop` without a separate PR merge

- AROVIA-003 — Customer Management; commit `feaa69535cef0d32b5a35466b94a2703b8c0e2da` (`feat(customer): implement customer management`).

### Current local implementation branch

`feature/AROVIA-QA-002-backend-integration-support`

AROVIA-QA-002 has been implemented and both local backend validation commands passed. Its shared support class and three refactored integration-test files are not yet present on the verified `develop` branch. No GitHub PR or merge is recorded for QA-002.

No commit, push or merge for QA-002 has been performed as part of this handoff update.

## 12. Milestone history

### AROVIA-001 — Product & Engineering Foundation

Established the monorepo, Spring Boot API bootstrap, frontend workspace and engineering foundations.

### AROVIA-002A.1 — Registration and Email Verification

Implemented identity registration, password hashing, email verification, encrypted notification outbox, resend controls and audit events.

### AROVIA-002A.2 — Authentication, Sessions and Account Recovery

Implemented login, server-side sessions, CSRF protection, session fixation protection, logout, password recovery and session invalidation after password reset.

### AROVIA-002A.3 — Identity and Notifications Modularization

Introduced application-owned ports and clearer module boundaries for Identity and Notifications.

### Final Identity Web-Boundary Cleanup

Introduced the authenticated-user application contract and authentication audit port. Removed the Identity web-layer dependency on concrete infrastructure implementations.

### AROVIA-002B.0 — Public Website and Signup Experience

Added the public marketing homepage, signup flow and authenticated workspace route while reusing the existing identity registration and verification flow.

### AROVIA-002B — Organization Provisioning, Workspace and Context Foundation

Implemented organization creation, organization profiles, membership, owner-role assignment, active organization selection and trusted request context resolution.

### AROVIA-003 — Customer Management

Implemented the first reusable business capability with customer creation, directory, search, pagination, details, updates and archiving.

Customer Management is present on `develop` in commit `feaa69535cef0d32b5a35466b94a2703b8c0e2da`.

### AROVIA-QA-001 — CI Workflow

Added GitHub Actions pull-request checks for backend and frontend verification.

PR #5 was merged to `develop` in commit `7a363a6d4397b54facaafdb3887439f5b5793619`.

Both CI jobs passed.

### AROVIA-QA-002 — Shared PostgreSQL Integration-Test Infrastructure

Centralized PostgreSQL integration-test configuration and refactored the Identity, Organization and Email Outbox integration-test suites to use it.

The developer reported `./gradlew clean test` and `./gradlew integrationTest` passing after the refactor.

GitHub PR/merge remains pending.

---

## 13. Next milestone — AROVIA-QA-003: HTTP-Level Security and Tenant-Isolation Coverage

The next quality slice should strengthen automated tests around the security boundary that protects tenant-owned business capabilities.

Focus areas:

1. Verify unauthenticated requests are rejected.
2. Verify authenticated requests without an active organization context receive the expected context-required response.
3. Verify authenticated requests with a valid active organization can access their own customer records.
4. Verify CSRF protection on state-changing browser requests.
5. Verify users cannot read, update or archive customers belonging to another organization.
6. Verify stale or revoked organization membership does not retain access.
7. Keep tests deterministic and use the dedicated PostgreSQL integration-test configuration established in QA-002.

This is a proposed next slice based on the current coverage gaps; it is not yet implemented.

After that, continue with frontend component testing and critical end-to-end workflow coverage. Keep each as a complete, independently validated vertical slice.
