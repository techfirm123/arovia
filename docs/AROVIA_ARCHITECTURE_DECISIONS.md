
# Arovia — Architecture Decisions

**Version:** 0.2  
**Date:** 12 November 2026  
**Status:** Accepted architectural direction; future capabilities remain roadmap items

## 1. Product and platform direction

| ID | Decision | Rationale | Status |
|---|---|---|---|
| ADR-001 | Focus the initial product on computer sales and service businesses | Establishes domain depth and a clear first market | Accepted |
| ADR-002 | Design optimized future-state workflows rather than copying existing procedures | Arovia should improve business operations, not merely digitize current habits | Accepted |
| ADR-003 | Design and implementation proceed hand in hand through vertical slices | Shortens feedback loops and avoids speculative upfront design | Accepted |
| ADR-004 | Use a modular monolith for the backend | Keeps deployment and operations simple while preserving domain boundaries | Accepted |
| ADR-005 | Use Java 21 and Spring Boot for the backend | Aligns with the chosen technology direction | Accepted |
| ADR-006 | Use Next.js, React and TypeScript for the frontend | Supports responsive web UX and a cohesive application shell | Accepted |
| ADR-007 | Use PostgreSQL for durable business data | Provides relational integrity for connected business transactions | Accepted |
| ADR-008 | Introduce PostgreSQL and Flyway with the first persisted business capability | Keeps schema design tied to an implemented workflow | Accepted for AROVIA-002 |
| ADR-009 | Design tenant-aware foundations from the beginning | Avoids expensive retrofitting when onboarding additional organizations | Accepted |
| ADR-010 | Keep AI providers pluggable and AI tools application-mediated | Supports local/cloud options and prevents agents bypassing business rules | Accepted |
| ADR-011 | Consequential AI actions require backend-enforced approval | Protects financial, inventory and customer-facing operations | Accepted |
| ADR-012 | Model catalogue products, physical stock units, customer devices and repair jobs separately | Preserves traceability across purchase, sale, ownership and repair lifecycles | Accepted |
| ADR-013 | Keep the initial web experience responsive; evaluate PWA/native apps later | Delivers desktop and mobile access without multiplying clients prematurely | Accepted |

## 2. Arovia Core and organization model

| ID | Decision | Rationale | Status |
|---|---|---|---|
| ADR-014 | Establish Arovia Core as an industry-neutral platform foundation | Identity, organizations, access control and shared infrastructure should not depend on an industry | Accepted |
| ADR-015 | User identity is global to Arovia and independent of organization membership | A person may belong to multiple organizations and use different roles in each | Accepted |
| ADR-016 | Organization is the canonical tenant and data-isolation boundary; Workspace is its user-facing representation | Avoids creating duplicate tenant, organization and workspace entities for the same business | Accepted |
| ADR-017 | Keep organization profile and industry-specific business profile separate from identity | Business details must not become authentication or user-account concerns | Accepted |
| ADR-018 | Support multiple organization memberships and multiple locations in the domain model | Avoids single-owner and single-location assumptions becoming structural limitations | Accepted |
| ADR-019 | Enforce tenant access using authenticated membership and backend authorization | A client-supplied organization ID must never grant access | Accepted |

### Organization and workspace terminology

- **UserAccount:** A person's platform-wide identity.
- **Organization:** The organization that owns business data and represents the tenant boundary.
- **Workspace:** The application context through which an authorized member operates an organization.
- **OrganizationMembership:** The relationship between a user and an organization.
- **OrganizationProfile:** General organization identity and contact information.
- **BusinessProfile:** Industry-specific operational and commercial details.
- **OrganizationLocation:** A physical or operational location belonging to an organization.

An organization may initially represent a small owner-operated business. The model should not assume that every organization is a large company or legal corporation.

Do not create both `Tenant` and `Organization` records for the same isolation boundary.

## 3. Composable business architecture

| ID | Decision | Rationale | Status |
|---|---|---|---|
| ADR-020 | Architecturally distinguish core platform capabilities from business-domain capabilities | Prevents computer-business functionality from leaking into platform identity and organization concerns | Accepted |
| ADR-021 | Keep business modules independently responsible for their domain rules and data ownership | Enables controlled composition and future reuse | Accepted |
| ADR-022 | Industry packs are compositions of supported modules, configuration and workflows | Allows industry specialization without permanently coupling the platform to one industry | Accepted as target direction |
| ADR-023 | Prefer configuration and composition over customer-specific code generation when practical | Reduces maintenance and fragmentation | Accepted as target direction |
| ADR-024 | Introduce module activation, entitlements and runtime composition only when real product requirements justify them | Avoids prematurely building a generic module engine | Deferred |
| ADR-025 | Preserve module boundaries without introducing a separate deployable service for every module | Maintains a practical modular-monolith architecture | Accepted |

### Conceptual platform layers

1. **Arovia Core Platform**
    - Identity and authentication.
    - Organization and membership management.
    - Authorization and audit.
    - Locations and shared infrastructure.
    - Notifications and integrations.

2. **Business Capabilities**
    - Customer and supplier management.
    - Product and inventory management.
    - Purchasing and sales.
    - Billing and operational finance.
    - Service management.
    - Reporting and analytics.

3. **Industry Packs**
    - Computer sales and services.
    - Restaurants and cafes.
    - Automobile services.
    - Wholesale and distribution.
    - Other industries when supported by validated requirements.

4. **Experience and Application Composition**
    - Module selection and activation.
    - Configurable workflows.
    - Personalized navigation and dashboards.
    - Business terminology and preferences.
    - Branding and customer-facing experiences.

These are logical architectural layers, not separate microservices or independently deployed systems by default.

## 4. AI and platform evolution

| ID | Decision | Rationale | Status |
|---|---|---|---|
| ADR-026 | AI is a platform capability, not merely a standalone chatbot | AI should understand business context and assist with real workflows | Accepted |
| ADR-027 | Separate business-facing AI from platform-administration AI through distinct authorization boundaries | Prevents customer business permissions from being confused with platform administration | Accepted |
| ADR-028 | AI workers access business information and operations through controlled application tools | Preserves domain rules, permissions and auditability | Accepted |
| ADR-029 | AI-generated feature and module proposals follow a governed review and approval lifecycle | Customers can influence product evolution without bypassing platform governance | Accepted as target direction |
| ADR-030 | AI-generated code and modules require automated validation and human engineering review before release | Protects production stability and maintainability | Accepted |
| ADR-031 | Classify feature requests as configuration, enhancement/composition or genuinely new capability before proposing new modules | Avoids unnecessary capability proliferation | Accepted as target direction |

Arovia Evolution Studio is a future platform capability. It is not part of the immediate implementation scope.

## 5. BusinessLab relationship

| ID | Decision | Rationale | Status |
|---|---|---|---|
| ADR-032 | BusinessLab remains a distinct product with its own identity and simulation engine | Preserves its ability to serve other applications and business contexts | Accepted as strategic direction |
| ADR-033 | Arovia is the production source of truth for actual business operations | Maintains a clear authority boundary | Accepted |
| ADR-034 | BusinessLab experiments must run in isolated environments and must never mutate production records | Protects real business operations | Accepted |
| ADR-035 | Future integration should use explicit, versioned application-configuration and data contracts | Reduces coupling between independently evolving products | Accepted as target direction |
| ADR-036 | Prefer synthetic or anonymized data for simulation; production snapshots require explicit authorization and appropriate safeguards | Protects business and customer information | Accepted |
| ADR-037 | Simulation outcomes represent estimates and scenarios, not guaranteed predictions | Prevents experimental outputs from being misrepresented as actual business results | Accepted |

BusinessLab integration is deferred until Arovia has sufficiently stable business modules, configuration contracts and representative operational data.

## 6. Data, security and reliability

| ID | Decision | Rationale | Status |
|---|---|---|---|
| ADR-038 | Use a shared PostgreSQL database and shared application deployment initially, with tenant-scoped records | Provides a practical first multi-tenant architecture | Accepted |
| ADR-039 | Resolve organization access from authenticated membership, never arbitrary client-provided tenant context | Prevents cross-tenant data exposure | Accepted |
| ADR-040 | Use tenant-aware constraints, indexes and automated cross-tenant access tests | Protects data integrity and isolation | Accepted |
| ADR-041 | Preserve actor, action, time, reason and relevant source references for consequential changes | Enables traceability and investigation | Accepted |
| ADR-042 | Use durable, retryable handling for important outbound side effects such as verification emails and notifications | Prevents external delivery failures from corrupting or losing committed business state | Accepted |
| ADR-043 | Never execute external network delivery while holding a critical business database transaction open | Avoids long-running transactions and inconsistent external side effects | Accepted |
| ADR-044 | Require email verification before a user can activate an organization workspace | Establishes verified identity before business onboarding | Accepted |

For durable outbound delivery, use a transactional outbox or an equivalent mechanism with explicit retry, idempotency and failure handling. Do not rely exclusively on an in-memory callback after a database commit.

## 7. Technology and implementation principles

- Backend: Java 21 and Spring Boot.
- Frontend: Next.js, React and TypeScript.
- Database: PostgreSQL.
- Migrations: Flyway.
- API: REST with explicit contracts and validation.
- Architecture: Modular monolith.
- Identity: Platform-wide user accounts, organization memberships and server-controlled access.
- AI: Provider gateway supporting local and cloud providers.
- Messaging: Official WhatsApp Business Platform integration.
- Web experience: Responsive desktop/mobile web; evaluate PWA support when justified.

Keep external providers replaceable through deliberate adapters.

Do not introduce Kubernetes, microservices, event streaming infrastructure or a generic workflow engine without an identified operational need.

## 8. Product evolution roadmap

1. Establish Arovia Core and validate the computer sales and service pilot.
2. Introduce supported business configurability and module activation.
3. Introduce contextual Business AI.
4. Introduce governed Arovia Evolution Studio.
5. Integrate BusinessLab through versioned contracts and isolated experiments.
6. Expand industry packs and application composition based on validated demand.

These stages are directional. They do not imply that future capabilities must wait rigidly for every preceding capability if an independently validated requirement justifies parallel work.

## 9. Revisit conditions

- Revisit organization and membership complexity after onboarding additional real organizations.
- Revisit module activation when there is a concrete need to offer different capability combinations.
- Revisit module dependency/version management when multiple industry packs are supported.
- Revisit asynchronous infrastructure when durable workloads require more than the initial outbox and scheduled processing.
- Revisit BusinessLab integration after production business modules and configuration contracts stabilize.
- Revisit deployment topology only when operational scale, reliability or isolation requirements justify it.
- Revisit accounting scope after validating statutory and accountant-led requirements.

## 10. Governing principle

**The expanded Arovia vision is an architectural north star, not permission for uncontrolled scope expansion.**

Build a real, useful product for computer sales and service businesses first. Preserve clean platform boundaries, validate each vertical slice, and allow real requirements to guide the evolution toward a composable, cross-industry business platform.