# Arovia — Development Handoff

**Current milestone:** AROVIA-003 — Customer Management  
**Implementation status:** Complete — local automated validation and manual workflow validation reported passing by the developer.  
**Git status:** Feature branch is local; GitHub PR and merge are pending.  
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
**Status:** Implementation and manual acceptance complete locally.

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

The user manually verified the customer workflow, including creation, persistence after refresh, editing and archiving.

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

The following results were reported by the developer as passing locally on 23 November 2026. They were not independently re-executed while preparing this handoff.

| Area | Command / check | Result |
|---|---|---|
| Backend compilation | Gradle compilation | Passed |
| Backend unit tests | `./gradlew test` | Passed |
| Backend integration tests | `./gradlew integrationTest` | Passed |
| Frontend lint | `pnpm lint` | Passed |
| Frontend typecheck | `pnpm typecheck` | Passed |
| Frontend production build | `pnpm build` | Passed |
| Customer creation | Manual workflow | Passed |
| Customer persistence after refresh | Manual workflow | Passed |
| Customer directory and details | Manual workflow | Passed |
| Customer editing | Manual workflow | Passed |
| Customer archiving | Manual workflow | Passed |
| Archive confirmation UI | Visual/manual validation | Passed |

The manual customer workflow was demonstrated using the local application.

The developer reported that the complete workflow is working.

**Important:** Automated frontend component tests have not yet been established. The frontend package currently has no dedicated test runner or Customer test files.

The existing backend unit and integration test suites passed, but this does not mean every scenario listed in the future testing strategy has already been covered.

## 10. Current limitations and follow-ups

- Standardized frontend test infrastructure has not yet been established.
- Identity and Organization need a consolidated automated test coverage review.
- Customer frontend component tests are pending.
- Security and cross-organization isolation coverage should be reviewed and strengthened.
- Role-based business capability authorization policy has not yet been fully established.
- Production CORS, cookie security, SMTP delivery, encryption-key provisioning and rotation require deployment-specific validation.
- No production deployment is implied by local validation.

## 11. Git and milestone status

Repository:

`https://github.com/techfirm123/arovia`

Default development branch:

`develop`

Current implementation branch:

`feature/AROVIA-003-customer-management`

Previously merged milestone:

- AROVIA-002B — Organization Provisioning, Workspace and Context Foundation.
- PR #4 merged to `develop`.
- Merge commit: `c09ec3f60ed819860977de61a4d3479aef0c81ae`.

AROVIA-003 is locally validated but its feature branch has not yet been found on GitHub. No PR or merge is recorded for this milestone.

The next Git operation is to review the local working tree, commit the Customer Management changes, push the feature branch, create a PR against `develop` and merge after review.

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

Backend compilation, unit tests, integration tests, frontend lint, typecheck, production build and manual workflow validation were reported passing locally.

Implementation is complete. GitHub merge remains pending.

---

## 13. Next milestone — AROVIA-QA-001: Platform Testing Foundation

Before introducing the next business capability, establish a consistent testing strategy and reusable test infrastructure for Arovia.

The testing foundation should cover four areas:

1. Backend testing — JUnit, Spring Boot Test, Mockito and PostgreSQL integration testing.
2. Frontend testing — Vitest, React Testing Library, jest-dom and realistic user interactions.
3. Architecture and security testing — module boundaries, authentication, authorization, CSRF and tenant isolation.
4. End-to-end testing — critical business workflows using Playwright where appropriate.

### Proposed workstreams

| Workstream | Scope |
|---|---|
| QA-001A | Testing architecture and conventions |
| QA-001B | Backend test infrastructure |
| QA-001C | Frontend test infrastructure |
| QA-001D | Identity test coverage |
| QA-001E | Organization test coverage |
| QA-001F | Customer test coverage |
| QA-001G | Critical end-to-end workflows and reporting |

### Testing principles

- Use isolated test data.
- Run PostgreSQL integration tests against a dedicated test database.
- Do not depend on local development records.
- Test security behavior at HTTP level.
- Verify cross-organization isolation explicitly.
- Keep tests deterministic and independent of execution order.
- Include automated tests in every future vertical slice.

The test engine is reusable infrastructure. Identity, Organization and Customer are its first platform modules to be brought under a consistent quality baseline.

**Next action:** Complete the AROVIA-003 Git commit and merge before starting AROVIA-QA-001.
