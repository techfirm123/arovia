# Arovia — Architecture Decisions

**Version:** 0.3  
**Date:** 13 November 2026  
**Status:** Accepted architectural direction; future capabilities remain roadmap items

---

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
| ADR-010 | Keep AI providers pluggable and AI tools application-mediated | Supports local/cloud options and prevents agents bypassing business rules | Accepted, when AI is activated |
| ADR-011 | Consequential AI actions require backend-enforced approval | Protects financial, inventory and customer-facing operations | Accepted, when applicable |
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
   - Shared platform configuration.

2. **Composable Business Modules**
   - Customer and supplier management.
   - Product and inventory management.
   - Purchasing and sales.
   - Billing and operational finance.
   - Service management.
   - Reporting and analytics.
   - AI as an optional, independently activatable module.

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

The AI module is a peer in the business module catalogue. It is not part of the mandatory Core Platform and is not a dependency of other business modules.

## 4. Optional AI module and governed platform evolution

| ID | Decision | Rationale | Status |
|---|---|---|---|
| ADR-026 | AI is an optional, independently activatable module rather than a prerequisite for Arovia Core or operational business modules | Arovia must deliver complete business operations without AI, model providers or AI subscriptions | Accepted |
| ADR-027 | Separate business-facing AI from platform-administration AI through distinct authorization boundaries | Prevents customer business permissions from being confused with platform administration | Accepted as target direction |
| ADR-028 | AI workers access business information and operations through controlled application tools and module interfaces | Preserves domain rules, permissions and auditability | Accepted, when AI is activated |
| ADR-029 | AI-generated feature and module proposals follow a governed review and approval lifecycle | Customers can influence product evolution without bypassing platform governance | Accepted as target direction |
| ADR-030 | AI-generated code and modules require automated validation and human engineering review before release | Protects production stability and maintainability | Accepted |
| ADR-031 | Classify feature requests as configuration, enhancement/composition or genuinely new capability before proposing new modules | Avoids unnecessary capability proliferation | Accepted as target direction |
| ADR-045 | Organizations independently choose whether to activate the AI module, subject to entitlement and authorized administration | AI adoption should be an explicit business choice, not an implicit platform requirement | Accepted as target direction |
| ADR-046 | Business modules must not depend on AI runtime classes, provider SDKs or AI availability | Prevents AI implementation and provider failures from affecting core business operations | Accepted |
| ADR-047 | AI integrates with other modules only through explicit, permission-aware application contracts | Keeps AI separate while allowing useful cross-module assistance | Accepted as target direction |
| ADR-048 | AI-specific dedicated and contextual experiences belong to the AI module | Provides a consistent AI experience without embedding AI business logic in every module | Accepted as target direction |
| ADR-049 | Standard reports, calculations, business rules and deterministic workflows remain available without AI | Keeps essential business intelligence and operational automation independent of model inference | Accepted |
| ADR-050 | AI processing, provider usage and AI-specific background work must not occur for an organization when AI is inactive | Prevents hidden data processing, unintended provider usage and unexpected costs | Accepted |
| ADR-051 | AI failures must be isolated from essential business transactions and workflows | An AI outage or provider timeout must not interrupt sales, payments, inventory or service operations | Accepted |

### AI module boundaries

The AI module may provide:

- Natural-language business assistance.
- Contextual summaries and explanations.
- Recommendations and decision support.
- Drafting and preparation of business actions.
- Approval-bound execution of authorized actions.
- AI-assisted workflows and automations.

The AI module owns AI-specific behavior, not the business records used to answer its questions.

For example:

- Customer records remain owned by the Customer module.
- Stock levels and stock movements remain owned by Inventory.
- Sales transactions remain owned by Sales.
- Repair jobs remain owned by Service.
- AI may retrieve authorized information and request approved operations through those modules.

### AI integration infrastructure

The AI module may use internal infrastructure such as:

- Model-provider adapters.
- Model capability selection.
- Context assembly and retrieval.
- Tool and action orchestration.
- Evaluation and observability.
- Usage tracking and policy enforcement.

These are technical implementation components of the optional AI capability. Their existence must not make AI a runtime prerequisite for the rest of Arovia.

### AI authorization

Every AI request must respect:

1. Whether the organization has activated AI.
2. Whether the organization is entitled to the requested capability.
3. Whether the requesting user is authenticated.
4. Whether that user has permission to access the relevant business data.
5. Whether the relevant business module is enabled and authorizes the operation.
6. Whether the action requires approval.

AI must not use its own elevated identity to bypass the user's permissions.

Consequential actions must be executed through the same backend application services and business rules used by normal application operations.

### Dedicated and contextual AI experiences

The AI module may provide a dedicated AI workspace.

It may also expose contextual entry points inside other modules through explicit extension points or UI integration contracts.

The host module should not embed provider-specific or model-specific logic.

When AI is inactive, AI-specific navigation, actions and processing should not be presented as available. All non-AI functionality remains complete.

### Platform AI and AI-assisted development

Platform-administration AI and AI-assisted module creation remain longer-term capabilities with separate permission boundaries.

Arovia retains responsibility for deciding what becomes part of the platform. AI may assist with requirement analysis, engineering tasks, code generation and validation, but release publication requires the appropriate human and automated controls.

Arovia Evolution Studio is a future platform capability, not part of the immediate implementation scope.

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

BusinessLab must be usable independently of the Arovia AI module. AI may optionally assist with scenario creation or interpretation, but it is not a prerequisite for simulation.

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

AI-related work must follow the same reliability principles. AI provider calls must not be made while holding critical business database transactions open.

## 7. Technology and implementation principles

- Backend: Java 21 and Spring Boot.
- Frontend: Next.js, React and TypeScript.
- Database: PostgreSQL.
- Migrations: Flyway.
- API: REST with explicit contracts and validation.
- Architecture: Modular monolith.
- Identity: Platform-wide user accounts, organization memberships and server-controlled access.
- AI: Optional module using provider adapters for local and cloud providers.
- Messaging: Official WhatsApp Business Platform integration.
- Web experience: Responsive desktop/mobile web; evaluate PWA support when justified.

Keep external providers replaceable through deliberate adapters.

AI provider configuration and dependencies must remain inside the optional AI boundary. The core application must not require AI provider credentials or connectivity.

Do not introduce Kubernetes, microservices, event streaming infrastructure or a generic workflow engine without an identified operational need.

## 8. Product evolution roadmap

The detailed roadmap is maintained in `docs/AROVIA_PRODUCT_ROADMAP.md`.

The strategic sequence is:

1. Establish the engineering and application foundation.
2. Complete identity, organization and secure workspace foundations.
3. Build complete computer sales and service business capabilities that work without AI.
4. Improve operational maturity, reporting and deterministic workflows.
5. Introduce organization-level module composition and application configuration.
6. Offer AI as an optional, independently activatable module.
7. Integrate BusinessLab through versioned contracts and isolated experiments.
8. Expand industry packs and the broader application ecosystem.

AI is intentionally not a prerequisite for any earlier stage.

These stages are directional. They do not imply that future capabilities must wait rigidly for every preceding capability if an independently validated requirement justifies parallel work.

## 9. Revisit conditions

- Revisit organization and membership complexity after onboarding additional real organizations.
- Revisit module activation when there is a concrete need to offer different capability combinations.
- Revisit module dependency/version management when multiple industry packs are supported.
- Revisit asynchronous infrastructure when durable workloads require more than the initial outbox and scheduled processing.
- Revisit BusinessLab integration after production business modules and configuration contracts stabilize.
- Revisit deployment topology only when operational scale, reliability or isolation requirements justify it.
- Revisit accounting scope after validating statutory and accountant-led requirements.
- Revisit AI module capabilities after core business records, permissions and module interfaces are sufficiently reliable.
- Revisit AI provider orchestration and deployment topology only when the optional AI module has a validated use case.

## 10. Governing principle

**Arovia must remain a complete, useful and reliable business operating platform without AI.**

The optional AI module should increase the value of an already useful platform. It must not become a prerequisite for core operations, a hidden dependency inside business modules or a reason that an organization must share data with an AI provider.

The expanded Arovia vision remains composable and cross-industry. The platform should evolve incrementally, preserve module independence and let real business requirements guide implementation.

Build the business platform first. Let organizations choose AI when it adds value to their operations.