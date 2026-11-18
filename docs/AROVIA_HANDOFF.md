# Arovia — Development Handoff

**Current milestone:** AROVIA-002A.2 — Authentication, Sessions & Account Recovery; application/infrastructure boundary hardening complete  
**Status:** Identity and Notifications refactoring slices reported passing by the developer. Earlier authentication and frontend validation remains as recorded below. Merge/deployment status has not been updated.  
**Last updated:** 18 November 2026

## Product direction

Arovia is evolving toward an AI-powered, composable business operating platform. The initial real-world validation domain remains computer sales and service. The broader cross-industry direction is an architectural north star, not a commitment to implement every industry immediately.

Businesses should be able to configure and evolve the capabilities they need. The platform should favor reusable business capabilities and explicit module boundaries over one rigid, industry-specific application. AI is an important platform capability, but core business operations must remain usable without depending on AI. Arovia should evolve incrementally through validated business workflows rather than speculative abstractions.

The pilot business is a design partner and validation environment, not a process blueprint. The product team defines optimized workflows from first principles, then validates usability, exceptions, operational fit and outcomes with the pilot business.

## Development methodology

Build and design proceed hand in hand through complete vertical slices:

1. Define the business capability and desired outcome.
2. Design the workflow and UX.
3. Define domain, API, persistence, permissions and integration changes.
4. Implement the complete slice.
5. Test the implementation and walk through realistic business scenarios.
6. Refine the design and update architecture and handoff documentation.

Use one feature branch per implementation slice. Keep `main` stable and merge only after local validation. Provide complete, copy-paste-ready file contents with exact repository paths for implementation changes. Update this handoff after each completed milestone. Do not commit or push changes unless explicitly requested.

## Technology stack

- Java 21 (locally verified with Java 21.0.11)
- Spring Boot 4.1.2
- Gradle Wrapper 9.9.0 (`./gradlew`); use the wrapper for Arovia builds
- Next.js App Router, React and TypeScript
- pnpm workspace (root package manager declaration: pnpm 11.22.0)
- PostgreSQL with Flyway migrations
- Spring Session JDBC for server-side sessions
- Modular monolith backend

The machine also has global Gradle 9.9.1 installed, but the repository wrapper is pinned to 9.9.0. The wrapper distribution was reused from the BusinessLab project after the 9.9.1 wrapper download timed out. The Arovia wrapper now runs successfully; no Gradle downgrade or project build-file change was required for that workaround.

## Current implementation

### Frontend

- A responsive Arovia workspace shell is running locally at `http://localhost:3000`.
- Navigation includes Overview, Customers, Sales & Billing, Service Desk, Products & Inventory, Purchasing, Finance, Communications, AI Workforce, Reports and Settings.
- The Overview dashboard includes illustrative KPI cards, a service-desk preview, recent activity, an attention panel and an AI workforce preview.
- Dashboard figures and activity rows are illustrative sample data; they are not backed by business records.
- Quick-create, navigation, search filtering, notification preview and mobile sidebar interactions are local UI interactions.
- Authentication pages are implemented: `/login`, `/forgot-password`, `/reset-password` and `/verify-email`.
- `IdentityBoundary` checks the current identity, protects the workspace, redirects unauthenticated users to login and provides sign-out behaviour.
- The frontend identity API helper performs credentialed API calls and obtains/sends the CSRF token for protected requests.
- The login page now presents one password-recovery link: **Forgot your password?**
- The dashboard remains a presentation shell. Its sample figures and activity rows are not persistent business records.

### Backend — identity and authentication

- Spring Boot application bootstrap is running locally on port `8080`.
- `GET /api/v1/system/health` returns an application health payload. Actuator health/info endpoints are configured.
- PostgreSQL persistence and Flyway migrations are in place for the identity foundation.
- Registration creates a user identity and stores a password hash using Argon2id.
- Email verification uses cryptographically random tokens; only token hashes are persisted. Verification messages are delivered through an encrypted transactional outbox and SMTP.
- Resend verification is rate-limited. The email-verification page requires an explicit user action so mail scanners do not consume a verification token merely by opening the link.
- Login uses Spring Security's authentication manager and DAO authentication provider.
- Authenticated state is stored in server-side Spring Session JDBC sessions. The browser receives the `AROVIA_SESSION` cookie with HttpOnly and SameSite=Lax settings; Secure is enabled for production configuration.
- Session fixation protection changes the session identifier after authentication. An absolute session lifetime filter enforces the configured maximum lifetime.
- CSRF protection uses a session-backed token repository and the `X-CSRF-TOKEN` request header. A CSRF bootstrap endpoint is available to the frontend.
- Login attempts are rate-limited using IP address and normalized email. Login failures use a generic credentials message.
- Identity session endpoints are implemented: create session (login), read current identity (`/me`) and delete current session (logout).
- Password recovery request returns a generic accepted response to avoid disclosing whether an account exists. Only active, verified accounts are eligible for reset email delivery.
- Password-reset tokens are cryptographically random; only their SHA-256 hashes are persisted. The raw token is held in the encrypted outbox payload for delivery. Tokens expire after 30 minutes and can be consumed only once.
- A new reset request revokes the prior active reset token. A successful reset updates the Argon2id password hash, consumes the reset token, revokes other outstanding reset tokens and invalidates the user's existing server-side sessions.
- Password-reset request and confirmation paths have rate limits. Relevant identity and reset events are audited.
- The email outbox dispatcher handles both email-verification and password-reset messages while retaining retry/dead-letter behaviour.

### Persistence and migrations

- Flyway migrations V1–V3 establish the current identity, verification/notification and authentication/session/password-reset persistence foundations.
- V3 creates the Spring Session JDBC tables (`spring_session` and `spring_session_attributes`) and `password_reset_token`.
- `password_reset_token` stores token hashes and lifecycle timestamps; the raw reset token is not stored in the token table.
- Spring Session's `principal_name` is sized to support normalized email addresses and indexed for user-session lookup/revocation.
- No business organization/workspace provisioning tables were introduced in the identity slice.

### Not yet implemented

- Business organization/workspace provisioning and business profile setup.
- Organization membership lifecycle, invitations and role/permission management.
- Tenant authorization and isolation enforcement for business data.
- Persistent customer, product, inventory, purchasing, sales, billing, finance and service workflows.
- Official WhatsApp Business Platform integration.
- AI workers and approval workflows.
- Production deployment and production SMTP/security configuration verification.

## Architecture principles and decisions

- Keep a modular monolith; do not split deployable services prematurely.
- Platform concerns and business-domain capabilities must have clear boundaries.
- User identity is platform-level. Organization/workspace provisioning and tenant membership are subsequent capabilities; identity registration alone does not create a business organization.
- Organization/workspace context must become the canonical tenant boundary for business data. Tenant-aware authorization must be enforced before business data is introduced.
- The first real-world business domain remains computer sales and service, while preserving clean boundaries for later composability and industry expansion.
- A product catalogue item, physical stock unit, customer-owned device and repair job are separate concepts.
- Inventory changes must be represented by traceable stock movements.
- AI workers call application use cases through controlled tools; they do not access the database directly.
- Consequential actions use backend-enforced permissions, approval policies and audit records.
- Avoid introducing persistent schema until the corresponding workflow has been designed.
- Password recovery must not reveal account existence; reset tokens are single-use, time-limited, stored as hashes and delivered through the encrypted outbox.
- Password changes revoke existing sessions so previously authenticated browsers must authenticate again.

## 2026-11-18 — Application / Infrastructure Boundary Refactoring

**Status:** Complete for the five refactoring slices listed below. The developer reported that focused tests, the full backend test suite and `integrationTest` passed after each slice. These commands were run locally by the developer and were not independently re-executed while updating this handoff.

**Purpose:** Make application use cases depend on application-owned ports rather than concrete JDBC, token-generation or encryption implementations. Keep persistence, encryption and transactional behavior in infrastructure adapters. This is an internal dependency-direction refactor; no API or database migration changes were intended.

**Completed slices:**

1. **Registration persistence port** — introduced `IdentityRegistrationPort`; `IdentityRegistrationService` depends on the application contract while the JDBC registration store implements it. The registration transaction continues to coordinate account creation, verification token persistence, notification outbox insertion and audit recording.
2. **Token provider port** — introduced `VerificationTokenProvider`; registration, email verification, resend and password-reset use cases depend on the provider rather than the concrete token generator.
3. **Email verification persistence ports** — introduced `IdentityEmailVerificationPort` and `IdentityEmailVerificationResendPort`; the verification and resend JDBC stores implement the contracts. Existing token locking, account activation, cooldown, token rotation, notification superseding and audit behavior were retained.
4. **Password-reset persistence port** — introduced `IdentityPasswordResetPort`; the password-reset application service depends on it. The JDBC adapter retains reset-token validation/consumption, password update, outstanding-token revocation, Spring Session deletion and audit persistence within the transaction.
5. **Rate-limit persistence port** — introduced `IdentityRateLimitPort`; login, registration/resend and password-reset rate limiters depend on the application contract while the JDBC adapter retains atomic bucket updates and subject hashing.
6. **Notifications application ports** — introduced `EmailOutboxPersistencePort` and `OutboxPayloadEncryptionPort`, with infrastructure adapters for `EmailOutboxStore` and `OutboxPayloadCipher`. `NotificationOutboxService` depends on these ports. The mandatory-existing-transaction contract for enqueue/supersede remains. The dispatcher remains infrastructure-owned and was not refactored in this slice.

**Architecture test:** Added a rule prohibiting Notifications application classes from depending on Notifications infrastructure. Existing module-boundary rules remain in place.

**Data/API impact:** No Flyway migration, endpoint, request/response contract or intended data-model change.

**Validation reported by developer:** Focused tests for each changed application service, `./gradlew test`, and `./gradlew integrationTest` all passed after the respective slices. The exact commands and results were confirmed in the development conversation on 18 November 2026.

**Files:** New ports under `apps/api/src/main/java/com/arovia/api/identity/application/port/` and `apps/api/src/main/java/com/arovia/api/notifications/application/port/`; JDBC adapters and application services under their existing module packages; related service tests and `PlatformArchitectureTest` updated.

**Remaining review before AROVIA-002B:** Reinspect the current working tree for any remaining outward dependencies, especially web-layer references to Identity infrastructure and cross-module configuration ownership. The uploaded source archive used during the review predates these refactoring slices, so it is not authoritative evidence of the current post-refactor imports. Do not infer that the remaining items are still present without checking the current branch.

## Validation status

The following results were reported by the developer as passing on 16 November 2026. These commands were run locally by the developer; they were not independently re-executed while preparing this handoff.

| Area | Command / check | Result |
|---|---|---|
| Gradle wrapper | `cd apps/api && ./gradlew --version` | Previously passed; Gradle 9.9.0, JVM 21.0.11 |
| Backend unit tests | `cd apps/api && ./gradlew clean test` | Passed |
| Backend integration tests | `cd apps/api && ./gradlew integrationTest` | Passed |
| Frontend lint | `pnpm lint:web` | Passed |
| Frontend typecheck | `pnpm typecheck:web` | Passed |
| Frontend production build | `pnpm build:web` | Passed |
| Registration and verification | Local end-to-end flow | Previously validated in AROVIA-002A.1 |
| Login and session | Login, identity retrieval, refresh persistence and logout flow | Demonstrated locally |
| Password recovery request | Forgot-password request and generic confirmation | Demonstrated locally |
| Reset email | Mailpit received the reset email with the local reset URL | Demonstrated locally |
| Password reset | Reset page accepted a new password and displayed success confirmation | Demonstrated locally |
| Old password rejection | Login attempt with the previous password returned the generic invalid-credentials message | Passed |
| Session revocation | Browser A was logged out after the password was reset in Browser B | Passed |
| Authenticated workspace | Sign-in with the new password returned to the Arovia dashboard | Demonstrated locally |
| Visual review | Login, forgot-password, reset-password, reset-success and dashboard screens | Reviewed locally |

The reset email is sent from the configured local Mailpit/SMTP environment. Production email delivery, deployment cookie settings, production secrets and production-origin CORS have not been validated by this local slice.

## AROVIA-001 handoff record

### 2026-11-12 — AROVIA-001 Product & Engineering Foundation

- **Status:** Complete — local validation passed; initial foundation commit pushed to GitHub.
- **Branch:** `feature/AROVIA-001-foundation` was pushed first; `main` was then created from the same validated commit and pushed. GitHub default branch is set to `main`.
- **Commit:** `83768925455493e1eb01dc7e32db66efd9bf2ae6` (initial AROVIA-001 foundation commit; shared by the initial `main` baseline and feature branch).
- **Scope/files:** Starter monorepo and workspace shell; Spring Boot API bootstrap and health endpoint; Gradle Wrapper 9.9.0 files under `apps/api/gradle/wrapper/` plus `apps/api/gradlew` and `apps/api/gradlew.bat`; ESLint JSX apostrophe correction in `apps/web/app/page.tsx`.
- **Data or migration impact:** None at this milestone.
- **Validation:** `./gradlew clean test`, `pnpm lint:web`, `pnpm typecheck:web` and `pnpm build:web` passed. The API health endpoint returned `UP`; the local frontend dashboard rendered.
- **Manual acceptance:** Local dashboard visually reviewed. Sample data and placeholder modules are understood to be non-persistent preview content.
- **Deployment:** Not deployed; local development only.
- **Decisions / follow-ups:** Standardize Arovia builds on the Gradle 9.9.0 wrapper. Keep the generated wrapper files in version control. The validated foundation is pushed, and `main` is the GitHub default branch. Continue using one feature branch per implementation slice.
- **Next slice:** AROVIA-002 — Business Workspace & Identity Foundation.

## AROVIA-002 milestone records

### AROVIA-002A.1 — Registration and Email Verification

- **Status:** Complete — backend integration and frontend validation were reported as passing.
- **Scope:** User registration, password hashing, email-verification token generation and hashing, encrypted transactional email outbox, SMTP delivery, verification confirmation, resend cooldown/rate limiting and audit events.
- **Security decisions:** Raw verification tokens are not persisted in plaintext. Email verification requires an explicit action in the browser to avoid link-prefetch/mail-scanner token consumption. Resend requests are rate-limited.
- **Validation:** The developer reported the registration/verification integration suite passed (7 tests, 0 skipped); frontend lint, typecheck and production build passed.
- **Known boundary:** This slice establishes user identity only; it does not provision a business organization or implement tenant authorization.

### 2026-11-16 — AROVIA-002A.2 Authentication, Sessions & Account Recovery

- **Status:** Complete — backend unit tests, backend integration tests, frontend lint/typecheck/build and the manual security scenarios listed above passed locally.
- **Branch:** `feature/AROVIA-002A2-authentication-sessions` is the implementation branch for this slice. No merge to `develop`/`main` or deployment is recorded here.
- **Scope/files:**
  - Backend security/session configuration and session controller in `apps/api/src/main/java/com/arovia/api/identity/infrastructure/IdentityWebSecurityConfiguration.java` and `apps/api/src/main/java/com/arovia/api/identity/web/IdentitySessionController.java`.
  - Flyway V3 schema in `apps/api/src/main/resources/db/migration/V3__create_authentication_sessions_and_password_reset_tokens.sql`.
  - Password-reset payload, cipher/outbox dispatch support, request rate limiter, exceptions, persistence store, service, request/confirmation DTOs, response DTOs and controller under `apps/api/src/main/java/com/arovia/api/identity/` and `apps/api/src/main/java/com/arovia/api/notifications/`.
  - Outbox dispatcher test helper updated for the additional password-reset TTL constructor argument in `apps/api/src/test/java/com/arovia/api/notifications/infrastructure/EmailOutboxDispatcherTest.java`.
  - Frontend identity API helper at `apps/web/lib/identity-api.ts`; shared auth components/styles under `apps/web/components/auth/`; login, forgot-password and reset-password pages under `apps/web/app/`; and `apps/web/app/layout.tsx` updated to apply the identity boundary.
  - `apps/web/components/auth/LoginForm.tsx` uses a single **Forgot your password?** recovery link.
- **Endpoints:** `POST /api/v1/identity/sessions`, `GET /api/v1/identity/me`, `DELETE /api/v1/identity/sessions/current`, `GET /api/v1/identity/csrf`, `POST /api/v1/identity/password-resets` and `POST /api/v1/identity/password-resets/confirm`.
- **Security behaviour:** Server-side sessions, CSRF protection, session fixation protection, absolute session lifetime, login and password-reset rate limits, generic login/recovery responses, single-use 30-minute reset tokens, encrypted reset-email outbox delivery, password hashing and session invalidation after password change.
- **Validation:** `cd apps/api && ./gradlew clean test` — passed; `cd apps/api && ./gradlew integrationTest` — passed; `pnpm lint:web` — passed; `pnpm typecheck:web` — passed; `pnpm build:web` — passed.
- **Manual acceptance:** Forgot-password request, Mailpit delivery, reset link, password update, old-password rejection, Browser A session revocation after reset in Browser B, new-password login and dashboard access were demonstrated locally.
- **Data or migration impact:** V3 adds Spring Session JDBC tables and password-reset token persistence. No organization/workspace tables were added.
- **Deployment:** Local development only. Do not infer production readiness from local validation.
- **Known follow-ups:** Verify production CORS origins, Secure cookie behaviour, SMTP delivery, encryption-key provisioning/rotation and session revocation under the production deployment configuration before release.
- **Next slice:** AROVIA-002B — Organization/Business Workspace Provisioning. Follow with AROVIA-002C — Tenant Authorization & Isolation before introducing tenant-owned business data.

## Next milestone — AROVIA-002B Organization / Business Workspace Provisioning

Design and implement organization/workspace creation as a separate complete vertical slice. Define the onboarding flow, business profile, organization lifecycle, owner membership and the relationship between global user identity and organization membership. Do not treat successful identity registration as automatic organization provisioning unless the approved onboarding workflow explicitly requires it.

After provisioning, implement AROVIA-002C tenant authorization and isolation. The backend must establish and enforce the active organization context and verify membership/permissions on every tenant-scoped operation before customer, product, inventory, purchasing, sales, billing, finance or service data is introduced.

## 2026-11-18 — Final Identity Web-Boundary Cleanup

**Status:** Complete — the developer reported the focused principal/architecture tests, full backend test suite and integration tests passed after this cleanup. Tests were not independently executed during handoff preparation.

**Purpose:** Remove the remaining Identity web-layer dependencies on concrete infrastructure implementations while preserving the existing Spring Security integration and session behavior.

**Completed changes:**
- Added the application-facing `IdentityAuthenticatedUser` contract. `IdentityUserPrincipal` implements the contract while remaining the Spring Security `UserDetails`/`CredentialsContainer` implementation.
- Added `IdentityAuthenticationAuditPort`; `IdentityAuthenticationAuditStore` implements the port.
- Updated `IdentitySessionController` to consume the authenticated-user contract and audit port instead of concrete Identity infrastructure classes.
- Added an ArchUnit rule prohibiting Identity web classes from depending on Identity infrastructure.

**Behavior intended to remain unchanged:** AuthenticationManager flow, session fixation protection, server-side session persistence, CSRF token rotation, security-context persistence, logout invalidation and authentication audit event semantics. No Flyway migration or API contract change was intended.

**Validation reported by developer:** Focused `IdentityUserPrincipalTest` and `PlatformArchitectureTest`, full `./gradlew test`, and `./gradlew integrationTest` all passed.

**Milestone conclusion:** AROVIA-002A identity registration, authentication and recovery foundations are complete, and the application/infrastructure and web/infrastructure dependency-boundary cleanup is reported passing. No merge or deployment status is inferred.

## Next milestone — AROVIA-002B Organization / Business Workspace Provisioning

Proceed with the organization provisioning vertical slice defined in `docs/AROVIA-002_IDENTITY_AND_ORGANIZATION_DESIGN.md`. A verified, active global user must be able to create an organization. Organization, general organization profile, industry-specific business profile (if included in the agreed first setup contract), initial location when provided, active owner membership, owner-role assignment and organization-created audit event must be created atomically. The backend derives the owner from the authenticated session; it must never accept an arbitrary owner user ID from the browser.

The Organization is the canonical tenant boundary; Workspace is its user-facing representation. UserAccount remains global and must not acquire an `organization_id`. Multiple organization memberships must be supported by the schema. Tenant context authorization and cross-tenant enforcement remain the subsequent AROVIA-002C milestone. Billing/subscription provisioning is outside the AROVIA-002B scope described in the design baseline.
