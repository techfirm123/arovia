
# AROVIA-002 — Identity & Organization Design

**Project:** Arovia  
**Document:** Platform Identity, Organization and Access Foundation  
**Version:** 0.1  
**Date:** 12 November 2026  
**Status:** Design baseline — ready for implementation review  
**Related documents:**
- `docs/AROVIA_ARCHITECTURE_DECISIONS.md`
- `docs/AROVIA-001_DISCOVERY_BASELINE.md`

---

## 1. Purpose

AROVIA-002 establishes the platform identity and organization foundation.

It must allow a person to:

1. Register a global Arovia account.
2. Verify ownership of their email address.
3. Authenticate securely.
4. Create an organization.
5. Become an owner of that organization.
6. Access the organization through an authenticated membership.
7. Eventually belong to and switch between multiple organizations without mixing their data.

This capability is foundational infrastructure. It must not be coupled to the initial computer sales and service industry.

The first pilot organization will use the computer sales and service configuration, but the identity model must remain industry-neutral.

## 2. Design principles

The following principles govern this implementation.

1. **Global identity:** A user has one Arovia identity, independent of any organization.
2. **Organization as tenant:** An organization is the canonical tenant and data-isolation boundary.
3. **Membership-based access:** A user accesses an organization only through an active membership.
4. **Verified identity:** An account must have a verified email address before it can create or activate an organization workspace.
5. **Secure defaults:** Passwords, sessions, verification tokens and account recovery must use established security practices.
6. **Transactional consistency:** Related database changes must commit or roll back together.
7. **Reliable external delivery:** Email delivery must happen outside the business transaction through a durable outbox.
8. **No tenant trust from the browser:** Client-supplied organization identifiers are selectors, never proof of authorization.
9. **Progressive implementation:** Identity, organization provisioning and tenant authorization are separate implementation slices.
10. **Modular monolith:** These capabilities remain modules in the existing Spring Boot application.

## 3. Terminology and domain boundaries

The following terms are authoritative for this implementation.

| Term | Meaning |
|---|---|
| UserAccount | A person's global authenticated identity in Arovia |
| Organization | The tenant and data-isolation boundary |
| OrganizationMembership | A user's relationship with an organization |
| OrganizationProfile | General profile and operating information for an organization |
| BusinessProfile | Industry-specific business information, separate from identity |
| OrganizationLocation | A physical or operational location belonging to an organization |
| Workspace | The user-facing experience for accessing an organization |
| OrganizationRole | A role assigned to an organization membership |
| Permission | A specific operation an organization member is authorized to perform |

### 3.1 Important distinctions

- A UserAccount does not belong to one organization.
- A UserAccount must not contain an `organization_id`.
- An Organization does not represent an authenticated person.
- A Workspace is not a separate tenant or identity entity.
- An OrganizationMembership connects a UserAccount to an Organization.
- A BusinessProfile must not be embedded in authentication or identity records.
- Industry classification must not determine the structure of global identity records.

A user may eventually own or participate in multiple organizations.

## 4. Scope

### 4.1 Included

- Global user registration.
- Email normalization and uniqueness.
- Password storage.
- Email verification.
- Verification resend.
- Secure login and logout.
- Server-side authenticated sessions.
- Password reset foundation.
- Organization creation.
- General organization profile.
- Initial organization location.
- Initial owner membership.
- Organization membership listing.
- Organization context resolution.
- Initial role and permission foundation.
- Tenant isolation enforcement.
- Identity and organization security audit events.
- Durable transactional email outbox.

### 4.2 Explicitly excluded

The following capabilities are not part of this implementation:

- Organization invitations and team onboarding beyond the initial owner.
- Complex custom role management UI.
- Enterprise SSO.
- Social login.
- Multi-factor authentication.
- Billing plans and subscription management.
- Organization/module entitlement management.
- Runtime industry-pack activation.
- Generic module marketplace.
- Custom workflow engine.
- BusinessLab integration.
- Evolution Studio.
- AI-generated code or autonomous production changes.

These capabilities may be designed later when justified by actual requirements.

## 5. Implementation decomposition

AROVIA-002 is divided into independently testable slices.

### AROVIA-002A.1 — Global Identity Registration

Deliver:

- UserAccount persistence.
- Email normalization.
- Password hashing.
- Registration API.
- Verification token creation.
- Email verification API.
- Verification resend API.
- Encrypted transactional email outbox.
- Local email delivery through Mailpit.
- Identity audit events.
- Registration and verification tests.

**Completion condition:** A person can register, receive an email through the configured mail provider and verify their account. The account cannot authenticate as active before verification.

### AROVIA-002A.2 — Authentication and Account Recovery

Deliver:

- Login.
- Logout.
- Current authenticated identity.
- Server-side session management.
- CSRF protection.
- Login rate limiting.
- Password reset request and confirmation.
- Session revocation after password reset.
- Authentication and recovery tests.

**Completion condition:** A verified user can authenticate securely, maintain a server-managed session, log out and recover account access.

### AROVIA-002B — Organization Provisioning

Deliver:

- Organization persistence.
- OrganizationProfile.
- OrganizationLocation.
- Initial organization setup.
- Initial owner membership.
- Initial owner role assignment.
- Organization listing.
- Organization lifecycle foundation.
- Organization creation audit events.

**Completion condition:** A verified authenticated user can create an organization and become its initial owner. The organization, profile, initial location and owner membership are created atomically.

### AROVIA-002C — Access and Tenant Isolation

Deliver:

- Organization membership resolution.
- Organization context selection.
- Membership lifecycle.
- Role and permission enforcement.
- Tenant-scoped application services and repositories.
- Tenant-aware database constraints.
- Cross-tenant access tests.
- Organization access audit events.

**Completion condition:** A user can access only organizations for which they have an active membership. Tenant-owned records cannot be read or modified across organization boundaries.

---

# PART I — GLOBAL IDENTITY

## 6. UserAccount

UserAccount represents one global Arovia user.

It is not owned by an organization.

### 6.1 Proposed attributes

| Attribute | Purpose |
|---|---|
| id | UUID primary key |
| email | Displayable email address |
| normalized_email | Canonical email used for uniqueness and lookup |
| password_hash | Adaptive password hash |
| display_name | User's display name |
| status | Account lifecycle state |
| email_verified_at | Timestamp when email ownership was verified |
| created_at | Creation timestamp |
| updated_at | Last modification timestamp |
| version | Optimistic concurrency version where useful |

Do not store plaintext passwords.

Do not add `organization_id` to this table.

Do not use the email address as the permanent primary identifier. The UUID is the identity key.

### 6.2 Account lifecycle

Initial account states:

- `PENDING_EMAIL_VERIFICATION`
- `ACTIVE`
- `SUSPENDED`
- `DEACTIVATED`

Expected transitions:

`PENDING_EMAIL_VERIFICATION → ACTIVE`

`ACTIVE → SUSPENDED`

`SUSPENDED → ACTIVE`

`ACTIVE → DEACTIVATED`

A suspended or deactivated account cannot authenticate.

A suspended account must not gain access to an organization even if its membership remains active.

An organization membership being suspended must not automatically suspend the user's global identity.

## 7. Email normalization and uniqueness

Registration must normalize email addresses before persistence.

Initial normalization policy:

1. Trim surrounding whitespace.
2. Normalize the address to lowercase for account lookup and uniqueness.
3. Validate the address format.
4. Preserve the supplied display form separately if required.
5. Do not apply provider-specific transformations such as removing dots or plus-addressing suffixes.

The database must enforce uniqueness on `normalized_email`.

Application-level duplicate checks are not sufficient. Database uniqueness must handle concurrent registration attempts.

The normalization rule must be applied consistently to registration, login, verification resend and account recovery.

## 8. Password policy

Use a modern adaptive password-hashing algorithm.

### 8.1 Initial policy

- Use Argon2id.
- Minimum password length: 15 characters for single-factor password authentication.
- Support at least 64 characters.
- Do not impose uppercase/lowercase/number/symbol composition rules.
- Reject commonly used or known-compromised passwords using a suitable blocklist.
- Support password managers, autofill and paste.
- Do not require periodic password changes without evidence of compromise.
- Validate password length safely without logging or exposing the password.

OWASP's Password Storage Cheat Sheet identifies Argon2id as the preferred password hashing option and documents minimum parameter configurations. Its 19 MiB memory, two iterations and one degree of parallelism configuration is a baseline, not a reason to avoid benchmarking the implementation.

NIST SP 800-63B-4 specifies a 15-character minimum for single-factor passwords, recommends allowing at least 64 characters and prohibits additional composition rules.

### 8.2 Hashing implementation

- Use a maintained Argon2id implementation.
- Use a unique cryptographic salt per password.
- Store the encoded password hash, including its algorithm parameters.
- Never store passwords in logs, audit events, exception messages or outbox payloads.
- Benchmark the selected configuration on the target runtime.
- Support rehashing after successful authentication when hashing parameters are upgraded.
- Keep any optional pepper outside the database in managed application secrets.

Do not invent a custom password-hashing algorithm.

## 9. Email verification

Email verification is mandatory before account activation.

It confirms control of the registered email inbox. It is not multi-factor authentication and must not be represented as phishing-resistant authentication.

### 9.1 Verification token

Generate a cryptographically secure random token containing 256 bits of entropy.

Represent it using URL-safe Base64 without padding.

Persist only a SHA-256 hash of the raw token in the verification-token table.

The raw token must not be persisted in plaintext, written to logs or included in application error messages.

### 9.2 Token attributes

Proposed `email_verification_token` fields:

| Attribute | Purpose |
|---|---|
| id | UUID primary key |
| user_account_id | Related user |
| token_hash | SHA-256 hash of the token |
| expires_at | Expiration timestamp |
| consumed_at | Successful consumption timestamp |
| revoked_at | Revocation timestamp |
| created_at | Creation timestamp |

Use a unique constraint on `token_hash`.

Allow only one unconsumed and unrevoked verification token per user at a time. Reissuing a token must revoke earlier active tokens.

An expired token must never activate an account.

### 9.3 Expiration and resend

Initial proposed policy:

- Verification token validity: 24 hours.
- Resend cooldown: 60 seconds per account.
- Additional rate limits by IP and normalized email.
- Resend creates a new token and revokes the previous active token.
- Resend does not change the account's registration timestamp.
- A verified account must not receive a new verification token.

The exact rate limits must be configurable and validated before public deployment.

### 9.4 Verification-link behavior

The email link should open the frontend verification page.

A browser GET request must not consume the token automatically. Email security scanners and link-preview services may prefetch links.

The frontend should display the verification action and submit the token to the backend through an explicit POST request.

This makes token consumption an intentional state-changing operation.

### 9.5 Verification transaction

The backend must perform the following inside one database transaction:

1. Hash the submitted token.
2. Find and lock the matching token record.
3. Verify that the token is not consumed or revoked.
4. Verify that the token has not expired.
5. Lock or update the related UserAccount.
6. Mark the token as consumed.
7. Set `email_verified_at`.
8. Change account status to `ACTIVE`.
9. Commit the transaction.

The token consumption and account activation must be atomic.

Two concurrent confirmation requests must not consume the same token independently.

Invalid, expired, revoked and previously consumed tokens must not activate an account.

---

# PART II — RELIABLE EMAIL DELIVERY

## 10. Transactional outbox

Email delivery is an external side effect.

The application must not hold an identity database transaction open while communicating with an SMTP server or external email provider.

A durable transactional outbox will be used.

### 10.1 Outbox responsibility

The outbox records that an email must be delivered.

The account, verification-token hash and email-delivery request must be committed in the same database transaction.

A separate worker performs delivery after the transaction commits.

### 10.2 Proposed outbox attributes

| Attribute | Purpose |
|---|---|
| id | UUID primary key |
| event_type | Delivery purpose, e.g. `EMAIL_VERIFICATION_REQUESTED` |
| aggregate_type | Related domain type |
| aggregate_id | Related entity ID |
| payload_ciphertext | Encrypted delivery payload |
| payload_nonce | Encryption nonce |
| encryption_key_version | Key version used for encryption |
| status | Delivery lifecycle state |
| attempt_count | Number of delivery attempts |
| next_attempt_at | Next retry timestamp |
| locked_until | Worker lease expiration |
| last_error_code | Sanitized failure classification |
| created_at | Creation timestamp |
| sent_at | Successful delivery timestamp |

The encrypted payload may contain the recipient address, template data and raw token.

### 10.3 Outbox payload encryption

The raw email-verification token is required by the delivery worker to construct the verification link.

It must therefore be temporarily recoverable by an authorized dispatcher without storing it as plaintext.

Use authenticated encryption such as AES-256-GCM for the outbox payload.

- Encryption keys must come from managed application secrets or an appropriate secret store.
- Keys must never be stored in the same database record as the encrypted payload.
- Store a key-version identifier to support rotation.
- Authenticate the expected event/context as associated data where appropriate.
- Never log decrypted payloads or complete verification URLs.
- Restrict decryption to the email-delivery component.
- Clear the encrypted payload after successful delivery when it is no longer required.
- Retain only sanitized delivery metadata for operational traceability.

Local development must use a locally configured secret, not a hard-coded key.

### 10.4 Outbox lifecycle

Initial statuses:

- `PENDING`
- `PROCESSING`
- `RETRY`
- `SENT`
- `DEAD_LETTER`

Expected flow:

`PENDING → PROCESSING → SENT`

On transient failure:

`PROCESSING → RETRY → PROCESSING`

On exhausted attempts or permanent delivery failure:

`PROCESSING → DEAD_LETTER`

A dead-letter record must remain visible for operational investigation and controlled retry.

Do not store raw exception messages containing email content, verification URLs or credentials.

### 10.5 Dispatcher behavior

The dispatcher must:

1. Claim pending or due retry records in a short database transaction.
2. Use a worker lease to prevent concurrent processing of the same row.
3. Decrypt the delivery payload.
4. Send the email outside the claiming transaction.
5. Mark the record as `SENT` after successful provider acceptance.
6. Clear sensitive encrypted payload data after successful delivery.
7. Record sanitized delivery metadata.
8. Schedule retry with exponential backoff and jitter on transient failures.
9. Move permanently failed or exhausted records to `DEAD_LETTER`.

Use row locking and a mechanism such as PostgreSQL `FOR UPDATE SKIP LOCKED` for concurrent workers.

Do not assume exactly-once email delivery is possible. A provider may accept an email while the application loses the response. Retrying can produce a duplicate email.

Duplicate verification emails must therefore remain safe: they should contain the same token when retrying the same outbox event.

A new resend request creates a new token and revokes the previous one.

### 10.6 Local and production delivery

Local development:

- Use Mailpit as the SMTP inbox.
- Do not log verification URLs as a substitute for email delivery.
- Keep SMTP host, port and credentials externalized.

Production:

- Use configurable SMTP or an approved email provider.
- Keep credentials and encryption keys outside source control.
- Support delivery failure monitoring and controlled retries.

---

# PART III — AUTHENTICATION AND ACCOUNT RECOVERY

## 11. Authentication model

Use server-side sessions for browser authentication.

Do not introduce JWT access-token and refresh-token complexity for the initial browser application without a concrete requirement.

### 11.1 Login

A user can authenticate only when:

- The account exists.
- The account status is `ACTIVE`.
- The supplied password is valid.
- Rate-limit and security controls permit the attempt.

Login must not create a valid session for an unverified, suspended or deactivated account.

Failed authentication responses must not reveal whether an arbitrary email address has an account.

### 11.2 Session management

Use server-managed sessions with a persistent session store suitable for the deployment model.

Spring Session JDBC is the initial candidate for the current Spring Boot/PostgreSQL stack.

Session requirements:

- Generate unpredictable session identifiers.
- Rotate the session identifier after successful authentication.
- Protect against session fixation.
- Invalidate sessions on logout.
- Support administrative and security-driven session revocation.
- Revoke affected sessions after password reset.
- Avoid persisting authentication secrets in browser local storage.
- Do not use an unrevocable, long-lived browser authentication token.

Proposed initial session cookie:

| Attribute | Value |
|---|---|
| Name | `AROVIA_SESSION` |
| HttpOnly | `true` |
| Secure | `true` in production |
| SameSite | `Lax` for the preferred same-site deployment |
| Path | `/` |
| Domain | Omitted; use a host-only cookie |

Production frontend and API hosting should preferably use the same site.

If a cross-site deployment is required, explicitly review the cookie policy, credentialed CORS configuration and CSRF protections before enabling it.

Proposed session inactivity and absolute timeouts must be configurable. Initial defaults should be evaluated against the real operating workflow before public release.

### 11.3 CSRF

Cookie-based authentication requires CSRF protection.

- Enable backend CSRF protection for state-changing browser requests.
- Provide the frontend with a supported CSRF-token acquisition mechanism.
- Send the CSRF token on unsafe requests.
- Do not treat `SameSite` cookies as a complete CSRF defense.
- Keep credentialed CORS origins explicit; never use wildcard origins with credentials.

## 12. Login rate limiting

Implement protection against automated credential guessing.

Rate limits should consider:

- Client IP.
- Normalized account identifier.
- Repeated failures over a configurable time window.
- Registration and email-resend abuse.
- Password-reset abuse.

Avoid a permanent account lockout that would allow an attacker to deny service to a legitimate user.

Use progressive throttling and bounded temporary restrictions.

Use edge-level protection in production in addition to application-level controls.

The application-level implementation must work across application instances. A purely in-memory limiter is not sufficient as the production enforcement mechanism.

## 13. Password reset

Account recovery is part of the identity lifecycle.

### 13.1 Password-reset request

The request must return a generic response regardless of whether the email exists.

A password reset must not reveal account existence.

### 13.2 Reset token

Use a separate password-reset token model rather than prematurely creating a generalized token framework.

Proposed policy:

- 256 bits of cryptographic randomness.
- Persist only the token hash.
- 30-minute validity.
- Single-use.
- Revoke older active reset tokens when a new request is accepted.
- Deliver the raw token only through the encrypted transactional outbox.
- Never expose tokens in logs or analytics.

### 13.3 Password reset confirmation

Perform the following atomically:

1. Validate and lock the reset-token record.
2. Check that it is active and unexpired.
3. Validate the new password.
4. Hash the new password.
5. Update the user's password hash.
6. Consume the reset token.
7. Revoke all existing authenticated sessions.
8. Record the security audit event.
9. Commit.

A password reset must not change the user's email-verification state.

---

# PART IV — ORGANIZATION AND MEMBERSHIP

## 14. Organization

Organization is the canonical tenant boundary.

Every organization-owned business record introduced in subsequent slices must be associated with an organization.

### 14.1 Proposed attributes

| Attribute | Purpose |
|---|---|
| id | UUID primary key |
| name | Organization display name |
| slug | Human-readable organization identifier |
| status | Organization lifecycle state |
| created_at | Creation timestamp |
| updated_at | Last modification timestamp |
| version | Optimistic concurrency version where useful |

An organization is not the same entity as its industry-specific business profile.

### 14.2 Organization lifecycle

Initial states:

- `ACTIVE`
- `SUSPENDED`
- `CLOSED`

An organization must not be created until the authenticated user has a verified active UserAccount.

A new organization is provisioned as active only after all required setup records and its initial owner membership have been created successfully.

Suspending an organization prevents normal organization operations without deactivating every member's global account.

## 15. OrganizationProfile

OrganizationProfile contains general organization information.

Proposed attributes:

- organization_id
- display_name
- legal_name, where applicable
- country_code
- default_currency
- timezone
- contact_email, where applicable
- contact_phone, where applicable
- created_at
- updated_at

Do not assume that every organization has the same legal or tax requirements.

India is the provisional initial market. Country, currency, tax and jurisdiction information must remain configurable.

The initial industry classification may be `COMPUTER_SALES_SERVICE`, but it must not be embedded into UserAccount or authentication logic.

## 16. BusinessProfile

BusinessProfile holds industry-specific operational characteristics.

It is distinct from:

- UserAccount
- Organization
- OrganizationProfile
- OrganizationMembership

For the initial computer sales and service pilot, it may eventually hold business-specific configuration such as:

- Business type and operating model.
- Sales and repair capabilities.
- Applicable operational preferences.
- Industry-specific terminology and setup.

Do not create a generic industry configuration engine in AROVIA-002.

Introduce only fields that are required by the first real organization onboarding flow.

## 17. OrganizationLocation

An organization may have multiple operational locations.

The initial pilot is expected to use one primary shop/workshop location, but the model must not assume that an organization can have only one location forever.

Initial attributes:

- id
- organization_id
- name
- location_code
- address fields, where applicable
- timezone, where an override is needed
- is_primary
- status
- created_at
- updated_at

Location-specific inventory, sales and service data will be introduced by later domain slices.

A location must belong to exactly one organization.

## 18. OrganizationMembership

OrganizationMembership connects a global UserAccount to an Organization.

Proposed attributes:

| Attribute | Purpose |
|---|---|
| id | UUID primary key |
| organization_id | Related organization |
| user_account_id | Related user |
| status | Membership lifecycle |
| joined_at | Membership activation timestamp |
| created_at | Creation timestamp |
| updated_at | Modification timestamp |

Enforce uniqueness on `(organization_id, user_account_id)`.

Initial membership states:

- `ACTIVE`
- `SUSPENDED`
- `REMOVED`

Future invitation support may introduce `INVITED`.

A membership is valid only when:

- UserAccount status is `ACTIVE`.
- Organization status is `ACTIVE`.
- Membership status is `ACTIVE`.

Organization membership suspension or removal must not delete the global UserAccount.

## 19. Organization roles and permissions

Authorization must support multiple users and granular responsibilities.

The initial model should separate:

- OrganizationMembership.
- OrganizationRole.
- Permission.
- Membership-to-role assignment.
- Role-to-permission assignment.

A membership may eventually have more than one role.

Initial built-in roles may include:

- `OWNER`
- `ADMIN`
- `SALES`
- `TECHNICIAN`
- `INVENTORY`
- `FINANCE`
- `VIEWER`

The exact initial role set must be validated against the pilot's actual working arrangements.

Permission examples:

- `organization.read`
- `organization.update`
- `organization.members.read`
- `organization.members.manage`
- `customers.read`
- `customers.create`
- `customers.update`
- `inventory.read`
- `inventory.adjust`
- `sales.create`
- `sales.approve`
- `service.manage`
- `finance.read`
- `finance.manage`

The first owner membership must receive the owner role.

Do not use frontend visibility as authorization. Every protected backend operation must enforce the required permission.

Do not allow an organization administrator to grant platform-level privileges.

---

# PART V — ORGANIZATION PROVISIONING

## 20. Initial organization creation workflow

Organization creation occurs after global identity verification and authentication.

Expected workflow:

1. User registers a global Arovia account.
2. Arovia sends a verification email.
3. User verifies the email address.
4. User signs in.
5. User begins organization setup.
6. User provides organization information.
7. Arovia creates the Organization.
8. Arovia creates the OrganizationProfile.
9. Arovia creates the initial OrganizationLocation, where applicable.
10. Arovia creates the initial OrganizationMembership.
11. Arovia assigns the owner role.
12. The transaction commits.
13. The user enters the organization's Workspace experience.

The initial organization setup should be short and progressive.

Do not ask the owner for every future business preference during registration.

### 20.1 Atomic provisioning

The following operations must occur in one database transaction:

- Organization creation.
- OrganizationProfile creation.
- Initial location creation when included in the request.
- Initial owner membership creation.
- Initial owner role assignment.
- Required setup defaults.

If any required operation fails, the entire transaction must roll back.

A partially provisioned active organization must not be exposed to the user.

### 20.2 Multiple organizations

The domain model must allow a user to belong to multiple organizations.

The initial UI may focus on creating and using one organization.

A later interface can provide organization switching without changing global identity.

Do not duplicate the user's global identity to support another organization.

---

# PART VI — API CONTRACTS

## 21. API conventions

Base path:

`/api/v1`

Use JSON request and response bodies.

Use consistent validation and error handling.

Use RFC 9457 Problem Details or a compatible standardized error representation.

Do not expose database exceptions, stack traces, token values or internal cryptographic details in API responses.

Use UUID identifiers.

All API timestamps must use a consistent ISO-8601 representation with timezone information.

## 22. Registration

### Endpoint

`POST /api/v1/identity/registrations`

### Request

```json
{
  "displayName": "Sample User",
  "email": "user@example.com",
  "password": "a-long-user-selected-passphrase"
}
```

### Response

HTTP `202 Accepted`

```json
{
  "message": "If an account can be created for this address, you will receive an email with the next steps."
}
```

The response must not confirm whether the email address already belongs to an account.

The same response shape and status should be returned for duplicate and newly registered addresses.

A unique database constraint must handle concurrent requests.

Registration must not create an Organization.

## 23. Email verification confirmation

### Endpoint

`POST /api/v1/identity/email-verification/confirm`

### Request

```json
{
  "token": "<verification-token>"
}
```

### Success

HTTP `204 No Content`

Invalid or expired tokens must not activate the account.

The backend must consume the token and activate the account atomically.

## 24. Email verification resend

### Endpoint

`POST /api/v1/identity/email-verification/resend`

### Request

```json
{
  "email": "user@example.com"
}
```

### Response

HTTP `202 Accepted`

```json
{
  "message": "If the account requires verification, instructions will be sent."
}
```

Do not reveal whether the address exists or whether it is already verified.

Apply per-account and per-IP throttling.

## 25. Login

### Endpoint

`POST /api/v1/identity/sessions`

### Request

```json
{
  "email": "user@example.com",
  "password": "a-long-user-selected-passphrase"
}
```

### Success

HTTP `200 OK`

Set the server-managed session cookie.

```json
{
  "user": {
    "id": "00000000-0000-0000-0000-000000000001",
    "displayName": "Sample User",
    "email": "user@example.com",
    "emailVerified": true
  }
}
```

The example identifier is illustrative only.

A login attempt must not create an authenticated session for an inactive account.

## 26. Current identity

### Endpoint

`GET /api/v1/identity/me`

### Success

HTTP `200 OK`

Return the authenticated user's global identity and account state.

Do not expose password hashes, authentication secrets or internal security metadata.

Organization membership information should be retrieved through organization APIs rather than embedding tenant-specific data into UserAccount.

## 27. Logout

### Endpoint

`DELETE /api/v1/identity/sessions/current`

### Success

HTTP `204 No Content`

Invalidate the server-side session and clear the session cookie.

The request must satisfy CSRF protection.

## 28. Password recovery

### Request endpoint

`POST /api/v1/identity/password-resets`

Request:

```json
{
  "email": "user@example.com"
}
```

Response: HTTP `202 Accepted` with a generic message.

### Confirmation endpoint

`POST /api/v1/identity/password-resets/confirm`

Request:

```json
{
  "token": "<password-reset-token>",
  "newPassword": "a-new-long-user-selected-passphrase"
}
```

Success: HTTP `204 No Content`.

The token must be validated and consumed atomically with the password update and session revocation.

## 29. Organization APIs

These APIs are introduced in AROVIA-002B.

### Create organization

`POST /api/v1/organizations`

Illustrative request:

```json
{
  "name": "Example Computer Systems",
  "industryCode": "COMPUTER_SALES_SERVICE",
  "countryCode": "IN",
  "defaultCurrency": "INR",
  "timezone": "Asia/Kolkata",
  "primaryLocation": {
    "name": "Main Shop",
    "locationCode": "MAIN"
  }
}
```

Only authenticated active users with verified email addresses may create organizations.

The backend derives the owner identity from the authenticated principal. It must not accept an arbitrary owner user ID from the browser.

Success: HTTP `201 Created`.

Return the organization identifier, general profile and initial membership summary.

### List organizations

`GET /api/v1/organizations`

Return only organizations for which the authenticated user has an active membership.

Do not return organizations based on a supplied user ID.

### Organization details

`GET /api/v1/organizations/{organizationId}`

The backend must verify active membership and organization status before returning details.

## 30. Organization context

Organization-specific APIs must run inside a validated OrganizationContext.

The frontend may send an organization identifier as a context selector.

For example:

`X-Organization-Id: <organization-uuid>`

This identifier is untrusted input.

For every request that uses it, the backend must:

1. Resolve the authenticated UserAccount.
2. Resolve the requested Organization.
3. Confirm that the Organization is active.
4. Confirm that the UserAccount is active.
5. Confirm that the user's OrganizationMembership is active.
6. Resolve the user's effective roles and permissions.
7. Establish the validated OrganizationContext for the request.
8. Execute tenant-scoped application operations.

Do not rely solely on an organization identifier stored in frontend state or a client-provided header.

The context must be validated on every relevant request.

---

# PART VII — TENANT ISOLATION

## 31. Tenant model

Initial deployment:

- One Spring Boot application.
- One PostgreSQL database.
- Shared application infrastructure.
- Shared database with organization-scoped records.

Every tenant-owned business record introduced after AROVIA-002 must contain an organization reference.

Global identity records are not tenant-owned.

Examples of global records:

- UserAccount.
- Password credentials.
- Email verification tokens.
- Platform authentication sessions.
- Global permission definitions, where applicable.

Examples of tenant-owned records:

- OrganizationProfile.
- OrganizationLocation.
- OrganizationMembership.
- Organization roles.
- Customers.
- Products.
- Inventory.
- Sales.
- Service jobs.
- Payments.
- Organization settings.

### 31.1 Database enforcement

Use:

- Organization foreign keys.
- Organization-aware unique constraints.
- Tenant-scoped indexes.
- Composite foreign keys where practical.
- Repository queries scoped to validated organization context.
- Automated cross-tenant access tests.

A unique constraint for a tenant-owned business identifier should normally include `organization_id`.

### 31.2 Service-layer enforcement

Tenant-scoped application services must require a validated OrganizationContext.

Do not allow business services to accept arbitrary organization IDs without authorization checks.

Do not rely on the frontend to filter records.

Do not expose repository access that bypasses tenant scoping.

### 31.3 PostgreSQL Row-Level Security

PostgreSQL RLS may be evaluated as an additional isolation layer later.

Do not introduce RLS without validating:

- Connection-pool reuse.
- Transaction boundaries.
- Tenant context propagation.
- Background workers.
- Scheduled jobs.
- Administrative operations.
- Test behavior.

RLS is not a substitute for correct application authorization.

### 31.4 Required security tests

At minimum:

- User A cannot read User B's private identity data.
- User A cannot access an organization without membership.
- Suspended membership cannot access organization data.
- Removed membership cannot access organization data.
- Suspended organization cannot be used for normal operations.
- A user belonging to Organization A cannot read Organization B's records without membership.
- Cross-organization update and delete attempts are denied.
- Tenant-scoped unique constraints permit the same business identifier in different organizations where appropriate.
- Background processing uses the correct organization context.

---

# PART VIII — AUDIT AND SECURITY EVENTS

## 32. Identity audit

Capture important identity events:

- Registration accepted.
- Email verification requested.
- Email verification completed.
- Verification resend requested.
- Login succeeded.
- Login failed.
- Logout completed.
- Password reset requested.
- Password changed.
- Sessions revoked.
- Account suspended.
- Account reactivated.
- Account deactivated.

Audit records should contain enough metadata for investigation without storing passwords, raw tokens, session identifiers or verification URLs.

## 33. Organization audit

Capture:

- Organization created.
- Organization profile updated.
- Initial location created.
- Owner membership created.
- Member invited, when implemented.
- Membership activated, suspended or removed.
- Role assignment changed.
- Organization suspended or reactivated.

Tenant-owned audit records must themselves be organization-scoped.

Preserve actor, action, target, timestamp and relevant correlation identifiers.

Do not expose security audit metadata to ordinary organization users without an explicit permission.

---

# PART IX — BACKEND MODULE STRUCTURE

## 34. Proposed package boundaries

The implementation remains inside the existing Java/Spring Boot application.

Proposed high-level package structure:

```text
com.arovia.api
├── identity
│   ├── domain
│   ├── application
│   ├── infrastructure
│   └── web
│
├── tenancy
│   ├── domain
│   ├── application
│   ├── infrastructure
│   └── web
│
├── businessprofile
│   ├── domain
│   ├── application
│   ├── infrastructure
│   └── web
│
├── audit
│   ├── domain
│   ├── application
│   └── infrastructure
│
├── notifications
│   ├── application
│   └── infrastructure
│
├── shared
│   ├── error
│   ├── security
│   └── configuration
│
└── system
```

The exact package structure may be refined during implementation.

Do not create empty modules or placeholder abstractions for capabilities that are not yet implemented.

### 34.1 Module responsibilities

**identity**
- UserAccount.
- Password credentials.
- Registration.
- Email verification.
- Authentication.
- Session lifecycle.
- Password recovery.

**tenancy**
- Organization.
- OrganizationMembership.
- OrganizationRole.
- Permission enforcement.
- OrganizationContext.
- Tenant isolation.

**businessprofile**
- OrganizationProfile.
- BusinessProfile.
- OrganizationLocation.
- General and industry-specific organization configuration.

**audit**
- Global identity audit events.
- Organization-scoped audit events.

**notifications**
- Email templates and dispatch interfaces.
- Outbox delivery worker.
- Delivery retry and failure handling.

Synchronous module interactions should use deliberate application interfaces.

Avoid cross-module repository access.

---

# PART X — DATABASE AND MIGRATION STRATEGY

## 35. Migration boundaries

AROVIA-001 does not yet provide business persistence.

The AROVIA-002 migrations must introduce persistence incrementally.

### AROVIA-002A.1

Create only the tables required for:

- UserAccount.
- Email verification tokens.
- Transactional outbox.
- Required identity audit events.

Do not create Organization tables in this migration.

### AROVIA-002A.2

Add persistence required for:

- Server-side sessions.
- Password recovery tokens.
- Authentication security events or supporting throttling state, where selected.

### AROVIA-002B

Create:

- Organization.
- OrganizationProfile.
- OrganizationLocation.
- OrganizationMembership.
- OrganizationRole.
- Role-to-permission assignments.
- Membership-to-role assignments.
- Organization audit support.

### AROVIA-002C

Add or refine:

- Tenant-aware constraints.
- Context-related indexes.
- Permission enforcement support.
- Any additional persistence required by tested authorization scenarios.

All schema changes must be versioned through Flyway.

Do not modify an already-applied migration to introduce a new feature. Add a new migration.

Use database constraints for critical invariants rather than relying exclusively on Java validation.

## 36. Database access strategy

Use PostgreSQL and explicit transaction boundaries.

JDBC is acceptable for persistence where it gives clear control over transactional operations and tenant filtering.

Do not introduce an ORM solely for convenience.

Choose the persistence approach that best fits the actual aggregate and query requirements.

Keep SQL and repository operations inside their owning module.

---

# PART XI — FRONTEND EXPERIENCE

## 37. Identity experience

Initial frontend routes:

- `/register`
- `/verify-email`
- `/login`
- `/forgot-password`
- `/reset-password`

The user journey should be clear:

1. Register.
2. Check email.
3. Verify email.
4. Sign in.
5. Continue to organization setup.

The frontend must not treat client-side state as proof of authentication.

Authenticated identity and session state must come from backend endpoints.

## 38. Organization onboarding experience

Introduced in AROVIA-002B.

Proposed route:

- `/onboarding/organization`

Initial onboarding should collect only essential information:

- Organization name.
- Country.
- Default currency.
- Time zone.
- Initial industry classification.
- Primary location, where applicable.

Do not force the user through advanced tax, invoice, inventory and service configuration during account registration.

Those settings should be introduced when the corresponding operational capability is built.

## 39. Workspace experience

Once the organization has been provisioned, the user enters the organization Workspace experience.

The interface may display:

- Organization name.
- Current organization context.
- Available navigation.
- User identity.
- Organization switching when multiple memberships exist.

The Workspace experience must never be confused with tenant authorization.

The backend remains responsible for validating access.

---

# PART XII — TESTING AND ACCEPTANCE

## 40. Identity registration tests

Verify:

- Valid registration creates one pending UserAccount.
- Password is stored only as an adaptive hash.
- Password is never stored in plaintext.
- Email normalization is consistent.
- Duplicate normalized emails cannot create duplicate users.
- Concurrent registration cannot bypass uniqueness.
- Duplicate registration receives a generic response.
- Invalid passwords are rejected.
- Weak or compromised passwords are rejected when blocklist enforcement is available.
- Registration and outbox creation commit atomically.
- Registration rollback leaves no partial account or outbox event.

## 41. Email verification tests

Verify:

- Token is generated securely.
- Only the token hash is persisted in the verification table.
- Raw token exists only in the encrypted outbox payload while delivery is pending.
- Token is not present in application logs.
- Valid token activates the account.
- Invalid token does not activate the account.
- Expired token does not activate the account.
- Revoked token does not activate the account.
- Consumed token cannot be reused.
- Concurrent confirmation requests cannot consume the token more than once.
- Resend revokes the previous token.
- Resend throttling is enforced.
- Verified accounts do not receive unnecessary verification tokens.
- Activation and token consumption roll back together on failure.

## 42. Outbox tests

Verify:

- Outbox record is committed with the account and token.
- Delivery happens only after commit.
- Dispatcher claims records safely.
- Concurrent workers do not process the same active lease.
- Transient failure schedules retry.
- Permanent or exhausted failure reaches dead-letter state.
- Delivery retry is safe when a previous provider response is ambiguous.
- Successful delivery clears the encrypted payload.
- Encryption-key version is recorded.
- Missing or invalid encryption key fails safely.
- Sensitive email payload and tokens never appear in logs.

## 43. Authentication tests

Verify:

- Verified active user can log in.
- Unverified account cannot create an authenticated session.
- Suspended account cannot log in.
- Deactivated account cannot log in.
- Incorrect password is rejected.
- Failed login response does not reveal account existence.
- Login throttling works.
- Session identifier changes after authentication.
- Logout invalidates the server-side session.
- CSRF protection blocks invalid state-changing requests.
- Current identity endpoint returns only authenticated user's information.
- Password reset invalidates prior sessions.

## 44. Organization tests

Verify:

- Unverified users cannot create organizations.
- Unauthenticated users cannot create organizations.
- Active verified user can create an organization.
- Organization and profile are created atomically.
- Initial location is created when included.
- Initial owner membership is created.
- Initial owner role is assigned.
- Failure rolls back all related records.
- The owner identity is derived from authentication, not request input.
- One user may belong to multiple organizations.
- Duplicate membership cannot be created.

## 45. Tenant isolation tests

Verify:

- Requests without a valid authenticated user are rejected.
- Requests without required organization context are rejected.
- Inactive membership is denied.
- Removed membership is denied.
- Suspended organization access is denied.
- Cross-organization reads are denied.
- Cross-organization writes are denied.
- Permissions are enforced by the backend.
- Organization identifiers supplied by the client are always validated.

Tests must exercise application services and API endpoints, not only isolated helper methods.

---

# PART XIII — IMPLEMENTATION EXIT CRITERIA

## 46. AROVIA-002 completion criteria

AROVIA-002 is considered complete only when all of the following are true:

### Identity
- A global user account can be registered.
- Email verification is mandatory.
- Passwords are securely hashed.
- Account recovery works.
- Authentication and logout work.
- Server-side session management is validated.
- Security-sensitive actions are audited.

### Organization
- A verified user can create an organization.
- Initial organization records are created atomically.
- The creator receives an active owner membership.
- The model supports multiple organizations per user.

### Authorization
- Organization context is validated on every relevant request.
- Membership and role checks are enforced by the backend.
- Tenant-owned data is isolated.
- Cross-tenant tests pass.

### Reliability
- Transactional email delivery works locally.
- Outbox retries work.
- Sensitive tokens are not stored or logged in plaintext.
- Flyway migrations work from a clean database.

### Engineering quality
- Backend tests pass.
- Frontend lint passes.
- Frontend type checking passes.
- Frontend production build passes.
- Manual registration, verification, login and onboarding scenarios are validated.
- `AROVIA_HANDOFF.md` is updated.
- The feature branch is committed and merged into `main` only after validation.

---

# PART XIV — OPEN IMPLEMENTATION DECISIONS

## 47. Decisions to confirm during implementation

The following details should be verified or tuned as implementation begins:

1. Exact Argon2id runtime configuration after benchmarking.
2. Password blocklist source and availability.
3. Email provider configuration and production delivery settings.
4. Outbox encryption-key provisioning and rotation procedure.
5. Retry intervals, maximum attempts and operational alerting.
6. Application-level and edge-level rate limits.
7. Session idle and absolute expiration values.
8. Exact initial organization profile and location fields.
9. Initial owner permissions and pilot staff roles.
10. Production frontend/API domain arrangement and cookie/CORS settings.

Do not invent secrets, keys or production credentials.

Do not hard-code security settings that need environment-specific configuration.

## 48. References

Security decisions should be checked against the authoritative guidance when their implementation is finalized.

- OWASP Password Storage Cheat Sheet:  
  https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html

- NIST SP 800-63B-4 — Authentication and Authenticator Management:  
  https://pages.nist.gov/800-63-4/sp800-63b.html

- NIST SP 800-63B-4 implementation FAQs:  
  https://pages.nist.gov/800-63-4-Implementation-Resources/faqs/

The values and controls in this document form the initial engineering baseline. They do not replace a production security review or deployment-specific threat assessment.

---

## 49. Final design decision

AROVIA-002 establishes a reusable platform identity and organization foundation.

The core relationship is:

`UserAccount → OrganizationMembership → Organization`

The organization is the tenant boundary.

The workspace is the experience presented to an authorized member.

Identity remains global, organization data remains isolated, and business-specific configuration remains separate from platform authentication.

The implementation will proceed through AROVIA-002A.1, AROVIA-002A.2, AROVIA-002B and AROVIA-002C.

The immediate implementation target is **AROVIA-002A.1 — Global Identity Registration and Email Verification**.

No generic industry engine, module marketplace, AI-generated production code or BusinessLab integration is required to complete this foundation.
