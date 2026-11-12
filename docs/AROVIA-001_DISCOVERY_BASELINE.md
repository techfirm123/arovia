# AROVIA-001 — Product Discovery & Domain Architecture

**Project:** Arovia  
**Document:** Discovery baseline and preliminary architecture  
**Version:** 0.1  
**Date:** 12 November 2026  
**Status:** Living baseline — design and implementation proceed together through vertical slices

---

## 1. Executive summary

Arovia is an AI-powered business operating platform focused initially and deliberately on **computer sales and service businesses**. The first design partner is a small, owner-operated computer sales and repair business.

Arovia will bring customer management, product catalogue, serial-number-aware inventory, procurement, sales, billing, repair operations, customer communication, operational finance and business intelligence into one application. Specialized AI workers will use shared, permission-controlled business context to investigate, prepare recommendations and coordinate work. Consequential actions will be authorized by a person or by an explicitly configured business policy.

The product will be specialized at the domain layer while retaining reusable platform foundations. It will not attempt to support unrelated business verticals during the initial product phase.

### Product positioning

> **Arovia is the AI-powered operating system for computer retailers and service centres.**

It is not merely accounting software, inventory software, a repair-ticket tool or a chatbot. It is the shared operational system connecting the business's customers, products, stock, sales, devices, repairs, money and communications.

## 2. Product vision and principles

### Vision

Give independent computer-business owners an integrated digital operating environment and an intelligent workforce that helps them run daily operations with less manual coordination and better visibility.

### Product principles

1. **Industry depth before horizontal breadth.** Solve computer sales and repair workflows exceptionally well before entering another vertical.
2. **One authoritative business record.** Customers, products, stock, sales, service jobs, payments and conversations must be connected rather than duplicated across modules.
3. **Operational truth comes from the application.** AI must retrieve actual customer, price, stock, payment and job data; it must not invent or infer business facts.
4. **Human control for consequential actions.** AI may observe, analyze and prepare. Execution of externally visible, financial, inventory-affecting or otherwise consequential actions must follow permissions and approval policy.
5. **Traceability by design.** Preserve who changed what, when, why and through which workflow.
6. **Progressive complexity.** Keep the first release practical for an owner-operated shop. Add staff roles and advanced workflows when the business needs them.
7. **Mobile-first operational access, desktop-efficient management.** Common counter and workshop tasks should work on a phone; cataloguing, reporting and administration should work particularly well on desktop.
8. **Extensible platform, specialized first product.** Make tenant, identity, integration, audit and AI infrastructure reusable without creating a generic industry configuration framework prematurely.

## 3. Target customer and pilot assumptions

### Initial target segment

Small businesses that sell, service or both sell and service:

- Laptops and desktop computers.
- Computer components, peripherals and accessories.
- Networking and related IT hardware.
- Laptop and desktop repair, maintenance and upgrades.

### Primary pilot persona

**Owner-operator** — manages customer enquiries, quotations, purchases, stock, sales, service jobs, payments and business decisions. The owner may also perform repairs or coordinate one or more technicians.

### Secondary personas to accommodate in the model

- Sales/counter operator.
- Service technician.
- Store or inventory operator.
- Bookkeeper/accountant (potentially read-only or limited access).

The first pilot may begin with one owner account. The data model and authorization design should not assume that the business will always have only one user.

### Working assumptions to validate

- One legal business and one primary shop/workshop location for the first pilot.
- One owner is the primary decision-maker.
- The business handles both sales and repairs, or at minimum needs the product to support both flows.
- Products may be tracked by quantity, serial number, or both depending on the item.
- Customers may contact the business in person, by phone and through WhatsApp.
- The business needs practical operational records and management reports before it needs a full accounting package.
- India is the provisional first-market jurisdiction. Tax configuration and legal document requirements must be confirmed against the pilot's registration status and accountant's guidance.

These are assumptions, not confirmed business requirements.

## 4. Product scope

### 4.1 Initial product capabilities

| Capability | Initial scope |
|---|---|
| Business setup | Business profile, location, invoice settings, financial-year configuration and tax profile |
| Identity and access | Owner account, membership, role/permission foundation |
| Customer management | Customer profiles, contact details, addresses, notes, purchase and service history |
| Product catalogue | Brands, categories, product models/SKUs, specifications, units, pricing and tax classification |
| Inventory | Stock balances, stock movements, serial-tracked units, reservations and stock adjustments |
| Procurement | Suppliers, purchase orders, goods receipt and supplier bills/payments at an operational level |
| Sales | Enquiries, quotations, sales orders, invoices, discounts, payments, returns and warranty registration |
| Service and repairs | Device intake, condition/accessory capture, diagnosis, estimates, approvals, work orders, parts, testing, delivery and service warranty |
| Communications | Conversation history, inbound/outbound message records, templates, consent and WhatsApp integration |
| Financial operations | Receivables, payables, expenses, payment records and cash/bank summaries |
| Notifications | Internal reminders and customer communication tasks |
| Business intelligence | Daily operating summary, sales/service/stock/dues dashboards and basic trends |
| AI workforce | Context-aware assistants, recommendations, drafts, controlled tools and approval workflow |

### 4.2 Explicitly deferred

- Support for unrelated industries and generic business templates.
- Full double-entry accounting, statutory bookkeeping and tax-return filing.
- Automated e-invoice, e-way bill or GST portal submissions until the pilot's applicability and integration needs are confirmed.
- Payroll and HR management.
- E-commerce storefront, marketplace synchronization and complex omnichannel order orchestration.
- Advanced multi-warehouse logistics and inter-branch transfers.
- Native iOS/Android applications; begin with responsive web/PWA evaluation.
- Autonomous AI execution of payments, refunds, discounts, stock write-offs or customer messages without policy-controlled authorization.
- Microservices, Kubernetes and distributed infrastructure before measurable need.
- General-purpose industry configuration engine.

## 5. Core business workflows

### 5.1 Customer enquiry to computer sale

1. An enquiry arrives at the shop or through a recorded communication channel.
2. The operator identifies an existing customer or creates a new customer record.
3. The operator captures the customer's need, budget and relevant preferences.
4. Product catalogue and current available stock are consulted.
5. A quotation is prepared with item, serial/stock allocation where appropriate, pricing, discount and tax treatment.
6. The quotation is shared with the customer; an AI worker may prepare the draft, but external sending follows configured approval policy.
7. Customer acceptance converts the quote into a sale/order.
8. The business records payment, issues the applicable invoice/document and completes delivery.
9. Inventory is reduced through traceable stock movements; serial-tracked units are linked to the customer and sale.
10. Warranty and purchase history are available on the customer's profile.

**Key controls:** Do not sell more available stock than policy allows; preserve quote/invoice history; do not silently edit a posted invoice; record returns, credit notes and refunds as explicit follow-on transactions.

### 5.2 Device intake to completed repair

1. A customer requests repair or maintenance.
2. Staff identify the customer and register the device, including make/model, serial number/IMEI if relevant, reported issue and ownership context.
3. Staff record physical condition, visible damage, accessories received and intake photographs where useful.
4. A service job is created and a receipt/intake acknowledgement is generated.
5. A technician records diagnosis, likely cause, estimated parts, labour, cost and turnaround time.
6. The estimate is presented to the customer and explicit approval or rejection is recorded.
7. An approved work order is created; parts are reserved/consumed with inventory movements.
8. Repair work and status changes are recorded, including technician notes and test results.
9. The customer receives appropriate status updates under the communication policy.
10. Payment is recorded, delivery/collection is acknowledged and any service warranty is registered.
11. The full service history remains attached to the device and customer.

**Key controls:** No repair work beyond agreed diagnostic scope without authorization; preserve device intake condition; distinguish customer-owned devices from shop stock; never store device passwords in ordinary notes. If temporary credentials are ever supported, they require a separately reviewed, encrypted, short-retention design and explicit customer consent.

### 5.3 Purchase to available stock

1. Staff record supplier and purchase requirements.
2. A purchase order is prepared and approved according to business policy.
3. Goods are received against the order, with quantity, condition, unit cost and serial numbers where applicable.
4. Differences, damaged goods and short receipts are recorded.
5. Stock movements make accepted goods available for sale or service use.
6. Supplier bill and payment/outstanding balance are recorded.
7. Reorder recommendations use actual stock, sales velocity, repair-part usage and lead time where data is available.

**Key controls:** A purchase order does not itself increase stock. Goods receipt does. Stock corrections require a reason and audit record.

### 5.4 Daily owner review

The owner should be able to see, in one place:

- Sales and collections for the day/period.
- Open quotations and customer follow-ups.
- New, pending, delayed and ready-for-delivery service jobs.
- Low-stock and reorder candidates.
- Customer dues and supplier payables.
- Expenses and cash/bank movement summaries.
- Important exceptions requiring human attention.

AI-generated summaries must link back to source records and distinguish confirmed facts from recommendations or estimates.

## 6. Domain model — preliminary

A critical modelling rule: **Product, stock unit, customer-owned device and repair job are not the same entity.**

### 6.1 Platform and business identity

- **Tenant/Business:** The legal or operational business boundary used for data isolation.
- **BusinessProfile:** Name, address, contact details, branding, time zone, currency and jurisdiction.
- **BusinessLocation:** Shop, workshop or stock location. The pilot may begin with one.
- **User:** Authenticated human identity.
- **Membership:** A user's relationship with a business.
- **Role/Permission:** Access policy such as Owner, Sales, Technician or Finance.
- **BusinessSettings:** Numbering sequences, financial year, invoice configuration, tax profile and operational preferences.

### 6.2 Customer and supplier domain

- **Customer:** Person or organization buying products or requesting services.
- **CustomerContactPoint:** Phone, email and other communication endpoints with verification/preference metadata where applicable.
- **CustomerAddress:** Billing, delivery or service address.
- **CustomerNote/Activity:** Human-entered context and operational history.
- **Supplier:** Vendor providing products, parts or services.
- **SupplierContact:** Supplier contact details and communication preferences.

Do not use phone number as the sole permanent customer identifier. Support duplicate detection and later customer merge with audit history.

### 6.3 Product catalogue and inventory

- **Brand:** Manufacturer/brand.
- **ProductCategory:** Hierarchical category.
- **Product:** Product model or sellable catalogue definition.
- **SKU/ProductVariant:** Specific sellable configuration, where a model has variants.
- **ProductSpecification:** Structured attributes such as processor, memory, storage, screen size and compatibility details.
- **UnitOfMeasure:** Piece, pack, metre, etc. as required.
- **TaxClassification:** HSN/SAC or jurisdiction-specific classification and tax treatment.
- **StockLocation:** Physical stock location.
- **InventoryBalance:** Derived/current quantity view by tenant, location and SKU.
- **StockMovement:** Immutable record of receipt, sale, return, reservation, release, transfer, repair consumption or adjustment.
- **StockUnit:** Individual physical unit, especially for serial-tracked devices/components; includes serial number, SKU, current status and acquisition reference.
- **StockReservation:** Allocation of quantity or specific serial units to a quote/order/work order.

The movement ledger is the traceability source. A mutable balance is a projection for fast reads and must be reconcilable against movements.

### 6.4 Procurement

- **PurchaseOrder** and **PurchaseOrderLine**.
- **GoodsReceipt** and **GoodsReceiptLine**.
- **SupplierBill**.
- **SupplierPayment**.
- **SupplierReturn/DebitAdjustment** where needed.

Receiving goods and recording a supplier bill are related but distinct processes. The system must support partial deliveries and discrepancies.

### 6.5 Sales and billing

- **SalesEnquiry** (optional initial entity if enquiries need their own lifecycle).
- **Quote** and **QuoteLine**.
- **SalesOrder** and **SalesOrderLine**.
- **Invoice** and **InvoiceLine**.
- **Payment** and **PaymentAllocation**.
- **SalesReturn** and **CreditNote/Refund** as applicable.
- **WarrantyRegistration**.
- **CustomerDevice:** A device owned/used by a customer, linked to the original sale/stock unit when known, but also registrable for devices bought elsewhere.

Posted commercial documents should be treated as immutable. Corrections must use an explicit cancellation, credit/debit note, return or adjustment process appropriate to the document type and applicable rules.

### 6.6 Service and repairs

- **ServiceJob:** The customer-facing repair/service case and lifecycle.
- **DeviceIntake:** Device identity, condition, accessories, reported issue and intake acknowledgement.
- **Diagnosis:** Technician findings and recommended work.
- **ServiceEstimate** and **EstimateLine:** Labour, parts, tax and expected timing.
- **CustomerAuthorization:** Approval/rejection, scope, amount and timestamp.
- **WorkOrder:** Authorized execution plan.
- **WorkTask:** Work performed by a technician.
- **PartConsumption:** Parts reserved/consumed against the job and corresponding stock movements.
- **TestResult/QualityCheck:** Repair validation.
- **ServiceStatusEvent:** Traceable status changes.
- **ServiceWarranty:** Warranty terms for completed repair/work.
- **DeviceCustodyEvent:** Intake, internal handoff and customer collection events where needed.

Suggested service lifecycle: `RECEIVED → DIAGNOSIS → ESTIMATE_PENDING → AWAITING_APPROVAL → APPROVED → IN_PROGRESS → QUALITY_CHECK → READY_FOR_COLLECTION → DELIVERED → CLOSED`.

Exception states such as `REJECTED`, `ON_HOLD`, `WAITING_FOR_PARTS`, `UNREPAIRABLE` and `CANCELLED` should be modelled explicitly. State transitions must be validated by application rules, not free-form text.

### 6.7 Finance and operational money records

- **Expense** and **ExpenseCategory**.
- **Receivable** / customer outstanding balance projection.
- **Payable** / supplier outstanding balance projection.
- **Payment** with method, reference, amount, date and allocation.
- **Cash/BankAccount** and **CashMovement** as needed for reconciliation.

Initial financial operations should provide reliable transaction records, dues and cash-flow visibility. Do not claim full accounting compliance until a proper accounting model, reconciliation and statutory scope are designed.

### 6.8 Communication and AI

- **Conversation:** A thread with a customer on a channel.
- **Message:** Inbound/outbound communication with provider identifiers, timestamps and delivery status.
- **ChannelAccount:** Tenant's connected WhatsApp Business Platform account/phone-number configuration.
- **ConsentRecord:** Channel and purpose-specific customer opt-in/communication preference evidence.
- **MessageTemplate:** Approved or configured reusable message definition.
- **AIWorkerDefinition:** Role, allowed tools, policies and prompt/configuration version.
- **AIJob/AgentRun:** Trigger, context references, model/provider, status and execution trace.
- **ProposedAction:** Structured action prepared by an AI worker.
- **ApprovalRequest/ApprovalDecision:** Human authorization and decision evidence.
- **ToolExecution:** Application service invocation, idempotency key, result and audit reference.
- **KnowledgeReference:** Link to business records or approved documents used to ground an answer.

## 7. Preliminary modular-monolith architecture

### 7.1 Technology direction

| Layer | Initial direction |
|---|---|
| Backend | Java 21, Spring Boot |
| Frontend | Next.js, React, TypeScript |
| Database | PostgreSQL |
| API | REST with an OpenAPI contract |
| Architecture | Modular monolith |
| AI | Provider gateway for local and cloud models |
| Messaging | Official WhatsApp Business Platform integration |
| Web experience | Responsive desktop/mobile web; evaluate PWA support |

### 7.2 Suggested backend modules

1. `identity` — authentication, user identity and sessions.
2. `tenancy` — tenant/business lifecycle, memberships and tenant context.
3. `business-profile` — business settings, locations, tax and document configuration.
4. `customers` — customers, contacts, addresses and activities.
5. `catalog` — brands, categories, products, variants and specifications.
6. `inventory` — stock units, balances, movements, reservations and adjustments.
7. `procurement` — suppliers, purchase orders, goods receipts and supplier bills.
8. `sales` — quotations, sales orders, invoices, returns and warranty registration.
9. `service` — intake, diagnosis, estimates, approvals, work orders and delivery.
10. `finance` — expenses, payments, receivables, payables and summaries.
11. `communications` — conversations, messages, channel accounts and templates.
12. `integrations` — WhatsApp and future external-system adapters.
13. `workflow` — business process orchestration, scheduled work and approvals.
14. `notifications` — in-app and external notification delivery.
15. `ai` — model gateway, worker runtime, context assembly, tool registry and guardrails.
16. `analytics` — read models, dashboards and operational summaries.
17. `audit` — durable actor/action audit history.

Modules should expose deliberate application interfaces and own their business rules. Avoid cross-module repository access and shared mutable entity models. Start with one deployable backend and one PostgreSQL database. Do not create microservices merely to mirror module boundaries.

### 7.3 Module interaction and events

- Use synchronous application interfaces for operations that require immediate validation and response.
- Use domain events for decoupled reactions such as notifications, analytics projections and AI task triggers.
- Persist events through a transactional outbox when a committed business change must reliably trigger asynchronous work.
- Consumers must be idempotent; external webhooks and message callbacks must be deduplicated.
- Stock and financial mutations must be transactional and protected against concurrent updates.

Potential events include `CustomerCreated`, `QuoteApproved`, `SaleCompleted`, `InvoiceIssued`, `PaymentRecorded`, `GoodsReceived`, `StockBelowThreshold`, `ServiceJobReceived`, `EstimateApproved`, `RepairStatusChanged` and `ServiceJobDelivered`.

### 7.4 Multi-tenancy foundation

Design tenant awareness from the first migration even though the pilot begins with one business.

Recommended initial approach:

- Shared PostgreSQL database and shared application deployment.
- `tenant_id` on every tenant-owned business record.
- Resolve tenant access from authenticated membership; never trust an arbitrary tenant ID supplied by the browser.
- Tenant-scoped application services and repository queries.
- Tenant-aware unique constraints and indexes; composite foreign-key enforcement where practical.
- Automated tests proving that one tenant cannot read or mutate another tenant's data.
- Evaluate PostgreSQL Row-Level Security as an additional isolation layer after validating connection-pool and transaction-context handling.

Keep the option open for higher-isolation deployment models later; do not implement database-per-tenant complexity for the first pilot without a concrete need.

### 7.5 Frontend structure

The application should have a consistent business workspace with role-aware navigation and shared search/context. Initial information architecture:

- Overview
- Customers
- Sales
- Service Desk
- Products
- Inventory
- Purchases / Suppliers
- Finance
- Communications
- Reports
- AI Workforce
- Settings

The UI must make frequent tasks fast: create a customer, search a product, check availability, create a quote, receive a device, update a repair status, record payment and find pending work. Responsive layout is a baseline requirement, not a later redesign.

## 8. AI workforce architecture and governance

### 8.1 Initial workers

| Worker | Primary responsibility | Example output |
|---|---|---|
| Customer Assistant | Understand customer questions and retrieve relevant history | Contextual response draft and follow-up task |
| Sales Assistant | Discover products and prepare sales material | Availability-backed recommendations and quote draft |
| Service Coordinator | Assist with job intake, diagnosis coordination and customer updates | Intake summary, pending-approval alert and status draft |
| Inventory Assistant | Monitor stock and repair-part consumption | Reorder recommendation with supporting movement/sales evidence |
| Finance Assistant | Summarize recorded transactions and dues | Outstanding-payment summary and anomaly to review |
| Business Analyst | Explain business performance and operational exceptions | Daily brief with links to source records |

### 8.2 Shared business context

Workers should not maintain disconnected copies of business truth. A context service should retrieve permission-filtered, tenant-scoped facts from authoritative modules and assemble task-specific context.

Each AI response involving business facts should distinguish:

- Verified facts read from Arovia records.
- Calculations derived from those facts.
- Model-generated interpretations or recommendations.
- Unknown or missing data.

A worker must not claim stock availability, price, payment status, warranty coverage or repair completion unless the relevant system of record confirms it.

### 8.3 Tool execution model

The model must not connect directly to PostgreSQL or bypass domain rules. It can request narrowly scoped tools that call normal application services.

Recommended action lifecycle:

`TRIGGERED → CONTEXT_ASSEMBLED → ANALYZED → PROPOSED → AWAITING_APPROVAL (when required) → EXECUTED → VERIFIED`

Rejected, expired, failed and cancelled states must be recorded.

Examples:

- Safe read: search a customer or query current stock, subject to permissions.
- Draft: prepare a quote, repair update or customer response.
- Approval-required: issue an invoice, apply a non-standard discount, send an external message, consume/write off stock, issue a refund or record a consequential financial adjustment.

Approval must bind to the exact proposed action and its relevant data. If the proposal changes after approval, it must be approved again. Execution must be idempotent and auditable.

### 8.4 Model/provider abstraction

Create a provider-neutral gateway for local and cloud model providers. The abstraction should cover model capabilities, structured output, tool calling, streaming, embeddings and health checks as supported by each provider. Do not assume every local model supports every capability.

Provider selection should be configurable by tenant/business policy and task sensitivity. Sensitive business/customer data must not be sent to a cloud provider unless the business has explicitly enabled that provider and the applicable privacy/data-processing terms have been reviewed.

## 9. WhatsApp integration strategy

Integrate only through the **official WhatsApp Business Platform**, initially evaluating the Cloud API and its webhook-based integration model. Do not build on WhatsApp Web automation, browser scraping or unofficial session libraries.

The integration boundary should handle:

- Business account and phone-number onboarding/configuration.
- Webhook verification and inbound message ingestion.
- Outbound messages and template selection.
- Delivery/read/failure status callbacks where available.
- Opt-in and opt-out/communication preferences.
- Provider error handling, rate limits, retries and idempotency.
- Conversation-to-customer matching with duplicate/unknown-contact handling.
- Tenant-specific credentials stored securely.

Meta's developer hub lists API references, webhooks, error codes, policy enforcement, rate limits, opt-in guidance, changelog and pricing resources. These must be revisited during the integration milestone because platform policies and pricing can change: https://whatsappbusiness.com/developers/developer-hub/ .

Do not assume that an inbound customer message grants unrestricted permission for all future outbound communication. Apply the current platform rules, consent requirements, template requirements and message-category pricing at implementation time.

## 10. India-first billing and tax design — validation required

India is the provisional pilot jurisdiction, but the pilot business's GST registration, composition/regular status, invoicing needs and e-invoicing applicability have not yet been confirmed.

The billing model should therefore be configuration-driven rather than hard-coding a single tax treatment. It should support the relevant supplier identity, document numbering, customer details, goods/services classification, HSN/SAC, quantity, taxable value, discounts, tax rates and amounts, and place-of-supply details where applicable.

CBIC's published Rule 46 information lists core tax-invoice particulars including supplier identity/GSTIN, financial-year-unique serial number, issue date, recipient details where applicable, HSN/SAC, description, quantity for goods, taxable value, tax rate and tax amount, and place-of-supply details for relevant inter-State supplies. The exact obligations and any later amendments must be checked for the pilot before production billing is enabled: https://cbic-gst.gov.in/gst-invoice-rules.html .

This document is a product-design baseline, not tax or legal advice. Validate the implementation with the business's accountant or qualified GST advisor. E-invoicing, e-way bills, return filing and portal integrations remain out of scope until applicability is established.

## 11. Security, privacy and reliability requirements

- Enforce tenant and user authorization in backend services, not only in frontend navigation.
- Use least-privilege roles and explicit permissions for financial, inventory and customer-data operations.
- Audit sensitive reads where justified and all consequential business mutations.
- Protect passwords, API credentials, WhatsApp tokens and provider secrets using established secret-management practices.
- Encrypt traffic and protect sensitive data at rest; define retention and deletion policies.
- Do not store customer device passwords in free-form service notes.
- Validate uploads, restrict file access and record document ownership.
- Use idempotency keys for payments, webhook ingestion and AI-triggered commands where duplicate execution would be harmful.
- Use transactional boundaries for sales completion, payment allocation, goods receipt and parts consumption.
- Provide backups and test restoration before the pilot handles real operational records.
- Define error handling and retry policies for external messaging and provider outages.
- Keep an audit trail for approval decisions and the exact action executed.
- Separate operational records from AI-generated summaries; AI content must never silently overwrite source records.

## 12. Initial release strategy

The release sequence should be adjusted after observing the pilot business. A proposed order is:

### Foundation slice

- Repository and application foundation.
- Tenant/business setup and identity.
- Business settings and audit infrastructure.
- Responsive application shell and navigation.

### Operational backbone

- Customers and suppliers.
- Product catalogue and tax classification.
- Stock ledger, serial tracking and stock search.
- Purchase and goods receipt.

### Sales operations

- Quotations, sales orders, invoices and payments.
- Warranty registration and customer purchase history.
- Returns and correction workflows.

### Service operations

- Device intake and custody history.
- Diagnosis, estimates and customer authorization.
- Work orders, parts consumption, testing and delivery.
- Service warranty and customer notifications.

### Communication and intelligence

- Official WhatsApp integration.
- Conversation timeline and consent preferences.
- Operational dashboards and owner daily brief.

### AI workforce

- AI provider gateway and policy framework.
- Read-only contextual workers first.
- Drafting and recommendation workflows.
- Approval-bound tool execution.
- Evaluation, audit and monitoring.

AI should not be postponed until the very end as a cosmetic chatbot, but reliable business records and application tools must exist before workers are allowed to act on them.

## 13. Initial acceptance scenarios

The pilot is not ready merely because screens exist. At minimum, the product must demonstrate these end-to-end scenarios:

1. Register a customer, create a quotation from actual catalogue/stock data, complete a sale, record payment, issue the configured document and trace the serial number to the customer.
2. Receive a laptop for repair, record condition/accessories, diagnose it, prepare an estimate, obtain approval, consume an identified part, complete quality checks, record payment and deliver the device.
3. Receive serial-tracked and quantity-tracked stock from a supplier and reconcile the resulting stock balance against movement history.
4. Search a customer and view their sales, device ownership, repairs, payments and communication history in one place.
5. Demonstrate that a user from one tenant cannot access another tenant's records through UI, API or AI tools.
6. Ask an AI worker for available stock and verify that its answer is grounded in live application data and provides traceable evidence.
7. Ask an AI worker to prepare a customer message and verify that it cannot send the message before the applicable approval/policy check.
8. Simulate duplicate webhooks and retried commands and verify that no duplicate message, payment, stock movement or business transaction is created.

## 14. Risks and design concerns

| Risk | Mitigation |
|---|---|
| Overbuilding for hypothetical industries | Keep initial domain computer-specific; defer generic vertical engine |
| Incorrect stock balances | Immutable movement history, transactional updates and reconciliation |
| Serial-number and warranty gaps | Model stock units and customer devices separately |
| Repair disputes | Intake condition, accessories, custody events and customer approvals |
| Incorrect tax documents | Configurable tax profile; validation with accountant; do not hard-code rates or legal rules |
| WhatsApp policy changes | Official API adapter; revisit Meta docs and policy before release |
| AI hallucinations | Ground responses in system records; cite linked records; mark uncertainty |
| Unauthorized AI actions | Narrow tools, policy checks, action-bound approvals and audit trail |
| Tenant data leakage | Tenant context enforcement, scoped queries and cross-tenant tests |
| Excessive architecture complexity | One deployable modular monolith; add infrastructure only when justified |

## 15. Open questions for pilot validation

These questions are validation prompts, not instructions to copy the current process. Arovia defines the proposed future-state workflow first, then uses the pilot business to test usability, operational fit, exceptions, constraints and outcomes. Existing tools and habits matter when planning migration or identifying real-world constraints, but they do not dictate the product design.

### Business profile and daily operations

- Is the business registered for GST? What is the registration/tax scheme and who configures tax treatment?
- Is the business one shop/workshop or are there multiple locations?
- Who performs sales, purchasing, repairs and bookkeeping today?
- Which existing records, spreadsheets, billing tools or registers contain data that may need to be migrated?
- Does the proposed Arovia workflow reduce delays, missed follow-ups or financial leakage compared with the current outcome?
- Which steps in the proposed workflow feel confusing, repetitive or unnecessary to the owner?

### Sales and inventory

- Which product types require serial-number tracking? Are accessories quantity-only?
- Are quotations commonly created? How long are prices held?
- Does the business sell used/refurbished devices, custom-built PCs or assembled systems?
- How are discounts, customer advances, part-payments, returns and exchanges handled?
- Are stock levels updated immediately today, or reconciled periodically?

### Repairs

- What are the actual repair statuses and handoff points?
- Is diagnosis charged? Is the fee adjusted against the repair if accepted?
- Are estimates frequently revised after opening a device?
- Are spare parts reserved at diagnosis or only consumed when fitted?
- How are customer data, device passwords, warranty exclusions and physical condition documented?
- What are the rules for abandoned or uncollected devices?

### Finance and communication

- Which payment methods are accepted (cash, UPI, card, bank transfer, credit)?
- Are customer credit/dues and supplier balances tracked?
- Is a separate accounting package already used?
- Is WhatsApp Business App currently used, and is the owner prepared to onboard a number to the official Business Platform if required?
- Which messages should be sent automatically, drafted for approval or never automated?

## 16. Decisions recorded in this baseline

| Decision | Status |
|---|---|
| Initial target industry is computer sales and service | Agreed |
| Build the specialized product before expanding to other verticals | Agreed |
| Java 21 + Spring Boot backend | Initial technology direction |
| Next.js + React + TypeScript frontend | Initial technology direction |
| PostgreSQL database | Initial technology direction |
| Modular monolith | Initial architecture direction |
| Local and cloud AI provider abstraction | Initial architecture direction |
| Consequential AI actions require authorization | Product principle |
| Tenant-aware foundation from the beginning | Recommended; confirm in architecture review |
| India-first GST-aware billing design | Provisional assumption; validate with pilot |
| Official WhatsApp Business Platform integration | Product direction; implementation details to validate |
| Single primary location and owner-operated workflow | Pilot assumption; validate |

## 17. Design-and-build execution

Arovia will not wait for a complete upfront specification before implementation. Product design and engineering will proceed hand in hand through complete, independently testable vertical slices.

For each slice:

1. Define the business outcome and user problem.
2. Design the optimized future-state workflow and responsive UX.
3. Define only the domain, API, persistence, permission and integration decisions required by that slice.
4. Implement the frontend and backend capability together.
5. Run automated tests and validate realistic business scenarios with the design partner.
6. Refine the workflow and implementation based on usability, operational constraints and exceptions.
7. Update architecture decisions and `AROVIA_HANDOFF.md` before closing the slice.

The pilot business validates the design; its current procedures are not the product blueprint. We should adopt an existing practice only when it is demonstrably effective or required by a real business, legal or operational constraint.

### Immediate sequence

1. Complete the application foundation and confirm local frontend/backend startup.
2. Build the first usable Business Workspace and Onboarding capability, introducing PostgreSQL and Flyway with the actual workspace workflow.
3. Validate the workspace setup and dashboard experience with the owner.
4. Move into the next highest-value business capability as a complete vertical slice.

**Exit criterion for each slice:** The capability works end to end, has automated test coverage appropriate to its risk, has been walked through using realistic scenarios, and has an up-to-date handoff record. AROVIA-001 is a living foundation rather than a long design phase that blocks product delivery.

---

## References

- Meta / WhatsApp Business Platform Developer Hub: https://whatsappbusiness.com/developers/developer-hub/ (API documentation, webhooks, error codes, policy enforcement, rate limits, opt-in, changelog and pricing resources; revalidate at implementation time).
- Central Board of Indirect Taxes and Customs — GST Invoice Rules: https://cbic-gst.gov.in/gst-invoice-rules.html (reference for tax-invoice particulars; validate current applicability and amendments with a qualified advisor).