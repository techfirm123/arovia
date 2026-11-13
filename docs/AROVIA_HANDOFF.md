
# Arovia — Development Handoff

**Last updated:** 13 November 2026  
**Current milestone:** AROVIA-002A.1 — Global Identity Registration & Email Verification  
**Status:** Core implementation and local end-to-end validation completed; final integration-test hardening and checkpoint commit pending.

---

## 1. Product vision

Arovia is an AI-powered, composable business platform that enables businesses to build, configure, operate and continuously evolve integrated business applications.

The initial pilot focuses on computer sales and service businesses.

The larger architectural vision includes:

1. Arovia Core Platform — global identity, organizations, memberships, permissions and shared infrastructure.
2. Business Modules — customers, suppliers, products, inventory, sales, purchases, finance and service management.
3. Industry Packs — combinations of reusable modules and workflows.
4. Experience Composition — configurable navigation, dashboards, terminology and business experiences.
5. Arovia Intelligence — business-facing and platform-facing AI capabilities.
6. Governed Evolution — AI-assisted configuration and feature development with human approval.
7. BusinessLab Integration — isolated simulation and experimentation capabilities.

This expanded vision is an architectural north star, not a reason to expand the immediate implementation scope.

Continue delivering focused vertical slices and validate real workflows with the initial computer sales and service pilot.

BusinessLab remains a separate product. Production business data belongs to Arovia; simulations must not mutate production data.

---

## 2. Architecture principles

- Global UserAccount is separate from Organization.
- Organization is the canonical tenant and data-isolation boundary.
- OrganizationMembership grants access to an organization.
- Workspace represents the user experience, not a separate tenant.
- OrganizationProfile and BusinessProfile remain separate from identity.
- A user may eventually belong to multiple organizations.
- Tenant access must be resolved from authenticated membership.
- Never trust client-supplied organization identifiers as authorization.
- AI does not access the database directly.
- Consequential actions require backend authorization, auditability and appropriate approval.
- Keep a modular monolith; do not introduce microservices prematurely.
- Preserve architectural boundaries without building speculative generic infrastructure.

---

## 3. Technology stack

### Backend
- Java 21
- Spring Boot 4.1.2
- Gradle Wrapper 9.9.0
- Spring JDBC
- Spring Security Crypto
- Argon2id password hashing
- Bouncy Castle provider
- PostgreSQL 18.6
- Flyway 12.4.0 through Spring Boot dependency management
- Spring Mail / JavaMailSender
- AES-256-GCM for encrypted outbox payloads

### Frontend
- Next.js 16
- React 19
- TypeScript
- pnpm workspace
- Next.js App Router

### Local development
- IntelliJ IDEA
- PostgreSQL
- Mailpit
- macOS
- Java 21

Use the Gradle Wrapper (`./gradlew`) rather than global Gradle.

---

## 4. Repository structure

Repository:

`https://github.com/techfirm123/arovia`

Relevant structure:

```text
arovia/
├── apps/
│   ├── api/
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── java/com/arovia/api/
│   │   │   │   │   ├── identity/
│   │   │   │   │   │   ├── application/
│   │   │   │   │   │   ├── domain/
│   │   │   │   │   │   ├── infrastructure/
│   │   │   │   │   │   └── web/
│   │   │   │   │   ├── notifications/
│   │   │   │   │   │   ├── domain/
│   │   │   │   │   │   └── infrastructure/
│   │   │   │   │   └── system/
│   │   │   │   └── resources/
│   │   │   │       ├── db/migration/
│   │   │   │       └── application.yml
│   │   │   └── test/
│   │   └── build.gradle
│   │
│   └── web/
│       └── app/
│           ├── verify-email/
│           │   ├── page.tsx
│           │   └── verify-email-client.tsx
│           ├── layout.tsx
│           ├── globals.css
│           └── page.tsx
│
├── docs/
│   ├── AROVIA_HANDOFF.md
│   ├── AROVIA_ARCHITECTURE_DECISIONS.md
│   ├── AROVIA-001_DISCOVERY_BASELINE.md
│   └── AROVIA-002_IDENTITY_AND_ORGANIZATION_DESIGN.md
│
├── package.json
└── pnpm-workspace.yaml
```

---

## 5. Git and branch strategy

Default branch:

`main`

Initial foundation commit:

`83768925455493e1eb01dc7e32db66efd9bf2ae6`

Current feature branch:

`feature/AROVIA-002-business-workspace-identity`

The feature branch has been pushed to GitHub.

The previously pushed identity implementation checkpoint contains:
- Global identity registration.
- Email verification.
- Encrypted transactional email outbox.
- Mail delivery dispatcher.
- Verification frontend.
- Identity and organization design documentation.
- Updated architecture decisions.

The subsequent V2 resend and rate-limiting implementation is awaiting its own checkpoint commit and push.

Do not merge into `main` until the complete feature slice is reviewed and validated.

---

## 6. Current milestone: AROVIA-002A.1

### Objective

Implement global user registration, verified email ownership and the reliable email-delivery foundation.

The implementation must remain independent of organization creation and industry-specific business configuration.

### 6.1 Global identity

Implemented UserAccount fields:

- UUID identifier.
- Email.
- Normalized email.
- Password hash.
- Display name.
- Account status.
- Email verification timestamp.
- Created and updated timestamps.
- Optimistic version field.
- Last verification email request timestamp.

Initial account states:

- `PENDING_EMAIL_VERIFICATION`
- `ACTIVE`
- `SUSPENDED`
- `DEACTIVATED`

The user account is global and has no organization_id.

### 6.2 Password protection

Implemented:
- Argon2id password hashing.
- 16-byte salt.
- 32-byte output hash.
- Parallelism: 1.
- Memory: 19,456 KiB.
- Iterations: 2.
- Unicode NFC normalization.
- Minimum 15 code points.
- Maximum 128 code points.
- Initial local password denylist.
- Rejection of passwords containing the email local part.

Password hashing occurs before the registration database transaction.

A comprehensive compromised-password checker is still required before public launch.

### 6.3 Verification tokens

Implemented:
- 256-bit cryptographically random token.
- URL-safe Base64 without padding.
- SHA-256 token hash persisted in PostgreSQL.
- 24-hour initial expiry.
- Single-use verification.
- Token revocation when a replacement is created.
- Atomic account activation and token consumption.

The verification frontend requires an explicit user action. A browser GET request or email security scanner does not automatically consume the token.

---

## 7. Transactional email outbox

### Encrypted payload

Implemented:
- AES-256-GCM.
- 12-byte random nonce.
- 128-bit authentication tag.
- Outbox ID and event type bound as associated data.
- Encryption-key version stored with each message.
- Separate key from the rate-limit HMAC key.

The encrypted payload contains the email recipient, display name and raw verification token.

The token is stored as a hash in the verification-token table.

Raw verification URLs and tokens must never be written to application logs.

### Delivery lifecycle

Implemented statuses:

- `PENDING`
- `PROCESSING`
- `RETRY`
- `SENT`
- `DEAD_LETTER`

The dispatcher:
- Claims records using PostgreSQL row locking and `SKIP LOCKED`.
- Commits the claim before SMTP delivery.
- Sends outside the database transaction.
- Retries transient SMTP failures using exponential backoff and jitter.
- Uses a maximum of eight delivery attempts.
- Moves exhausted messages to dead letter.
- Clears encrypted payload and nonce after successful delivery.

Delivery semantics are at-least-once. Duplicate email delivery is possible if the SMTP provider accepts a message but the application crashes before marking it sent.

Verification tokens remain single-use.

### Local email setup

Mailpit:
- SMTP: `localhost:1025`
- Web inbox: `http://localhost:8025`

The local test email sender is:

`no-reply@arovia.local`

---

## 8. Database migrations

PostgreSQL database:

`arovia`

Application database role:

`arovia_app`

### V1 — Identity registration foundation

File:

`apps/api/src/main/resources/db/migration/V1__create_identity_registration_foundation.sql`

Creates:

1. `user_account`
2. `email_verification_token`
3. `email_outbox`
4. `identity_audit_event`

Important constraints include:
- Unique normalized email.
- Account lifecycle state check.
- Verification token hash uniqueness.
- One active verification token per user.
- Outbox status and payload consistency checks.
- Foreign keys between identity records.
- Audit record ownership.

V1 was applied successfully to PostgreSQL 18.6.

### V2 — Rate limiting and resend cooldown

File:

`apps/api/src/main/resources/db/migration/V2__add_identity_rate_limiting_and_resend_cooldown.sql`

Adds:
- `verification_email_last_requested_at` to UserAccount.
- `identity_rate_limit_bucket`.
- HMAC-based subject identifiers.
- Persistent rate-limit counters.
- Bucket expiry and cleanup index.

V2 was applied successfully.

Observed Flyway history:

| Version | Description | Result |
|---|---|---|
| 1 | create identity registration foundation | Success |
| 2 | add identity rate limiting and resend cooldown | Success |

V1 and V2 must remain immutable after application. Future schema changes require additional migration files.

---

## 9. Implemented API endpoints

### Registration

`POST /api/v1/identity/registrations`

- Validates registration input.
- Normalizes email.
- Applies registration rate limits.
- Validates password policy.
- Hashes password.
- Generates verification token.
- Encrypts verification email payload.
- Creates account, token, outbox record and audit event transactionally.
- Returns a generic HTTP 202 response.

Duplicate registrations return the same generic response.

### Email verification confirmation

`POST /api/v1/identity/email-verification/confirm`

- Accepts a raw verification token.
- Hashes the token.
- Locks the token and user records.
- Validates expiry, revocation and consumption state.
- Activates the account.
- Consumes the token.
- Creates an audit event.
- Commits all changes atomically.

Success returns HTTP 204.

### Email verification resend

`POST /api/v1/identity/email-verification/resend`

- Applies IP and email rate limits.
- Returns the same generic response for unknown, verified, cooldown-blocked and rate-limited addresses.
- Enforces a 60-second account-level resend cooldown.
- Revokes previous unconsumed verification tokens.
- Supersedes pending or retrying old delivery messages.
- Creates a replacement token and encrypted outbox message transactionally.

Success returns HTTP 202.

### System health

`GET /api/v1/system/health`

The initial AROVIA-001 health endpoint remains available.

---

## 10. Rate limiting

Rate-limit subjects are stored as HMAC-SHA-256 digests.

The HMAC key is separate from the outbox encryption key.

Initial configurable limits:

| Operation | IP limit | Email limit |
|---|---:|---:|
| Registration | 10/hour | 3/hour |
| Verification resend | 20/hour | 5/hour |

Registration and resend counters are stored in PostgreSQL.

The implementation uses atomic PostgreSQL upserts, so counters are shared across application instances.

A scheduled cleanup job removes expired buckets in bounded batches.

Manual validation:
- Four registration attempts were made for one email.
- The email rate-limit counter reached four.
- One account was created.
- One email outbox record was created.
- One verification email was received in Mailpit.

The duplicate-account constraint also independently prevents duplicate account and outbox creation. Therefore, this manual scenario confirms counter persistence and duplicate handling, but is not a standalone proof of the rate-limit rejection decision.

---

## 11. Frontend verification experience

Route:

`/verify-email`

Files:
- `apps/web/app/verify-email/page.tsx`
- `apps/web/app/verify-email/verify-email-client.tsx`

Implemented:
- Arovia-branded verification page.
- Explicit Verify Email Address button.
- Verification progress state.
- Successful verification state.
- Invalid/expired token state.
- Failed-request state with retry.
- Removal of token from the visible browser URL.
- Next.js Link navigation.
- React hook-compliant state handling.
- Suspense boundary for `useSearchParams`.

The page calls:

`POST http://localhost:8080/api/v1/identity/email-verification/confirm`

The backend allows the configured frontend origin using explicit CORS configuration.

The existing AROVIA-001 dashboard remains an illustrative preview. It is not yet connected to authenticated workspace data.

---

## 12. Environment configuration

The following environment variables are required locally.

Database:
- `AROVIA_DB_URL`
- `AROVIA_DB_USERNAME`
- `AROVIA_DB_PASSWORD`

Outbox encryption:
- `AROVIA_OUTBOX_ENCRYPTION_KEY`
- `AROVIA_OUTBOX_ENCRYPTION_KEY_VERSION`

Rate-limit privacy:
- `AROVIA_RATE_LIMIT_HMAC_KEY`

Optional configuration:
- `AROVIA_WEB_BASE_URL`
- `AROVIA_WEB_ORIGIN`
- `AROVIA_MAIL_FROM`
- `AROVIA_EMAIL_VERIFICATION_TTL`
- `AROVIA_EMAIL_VERIFICATION_RESEND_COOLDOWN`
- `AROVIA_OUTBOX_POLL_INTERVAL_MS`
- `AROVIA_OUTBOX_BATCH_SIZE`
- `AROVIA_RATE_LIMIT_CLEANUP_INTERVAL_MS`
- `AROVIA_REGISTRATION_RATE_WINDOW`
- `AROVIA_REGISTRATION_IP_LIMIT`
- `AROVIA_REGISTRATION_EMAIL_LIMIT`
- `AROVIA_RESEND_RATE_WINDOW`
- `AROVIA_RESEND_IP_LIMIT`
- `AROVIA_RESEND_EMAIL_LIMIT`

Do not store actual passwords, AES keys or HMAC keys in this document or source control.

Keep encryption and HMAC keys stable across application restarts. Do not generate replacements while records or rate-limit buckets depend on the existing keys.

The local IntelliJ Run Configuration should contain the required environment variables.

---

## 13. Validation results

### Backend

The user reported that:

`cd apps/api && ./gradlew clean test`

passed after the V2 and resend/rate-limit implementation.

Individual test coverage includes:
- Password policy.
- Argon2id hashing.
- Verification-token generation.
- Encrypted outbox payload.
- Ciphertext tampering rejection.
- Associated-data validation.
- Email dispatcher success and retry behavior.
- Unsupported outbox event handling.
- Verification token hashing.
- Rate-limit subject hashing.
- Rate-limit application-service decisions.
- Resend preparation.

### Frontend

The user reported successful execution of:

- `pnpm lint:web`
- `pnpm typecheck:web`
- `pnpm build:web`

React lint issues in the verification page were corrected.

### Manual end-to-end registration

Successfully validated:
1. Registration API returned HTTP 202.
2. UserAccount was created with `PENDING_EMAIL_VERIFICATION`.
3. Password hash was confirmed to use Argon2id.
4. Verification token hash was persisted.
5. Encrypted outbox payload was created.
6. Mailpit received the verification email.
7. The verification page opened successfully.
8. User explicitly confirmed email verification.
9. Account status became `ACTIVE`.
10. Verification token was consumed.
11. Outbox status became `SENT`.
12. Encrypted payload and nonce were cleared.
13. Both registration and verification audit events were recorded.

### Manual resend validation

Successfully validated:
- Original verification token was revoked.
- Replacement token was created.
- Replacement token became the active token.
- Old and replacement verification emails were delivered through Mailpit.
- The 60-second resend cooldown was observed.
- The latest token remained valid for the configured 24-hour period.

### Manual rate-limit validation

Observed:
- Four registration attempts for one email.
- Persisted rate-limit counter reached four.
- Only one account existed for that email.
- Only one outbox email record existed.
- Mailpit showed one delivery for the test registration.

### Remaining integration-test hardening

The following automated database-level scenarios still need dedicated tests:

- PostgreSQL rate-limit upsert boundary behavior.
- Concurrent registration attempts.
- Concurrent resend attempts and cooldown locking.
- Registration transaction rollback.
- Resend transaction rollback.
- Concurrent verification confirmation.
- Outbox claim behavior with multiple workers.
- Recovery from expired processing leases.
- Rate-limit cleanup job behavior.

Do not represent mocked unit tests or manual checks as full PostgreSQL integration tests.

---

## 14. Known limitations and production hardening

Before public exposure:

1. Add a comprehensive compromised-password checker.
2. Complete distributed abuse protection and review limits.
3. Configure trusted proxy handling and edge-level rate limiting.
4. Implement encryption-key rotation and a decryption key ring.
5. Implement HMAC key rotation planning.
6. Add operational monitoring for dead-letter email messages.
7. Review email provider deliverability and sender-domain configuration.
8. Add account verification resend UI.
9. Add full PostgreSQL integration tests.
10. Review security logging and retention policies.
11. Perform deployment-specific security and privacy review.

Current registration and resend endpoints are local-development capabilities and are not yet approved for public exposure.

---

## 15. Not yet implemented

### AROVIA-002A.2
- Login.
- Logout.
- Server-side authentication sessions.
- Session persistence.
- Session fixation protection.
- CSRF protection.
- Current authenticated identity endpoint.
- Password-reset request.
- Password-reset confirmation.
- Session revocation after password change.
- Login abuse protection.

### AROVIA-002B
- Organization persistence.
- OrganizationProfile.
- BusinessProfile foundation.
- OrganizationLocation.
- Initial owner membership.
- Organization provisioning.
- Atomic organization onboarding.

### AROVIA-002C
- Organization context.
- Membership access enforcement.
- Organization roles and permissions.
- Tenant-aware service and repository access.
- Cross-tenant security tests.

No Organization table has been introduced in V1 or V2.

---

## 16. Next implementation slice

**AROVIA-002A.1 — Final integration hardening and closure**

Remaining:
- Add or complete PostgreSQL integration tests.
- Validate rate-limit rejection independently of duplicate-account suppression.
- Review the complete feature diff.
- Update this handoff with final test results.
- Commit and push the completed slice.

After closure, proceed to:

**AROVIA-002A.2 — Authentication, Sessions and Account Recovery**

The next slice must use server-managed sessions and should not introduce JWT complexity without a concrete requirement.

---

## 17. Engineering workflow

For every implementation slice:

1. Work on a dedicated feature branch.
2. Inspect existing implementation before modifying it.
3. Define workflow and domain behavior.
4. Implement complete, copy-ready files.
5. Validate backend and frontend.
6. Test real database and external-service behavior where applicable.
7. Update `AROVIA_HANDOFF.md`.
8. Review `git diff` and `git status`.
9. Commit and push the feature branch.
10. Merge only after review and validation.

`main` remains the stable integration branch.

Never commit secrets, local environment files, generated build output or IDE-specific files.
