# Arovia --- Development Handoff

**Current milestone:** AROVIA-002B.0 --- Public Website & Signup
Experience\
**Status:** Complete --- local end-to-end validation and frontend checks
reported passing by the developer. Merge/deployment status is not
updated here.\
**Last updated:** 18 November 2026

## Product direction

Arovia is an AI-powered, composable business operating platform. The
initial real-world validation domain is computer sales and service. The
broader cross-industry direction is an architectural north star, not a
commitment to implement every industry immediately.

Businesses should be able to configure and evolve the capabilities they
need. Favor reusable business capabilities and explicit module
boundaries over one rigid industry-specific application. AI is
important, but core business operations must remain usable without
depending on AI. Evolve incrementally through validated workflows rather
than speculative abstractions.

The pilot business is a design partner and validation environment, not a
process blueprint. Define optimized workflows from first principles,
then validate usability, exceptions, operational fit and outcomes with
the pilot business.

## Development methodology

Build and design proceed together through complete vertical slices:

1.  Define the business capability and desired outcome.
2.  Design the workflow and UX.
3.  Define domain, API, persistence, permissions and integration
    changes.
4.  Implement the complete slice.
5.  Test the implementation and walk through realistic business
    scenarios.
6.  Refine the design and update architecture and handoff documentation.

Use one feature branch per implementation slice. Keep the stable branch
validated and merge only after local validation. Provide complete,
copy-paste-ready file contents with exact repository paths. Update this
handoff after each completed milestone. Do not commit or push unless
explicitly requested.

## Technology stack

-   Java 21 (locally verified with Java 21.0.11)
-   Spring Boot 4.1.2
-   Gradle Wrapper 9.9.0 (`./gradlew`)
-   Next.js App Router, React and TypeScript
-   pnpm workspace (root package manager declaration: pnpm 11.22.0)
-   PostgreSQL with Flyway migrations
-   Spring Session JDBC for server-side sessions
-   Modular monolith backend

The machine also has global Gradle 9.9.1 installed, but the repository
wrapper is pinned to 9.9.0. The wrapper distribution was reused from the
BusinessLab project after the 9.9.1 wrapper download timed out. The
Arovia wrapper runs successfully.

## Current implementation

### Frontend --- public website and workspace

-   Public marketing homepage: `/`.
-   Public registration: `/signup`.
-   Authentication routes: `/login`, `/forgot-password`,
    `/reset-password` and `/verify-email`.
-   Authenticated workspace dashboard: `/app`. The existing dashboard
    was moved from `/` without intentionally changing its
    implementation.
-   `IdentityBoundary` permits the homepage, signup and existing
    authentication routes without authentication. Other workspace routes
    remain protected.
-   Successful login redirects to `/app`; logout returns to `/`.
-   Signup reuses the existing identity registration API and
    CSRF-protected frontend request mechanism.
-   Email verification continues to use the existing verification flow
    and `/verify-email`.
-   Workspace navigation includes Overview, Customers, Sales & Billing,
    Service Desk, Products & Inventory, Purchasing, Finance,
    Communications, AI Workforce, Reports and Settings.
-   The Overview dashboard includes illustrative KPI cards, a
    service-desk preview, recent activity, an attention panel and an AI
    workforce preview.
-   Dashboard figures and activity rows are illustrative sample data,
    not persistent business records. Quick-create, navigation, search
    filtering, notification preview and mobile sidebar interactions are
    local UI interactions.

### Backend --- identity and authentication

-   Spring Boot application runs locally on port `8080`;
    `GET /api/v1/system/health` returns an application health payload.
    Actuator health/info endpoints are configured.
-   PostgreSQL persistence and Flyway migrations establish the identity
    foundation.
-   Registration creates a user identity and stores an Argon2id password
    hash.
-   Email verification uses cryptographically random tokens; only token
    hashes are persisted. Verification messages are delivered through an
    encrypted transactional outbox and SMTP.
-   Resend verification is rate-limited. Verification requires an
    explicit browser action so mail scanners do not consume a token
    merely by opening a link.
-   Login uses Spring Security's authentication manager and DAO
    authentication provider.
-   Authenticated state is stored in server-side Spring Session JDBC
    sessions. The browser receives the `AROVIA_SESSION` cookie with
    HttpOnly and SameSite=Lax settings; Secure is enabled for production
    configuration.
-   Session fixation protection changes the session identifier after
    authentication. An absolute session lifetime filter enforces the
    configured maximum lifetime.
-   CSRF protection uses a session-backed token repository and the
    `X-CSRF-TOKEN` request header. A CSRF bootstrap endpoint is
    available to the frontend.
-   Login attempts are rate-limited using IP address and normalized
    email. Login failures use a generic credentials message.
-   Identity session endpoints support login, current identity (`/me`)
    and logout.
-   Password recovery returns a generic accepted response to avoid
    disclosing whether an account exists. Only active, verified accounts
    are eligible for reset email delivery.
-   Password-reset tokens are cryptographically random; only SHA-256
    hashes are persisted. The raw token is held in the encrypted outbox
    payload. Tokens expire after 30 minutes and can be consumed only
    once.
-   A new reset request revokes the prior active reset token. A
    successful reset updates the Argon2id password hash, consumes the
    reset token, revokes other outstanding reset tokens and invalidates
    the user's existing server-side sessions.
-   Password-reset request and confirmation paths have rate limits.
    Relevant identity and reset events are audited.
-   The email outbox dispatcher handles verification and password-reset
    messages with retry/dead-letter behaviour.
-   Public signup required no new backend endpoint, database migration
    or change to the registration contract.

### Persistence and migrations

-   Flyway migrations V1--V3 establish identity,
    verification/notification and authentication/session/password-reset
    persistence.
-   V3 creates Spring Session JDBC tables (`spring_session`,
    `spring_session_attributes`) and `password_reset_token`.
-   `password_reset_token` stores token hashes and lifecycle timestamps;
    raw reset tokens are not stored in the token table.
-   Spring Session's `principal_name` supports normalized email
    addresses and is indexed for user-session lookup/revocation.
-   No organization/workspace provisioning tables have been introduced.

### Not yet implemented

-   Business organization/workspace provisioning and business profile
    setup.
-   Organization membership lifecycle, invitations and role/permission
    management.
-   Tenant authorization and isolation enforcement for business data.
-   Persistent customer, product, inventory, purchasing, sales, billing,
    finance and service workflows.
-   Official WhatsApp Business Platform integration.
-   AI workers and approval workflows.
-   Production deployment and production SMTP/security configuration
    verification.

## Architecture principles and decisions

-   Keep a modular monolith; do not split deployable services
    prematurely.
-   Platform concerns and business-domain capabilities must have clear
    boundaries.
-   User identity is platform-level. Organization/workspace provisioning
    and tenant membership are separate capabilities; identity
    registration alone does not create an organization.
-   Organization is the canonical tenant boundary; Workspace is its
    user-facing representation. UserAccount remains global and must not
    acquire an `organization_id`.
-   Preserve membership history and a schema capable of representing
    multiple memberships. Initial-release policy: enforce one active
    business membership per user transactionally and with a database
    invariant; one business can have multiple users.
-   Tenant-aware authorization must be enforced before tenant-owned
    business data is introduced.
-   The first real-world domain remains computer sales and service,
    while preserving clean boundaries for later composability and
    industry expansion.
-   A product catalogue item, physical stock unit, customer-owned device
    and repair job are separate concepts.
-   Inventory changes must be represented by traceable stock movements.
-   AI workers call application use cases through controlled tools; they
    do not access the database directly.
-   Consequential actions use backend-enforced permissions, approval
    policies and audit records.
-   Avoid introducing persistent schema until the corresponding workflow
    has been designed.
-   Password recovery must not reveal account existence; reset tokens
    are single-use, time-limited, stored as hashes and delivered through
    the encrypted outbox.
-   Password changes revoke existing sessions.
-   Public signup creates a global user identity only; it does not
    implicitly create an organization or tenant membership.

## 2026-11-18 --- Application / Infrastructure Boundary Refactoring

**Status:** Complete for the refactoring slices listed below. The
developer reported focused tests, the full backend test suite and
`integrationTest` passing after each slice. These were local developer
runs and were not independently re-executed for this handoff.

**Purpose:** Make application use cases depend on application-owned
ports rather than concrete JDBC, token-generation or encryption
implementations. Keep persistence, encryption and transactional behavior
in infrastructure adapters. No API or database migration changes were
intended.

**Completed:** 1. **Registration persistence port** --- introduced
`IdentityRegistrationPort`; registration service depends on the
application contract while the JDBC registration store implements it.
The transaction still coordinates account creation, verification token
persistence, notification outbox insertion and audit recording. 2.
**Token provider port** --- introduced `VerificationTokenProvider`;
registration, email verification, resend and password-reset use cases
depend on the provider. 3. **Email verification persistence ports** ---
introduced `IdentityEmailVerificationPort` and
`IdentityEmailVerificationResendPort`; JDBC stores implement the
contracts. Token locking, account activation, cooldown, token rotation,
notification superseding and audit behavior were retained. 4.
**Password-reset persistence port** --- introduced
`IdentityPasswordResetPort`; JDBC adapter retains reset-token
validation/consumption, password update, outstanding-token revocation,
Spring Session deletion and audit persistence within the transaction. 5.
**Rate-limit persistence port** --- introduced `IdentityRateLimitPort`;
login, registration/resend and password-reset rate limiters depend on
the application contract. JDBC adapter retains atomic bucket updates and
subject hashing. 6. **Notifications application ports** --- introduced
`EmailOutboxPersistencePort` and `OutboxPayloadEncryptionPort`, with
infrastructure adapters for `EmailOutboxStore` and
`OutboxPayloadCipher`. `NotificationOutboxService` depends on these
ports. The mandatory-existing-transaction contract remains; dispatcher
remains infrastructure-owned.

An architecture rule prohibits Notifications application classes from
depending on Notifications infrastructure. Existing module-boundary
rules remain. No Flyway migration, endpoint, request/response contract
or intended data-model change.

**Validation reported:** Focused tests for changed application services,
`./gradlew test` and `./gradlew integrationTest` passed after the
respective slices.

**Files:** New ports under
`apps/api/src/main/java/com/arovia/api/identity/application/port/` and
`apps/api/src/main/java/com/arovia/api/notifications/application/port/`;
JDBC adapters, application services, related tests and
`PlatformArchitectureTest` updated.

## 2026-11-18 --- Final Identity Web-Boundary Cleanup

**Status:** Complete --- focused principal/architecture tests, full
backend test suite and integration tests reported passing.

-   Added application-facing `IdentityAuthenticatedUser`;
    `IdentityUserPrincipal` implements it while remaining the Spring
    Security `UserDetails`/`CredentialsContainer` implementation.
-   Added `IdentityAuthenticationAuditPort`;
    `IdentityAuthenticationAuditStore` implements it.
-   Updated `IdentitySessionController` to use the authenticated-user
    contract and audit port rather than concrete Identity infrastructure
    classes.
-   Added an ArchUnit rule prohibiting Identity web classes from
    depending on Identity infrastructure.

AuthenticationManager flow, session fixation protection, server-side
session persistence, CSRF token rotation, security-context persistence,
logout invalidation and authentication audit semantics were intended to
remain unchanged. No Flyway migration or API contract change was
intended.

**Validation reported:** `IdentityUserPrincipalTest`,
`PlatformArchitectureTest`, `./gradlew test` and
`./gradlew integrationTest` passed.

## Validation status

The following results were reported by the developer. They were run
locally and were not independently re-executed while preparing this
handoff.

  --------------------------------------------------------------------------------------------
  Area                    Command / check                              Result
  ----------------------- -------------------------------------------- -----------------------
  Gradle wrapper          `cd apps/api && ./gradlew --version`         Passed; Gradle 9.9.0,
                                                                       JVM 21.0.11

  Backend unit tests      `cd apps/api && ./gradlew clean test`        Passed

  Backend integration     `cd apps/api && ./gradlew integrationTest`   Passed
  tests                                                                

  Frontend lint (previous `pnpm lint:web`                              Passed
  slices)                                                              

  Frontend typecheck      `pnpm typecheck:web`                         Passed
  (previous slices)                                                    

  Frontend production     `pnpm build:web`                             Passed
  build (previous slices)                                              

  Registration and        Local end-to-end flow                        Passed in AROVIA-002A.1
  verification                                                         

  Login and session       Login, identity retrieval, session           Demonstrated locally
                          persistence and logout                       

  Password recovery       Request, Mailpit delivery, reset,            Demonstrated locally
                          old-password rejection and session           
                          revocation                                   

  Public homepage         `/` loads without authentication             Passed locally

  Public signup           `/signup` loads and submits registration     Passed locally
                          through existing API                         

  Email verification      Verification link and explicit confirmation  Passed locally
  after signup                                                         

  Login redirect          Successful login opens `/app`                Passed locally

  Protected workspace     Logged-out access to `/app` redirects to     Passed locally
                          `/login`                                     

  Logout navigation       Sign-out returns to `/`                      Passed locally

  Frontend lint (current  `pnpm --filter @arovia/web lint`             Passed
  slice)                                                               

  Frontend typecheck      `pnpm --filter @arovia/web typecheck`        Passed
  (current slice)                                                      

  Frontend production     `pnpm --filter @arovia/web build`            Passed
  build (current slice)                                                
  --------------------------------------------------------------------------------------------

Production email delivery, deployment cookie settings, production
secrets and production-origin CORS have not been validated. No
production readiness should be inferred from local validation.

## AROVIA-001 handoff record

### 2026-11-12 --- AROVIA-001 Product & Engineering Foundation

-   **Status:** Complete --- local validation passed; initial foundation
    commit pushed to GitHub.
-   **Branch:** `feature/AROVIA-001-foundation` was pushed first; `main`
    was then created from the same validated commit and pushed. GitHub
    default branch was set to `main` at that time.
-   **Commit:** `83768925455493e1eb01dc7e32db66efd9bf2ae6`.
-   **Scope/files:** Starter monorepo and workspace shell; Spring Boot
    API bootstrap and health endpoint; Gradle Wrapper 9.9.0 files under
    `apps/api/gradle/wrapper/` plus `apps/api/gradlew` and
    `apps/api/gradlew.bat`; ESLint JSX apostrophe correction in
    `apps/web/app/page.tsx`.
-   **Data/migration impact:** None.
-   **Validation:** `./gradlew clean test`, `pnpm lint:web`,
    `pnpm typecheck:web` and `pnpm build:web` passed. API health
    returned `UP`; local frontend dashboard rendered.
-   **Deployment:** Not deployed; local development only.
-   **Decision:** Standardize builds on the Gradle 9.9.0 wrapper and
    retain wrapper files in version control. Continue using one feature
    branch per slice.

## AROVIA-002 milestone records

### AROVIA-002A.1 --- Registration and Email Verification

-   **Status:** Complete --- backend integration and frontend validation
    reported passing.
-   **Scope:** User registration, password hashing, email-verification
    token generation and hashing, encrypted transactional email outbox,
    SMTP delivery, verification confirmation, resend cooldown/rate
    limiting and audit events.
-   **Security:** Raw verification tokens are not persisted in
    plaintext. Verification requires explicit browser action to avoid
    link-prefetch/mail-scanner token consumption. Resend requests are
    rate-limited.
-   **Validation:** Registration/verification integration suite reported
    passing (7 tests, 0 skipped); frontend lint, typecheck and
    production build passed.
-   **Boundary:** Establishes user identity only; does not provision a
    business organization or implement tenant authorization.

### 2026-11-16 --- AROVIA-002A.2 Authentication, Sessions & Account Recovery

-   **Status:** Complete --- backend unit tests, backend integration
    tests, frontend lint/typecheck/build and manual security scenarios
    passed locally.
-   **Branch:** `feature/AROVIA-002A2-authentication-sessions`; no merge
    to `develop`/`main` or deployment is recorded here.
-   **Backend scope:** Security/session configuration and session
    controller; Flyway V3 schema; password-reset payload, cipher/outbox
    dispatch support, rate limiter, exceptions, persistence store,
    service, DTOs and controller under Identity and Notifications
    modules.
-   **Frontend scope:** Identity API helper, shared auth
    components/styles, login, forgot-password and reset-password pages,
    and `apps/web/app/layout.tsx` identity boundary integration.
-   **Endpoints:** `POST /api/v1/identity/sessions`,
    `GET /api/v1/identity/me`,
    `DELETE /api/v1/identity/sessions/current`,
    `GET /api/v1/identity/csrf`, `POST /api/v1/identity/password-resets`
    and `POST /api/v1/identity/password-resets/confirm`.
-   **Security behaviour:** Server-side sessions, CSRF protection,
    session fixation protection, absolute session lifetime, login and
    password-reset rate limits, generic login/recovery responses,
    single-use 30-minute reset tokens, encrypted reset-email outbox
    delivery, password hashing and session invalidation after password
    change.
-   **Validation:** `cd apps/api && ./gradlew clean test`,
    `cd apps/api && ./gradlew integrationTest`, frontend lint, typecheck
    and production build passed.
-   **Manual acceptance:** Forgot-password request, Mailpit delivery,
    reset link, password update, old-password rejection, Browser A
    session revocation after reset in Browser B, new-password login and
    dashboard access demonstrated locally.
-   **Migration impact:** V3 adds Spring Session JDBC tables and
    password-reset token persistence. No organization/workspace tables
    were added.
-   **Follow-ups:** Verify production CORS origins, Secure cookie
    behaviour, SMTP delivery, encryption-key provisioning/rotation and
    session revocation under production configuration.

### 2026-11-18 --- AROVIA-002A.3 Identity & Notifications Modularization

-   **Status:** Complete --- developer reported backend tests and
    frontend lint/typecheck/build passing; changes reported merged into
    `develop`.
-   **Scope:** Identity application ports and frontend identity feature
    architecture were modularized, establishing clearer boundaries
    between application contracts, infrastructure adapters and frontend
    identity feature components.
-   **Data/API impact:** No intended endpoint or database migration
    change.

### 2026-11-18 --- AROVIA-002B.0 Public Website & Signup Experience

**Status:** Complete --- developer confirmed end-to-end manual
validation and all frontend checks passed locally.

**Objective:** Establish a public entry point for Arovia while
preserving the existing identity and authentication implementation.

**Completed changes:** - Added public marketing homepage at `/`. - Added
public registration page at `/signup`. - Added `RegistrationForm` using
the existing identity API and authentication UI conventions. - Extended
the frontend identity API with `registerIdentity`, using the existing
CSRF-protected request mechanism. - Updated `IdentityBoundary` to allow
public homepage and signup access without authentication. - Moved the
existing workspace dashboard from `/` to `/app`, preserving its existing
implementation. - Updated successful login navigation to `/app`. -
Updated logout navigation to `/`. - Retained `/login`,
`/forgot-password`, `/reset-password` and `/verify-email`.

**Backend/API impact:** No backend changes, database migrations or new
registration endpoints. Signup reuses
`POST /api/v1/identity/registrations` and the existing
email-verification flow.

**Manual validation reported:** - Public homepage loads without
authentication. - Signup page loads and registration succeeds. - Email
verification completes successfully. - Verified user can log in
successfully. - Successful login opens `/app`. - Authenticated workspace
dashboard renders correctly. - Logged-out access to `/app` redirects to
`/login`. - Sign-out returns to the public homepage.

**Automated validation reported:** - `pnpm --filter @arovia/web lint`
--- passed. - `pnpm --filter @arovia/web typecheck` --- passed. -
`pnpm --filter @arovia/web build` --- passed.

**Data/migration impact:** None.

**Known limitation:** Workspace dashboard continues to use illustrative
sample data. Live business workflows and persistent business records are
not implemented.

**Deployment:** Local development validation only. No deployment or
merge is implied by this milestone record.

**Conclusion:** Arovia now has a working public-to-authenticated user
journey. The existing identity foundation is reusable for subsequent
organization onboarding.

## Next milestone --- AROVIA-002B Organization / Business Workspace Provisioning

Use `docs/AROVIA-002_IDENTITY_AND_ORGANIZATION_DESIGN.md` as the design
baseline.

The onboarding workflow should allow a verified, active global user to
create an organization. Organization, general organization profile, any
agreed initial business-profile details, initial location when provided,
active owner membership, owner-role assignment and organization-created
audit event must be created atomically. The backend derives the owner
from the authenticated session; it must never accept an arbitrary owner
user ID from the browser.

Organization is the canonical tenant boundary; Workspace is its
user-facing representation. UserAccount remains global and must not
acquire an `organization_id`. Preserve membership history and a schema
capable of representing multiple memberships, while enforcing the
initial-release policy of one active business membership per user
transactionally and with a database invariant. One business can have
multiple users.

Tenant context authorization and cross-tenant enforcement remain the
subsequent AROVIA-002C milestone. Do not introduce tenant-owned
customer, product, inventory, purchasing, sales, billing, finance or
service records until tenant authorization and isolation are enforced.
Billing/subscription provisioning is outside the AROVIA-002B scope.

---------------------------------------------------------------------
## AROVIA-002B — Organization Provisioning, Workspace and Context Foundation

**Status:** Implementation completed and locally validated  
**Branch:** `feature/AROVIA-002B-organization-provisioning`  
**Date:** 19 November 2026

### 1. Objective

Establish the foundation for organizations as Arovia's canonical business tenant boundary, allowing authenticated users to create organizations, manage their organization context and enter a business workspace.

Identity remains global. Organization membership determines access to business resources.

### 2. Backend implementation

Implemented:

- Organization domain model and lifecycle statuses.
- Organization profile and primary location foundations.
- Organization membership and membership-role foundations.
- Support for users belonging to multiple organizations.
- Organization provisioning application service and persistence adapter.
- Organization creation API.
- Organization membership discovery API.
- Active organization selection and clearing APIs.
- Server-side session storage of selected organization.
- Database-backed validation of active membership and organization status.

### 3. Database

Added Flyway migration:

`V4__create_organization_provisioning_foundation.sql`

Tables:

- organization
- organization_profile
- organization_location
- organization_membership
- organization_membership_role
- organization_audit_event

No subsequent migration was required for the organization context foundation.

### 4. Tenant context foundation

Added:

- OrganizationContext
- OrganizationContextResolver
- OrganizationScoped annotation
- OrganizationContextInterceptor
- OrganizationWebConfiguration

The interceptor resolves the selected organization from the authenticated session and validates the user's current active membership before allowing organization-scoped handlers to execute.

The resolved context is attached to the individual HTTP request. It is not stored in a ThreadLocal.

This establishes the request-context foundation. Complete tenant isolation at every business persistence boundary remains part of subsequent validation and business-module implementation.

### 5. Frontend implementation

Implemented:

- Organization API client.
- Organization selector in the authenticated application workspace.
- Display of organization name and membership role.
- Empty-organization state and onboarding navigation.
- Organization onboarding form.
- Business profile and regional preference capture.
- Optional primary business location.
- Automatic selection of the newly created organization.
- Session-backed selection restoration after browser refresh.

The dashboard still contains illustrative sample business metrics. These are not persistent business records.

### 6. Browser integration findings

Resolved a CORS integration issue:

The organization selection endpoint uses PUT, but PUT was missing from the configured allowed CORS methods.

Updated the CORS configuration to allow PUT.

Verified:

- Organization creation succeeds.
- Newly created organization appears in the organization selector.
- Organization selection succeeds.
- OWNER membership role is displayed.
- Browser refresh retains the selected organization.
- Logout destroys the authenticated session and its selected organization.
- After a new login, the user can select the organization again.

Remembering the last selected organization across separate login sessions remains a possible future UX improvement, not part of this implementation.

### 7. Validation

Backend:
- Gradle unit tests passed.
- PostgreSQL integration tests passed.

Frontend:
- ESLint passed.
- TypeScript typecheck passed.
- Next.js production build passed.

Manual browser verification completed for organization creation, selection and refresh persistence.

### 8. Architectural decisions

- User identity remains global.
- Organization is the canonical tenant boundary.
- Users can belong to multiple organizations.
- Membership roles are organization-specific.
- Selected organization is stored in the authenticated server-side session.
- Session selection is not an authorization grant; membership is validated against the database.
- Business modules must consume trusted organization context rather than trusting organization IDs supplied by clients.
- Reusable business capabilities must remain independent of industry-specific compositions.
- No generic multi-tenant framework or unnecessary abstraction is introduced ahead of implementation evidence.

### 9. Next milestone

**AROVIA-003 — Customer Management**

First reusable business capability.

Initial scope:
- Customer creation.
- Customer directory and search.
- Customer details.
- Customer updates.
- Customer archival.
- Organization-scoped persistence and tenant-isolation integration tests.

The Customer Management module will be reusable across industry compositions, including the initial computer sales and service composition.

### 10. Git status

This milestone is ready for a local feature-branch commit after final validation.

No commit, push or merge is implied by this handoff update.
