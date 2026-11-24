# Arovia — Development Handoff

**Current milestone:** AROVIA-QA-005 — End-to-End Testing Foundation  
**Previous milestone:** AROVIA-QA-004 — Frontend Testing Foundation  
**Implementation status:** QA-004 complete and merged to `develop` through PR #8.  
**Current branch:** `feature/AROVIA-QA-005-e2e-testing`  
**Last updated:** 24 November 2026

---

## 1. Product direction

Arovia is an AI-powered, composable business operating platform.

The initial real-world validation domain is computer sales and service. The broader cross-industry direction is an architectural north star, not a commitment to implement every industry immediately.

The platform consists of:

1. **Platform Core** — identity, organizations, permissions, notifications, audit and shared foundations.
2. **Reusable Business Capabilities** — customers, products, inventory, sales, purchasing, finance and reporting.
3. **Industry Compositions** — industry-specific capabilities and workflows. Computer sales and service is the first composition.

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
**Status:** Implementation and manual acceptance complete.

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

The active directory supports search by customer number, display name, email and phone.

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

**Status:** Merged to `develop` through PR #6.

Implementation branch:

`feature/AROVIA-QA-002-backend-integration-support`

Shared test support class:

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

The developer confirmed that both local backend validation commands passed after the refactor:

- `./gradlew clean test`
- `./gradlew integrationTest`

#### AROVIA-QA-003 — HTTP-Level Security and Tenant-Isolation Coverage

**Status:** Merged to `develop` through PR #7.

All 8 HTTP-level security and tenant-isolation tests passed locally.

The objective was to verify the actual HTTP security boundary rather than relying only on manually constructed security contexts or isolated application-level tests.

The test suite exercises authentication, server-side sessions, CSRF protection, active organization selection and cross-organization customer access through HTTP requests.

**Validated scenarios:**

| # | Scenario | Expected result |
|---|---|---|
| 1 | Unauthenticated customer directory request | 401 Unauthorized |
| 2 | Authenticated user without an active organization | 409 Conflict |
| 3 | Authenticated user with a valid active organization | 200 OK |
| 4 | Customer creation without a CSRF token | 403 Forbidden |
| 5 | Customer creation with a valid CSRF token | 201 Created |
| 6 | Read a customer belonging to another organization | 404 Not Found |
| 7 | Update another organization's customer | 404 Not Found; original data unchanged |
| 8 | Archive another organization's customer | 404 Not Found; original status remains ACTIVE |

**Test implementation approach:**

The test setup uses the application's actual HTTP endpoints to establish an authenticated session.

The authentication flow exercised by the test includes:

1. Obtain a CSRF token from `GET /api/v1/identity/csrf`.
2. Authenticate through `POST /api/v1/identity/sessions`.
3. Preserve the server-side session cookie.
4. Obtain a fresh CSRF token after login.
5. Select the active organization through `PUT /api/v1/organizations/active`.
6. Execute customer API requests using the authenticated session and appropriate CSRF token.

Test users are created with passwords encoded through the configured `PasswordEncoder`, allowing the real authentication provider to authenticate them.

The tests use the servlet web application configuration and PostgreSQL integration-test support.

**Important Spring Security 7 / Jackson 3 findings:**

- Spring Security 7's CSRF request handling uses a masked token representation. Tests must use the token returned by the actual CSRF endpoint rather than supplying a manually generated raw UUID.
- The project uses Spring Boot 4.1.2, which uses Jackson 3 packages. The test imports must use `tools.jackson.databind` rather than `com.fasterxml.jackson.databind`.
- No production security weakening or removal of the absolute session lifetime filter was required to resolve the test setup.

The eight tests validate the listed scenarios. They do not establish complete security coverage for every application module or every membership lifecycle scenario.

#### AROVIA-QA-004 — Frontend Testing Foundation

**Status:** Merged to `develop` through PR #8.

Feature branch:

`feature/AROVIA-QA-004-frontend-testing`

Feature commit:

`9100748857a12a147f06d09792f46c317a901c69`

Merge commit:

`0c6dbfb1424e90fb23cdc0e93b375f2a348b99f3`

PR: https://github.com/techfirm123/arovia/pull/8

**Objective:**

Establish a maintainable frontend component testing foundation and cover the existing Customer Management workspace.

**Testing stack:**

- Vitest
- React Testing Library
- jsdom

**Customer workspace coverage:**

12 component tests cover:

- Customer directory loading.
- Customer directory display.
- Empty state.
- Loading error.
- Creation dialog open and close.
- Successful customer creation.
- Creation failure retaining the form.
- Edit form prepopulation.
- Successful customer editing.
- Edit failure retaining changes.
- Archive success.
- Archive cancellation.
- Archive failure retaining confirmation and displaying the error.

**Implementation notes:**

- `CustomersWorkspace.loadCustomers` dependency behavior was corrected by removing `selected` from the dependency list and using functional `setSelected`.
- The archive failure test uses the specific confirmation button selector `name: /^Archive customer$/`.
- Next.js-generated `next-env.d.ts` type references were retained.

**Validation confirmed by the developer:**

| Command | Result |
|---|---|
| `pnpm --filter @arovia/web typecheck` | Passed |
| `pnpm --filter @arovia/web test` | Passed — 12 tests |
| `pnpm --filter @arovia/web lint` | Passed — ESLint and architecture checks |
| `pnpm --filter @arovia/web build` | Passed |

QA-004 establishes component-level testing. It does not replace browser-level end-to-end testing.

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

**QA-003 validation:** Eight HTTP-level tests provide automated evidence for authentication enforcement, active organization context, CSRF protection and cross-organization customer read/update/archive isolation.

The tests verify that cross-organization operations return 404 and do not modify the original customer data.

## 8. Architecture tests

The existing `PlatformArchitectureTest` includes rules for:

- Customer domain must not depend on outer layers.
- Customer application must not depend on web or infrastructure.
- Organization must not depend on Customer.
- Top-level module dependency cycles must be prohibited.

These rules preserve the intended dependency direction as new capabilities are introduced.

## 9. Validation status

The following results have been reported by the developer.

| Area | Command / check | Result |
|---|---|---|
| Backend unit and architecture tests | `./gradlew clean test` after QA-002 refactor | Passed |
| Backend PostgreSQL integration tests | `./gradlew integrationTest` after QA-002 refactor | Passed |
| Identity integration tests | 7 tests | Passed |
| Organization integration tests | 3 tests | Passed |
| Email outbox integration test | 1 test | Passed |
| QA-003 HTTP security tests | 8 scenarios | Passed |
| QA-004 frontend component tests | `pnpm --filter @arovia/web test` | Passed — 12 tests |
| QA-004 frontend typecheck | `pnpm --filter @arovia/web typecheck` | Passed |
| QA-004 frontend lint and architecture | `pnpm --filter @arovia/web lint` | Passed |
| QA-004 frontend production build | `pnpm --filter @arovia/web build` | Passed |
| GitHub Actions — Backend verification | PR #5 | Passed |
| GitHub Actions — Frontend verification | PR #5 | Passed |
| Customer creation | Manual workflow | Passed |
| Customer persistence after refresh | Manual workflow | Passed |
| Customer directory and details | Manual workflow | Passed |
| Customer editing | Manual workflow | Passed |
| Customer archiving | Manual workflow | Passed |
| Archive confirmation UI | Visual/manual validation | Passed |

**Coverage boundaries:**

- QA-003 validates the eight listed HTTP security and tenant-isolation scenarios.
- QA-004 validates the listed Customer Management frontend component behaviors.
- Neither milestone establishes complete application-wide security or browser end-to-end coverage.
- QA-005 is intended to establish browser-level coverage for the critical workflows.

## 10. Current limitations and follow-ups

- Playwright end-to-end coverage for critical business workflows is pending.
- Identity, Organization and Customer need a consolidated automated coverage review.
- Role-based business capability authorization policy has not yet been fully established.
- Revoked/stale membership behavior should receive dedicated HTTP-level regression coverage.
- Production CORS, cookie security, SMTP delivery, encryption-key provisioning and rotation require deployment-specific validation.
- No production deployment is implied by local validation.

## 11. Git and milestone status

Repository:

`https://github.com/techfirm123/arovia`

Development branch:

`develop`

Latest verified `develop` commit:

`0c6dbfb1424e90fb23cdc0e93b375f2a348b99f3`

Commit message:

`Merge pull request #8 from techfirm123/feature/AROVIA-QA-004-frontend-testing`

### Merged milestones

| Milestone | PR | Status |
|---|---:|---|
| AROVIA-002B — Organization Provisioning | #4 | Merged |
| AROVIA-QA-001 — CI workflow | #5 | Merged |
| AROVIA-QA-002 — PostgreSQL integration-test infrastructure | #6 | Merged |
| AROVIA-QA-003 — HTTP security and tenant isolation | #7 | Merged |
| AROVIA-QA-004 — Frontend testing foundation | #8 | Merged |

Other implementation milestones:

- AROVIA-003 — Customer Management; commit `feaa69535cef0d32b5a35466b94a2703b8c0e2da`.

### QA-004 merge details

- Feature commit: `9100748857a12a147f06d09792f46c317a901c69`
- Merge commit: `0c6dbfb1424e90fb23cdc0e93b375f2a348b99f3`
- PR #8: https://github.com/techfirm123/arovia/pull/8

### Current QA-005 working branch

`feature/AROVIA-QA-005-e2e-testing`

The local branch was created from the latest verified `develop` commit above.

The handoff update is a local working-tree change and must be committed by the developer after reviewing and saving this document.

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

### AROVIA-QA-001 — CI Workflow

Added GitHub Actions pull-request checks for backend and frontend verification.

PR #5 was merged to `develop`.

Both CI jobs passed.

### AROVIA-QA-002 — Shared PostgreSQL Integration-Test Infrastructure

Centralized PostgreSQL integration-test configuration and refactored the Identity, Organization and Email Outbox integration-test suites to use it.

PR #6 was merged to `develop`.

The developer reported `./gradlew clean test` and `./gradlew integrationTest` passing after the refactor.

### AROVIA-QA-003 — HTTP-Level Security and Tenant-Isolation Coverage

Implemented HTTP-level integration tests covering authentication, active organization context, CSRF enforcement and cross-organization customer isolation.

All eight tests passed locally.

PR #7 was merged to `develop`.

### AROVIA-QA-004 — Frontend Testing Foundation

Established Vitest, React Testing Library and jsdom.

Added 12 Customer Management component tests covering directory, creation, editing and archive workflows, including success, failure and loading states.

Typecheck, frontend tests, lint/architecture checks and production build passed.

PR #8 was merged to `develop`.

## 13. Current milestone — AROVIA-QA-005 End-to-End Testing Foundation

**Objective:** Establish a small, reliable Playwright browser-testing foundation for the most important existing Arovia workflows.

Keep the milestone deliberately focused. QA-004 already provides component-level coverage; QA-005 should validate that the frontend, backend, authentication and organization context work together through a real browser.

### In-scope user journeys

1. **Authentication and workspace**
    - Log in using a dedicated test account.
    - Access an authenticated workspace route.
    - Log out.
    - Verify protected-route behavior after logout.

2. **Organization context**
    - Select an available organization.
    - Verify the workspace operates with the selected organization context.

3. **Customer lifecycle**
    - Open the customer directory.
    - Create a customer.
    - Verify the customer appears in the directory.
    - Edit the customer.
    - Archive the customer.

### Implementation expectations

- Use Playwright for browser automation.
- Use a dedicated E2E test account and test database.
- Do not make automated tests depend on real email delivery.
- Reuse existing application APIs and workflows.
- Keep test data isolated and clean up test-created data reliably.
- Do not weaken production authentication, authorization or CSRF behavior to simplify browser tests.
- Avoid broad test abstractions or unrelated production changes.

### CI integration

Add an E2E job to the existing GitHub Actions workflow.

The job should install browser dependencies, start the application stack using an isolated test configuration, execute the selected Playwright journeys and publish useful test reports on failure.

The existing backend and frontend verification jobs must remain intact.

### Completion criteria

- Playwright is configured and runs locally.
- The three critical journeys pass.
- Test data setup and cleanup are reliable.
- E2E execution is integrated into GitHub Actions.
- Existing backend and frontend checks continue to pass.
- `docs/AROVIA_HANDOFF.md` is updated with implementation details and actual validation results.

Do not expand this milestone into exhaustive browser coverage, signup/email verification automation or unrelated security testing.

## 14. Immediate development checkpoint

Current local branch:

`feature/AROVIA-QA-005-e2e-testing`

Expected base:

`0c6dbfb1424e90fb23cdc0e93b375f2a348b99f3`

Before implementation:

1. Restore and save this complete handoff document.
2. Review the resulting Git diff and ensure no architecture or milestone history was lost.
3. Commit the documentation update separately from QA-005 implementation.
4. Inspect the existing frontend route structure, authentication flow, organization switcher, customer workspace and backend test configuration.
5. Define the smallest viable Playwright setup based on the actual application.
6. Implement and validate the three selected browser journeys.
7. Integrate the E2E job into CI.
8. Update this handoff with actual results.

**Current achievement:** Arovia has backend unit/integration testing, HTTP-level security and tenant-isolation coverage, frontend component testing and CI verification. QA-005 adds the missing browser-level validation for critical user workflows.
