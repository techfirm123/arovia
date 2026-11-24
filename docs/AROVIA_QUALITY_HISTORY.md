# Arovia — Quality Engineering History

This document records the completed quality-engineering milestones and their validated scope. The current project state belongs in [`AROVIA_HANDOFF.md`](AROVIA_HANDOFF.md); this file is the durable QA history.

## Summary

| Milestone | Focus | PR | Result |
|---|---|---:|---|
| AROVIA-QA-001 | CI foundation | #5 | Merged; backend and frontend jobs passed |
| AROVIA-QA-002 | Shared PostgreSQL integration-test infrastructure | #6 | Merged; 11 existing integration tests retained and validated |
| AROVIA-QA-003 | HTTP security and tenant isolation | #7 | Merged; 8 HTTP-level scenarios passed locally |
| AROVIA-QA-004 | Frontend component testing | #8 | Merged; 12 component tests and frontend checks passed |
| AROVIA-QA-005 | Browser end-to-end testing | #9 | Merged; 5 Playwright tests and all CI jobs passed |

---

## AROVIA-QA-001 — CI workflow foundation

**PR:** [#5](https://github.com/techfirm123/arovia/pull/5)  
**Status:** Merged to `develop`.

Added `.github/workflows/ci.yml`, triggered for pull requests targeting `develop`, with manual `workflow_dispatch` support.

The initial workflow established two jobs:

- **Backend verification:** Java 21, Gradle Wrapper, PostgreSQL 18 service, backend unit/architecture tests and PostgreSQL integration tests; test reports are uploaded as artifacts.
- **Frontend verification:** Node.js 24, pnpm, frozen-lockfile install, lint/architecture checks, TypeScript validation and production build.

The workflow uses read-only repository permissions and concurrency cancellation for obsolete runs. Both initial jobs passed for PR #5.

## AROVIA-QA-002 — Shared PostgreSQL integration-test infrastructure

**PR:** [#6](https://github.com/techfirm123/arovia/pull/6)  
**Status:** Merged to `develop`.

Introduced shared test support at:

`apps/api/src/test/java/com/arovia/api/testing/PostgresIntegrationTestSupport.java`

The support centralizes the `arovia_test` database safety check, database connection configuration, test encryption keys/key versions, rate-limit HMAC key configuration, and disabled scheduling/non-web test properties.

The refactor was applied to:

- `IdentityPersistenceIntegrationTest` — 7 integration tests.
- `OrganizationProvisioningIntegrationTest` — 3 integration tests.
- `EmailOutboxOperationalSnapshotIntegrationTest` — 1 integration test.

The suites retained their test cases, per-suite encryption key versions and cleanup behavior. The developer reported both `./gradlew clean test` and `./gradlew integrationTest` passing after the refactor.

## AROVIA-QA-003 — HTTP-level security and tenant isolation

**PR:** [#7](https://github.com/techfirm123/arovia/pull/7)  
**Status:** Merged to `develop`; all 8 scenarios passed locally.

The tests exercise actual HTTP security boundaries, including authenticated sessions, CSRF, active organization context and cross-organization customer access.

| # | Scenario | Expected result |
|---|---|---|
| 1 | Unauthenticated customer directory request | 401 Unauthorized |
| 2 | Authenticated user without active organization | 409 Conflict |
| 3 | Authenticated user with valid active organization | 200 OK |
| 4 | Customer creation without CSRF token | 403 Forbidden |
| 5 | Customer creation with valid CSRF token | 201 Created |
| 6 | Read another organization's customer | 404; no data disclosure |
| 7 | Update another organization's customer | 404; original data unchanged |
| 8 | Archive another organization's customer | 404; original status remains ACTIVE |

The setup obtains CSRF tokens from the application's CSRF endpoint, authenticates through the real session endpoint, preserves the session cookie, obtains a fresh CSRF token after login, selects the active organization and performs the customer requests through HTTP.

Implementation findings recorded during this work:

- Spring Security 7 uses a masked CSRF token representation; tests should use the token returned by the real endpoint rather than inventing a raw UUID.
- Spring Boot 4.1.2 uses Jackson 3 packages; imports use `tools.jackson.databind` rather than `com.fasterxml.jackson.databind`.
- No production security weakening or removal of the absolute session lifetime filter was required for the test setup.

The eight scenarios do not imply exhaustive security coverage for every module or membership lifecycle.

## AROVIA-QA-004 — Frontend testing foundation

**PR:** [#8](https://github.com/techfirm123/arovia/pull/8)  
**Feature commit:** `9100748857a12a147f06d09792f46c317a901c69`  
**Merge commit:** `0c6dbfb1424e90fb23cdc0e93b375f2a348b99f3`  
**Status:** Merged to `develop`.

Established Vitest, React Testing Library and jsdom for the Customer Management frontend.

12 component tests cover directory loading/display, empty/error states, customer creation dialog and submission behavior, edit form prepopulation and submission, and archive success/cancellation/failure behavior.

Validation reported by the developer:

| Command | Result |
|---|---|
| `pnpm --filter @arovia/web typecheck` | Passed |
| `pnpm --filter @arovia/web test` | Passed — 12 tests |
| `pnpm --filter @arovia/web lint` | Passed — ESLint and architecture checks |
| `pnpm --filter @arovia/web build` | Passed |

The work also corrected the `CustomersWorkspace.loadCustomers` dependency behavior by removing `selected` from the dependency list and using functional `setSelected`. The archive failure test uses the explicit confirmation button selector `name: /^Archive customer$/`.

## AROVIA-QA-005 — End-to-end testing foundation

**PR:** [#9](https://github.com/techfirm123/arovia/pull/9)  
**Feature branch:** `feature/AROVIA-QA-005-e2e-testing`  
**Implementation commit:** `4d6e605b3f6a92c953d43bf173b357041473a4df`  
**Merge commit:** `7b089dc17a91ef3bac7cfc9a057a83a27452cd49`  
**Status:** Merged to `develop`.

### Browser coverage

Five Playwright tests cover the selected critical journeys:

| Spec | Scenario | Validated behavior |
|---|---|---|
| `authentication.spec.ts` | Protected-route redirect | Visiting `/app` unauthenticated redirects to `/login`. |
| `authentication.spec.ts` | Login and workspace | The test user signs in and reaches the authenticated workspace. |
| `authentication.spec.ts` | Logout and route protection | Logout returns to the public homepage; `/app` redirects to login afterward. |
| `organization.spec.ts` | Organization context | Selects an organization and verifies that selection persists after reload. |
| `customer-lifecycle.spec.ts` | Customer lifecycle | Creates a customer, edits it, verifies the update and archives it. |

### Implementation

- `apps/web/playwright.config.ts` configures the Chromium project, sequential execution, one CI worker, up to two CI retries, GitHub and HTML reporters, failure trace/screenshot/video retention, and the Next.js web server.
- `apps/web/e2e/global-setup.ts` registers a run-specific account through the real API, activates it in the isolated test database, logs in through the real API, creates an organization and selects it.
- `apps/web/e2e/support/auth.ts` provides reusable login and organization-selection helpers.
- The three spec files contain the five browser tests listed above.
- `apps/web/package.json` adds Playwright and E2E/UI/report commands; `pnpm-lock.yaml` records the dependency.
- `.github/workflows/ci.yml` adds a PostgreSQL-backed browser E2E job without removing the backend and frontend jobs.

### Test isolation and security

The E2E job provisions a fresh PostgreSQL 18 `arovia_test` service database. The E2E email includes the GitHub run ID and attempt to make each attempt unique. Registration and login use the real API, production password encoder, session handling and CSRF flow.

CI does not send real email. To avoid making browser tests dependent on email delivery, global setup marks the newly registered account verified directly in the disposable E2E database only. This does not weaken production verification behavior. The activation SQL is passed to `psql` over standard input with `-f -`, which allows psql to substitute its `-v` variable before sending SQL to PostgreSQL. The disposable service container is the test-data cleanup boundary.

### CI execution and results

The E2E job installs Java 21, Gradle, Node.js 24, pnpm 11.22.0 and Playwright Chromium with system dependencies; starts the Spring Boot API against the isolated database; waits for `/actuator/health/readiness`; and runs:

`pnpm --filter @arovia/web exec playwright test --workers=1`

Playwright starts the Next.js development server. The workflow uploads the HTML report, test results and API diagnostics as artifacts.

The first successful full feature run was workflow run #9. Its results:

- Backend verification — passed.
- Frontend verification — passed.
- Browser end-to-end tests — passed.
- Playwright summary — `5 passed (13.6s)`.

[Workflow run #9](https://github.com/techfirm123/arovia/actions/runs/45402450701)

After the final handoff update was committed and PR #9 merged, follow-up workflow run #10 also passed all three jobs, including the browser E2E job.

[Workflow run #10](https://github.com/techfirm123/arovia/actions/runs/45412709114)

### Coverage boundaries

The five tests cover only the selected critical journeys, not exhaustive browser coverage for all application modules. Signup and email verification are not browser-driven scenarios in this milestone; account creation uses the real registration API and the email-verification state is activated only in the disposable CI database. Production CORS, secure-cookie behavior, SMTP, production secrets and deployment configuration require separate environment-specific validation.

---

## Quality engineering conventions

- Keep backend unit/architecture tests, PostgreSQL integration tests, frontend component tests and browser E2E tests as distinct layers with clear responsibilities.
- Use disposable, explicitly named test databases for tests that perform database-level setup or verification bypasses.
- Do not weaken production authentication, CSRF, tenant authorization or email-verification behavior to make tests pass.
- Prefer real application HTTP flows for tests intended to validate security boundaries.
- Keep browser tests focused on user-visible critical journeys; cover detailed component states and domain edge cases at lower test layers.
- Upload useful test reports and application diagnostics on CI failures.
- Treat a green CI run as evidence for the checks it actually executes, not as a blanket production-readiness claim.
