# Arovia — Development Handoff

**Current milestone:** AROVIA-001 — Product & Engineering Foundation  
**Status:** Locally validated; Git commit/merge status not yet confirmed  
**Last updated:** 12 November 2026

## Product direction

Arovia is an AI-powered operating platform focused first on computer sales and service businesses. It is not a generic small-business platform in the initial release. The pilot business is a design partner and validation environment, not a process blueprint.

The product team defines optimized workflows from first principles, then validates usability, exceptions, operational fit and outcomes with the pilot business.

## Development methodology

Build and design proceed hand in hand through complete vertical slices:

1. Define the business capability and desired outcome.
2. Design the workflow and UX.
3. Define domain, API, persistence, permissions and integration changes.
4. Implement the complete slice.
5. Test the implementation and walk through realistic business scenarios.
6. Refine the design and update architecture and handoff documentation.

Use one feature branch per implementation slice. Keep `main` stable and merge only after local validation. Provide complete, copy-paste-ready file contents with exact repository paths for implementation changes. Update this handoff after each completed milestone.

## Initial stack

- Java 21 (locally verified with Java 21.0.11)
- Spring Boot 4.1.2
- Gradle Wrapper 9.9.0 (`./gradlew`); use the wrapper for Arovia builds
- Next.js App Router, React and TypeScript
- pnpm workspace (root package manager declaration: pnpm 11.22.0)
- PostgreSQL and Flyway when the first persisted business capability is introduced
- Modular monolith backend

The machine also has global Gradle 9.9.1 installed, but the repository wrapper is pinned to 9.9.0. The wrapper distribution was reused from the BusinessLab project after the 9.9.1 wrapper download timed out. The Arovia wrapper now runs successfully; no Gradle downgrade or project build-file change was required for that workaround.

## Current implementation

### Frontend

- A responsive Arovia workspace shell is running locally at `http://localhost:3000`.
- Navigation includes Overview, Customers, Sales & Billing, Service Desk, Products & Inventory, Purchasing, Finance, Communications, AI Workforce, Reports and Settings.
- The Overview dashboard includes illustrative KPI cards, a service-desk preview, recent activity, an attention panel and an AI workforce preview.
- Dashboard figures and activity rows are illustrative sample data; they are not backed by business records.
- Quick-create, navigation, search filtering, notification preview and mobile sidebar interactions are local UI interactions.
- The unescaped JSX apostrophe reported by ESLint in `apps/web/app/page.tsx` was corrected. Frontend lint, typecheck and production build subsequently passed.
- No business data is persisted or connected to the API yet.

### Backend

- Spring Boot application bootstrap is running locally on port `8080`.
- `GET /api/v1/system/health` returns an application health payload.
- Actuator health/info endpoints are configured.
- A focused MVC test is included for the health endpoint.
- Manual health verification returned `status: UP` and `application: arovia-api`.
- No database, authentication or business-domain persistence is connected yet.

### Not yet implemented

- PostgreSQL connection and Flyway migrations.
- Authentication and user identity.
- Business workspace persistence and onboarding.
- Tenant-aware authorization enforcement.
- Customer, product, inventory, purchasing, sales, billing, finance and service workflows.
- Official WhatsApp Business Platform integration.
- AI workers and approval workflows.

## Architecture principles

- Computer sales and service is the only initial business vertical.
- Keep a modular monolith; do not split deployable services prematurely.
- Platform concerns and computer-business domain capabilities must have clear boundaries.
- A product catalogue item, physical stock unit, customer-owned device and repair job are separate concepts.
- Inventory changes must be represented by traceable stock movements.
- AI workers call application use cases through controlled tools; they do not access the database directly.
- Consequential actions use backend-enforced permissions, approval policies and audit records.
- Avoid introducing persistent schema until the corresponding workflow has been designed.
- Tenant-aware boundaries must be designed into the foundation rather than added after business data exists.

## Validation status

All checks below were reported as passing on 12 November 2026.

| Area | Command / check | Result |
|---|---|---|
| Gradle wrapper | `cd apps/api && ./gradlew --version` | Passed; Gradle 9.9.0, JVM 21.0.11 |
| Backend tests | `cd apps/api && ./gradlew clean test` | Passed |
| Backend runtime | `cd apps/api && ./gradlew bootRun` | Started successfully |
| Backend health | `curl http://localhost:8080/api/v1/system/health` | Returned `status: UP`, `application: arovia-api` |
| Frontend lint | `pnpm lint:web` | Passed after JSX apostrophe correction |
| Frontend typecheck | `pnpm typecheck:web` | Passed |
| Frontend production build | `pnpm build:web` | Passed |
| Frontend runtime / visual review | Opened `http://localhost:3000` | Dashboard rendered and was reviewed |

The sample dashboard remains a presentation shell. Passing the foundation checks does not mean the business workflows, persistence, authentication or tenant isolation have been implemented.

## AROVIA-001 handoff record

### 2026-11-12 — AROVIA-001 Product & Engineering Foundation

- **Status:** Local validation passed; Git commit, push and merge are not yet confirmed.
- **Branch:** Not recorded; verify the active branch before committing.
- **Commit:** Not recorded.
- **Scope/files:** Starter monorepo and workspace shell; Spring Boot API bootstrap and health endpoint; Gradle Wrapper 9.9.0 files under `apps/api/gradle/wrapper/` plus `apps/api/gradlew` and `apps/api/gradlew.bat`; ESLint JSX apostrophe correction in `apps/web/app/page.tsx`.
- **Data or migration impact:** None. PostgreSQL and Flyway have not been introduced.
- **Validation:** `./gradlew clean test`, `pnpm lint:web`, `pnpm typecheck:web` and `pnpm build:web` passed. The API health endpoint returned `UP`; the local frontend dashboard rendered.
- **Manual acceptance:** Local dashboard visually reviewed. Sample data and placeholder modules are understood to be non-persistent preview content.
- **Deployment:** Not deployed; local development only.
- **Decisions / follow-ups:** Standardize Arovia builds on the Gradle 9.9.0 wrapper. Keep the generated wrapper files in version control. Verify repository status and branch, then commit the validated foundation.
- **Next slice:** AROVIA-002 — Business Workspace & Identity Foundation.

## Next milestone — AROVIA-002 Business Workspace & Identity Foundation

Design and implement this as a complete vertical slice. The initial scope is expected to cover:

- Business registration and creation of a business workspace.
- Owner identity and authentication.
- Business profile and operating details.
- Workspace membership and access boundaries.
- Tenant isolation enforced by the backend.
- PostgreSQL schema and Flyway migrations designed alongside the workflow and domain model.

Do not start by adding generic abstractions for every possible industry. Keep the first release optimized for computer sales and service businesses while preserving clean platform/domain boundaries.
