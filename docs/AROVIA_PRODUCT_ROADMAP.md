# Arovia — Product Roadmap

**Version:** 1.0  
**Date:** 13 November 2026  
**Status:** Current product direction — strategic stages are directional, not fixed delivery commitments

---

## 1. Product vision

Arovia is a **composable business operating platform** that enables businesses to build, configure and operate integrated applications around their actual business needs.

The initial product will be validated with a computer sales and service business. The long-term platform should support different industries through reusable business modules, industry packs and configurable experiences.

**AI is an optional, independently activatable module. It enhances Arovia, but it is not required to operate Arovia.**

A business must be able to adopt Arovia, manage its operations, complete transactions, access reports and run its day-to-day workflows without activating AI or connecting an AI provider.

The fundamental product principle is:

> Arovia must be a complete and useful business operating application on its own. AI is an additional capability that makes an already useful application more intelligent.

---

## 2. Non-negotiable product principles

### 2.1 AI-independent operations

All essential platform and business operations must function when AI is disabled.

This includes:

- User identity, authentication and organization access.
- Customer and supplier management.
- Product catalogue and inventory operations.
- Purchasing, receiving and stock movements.
- Sales, billing, payments and returns.
- Service and repair operations.
- Standard business reports and dashboards.
- Configured business rules, approvals and notifications.
- Search, filtering, audit history and exports.

These capabilities must not require an LLM, AI provider credentials, an AI runtime or an active AI subscription.

### 2.2 AI is an optional module

AI is a peer in the Arovia module catalogue, alongside Customers, Inventory, Sales, Finance, Service and other capabilities.

An organization can choose whether to activate it, subject to its entitlement and the organization's authorized administrator.

AI activation must not be assumed for every organization.

### 2.3 Business modules remain independent

A business module must own its domain rules, application services and source-of-truth data.

For example:

- Customers owns customer records.
- Inventory owns stock records and stock movement rules.
- Sales owns sales transactions and their lifecycle.
- Finance owns the financial records within its defined scope.
- Service owns repair jobs and service workflows.
- AI owns AI-specific interactions, context orchestration, recommendations and AI execution policies.

The AI module may consume authorized capabilities from other modules. It must not become the owner of their business data or rules.

### 2.4 AI must never become an operational dependency

Disabling AI, losing access to a model provider, exhausting an AI usage allowance or experiencing an AI outage must not prevent normal business operations.

AI requests must be isolated from critical business transactions. A failed AI operation must not leave a sale, payment, stock movement or repair workflow in an inconsistent state.

### 2.5 Standard intelligence does not require AI

Arovia should offer useful reporting, calculations, summaries, validation and deterministic automation without AI.

For example, normal stock-level reports, outstanding-payment reports, sales totals and configured reorder thresholds should work without a model.

AI may interpret this information, find patterns and recommend actions, but it should not be required to calculate or retrieve basic operational truth.

### 2.6 Modularity without premature abstraction

We will maintain clear module boundaries from the beginning.

However, a universal module marketplace, generic workflow engine, dynamic module loader or complex application-composition framework should only be introduced when actual product requirements justify it.

The goal is a maintainable modular monolith, not premature distribution into microservices.

---

## 3. Target product architecture

Arovia evolves through the following logical layers.

### Layer 1 — Arovia Core Platform

The shared foundation required by every organization and module.

Responsibilities include:

- Global identity and authentication.
- Organizations and memberships.
- Authorization and access control.
- Locations and organization settings.
- Module entitlements and activation foundations.
- Notifications and audit.
- Shared integration infrastructure.
- Platform configuration and operational services.

The core platform must not require AI to start, authenticate users or operate business modules.

### Layer 2 — Composable Business Modules

Independently bounded business capabilities.

Initial and potential modules include:

- Customers.
- Suppliers.
- Products and catalogue.
- Inventory.
- Purchasing.
- Sales and billing.
- Finance and expenses.
- Service management.
- Employees.
- Projects.
- Communications.
- Analytics and reporting.
- AI — optional.

Each module should have explicit responsibilities, APIs or application interfaces, permissions, events and data ownership.

The AI module is a peer module, not a mandatory service embedded into the other modules.

### Layer 3 — Industry Packs

Industry packs compose supported modules with relevant terminology, workflows and industry-specific functionality.

Initial pack:

**Computer Sales and Service**
- Customers and suppliers.
- Products and inventory.
- Device and serial-number tracking.
- Purchasing and goods receipt.
- Sales, billing and payments.
- Warranty management.
- Repair and service management.
- Relevant operational reporting.

Potential future packs include restaurants and cafes, automobile services, wholesale and distribution, professional services and other validated business domains.

An industry pack must not require AI. AI may be offered as an optional addition to any compatible pack.

### Layer 4 — Experience and Application Composition

Allows organizations to configure how their activated capabilities appear and work together.

Potential capabilities:

- Module selection and activation.
- Dependency-aware configuration.
- Navigation composition.
- Personalized dashboards.
- Workflow configuration.
- Business-specific terminology.
- Branding and themes.
- Customer-facing portals.
- Custom domains.

Application composition must work independently of AI activation.

### Optional AI Integration Infrastructure

The AI module will use internal technical infrastructure such as:

- Model-provider adapters.
- Model selection and capability handling.
- Context assembly and retrieval.
- Tool and action registry.
- Prompt and task orchestration.
- Evaluation and observability.
- Usage tracking and policy enforcement.

This is implementation infrastructure for the optional AI module. It does not make AI a required layer of the Arovia Core Platform.

---

## 4. AI module — product scope and integration model

### 4.1 What the AI module provides

When an organization activates AI, it may receive capabilities such as:

- Natural-language business assistance.
- Contextual explanations of business records.
- Sales, customer and inventory insights.
- Demand and trend analysis.
- Recommendations and decision support.
- Drafting of messages, reports and business documents.
- Assisted workflow preparation.
- Approval-bound business actions.
- Configurable AI-powered automations.

The AI module should grow progressively. It is not necessary to deliver every capability in its first release.

### 4.2 Where AI appears in the application

AI should have its own dedicated workspace and may also appear contextually inside other modules.

**Dedicated AI workspace**

A place to ask business questions, review insights, inspect recommendations, manage AI tasks and configure AI preferences.

**Contextual entry points**

An authorized user viewing a customer, product, sale, inventory item or service job may request relevant AI assistance from that context.

**Proactive insights**

AI may surface recommendations or exceptions when the organization has enabled the relevant capability and data access.

These are experiences provided by the AI module. Other business modules may expose extension points and authorized interfaces, but should not contain their own AI implementation.

When AI is inactive, AI-specific navigation, controls and processing should be absent or unavailable. Normal module experiences remain complete.

### 4.3 Integration contract

The AI module may interact with other modules through:

- Authorized application interfaces and use cases.
- Explicit read APIs.
- Domain events and approved subscriptions.
- Narrowly scoped tools.
- Versioned integration contracts.

AI must never connect directly to business databases or bypass module-level business rules.

Every AI request must respect:

- Organization activation and entitlement.
- The requesting user's identity and permissions.
- The permissions of the relevant business module.
- Data-sharing and provider policies.
- Applicable approval requirements.

The source module remains authoritative for business facts and mutations.

### 4.4 Controlled action lifecycle

AI should progress from safer assistance to more capable operations:

1. Retrieve authorized business information.
2. Explain and summarize verified facts.
3. Produce recommendations with supporting evidence.
4. Prepare drafts and proposed changes.
5. Request approval for consequential operations.
6. Execute approved actions through normal application services.
7. Verify and audit the result.

Approval must be enforced by the backend. An AI-generated recommendation must not silently become a business transaction.

### 4.5 Provider independence

The AI module should support replaceable local and cloud providers where practical.

An organization should be able to configure permitted providers and data-sharing preferences. No provider should be assumed to exist.

When no provider is configured or available, AI capabilities may be unavailable, but all non-AI Arovia operations continue normally.

### 4.6 Usage and privacy

AI activation, permissions, provider configuration and usage limits should be controlled at organization level.

AI processing must not run invisibly for organizations that have not activated AI. No AI-related usage or charges should arise merely from using ordinary business modules.

---

## 5. Product roadmap

The roadmap is organized by capability maturity and exit criteria rather than calendar estimates.

### Stage 0 — Engineering and application foundation

**Status: Completed**

Establish the project foundation.

Delivered foundations include:

- Repository and monorepo structure.
- Java 21 and Spring Boot backend foundation.
- Next.js, React and TypeScript frontend foundation.
- PostgreSQL-ready application configuration.
- Initial responsive application experience.
- Health endpoint and baseline validation.

The initial dashboard remains a presentation foundation, not a completed operational business application.

### Stage 1 — Identity, organization and secure workspace foundation

**Status: In progress**

Establish the common platform capabilities that every organization will use.

Completed on the current AROVIA-002 feature branch:

- Global user account registration.
- Password hashing and protected credentials.
- Mandatory email verification.
- Encrypted transactional email outbox.
- Verification resend and rate limiting.
- PostgreSQL persistence integration tests.

Next planned capabilities:

- Secure login and logout.
- Server-side session management.
- CSRF protection and session lifecycle handling.
- Authenticated identity endpoint.
- Password recovery and session revocation.
- Organization creation and activation.
- Organization membership and ownership.
- Secure workspace onboarding and access.

**Exit criteria**

A verified user can authenticate, establish or join an organization, access the correct workspace and be prevented from accessing organizations for which they have no membership.

These capabilities must work without AI.

### Stage 2 — First operational business platform: Computer Sales and Service

**Status: Planned**

Build the real operational application for the pilot business through complete vertical slices.

Potential capability sequence:

1. Customer management.
2. Supplier management.
3. Product catalogue and classification.
4. Inventory foundation and stock movement ledger.
5. Serial-number and device tracking.
6. Purchasing and goods receipt.
7. Quotations, sales and billing.
8. Payments, receivables and returns.
9. Warranty and customer device history.
10. Repair intake, diagnosis, estimates and work orders.
11. Parts consumption, service completion and delivery.
12. Operational finance and business summaries.
13. Customer communications and notifications.

The exact implementation sequence should be guided by domain dependencies and validated pilot value.

**Non-negotiable exit criteria**

The business must be able to execute its essential workflows with AI disabled and without any model provider configured.

The product must maintain accurate business records, transaction history, permissions and auditability.

### Stage 3 — Operational maturity and dependable business intelligence

**Status: Planned**

Make the initial business application reliable and useful for daily operations.

Potential capabilities:

- Role and permission refinement.
- Multi-location support where required.
- Standard operational dashboards and reports.
- Search and filtering across authorized records.
- Deterministic business rules.
- Configured reminders and notifications.
- Approval workflows for defined business operations.
- Data export and reconciliation.
- Backup and recovery validation.
- Operational monitoring and audit improvements.

This stage includes useful business intelligence that does not depend on AI.

**Exit criteria**

An owner and authorized staff can run daily operations, understand business performance and handle common exceptions without an AI assistant.

### Stage 4 — Composable module and application foundation

**Status: Planned**

Evolve the existing modular architecture into an organization-configurable platform when enough real modules and workflows exist.

Potential capabilities:

- Module catalogue and metadata.
- Organization-level module activation.
- Module dependencies and compatibility.
- Module entitlements.
- Module-specific settings.
- Navigation and dashboard composition.
- Configurable workflows and extension points.
- Versioned module contracts.

The first computer sales and service experience will become an industry pack composed from validated capabilities.

Do not build a generic module engine before there are sufficient real modules to validate its requirements.

**Exit criteria**

Arovia can represent an organization's selected capabilities and compose a coherent business experience without maintaining separate custom applications for each organization.

AI remains optional and is not a prerequisite for module activation or composition.

### Stage 5 — Optional Arovia AI module

**Status: Future; intentionally not a prerequisite for Stages 1–4**

Introduce AI as an independently activatable module once the underlying business records, permissions and application interfaces are sufficiently reliable.

Suggested implementation progression:

**5A. AI module foundation**
- Organization-level activation and entitlements.
- Provider configuration.
- Usage and cost controls.
- AI-specific permissions and audit.
- Context and tool orchestration boundaries.
- Data-sharing and privacy controls.

**5B. Read-only business assistance**
- Natural-language questions.
- Authorized customer, sales, stock and service queries.
- Grounded summaries.
- Evidence links to source business records.

**5C. Recommendations and drafting**
- Inventory and sales recommendations.
- Customer and service follow-up suggestions.
- Draft communications and documents.
- Explanations of trends and operational exceptions.

**5D. Approval-bound actions**
- Prepare business changes.
- Present exact proposed actions.
- Obtain required user approval.
- Execute through authorized module services.
- Verify and audit execution.

**5E. Proactive assistance and automation**
- Scheduled insights.
- Event-triggered analysis.
- Configurable AI workflows.
- Carefully governed automation policies.

**Exit criteria**

An organization can activate AI and gain additional capabilities without changing the correctness or availability of its existing non-AI workflows.

An organization that never activates AI must still receive the complete Arovia product defined by its selected business modules.

### Stage 6 — BusinessLab integration

**Status: Long-term strategic direction**

Keep BusinessLab as a distinct product with its own simulation engine and identity.

Potential integration capabilities:

- Versioned Arovia application-configuration contracts.
- Authorized and controlled data snapshots.
- Synthetic or anonymized business data.
- Isolated simulation environments.
- Scenario and workflow experimentation.
- Comparison of simulated outcomes.
- Safe evaluation of proposed configurations or modules.

Arovia remains the source of truth for production operations. BusinessLab must never directly mutate production records.

AI may optionally help formulate scenarios or interpret results, but BusinessLab integration must not require the Arovia AI module.

### Stage 7 — Cross-industry expansion and platform ecosystem

**Status: Long-term strategic direction**

Expand based on validated demand and reusable domain capabilities.

Potential capabilities:

- Additional industry packs.
- Expanded module catalogue.
- More sophisticated application composition.
- Customer-facing portals and custom branding.
- Reusable module distribution and versioning.
- Governed customer feature requests.
- Arovia Evolution Studio.
- AI-assisted engineering and module creation with human review.
- Broader BusinessLab integrations.

New industry support must not be achieved by making AI a mandatory dependency.

---

## 6. Product acceptance: proving Arovia works without AI

Every major release must be validated with AI disabled.

At minimum, tests and realistic walkthroughs should prove:

1. Users can authenticate and access authorized organizations.
2. Enabled business modules load and operate without AI configuration.
3. Essential transactions can be completed end to end.
4. Standard reports and dashboards return correct results.
5. Business rules and approvals execute deterministically.
6. Notifications and normal integrations continue to work.
7. AI provider credentials are not needed for application startup or normal business operations.
8. AI provider outages do not interrupt sales, payments, inventory or service workflows.
9. No AI jobs, background data processing or AI usage charges occur when AI is inactive.
10. Enabling AI does not bypass existing role, organization or module permissions.

The AI module must also be tested independently, including behavior when it is disabled, misconfigured, rate-limited or unavailable.

---

## 7. Architectural guardrails

- Keep the backend as a modular monolith until deployment boundaries are justified.
- Give each business module ownership of its domain rules and source-of-truth records.
- Keep AI orchestration and provider code within the AI boundary.
- Do not make core or business module services import AI runtime classes.
- Use explicit APIs, application interfaces, events and tool contracts for module integration.
- Do not give AI direct database access.
- Enforce tenant and user permissions in backend services.
- Require approval for consequential AI-generated actions.
- Keep standard reporting and deterministic automation independent of AI.
- Isolate AI failures from essential business transactions.
- Keep BusinessLab simulation isolated from production.
- Introduce generic module activation and composition only when validated requirements justify them.

---

## 8. Current execution priority

The immediate objective remains completing Arovia's secure identity and organization foundation.

The next implementation milestone is **AROVIA-002A.2 — Authentication, Sessions and Account Recovery**.

After that, continue with organization onboarding, membership and workspace access, then move into the first operational business capabilities.

We will not pause the core product to build AI infrastructure.

AI remains a future optional module that can be added after the core business application is useful, reliable and independently operable.

---

## 9. Roadmap governance

This roadmap is a strategic sequence, not a fixed contract for dates or scope.

For each implementation slice:

1. Define the business problem and expected outcome.
2. Design the domain boundaries and user workflow.
3. Implement a complete, testable vertical slice.
4. Validate it with realistic scenarios.
5. Verify that the capability does not introduce an unnecessary AI dependency.
6. Update architecture decisions and `AROVIA_HANDOFF.md`.
7. Close the slice with documented test results and known limitations.

A stage can be adjusted when real pilot evidence, technical dependencies or business priorities justify it.

---

## 10. Supersession and document authority

This roadmap records the product decision made on **13 November 2026** that Arovia must be fully useful without AI and that AI is an optional, independently activatable module.

Where earlier product discovery or handoff documents describe AI as mandatory, fundamental to every organization or required in the initial operational release, this roadmap and the accepted architecture decisions supersede those assumptions.

The expanded cross-industry and composable platform vision remains intact. Only the positioning of AI changes: **AI is an optional enhancement to Arovia, not a prerequisite for Arovia to function.**