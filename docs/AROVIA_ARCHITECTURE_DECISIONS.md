# Arovia — Initial Architecture Decisions

**Version:** 0.1  
**Date:** 12 November 2026  
**Status:** Initial decisions for the foundation slice

| ID | Decision | Rationale | Status |
|---|---|---|---|
| ADR-001 | Focus the first product on computer sales and service businesses | Enables domain depth and a clear pilot value proposition | Accepted |
| ADR-002 | Design optimized future-state workflows rather than copying existing procedures | Arovia should improve business operations, not merely digitize current habits | Accepted |
| ADR-003 | Design and implementation proceed hand in hand through vertical slices | Shortens feedback loops and avoids speculative upfront design | Accepted |
| ADR-004 | Use a modular monolith for the backend | Keeps deployment and operations simple while preserving domain boundaries | Accepted |
| ADR-005 | Use Java 21 and Spring Boot for the backend | Aligns with the chosen technology direction | Accepted |
| ADR-006 | Use Next.js, React and TypeScript for the frontend | Supports responsive web UX and a cohesive application shell | Accepted |
| ADR-007 | Use PostgreSQL for durable business data | Provides relational integrity for connected business transactions | Accepted |
| ADR-008 | Introduce PostgreSQL/Flyway with the first persisted business capability | Keeps schema design tied to an implemented workflow while retaining PostgreSQL as the chosen database | Proposed |
| ADR-009 | Design tenant-aware foundations from the beginning | Avoids expensive retrofitting when onboarding additional businesses | Accepted |
| ADR-010 | Keep AI providers pluggable and AI tools application-mediated | Supports local/cloud options and prevents agents bypassing business rules | Accepted |
| ADR-011 | Consequential AI actions require backend-enforced approval | Protects financial, inventory and customer-facing operations | Accepted |
| ADR-012 | Model catalogue products, physical stock units, customer devices and repair jobs separately | Preserves traceability across purchase, sale, ownership and repair lifecycles | Accepted |
| ADR-013 | Keep the initial web experience responsive; evaluate PWA/native apps later | Delivers desktop and mobile access without multiplying clients prematurely | Accepted |

## Revisit conditions

- Revisit tenancy complexity after the first additional pilot business is onboarded.
- Revisit asynchronous infrastructure when real workloads require durable jobs, retries or scheduled execution beyond in-process facilities.
- Revisit deployment topology only when operational scale or isolation requirements justify additional services.
- Revisit accounting scope after validating the pilot's statutory and accountant-led requirements.