# OSS-02 — Registration Lifecycle: Non-EU and EU Schemes — Implementation Specification

| Field | Value |
|---|---|
| Specification ID | OSS-02-SPEC |
| Petition | `petitions/OSS-02/OSS-02.md` |
| Outcome Contract | `petitions/OSS-02/OSS-02-outcome-contract.md` |
| Feature File | `petitions/OSS-02/OSS-02.feature` (42 scenarios) |
| Status | APPROVED FOR IMPLEMENTATION |
| Service | `osm2-registration-service` |
| Package Root | `dk.osm2.registration` |
| Java Version | 21 |
| Spring Boot | 3.5.0 |
| Database | PostgreSQL 16 |
| Schema | `registration` |

---

## 1. Scope and Constraints

This specification covers exactly what is stated in petition OSS-02 and its outcome contract (AC-OSS-02-01 through AC-OSS-02-25). Nothing beyond that scope is specified here.

### 1.1 In Scope

The complete registration lifecycle under the **Non-EU scheme** (ML §§ 66a–66c) and **EU scheme** (ML §§ 66d–66g):

- Electronic registration with effective-date calculation (FR-OSS-02-001 through FR-OSS-02-008)
- Mandatory identification field collection and validation (FR-OSS-02-009 through FR-OSS-02-012)
- VAT number assignment within 8 calendar days and delay notification (FR-OSS-02-013 through FR-OSS-02-019)
- EU scheme binding rule enforcement (FR-OSS-02-020 through FR-OSS-02-022)
- Change notifications with 10-day compliance tracking (FR-OSS-02-023 through FR-OSS-02-026)
- Voluntary deregistration with 15-day advance notice (FR-OSS-02-027 through FR-OSS-02-030)
- Forced exclusion for four criteria with two different effective-date rules (FR-OSS-02-031 through FR-OSS-02-036)
- Scheme switching without gap or overlap (FR-OSS-02-037)
- Transitional provision enforcement for pre-July-2021 registrants (FR-OSS-02-038)

### 1.2 Out of Scope

| Excluded topic | Reason |
|---|---|
| Import scheme (Importordningen) registration | OSS-03 petition |
| VAT return filing | OSS-04 petition |
| Payment | OSS-05 petition |
| Cross-border EU data exchange protocols | Separate concern |
| Ordinary Danish VAT registration | Separate system |
| Refund/deduction mechanics (ML §§ 66c, 66f) | Separate petition |
| Appeals against exclusion decisions | Separate concern |

### 1.3 Non-Negotiable Architecture Constraints

| Constraint | Rule | Source |
|---|---|---|
| `AuditableEntity` | All `@Entity` classes MUST extend `dk.ufst.opendebt.common.audit.AuditableEntity` | ADR-0013 |
| Bitemporality | `SchemeRegistration` entity MUST carry `validFrom`, `validTo` for valid time; Hibernate Envers `@Audited` for transaction time | ADR-0033 |
| Security | `keycloak-oauth2-starter` provides `SecurityFilterChain`. Do NOT declare one manually. Do NOT add `@PreAuthorize` at method level beyond role checks. | ADR-0005 |
| DTO pattern | Request and response DTOs MUST be Java `record` types | Project standard |
| Validation | Use `jakarta.validation` (`@Valid`, `@NotNull`, `@NotBlank`, etc.) on DTO fields | Project standard |
| State machine | Registration status transitions are fixed (section 3.1) | FR-OSS-02-031, ML § 66j |
| `SchemeType` enum | Codes are `NON_EU`, `EU`, `IMPORT` — statutory, do not rename | ADR-0031, OSS-01-SPEC |
| API-first | All controllers MUST carry OpenAPI (`@Operation`, `@ApiResponse`) annotations | ADR-0004 |
| No `valid_to` direct update | Valid-time columns must only be set through dedicated service methods (`ExclusionService.exclude(...)`, `DeregistrationService.deregister(...)`) — never via generic JPA update | ADR-0033 |

---

## 2. Domain Model

**Package:** `dk.osm2.registration.domain`

### 2.1 Enums

#### 2.1.1 `RegistrantStatus`

The status of the `Registrant` aggregate root.

| Constant | Meaning | Legal basis |
|---|---|---|
| `PENDING_VAT_NUMBER` | Registration submitted; VAT number not yet assigned | ML § 66b stk. 2, § 66e stk. 1 |
| `ACTIVE` | Registration confirmed; VAT number assigned and valid | ML § 66b stk. 2 |
| `DEREGISTERED` | Voluntary deregistration effective | ML § 66i |
| `EXCLUDED` | Forced exclusion effective | ML § 66j |
| `CESSATION_NOTIFIED` | Taxable person notified cessation; pending exclusion decision | FR-OSS-02-025, AC-OSS-02-18 |

> **Note:** The database `registrant_status` enum in V1 uses `PENDING` and `SUSPENDED`. This spec supersedes the DB draft. The Flyway migration must add `PENDING_VAT_NUMBER` and `CESSATION_NOTIFIED` and remove `PENDING` and `SUSPENDED` if those values have no production data, OR the Java enum constants must map to the existing DB values via `@Enumerated`/converter. The build engineer MUST reconcile the Java enum with the DB enum before the V3 migration. **Do not silently alias `PENDING_VAT_NUMBER` to `PENDING` without an explicit migration decision recorded.**

#### 2.1.2 `SchemeType`

Cross-service enum. Re-use the definition from OSS-01-SPEC, section 2.1. Import from the `osm2-common` module.

| Constant | ML Reference | MSD Reference |
|---|---|---|
| `NON_EU` | ML § 66a | MSD art. 358a |
| `EU` | ML § 66d | MSD art. 369a |
| `IMPORT` | ML § 66m | MSD art. 369l |

```
Source: ADR-0031, OSS-01-SPEC §2.1
```

#### 2.1.3 `ExclusionCriterion`

| Constant | Meaning | 2-year ban? | Legal basis |
|---|---|---|---|
| `CESSATION_NOTIFICATION` | Taxable person self-reported cessation of eligible supplies | No | ML § 66j stk. 1 nr. 1 |
| `PRESUMED_CESSATION` | No supplies reported in any consumption MS for 2 consecutive years | No | ML § 66j stk. 1 nr. 2 |
| `CONDITIONS_NOT_MET` | Eligibility conditions no longer satisfied | No | ML § 66j stk. 1 nr. 3 |
| `PERSISTENT_NON_COMPLIANCE` | Persistent failure to comply with scheme rules | **Yes** | ML § 66j stk. 1 nr. 4 |

```
Source: FR-OSS-02-031 through FR-OSS-02-035, AC-OSS-02-21
```

#### 2.1.4 `EuBindingRuleType`

| Constant | Meaning | Legal basis |
|---|---|---|
| `ML_66D_STK2` | Binding rule applies; Denmark selected under case (b) or (c) | ML § 66d stk. 2 |
| `NOT_APPLICABLE` | Case (a) — home establishment in Denmark; no binding commitment | ML § 66d stk. 1 nr. 1 |

```
Source: FR-OSS-02-020, FR-OSS-02-022, AC-OSS-02-11
```

#### 2.1.5 `ChangeNotificationStatus`

| Constant | Meaning |
|---|---|
| `TIMELY` | Submitted on or before the 10th day of the month following the change |
| `LATE_NOTIFICATION` | Submitted after the 10th day; flagged for compliance review |

```
Source: FR-OSS-02-023, FR-OSS-02-024, AC-OSS-02-13, AC-OSS-02-14
```

#### 2.1.6 `VatNumberUsageScope`

| Constant | Meaning | Legal basis |
|---|---|---|
| `NON_EU_SCHEME_ONLY` | Number usable only for Non-EU scheme; not valid for other VAT purposes | MSD art. 362 |
| `EU_SCHEME` | EU scheme VAT number (new or existing Danish number) | MSD art. 369d |

```
Source: FR-OSS-02-015, AC-OSS-02-07
```

### 2.2 JPA Entities

#### 2.2.1 `Registrant`

**Table:** `registration.registrant`
**Extends:** `dk.ufst.opendebt.common.audit.AuditableEntity`
**Audited:** `@Audited` (Hibernate Envers — transaction-time tracking of PII changes)

| Column | Java field | Type | Nullable | Notes |
|---|---|---|---|---|
| `id` | `id` | `UUID` | No | `@Id`, generated |
| `scheme_type` | `schemeType` | `SchemeType` | No | `@Enumerated(EnumType.STRING)` |
| `status` | `status` | `RegistrantStatus` | No | `@Enumerated(EnumType.STRING)` |
| `vat_number` | `vatNumber` | `String(50)` | No | Assigned after approval; scheme-specific scope |
| `vat_number_usage_scope` | `vatNumberUsageScope` | `VatNumberUsageScope` | Yes | Null until assigned; see FR-OSS-02-015 |
| `country_code` | `countryCode` | `String(2)` | No | ISO 3166-1 alpha-2 |
| `legal_name` | `legalName` | `String(200)` | No | Full legal name |
| `address_line1` | `addressLine1` | `String(200)` | Yes | |
| `address_line2` | `addressLine2` | `String(200)` | Yes | |
| `city` | `city` | `String(100)` | Yes | |
| `postal_code` | `postalCode` | `String(20)` | Yes | |
| `contact_email` | `contactEmail` | `String(200)` | Yes | Validated as RFC 5321 email |
| `contact_phone` | `contactPhone` | `String(50)` | Yes | |
| `binding_start` | `bindingStart` | `LocalDate` | Yes | EU binding rule start; null if `NOT_APPLICABLE` |
| `binding_end` | `bindingEnd` | `LocalDate` | Yes | EU binding rule end (31 Dec, year+2); null if `NOT_APPLICABLE` |
| `binding_rule_type` | `bindingRuleType` | `EuBindingRuleType` | Yes | Null for Non-EU registrants |
| `transitional_update_overdue` | `transitionalUpdateOverdue` | `boolean` | No | Default false; set by scheduled job (FR-OSS-02-038) |
| `transitional_update_submitted_at` | `transitionalUpdateSubmittedAt` | `LocalDate` | Yes | Date update submitted; clears overdue flag |
| `created_at` | (from `AuditableEntity`) | `LocalDateTime` | No | |
| `updated_at` | (from `AuditableEntity`) | `LocalDateTime` | No | |
| `created_by` | (from `AuditableEntity`) | `String` | No | JWT `sub`; `"system"` for Flyway default |
| `updated_by` | (from `AuditableEntity`) | `String` | No | |
| `version` | (from `AuditableEntity`) | `Long` | No | Optimistic lock |

**Indexes (from V1 schema):** `idx_registrant_vat (vat_number, country_code)`, `idx_registrant_status (status)`

**Constraints:**
- `vat_number` + `country_code` combination identifies a unique taxable person across schemes
- `binding_start` and `binding_end` MUST both be null or both non-null; enforced at service layer

```
Source: V1__init.sql, V2__audit_columns.sql, FR-OSS-02-022, ADR-0013
```

#### 2.2.2 `SchemeRegistration`

**Table:** `registration.scheme_registration`
**Extends:** `dk.ufst.opendebt.common.audit.AuditableEntity`
**Audited:** `@Audited` (Hibernate Envers — transaction-time tracking of lifecycle events)

| Column | Java field | Type | Nullable | Notes |
|---|---|---|---|---|
| `id` | `id` | `UUID` | No | `@Id`, generated |
| `registrant_id` | `registrant` | `Registrant` | No | `@ManyToOne(fetch = LAZY)` |
| `scheme_type` | `schemeType` | `SchemeType` | No | `@Enumerated(EnumType.STRING)` |
| `valid_from` | `validFrom` | `LocalDate` | No | **Valid-time axis start** (when effective in the real world) |
| `valid_to` | `validTo` | `LocalDate` | Yes | **Valid-time axis end** — null = currently active; set via `ExclusionService` or `DeregistrationService` ONLY |
| `notification_submitted_at` | `notificationSubmittedAt` | `OffsetDateTime` | Yes | When registrant submitted notification to SKAT; used for 10-day compliance check |
| `change_reason` | `changeReason` | `String` | Yes | Required when `valid_from` or `valid_to` is retroactive; free text |
| `created_at` | (from `AuditableEntity`) | `LocalDateTime` | No | |
| `updated_at` | (from `AuditableEntity`) | `LocalDateTime` | No | |
| `created_by` | (from `AuditableEntity`) | `String` | No | |
| `updated_by` | (from `AuditableEntity`) | `String` | No | |
| `version` | (from `AuditableEntity`) | `Long` | No | |

**Indexes (from V1 schema):** `idx_scheme_reg_registrant (registrant_id)`, `idx_scheme_reg_valid_range (valid_from, valid_to)`

**Critical invariants (enforced at service layer, never bypassed):**
- `valid_from` may precede `notification_submitted_at` by at most 10 days (early-delivery exception; ML § 66b stk. 3)
- `valid_to` for forced exclusion due to establishment change MUST equal the change date, not the quarter start (momsforordningen art. 58 stk. 2)
- `change_reason` MUST be non-null whenever `valid_from` or `valid_to` is backdated
- Only ONE `SchemeRegistration` row per registrant per scheme type may have `valid_to IS NULL` at any point in time

```
Source: ADR-0033, V1__init.sql, V2__audit_columns.sql, FR-OSS-02-033, FR-OSS-02-034
```

#### 2.2.3 `ExclusionBan`

**Table:** `registration.exclusion_ban`
**Extends:** `dk.ufst.opendebt.common.audit.AuditableEntity` (append-only; only `created_at`, `created_by` are populated)
**Audited:** NOT annotated with `@Audited` — row is immutable after insert; no Envers overhead needed

| Column | Java field | Type | Nullable | Notes |
|---|---|---|---|---|
| `id` | `id` | `UUID` | No | `@Id`, generated |
| `registrant_id` | `registrant` | `Registrant` | No | `@ManyToOne(fetch = LAZY)` |
| `scheme_type` | `schemeType` | `SchemeType` | No | Scheme from which the person was excluded |
| `exclusion_reg_id` | `exclusionRegistration` | `SchemeRegistration` | No | The `SchemeRegistration` row whose `valid_to` is the exclusion date |
| `ban_lifted_at` | `banLiftedAt` | `LocalDate` | No | Exclusion `valid_to` + 2 years; eligibility gate |
| `created_at` | (from `AuditableEntity`) | `LocalDateTime` | No | |
| `created_by` | (from `AuditableEntity`) | `String` | No | |

**Index (from V1 schema):** `idx_exclusion_ban_registrant (registrant_id, scheme_type, ban_lifted_at)`

**Constraint:** Rows are inserted by `ExclusionService` only when `ExclusionCriterion = PERSISTENT_NON_COMPLIANCE`. No rows for any other criterion. No deletes. No updates.

```
Source: V1__init.sql, V2__audit_columns.sql, FR-OSS-02-035, AC-OSS-02-21, ADR-0033
```

### 2.3 Value Objects (non-persisted)

#### 2.3.1 `EffectiveDateResult` (value object)

Returned by `EffectiveDateCalculationService`.

| Field | Type | Description |
|---|---|---|
| `effectiveDate` | `LocalDate` | Computed effective date |
| `ruleApplied` | `EffectiveDateRule` | Which rule was applied |
| `legalBasis` | `String` | E.g. `"ML § 66b stk. 2"` or `"ML § 66b stk. 3 / Momsforordningen art. 57d"` |

```
Source: FR-OSS-02-003, FR-OSS-02-004, FR-OSS-02-005
```

#### 2.3.2 `EffectiveDateRule` (enum, non-persisted)

| Constant | Meaning |
|---|---|
| `QUARTER_START` | Normal rule: first day of quarter following notification (ML § 66b stk. 2) |
| `EARLY_DELIVERY_EXCEPTION` | First delivery date; notified within 10-day window (ML § 66b stk. 3) |

```
Source: FR-OSS-02-003 through FR-OSS-02-005, AC-OSS-02-02 through AC-OSS-02-04
```

---

## 3. Business Rules

### 3.1 Registration State Machine

**Entity:** `Registrant.status` field

```
PENDING_VAT_NUMBER
    │
    ├─[VAT number assigned]──────────────────────────────► ACTIVE
    │                                                          │
    │                                               ┌──────────┼──────────────────┐
    │                                               │          │                  │
    │                                    [Voluntary      [Cessation        [Forced exclusion
    │                                   deregistration]  notified]          criteria 1-4]
    │                                               │          │                  │
    │                                               ▼          ▼                  │
    │                                        DEREGISTERED  CESSATION_NOTIFIED     │
    │                                                          │                  │
    │                                         [Exclusion decision]               │
    │                                                          ▼                  ▼
    │                                                      EXCLUDED ◄─────────────┘
    │
    └─[Registration rejected before VAT assignment]──► (row deleted or status = REJECTED — see note)
```

**Permitted transitions:**

| From | To | Trigger | Legal basis |
|---|---|---|---|
| `PENDING_VAT_NUMBER` | `ACTIVE` | VAT number assigned | FR-OSS-02-013, FR-OSS-02-017 |
| `ACTIVE` | `DEREGISTERED` | Voluntary deregistration (timely or deferred) | FR-OSS-02-027–030, ML § 66i |
| `ACTIVE` | `CESSATION_NOTIFIED` | Taxable person notifies cessation | FR-OSS-02-025, FR-OSS-02-031 nr. 1 |
| `ACTIVE` | `EXCLUDED` | Forced exclusion (criteria 1–4) by Skatteforvaltningen | FR-OSS-02-031, ML § 66j |
| `CESSATION_NOTIFIED` | `EXCLUDED` | Exclusion decision formally recorded | FR-OSS-02-031 nr. 1 |
| `EXCLUDED` | `PENDING_VAT_NUMBER` | New registration (only if ban lifted or no ban) | FR-OSS-02-035 |
| `DEREGISTERED` | `PENDING_VAT_NUMBER` | Voluntary re-registration; no penalty | FR-OSS-02-030, AC-OSS-02-17 |

**Forbidden transitions** (MUST throw `IllegalStateTransitionException`):**
- Any transition not in the table above
- `EXCLUDED → PENDING_VAT_NUMBER` when `ExclusionBan.banLiftedAt > LocalDate.now()`

> **Note on REJECTED:** The petition does not define a `REJECTED` status for the `Registrant`. Validation errors on submission (missing fields, EU establishment present for Non-EU) MUST be rejected at the API layer (HTTP 400 / 422) **before** a `Registrant` row is persisted. The `Registrant` entity is only created after input validation passes. This avoids orphaned rows with no valid transition path.

```
Source: FR-OSS-02-001, FR-OSS-02-002, FR-OSS-02-027–036, AC-OSS-02-01, AC-OSS-02-18, AC-OSS-02-21
```

### 3.2 Effective Date Algorithm

**Owner:** `EffectiveDateCalculationService`

The algorithm is identical for Non-EU scheme (ML § 66b stk. 2–3) and EU scheme (ML § 66e stk. 2):

```
INPUTS:
  notificationDate   : LocalDate  — date the notification is submitted to SKAT
  firstDeliveryDate  : LocalDate? — null if no prior eligible delivery

ALGORITHM:
  IF firstDeliveryDate IS NULL THEN
    → QUARTER_START: first day of calendar quarter following notificationDate
  ELSE
    deadline = 10th calendar day of the month following firstDeliveryDate
    IF notificationDate <= deadline THEN
      → EARLY_DELIVERY_EXCEPTION: effectiveDate = firstDeliveryDate
    ELSE
      → QUARTER_START: first day of calendar quarter following notificationDate

QUARTER_START computation:
  month = notificationDate.month
  year  = notificationDate.year
  quarterStartMonth = ((month - 1) / 3 + 1) * 3 - 2  // 1, 4, 7, or 10
  IF notificationDate == first day of that quarter THEN
    advanceToNextQuarter: quarterStartMonth += 3 (with year rollover)
  RETURN LocalDate.of(year, quarterStartMonth, 1)

  NOTE: A notification submitted ON the first day of a quarter produces the NEXT
  quarter's start as effective date (Scenario: "Notification on first day of quarter
  produces next quarter as effective date" → 2024-04-01 → 2024-07-01).
```

**Calendar quarter definitions (statutory, ML):**
- Q1: 1 Jan – 31 Mar; start = 1 Jan
- Q2: 1 Apr – 30 Jun; start = 1 Apr
- Q3: 1 Jul – 30 Sep; start = 1 Jul
- Q4: 1 Oct – 31 Dec; start = 1 Oct

**Gherkin boundary cases (MUST all pass):**

| Scenario | Notification date | First delivery | Expected effective date | Rule |
|---|---|---|---|---|
| Normal case | 2024-02-15 | null | 2024-04-01 | `QUARTER_START` |
| On Q start | 2024-04-01 | null | 2024-07-01 | `QUARTER_START` (advance) |
| Early delivery, day 10 | 2024-03-10 | 2024-02-08 | 2024-02-08 | `EARLY_DELIVERY_EXCEPTION` |
| Exactly on deadline | 2024-02-10 | 2024-01-20 | 2024-01-20 | `EARLY_DELIVERY_EXCEPTION` |
| One day late | 2024-02-11 | 2024-01-20 | 2024-04-01 | `QUARTER_START` |
| EU: early delivery | 2024-06-10 | 2024-05-05 | 2024-05-05 | `EARLY_DELIVERY_EXCEPTION` |
| EU: late delivery | 2024-06-11 | 2024-05-05 | 2024-07-01 | `QUARTER_START` |

```
Source: FR-OSS-02-003–005, FR-OSS-02-007–008, AC-OSS-02-02–04, Momsforordningen art. 57d
```

### 3.3 EU Scheme Binding Rule

**Legal basis:** ML § 66d stk. 2; Momsforordningen art. 369a

**Applies to:** EU scheme registrations where Denmark is selected under case (b) or (c) of ML § 66d stk. 1.
**Does NOT apply to:** Case (a) — home establishment in Denmark.

**Binding period calculation:**
```
bindingRuleType = ML_66D_STK2
bindingStart    = registrationConfirmedDate
bindingEnd      = LocalDate.of(registrationConfirmedDate.year + 2, 12, 31)

Example: confirmed in calendar year 2024 → bindingEnd = 2026-12-31
```

**Enforcement rule:**
- Any request to change the identification member state away from Denmark MUST be checked against `Registrant.bindingEnd`
- If `LocalDate.now() <= bindingEnd` AND `bindingRuleType == ML_66D_STK2`: REJECT with error code `EU_BINDING_PERIOD_ACTIVE` and include `bindingEnd` in the error payload
- **Exception:** If the change is triggered by a loss of eligibility (home/fixed establishment moves such that Denmark no longer qualifies), the change MUST be permitted. The `changeReason` for this case MUST be explicitly stated. The binding clock resets in the new identification member state (outside this service's scope).

```
Source: FR-OSS-02-020–022, AC-OSS-02-11, AC-OSS-02-12
```

### 3.4 Exclusion Re-entry Ban

**Legal basis:** ML § 66j stk. 1 nr. 4

**Applies to:** Forced exclusion with criterion `PERSISTENT_NON_COMPLIANCE` only.

**Ban calculation:**
```
banLiftedAt = exclusionSchemeRegistration.validTo + 2 years

Example: exclusion effective 2024-07-01 → banLiftedAt = 2026-06-30
  (Note: "until 2026-06-30" means re-registration IS allowed from 2026-07-01)
```

**Enforcement rule:**
- Before creating any new `SchemeRegistration` for a `Registrant`, check `ExclusionBan` table:
  ```sql
  SELECT 1 FROM exclusion_ban
  WHERE registrant_id = :id
    AND scheme_type = :scheme
    AND ban_lifted_at >= CURRENT_DATE
  ```
- If a row exists: REJECT with error code `EXCLUSION_BAN_ACTIVE` and include `banLiftedAt` in the error payload

**Criteria 1, 2, 3:** NO `ExclusionBan` row is created. Re-registration is permitted in any subsequent quarter.

**Voluntary deregistration:** NEVER creates an `ExclusionBan` row. Re-registration is always permitted (ML lov nr. 810/2020).

```
Source: FR-OSS-02-030, FR-OSS-02-035, AC-OSS-02-17, AC-OSS-02-21
```

### 3.5 Forced Exclusion Effective Date Rules

**Two mutually exclusive rules:**

| Trigger | Effective date rule | Legal basis |
|---|---|---|
| Criteria 1, 2, 3, or 4 (general) | First day of calendar quarter **following** the day the exclusion decision is sent electronically | ML § 66j stk. 1, Momsforordningen art. 58 stk. 1 |
| Change of home/fixed establishment removing Denmark's identification-MS status | **Date of the establishment change** (not next quarter) | Momsforordningen art. 58 stk. 2 |

The exclusion service MUST accept an `establishmentChangeDate: LocalDate?` parameter. If non-null, the second rule applies and `valid_to = establishmentChangeDate`. A non-null `changeReason` is mandatory when this overrides the quarter rule.

```
Source: FR-OSS-02-033, FR-OSS-02-034, AC-OSS-02-18, AC-OSS-02-22
```

### 3.6 Voluntary Deregistration Effective Date Rules

**Legal basis:** ML § 66i; Momsforordningen art. 57g stk. 1

```
INPUTS:
  notificationDate : LocalDate — date notification submitted
  currentQuarterEnd: LocalDate — last day of current calendar quarter

ALGORITHM:
  daysBeforeQuarterEnd = DAYS_BETWEEN(notificationDate, currentQuarterEnd)  [inclusive of currentQuarterEnd]
  IF daysBeforeQuarterEnd >= 15 THEN
    effectiveDate = first day of NEXT quarter
  ELSE
    effectiveDate = first day of the quarter AFTER next
    → communicate revised effective date to taxable person
```

**Boundary cases (MUST all pass):**

| Notification date | Quarter end | Days before | Effective date | Rule |
|---|---|---|---|---|
| 2024-03-14 | 2024-03-31 | 17 | 2024-04-01 | Timely |
| 2024-03-17 | 2024-03-31 | 14 | 2024-04-01 | Timely (exactly 14 days — recalculate: 31-17=14 days remaining; the scenario says "exactly 15 days before 2024-03-31" at 2024-03-17 which is 14 remaining calendar days) |

> **Clarification on the "exactly 15 days" scenario:** The feature file states: notification on 2024-03-17 is "exactly 15 days before 2024-03-31". Counting: 17 Mar to 31 Mar inclusive = 15 days remaining (17, 18 … 31). The threshold check is `DAYS_BETWEEN(notificationDate, quarterEnd inclusive) >= 15`. 2024-03-17 with quarterEnd 2024-03-31: `31 - 17 = 14` exclusive, or `15` inclusive. **Use inclusive counting: `(quarterEnd - notificationDate).toDays() >= 14` where 14 represents "the 15th day is the quarter end itself".** Verify against both scenarios in the feature file before finalising the comparison operator.

| 2024-03-20 | 2024-03-31 | 11 | 2024-07-01 | Late → deferred one quarter |

```
Source: FR-OSS-02-027–029, AC-OSS-02-15, AC-OSS-02-16
```

### 3.7 VAT Number 8-Day SLA

**Legal basis:** Momsbekendtgørelsen § 116 stk. 3, § 117 stk. 5; Momssystemdirektivet art. 362, art. 369d

```
INPUTS:
  completeInformationReceivedAt : OffsetDateTime
  currentDateTime               : OffsetDateTime

RULE:
  deadline = completeInformationReceivedAt + 8 calendar days
  IF currentDateTime <= deadline AND vatNumber NOT YET ASSIGNED:
    → OK (assignment in progress)
  IF vatNumber ASSIGNED by deadline:
    → ACTIVE; notify taxable person electronically
  IF deadline REACHED and vatNumber NOT ASSIGNED:
    → send delay notification with expected assignment date
    → registration remains PENDING_VAT_NUMBER
```

**EU-scheme special rule (FR-OSS-02-016):**
- If `Registrant.schemeType == EU` AND the taxable person already holds a Danish VAT number (verified by external reference or provided in registration): do NOT assign a new number. Link the existing number. Set status to `ACTIVE` immediately.

**FR-OSS-02-018 — Ordinary DK registration ceases:**
- If EU scheme uses the taxable person's ordinary Danish VAT number and that ordinary registration ceases: assign a new EU-scheme VAT number. Set `vatNumberUsageScope = EU_SCHEME`. This is triggered externally (not self-service).

```
Source: FR-OSS-02-013–019, AC-OSS-02-07–10
```

### 3.8 Transitional Provision (FR-OSS-02-038)

**Legal basis:** Lov nr. 810/2020; transitional deadline = 1 April 2022

```
CONDITION:
  Registrant registered BEFORE 2021-07-01
  AND Registrant.transitionalUpdateSubmittedAt IS NULL
  AND LocalDate.now() >= 2022-04-01

EFFECT:
  Registrant.transitionalUpdateOverdue = true
  Flag visible in compliance review queue
  Prevents new quarterly return periods from being opened (return-service checks this flag via API)

CLEARING:
  When complete identification update submitted:
    Registrant.transitionalUpdateSubmittedAt = LocalDate.now()
    Registrant.transitionalUpdateOverdue = false
```

The scheduled job that evaluates this condition is specified in section 4.3.

```
Source: FR-OSS-02-038, AC-OSS-02-25
```

---

## 4. Service Layer

**Package:** `dk.osm2.registration.service`

### 4.1 `RegistrationService`

**Responsibility:** Orchestrates the registration lifecycle. Accepts registration requests, delegates date calculation, persists entities, enforces state machine.

**Methods:**

```java
/**
 * Submit a new Non-EU or EU scheme registration notification.
 *
 * @param command  validated registration command (from DTO)
 * @return         created Registrant with assigned ID and PENDING_VAT_NUMBER status
 * @throws RegistrationValidationException  if mandatory fields missing or scheme eligibility violated
 * @throws ExclusionBanActiveException      if registrant is within a 2-year exclusion ban
 */
RegistrantView submitRegistration(RegistrationCommand command);

/**
 * Record a change notification for an existing registration.
 *
 * @param registrantId  identity of the registrant
 * @param command       change data
 * @return              updated RegistrantView with change notification status
 * @throws RegistrantNotFoundException      if registrantId does not exist
 * @throws InvalidRegistrantStatusException if registrant is not ACTIVE
 */
RegistrantView recordChangeNotification(UUID registrantId, ChangeNotificationCommand command);

/**
 * Record voluntary deregistration.
 *
 * @param registrantId      identity of the registrant
 * @param notificationDate  date notification was submitted
 * @return                  DeregistrationResult with effective date and, if deferred, the revised date
 * @throws RegistrantNotFoundException      if registrantId does not exist
 * @throws InvalidRegistrantStatusException if registrant is not ACTIVE
 */
DeregistrationResult voluntaryDeregister(UUID registrantId, LocalDate notificationDate);

/**
 * Retrieve a registrant by ID.
 *
 * @throws RegistrantNotFoundException if not found
 */
RegistrantView getRegistrant(UUID registrantId);
```

**Internal steps for `submitRegistration`:**
1. Validate mandatory fields (delegate to Jakarta Validation on the command object)
2. Check scheme-specific eligibility (EU establishment check for Non-EU scheme)
3. Check `ExclusionBan` for any active ban on this registrant+scheme combination
4. Compute effective date via `EffectiveDateCalculationService`
5. Persist `Registrant` (status = `PENDING_VAT_NUMBER`) and `SchemeRegistration` (`validFrom` = computed effective date)
6. Publish internal event for `VatAssignmentService` to begin 8-day SLA tracking
7. Return `RegistrantView`

```
Source: FR-OSS-02-001–008, FR-OSS-02-035, AC-OSS-02-01
```

### 4.2 `ExclusionService`

**Responsibility:** Forced exclusion decisions. Only callable by actors with the `CASEWORKER` role.

**Methods:**

```java
/**
 * Record a forced exclusion decision.
 *
 * Only Skatteforvaltningen (CASEWORKER role) may call this method.
 *
 * @param registrantId          identity of the registrant
 * @param criterion             which exclusion criterion applies
 * @param decisionDate          date the exclusion decision is sent electronically to the taxable person
 * @param establishmentChangeDate  if exclusion triggered by establishment move, the date of that move; else null
 * @return                      ExclusionResult with effective date and, if PERSISTENT_NON_COMPLIANCE, the ban end date
 * @throws RegistrantNotFoundException      if registrantId does not exist
 * @throws InvalidRegistrantStatusException if registrant is not ACTIVE or CESSATION_NOTIFIED
 * @throws UnauthorisedExclusionActorException  if caller does not hold CASEWORKER role
 */
ExclusionResult recordExclusion(
    UUID registrantId,
    ExclusionCriterion criterion,
    LocalDate decisionDate,
    @Nullable LocalDate establishmentChangeDate
);
```

**Internal steps:**
1. Verify caller holds `CASEWORKER` role (via `SecurityContextHolder`); throw `UnauthorisedExclusionActorException` otherwise
2. Compute effective date:
   - If `establishmentChangeDate != null`: `validTo = establishmentChangeDate`; `changeReason` = "Backdated exclusion — establishment change per Momsforordningen art. 58 stk. 2"
   - Else: `validTo` = first day of calendar quarter following `decisionDate`; standard `changeReason`
3. Set `SchemeRegistration.validTo` via direct field assignment (NOT via `save()` on the entity with uncontrolled flushing — use a repository update method to prevent Envers from missing the change)
4. Transition `Registrant.status` to `EXCLUDED`
5. If `criterion == PERSISTENT_NON_COMPLIANCE`: create `ExclusionBan` row with `banLiftedAt = validTo + 2 years`
6. Return `ExclusionResult`

```
Source: FR-OSS-02-031–036, AC-OSS-02-18–23
```

### 4.3 `EffectiveDateCalculationService`

**Responsibility:** Pure, stateless calculation of registration effective dates. No database access. No Spring beans injected other than optional `Clock`.

**Methods:**

```java
/**
 * Compute effective date for a new registration.
 *
 * @param notificationDate   date the notification is submitted (never null)
 * @param firstDeliveryDate  date of first eligible delivery; null if none before notification
 * @return                   EffectiveDateResult
 */
EffectiveDateResult computeEffectiveDate(LocalDate notificationDate, @Nullable LocalDate firstDeliveryDate);

/**
 * Compute effective date for voluntary deregistration.
 *
 * @param notificationDate  date the deregistration notification is submitted
 * @return                  DeregistrationEffectiveDateResult (includes whether notification was timely)
 */
DeregistrationEffectiveDateResult computeDeregistrationEffectiveDate(LocalDate notificationDate);

/**
 * Compute the first day of the calendar quarter following the given date.
 * If the given date IS the first day of a quarter, returns the start of the NEXT quarter.
 *
 * @param date  reference date
 * @return      first day of following quarter
 */
LocalDate nextQuarterStart(LocalDate date);
```

**This service MUST be `@Service` and injectable, but all methods MUST be pure functions with no side effects.** It is the sole computation engine for all date arithmetic in this service. Tests MUST cover every boundary case in section 3.2 and 3.6.

```
Source: FR-OSS-02-003–005, FR-OSS-02-007–008, FR-OSS-02-028–029
```

### 4.4 `VatAssignmentService`

**Responsibility:** VAT number assignment lifecycle — tracking the 8-day SLA and sending delay notifications.

**Methods:**

```java
/**
 * Assign a VAT number to a PENDING_VAT_NUMBER registrant.
 *
 * For EU scheme: if existingDanishVatNumber is provided, link it without creating a new number.
 * For Non-EU scheme: always assign a new unique number with scope NON_EU_SCHEME_ONLY.
 *
 * @param registrantId           identity of the registrant
 * @param existingDanishVatNumber  existing DK VAT number, or null
 * @return                       updated RegistrantView (status = ACTIVE)
 * @throws RegistrantNotFoundException      if not found
 * @throws InvalidRegistrantStatusException if registrant is not PENDING_VAT_NUMBER
 */
RegistrantView assignVatNumber(UUID registrantId, @Nullable String existingDanishVatNumber);

/**
 * Scheduled job — runs daily. Finds PENDING_VAT_NUMBER registrants where:
 *   notificationSubmittedAt + 8 days <= now AND vatNumber IS NULL
 * Sends delay notification. Does NOT throw if notification sending fails — logs and continues.
 *
 * Scheduling annotation: @Scheduled(cron = "0 0 6 * * *") or configurable via properties.
 */
void checkVatAssignmentSlaBreaches();
```

**VAT number generation:**
- Non-EU scheme: prefix `EU` + 9 digits (format as per Momssystemdirektivet art. 362; exact format must be confirmed with Skatteforvaltningen before implementation)
- EU scheme (new number): follows existing Danish VAT numbering conventions; exact format TBD with Skatteforvaltningen

> **Open question for implementation team:** The exact VAT number format and generation authority (internal vs. external registry) are not specified in the petition. Do not generate mock numbers that could collide with real numbers. Raise with Product Owner before implementing `VatAssignmentService.assignVatNumber`.

```
Source: FR-OSS-02-013–019, AC-OSS-02-07–10
```

### 4.5 `TransitionalComplianceService`

**Responsibility:** Evaluates and flags pre-July-2021 registrants who have not submitted identification updates.

**Methods:**

```java
/**
 * Scheduled job — runs daily from 2022-04-01 onwards.
 * Flags registrants where:
 *   SchemeRegistration.validFrom < 2021-07-01
 *   AND Registrant.transitionalUpdateSubmittedAt IS NULL
 *   AND LocalDate.now() >= 2022-04-01
 * Sets Registrant.transitionalUpdateOverdue = true.
 *
 * Idempotent: already-flagged registrants are not re-processed.
 */
void evaluateTransitionalCompliance();

/**
 * Record a transitional identification update for a registrant.
 * Clears the overdue flag and records the submission date.
 *
 * @throws RegistrantNotFoundException if not found
 */
RegistrantView recordTransitionalUpdate(UUID registrantId, TransitionalUpdateCommand command);
```

```
Source: FR-OSS-02-038, AC-OSS-02-25
```

---

## 5. REST API

**Package:** `dk.osm2.registration.controller`
**Base path:** `/api/v1/registrations`
**Security:** All endpoints require a valid JWT from `keycloak-oauth2-starter`. Role-based constraints noted per endpoint.

### 5.1 Endpoints

#### `POST /api/v1/registrations`

**Operation:** Submit new registration (Non-EU or EU scheme)
**Roles:** `TAXABLE_PERSON`, `INTERMEDIARY`, `CASEWORKER`
**Request body:** `RegistrationRequest` (see 5.2)
**Responses:**

| HTTP Status | Condition | Body |
|---|---|---|
| `201 Created` | Registration accepted | `RegistrantResponse` |
| `400 Bad Request` | Missing mandatory field | `ErrorResponse` with `MISSING_REQUIRED_FIELD:<fieldName>` |
| `409 Conflict` | Exclusion ban active | `ErrorResponse` with `EXCLUSION_BAN_ACTIVE`, `banLiftedAt` |
| `422 Unprocessable Entity` | Business rule violation (e.g. EU establishment present for Non-EU) | `ErrorResponse` with reason code |

---

#### `GET /api/v1/registrations/{registrantId}`

**Operation:** Retrieve a registrant by ID
**Roles:** `TAXABLE_PERSON`, `INTERMEDIARY`, `CASEWORKER`
**Responses:**

| HTTP Status | Condition | Body |
|---|---|---|
| `200 OK` | Found | `RegistrantResponse` |
| `404 Not Found` | No registrant with that ID | `ErrorResponse` |

---

#### `POST /api/v1/registrations/{registrantId}/changes`

**Operation:** Submit change notification
**Roles:** `TAXABLE_PERSON`, `INTERMEDIARY`, `CASEWORKER`
**Request body:** `ChangeNotificationRequest` (see 5.2)
**Responses:**

| HTTP Status | Condition | Body |
|---|---|---|
| `200 OK` | Change recorded | `ChangeNotificationResponse` with `notificationStatus` (TIMELY or LATE_NOTIFICATION) |
| `404 Not Found` | Registrant not found | `ErrorResponse` |
| `409 Conflict` | Registrant not in ACTIVE status | `ErrorResponse` with current status |

---

#### `POST /api/v1/registrations/{registrantId}/deregister`

**Operation:** Voluntary deregistration
**Roles:** `TAXABLE_PERSON`, `INTERMEDIARY`, `CASEWORKER`
**Request body:** `DeregistrationRequest`
**Responses:**

| HTTP Status | Condition | Body |
|---|---|---|
| `200 OK` | Deregistration recorded | `DeregistrationResponse` with effective date and whether deferred |
| `404 Not Found` | Registrant not found | `ErrorResponse` |
| `409 Conflict` | Registrant not in ACTIVE status | `ErrorResponse` |

---

#### `POST /api/v1/registrations/{registrantId}/exclusions`

**Operation:** Record forced exclusion (Skatteforvaltningen only)
**Roles:** `CASEWORKER` **only**. Return `403 Forbidden` for all other roles.
**Request body:** `ExclusionRequest` (see 5.2)
**Responses:**

| HTTP Status | Condition | Body |
|---|---|---|
| `201 Created` | Exclusion recorded | `ExclusionResponse` with effective date, criterion, and `banLiftedAt` if applicable |
| `403 Forbidden` | Caller does not hold `CASEWORKER` role | `ErrorResponse` with `UNAUTHORISED_EXCLUSION_ACTOR` |
| `404 Not Found` | Registrant not found | `ErrorResponse` |
| `409 Conflict` | Registrant not in ACTIVE or CESSATION_NOTIFIED status | `ErrorResponse` |

---

#### `POST /api/v1/registrations/{registrantId}/vat-assignment`

**Operation:** Assign VAT number (internal or CASEWORKER)
**Roles:** `CASEWORKER`, `SERVICE`
**Request body:** `VatAssignmentRequest`
**Responses:**

| HTTP Status | Condition | Body |
|---|---|---|
| `200 OK` | VAT number assigned; status → ACTIVE | `RegistrantResponse` |
| `404 Not Found` | Registrant not found | `ErrorResponse` |
| `409 Conflict` | Registrant not in PENDING_VAT_NUMBER | `ErrorResponse` |

---

#### `POST /api/v1/registrations/{registrantId}/transitional-update`

**Operation:** Submit mandatory identification update for pre-July-2021 registrant
**Roles:** `TAXABLE_PERSON`, `INTERMEDIARY`, `CASEWORKER`
**Request body:** `TransitionalUpdateRequest`
**Responses:**

| HTTP Status | Condition | Body |
|---|---|---|
| `200 OK` | Update recorded; flag cleared | `RegistrantResponse` |
| `404 Not Found` | Registrant not found | `ErrorResponse` |

---

#### `GET /api/v1/registrations/{registrantId}/binding-period`

**Operation:** Expose EU binding period details
**Roles:** `TAXABLE_PERSON`, `INTERMEDIARY`, `CASEWORKER`
**Responses:**

| HTTP Status | Condition | Body |
|---|---|---|
| `200 OK` | Found | `BindingPeriodResponse` with `bindingStart`, `bindingEnd`, `bindingRuleType` |
| `404 Not Found` | Registrant not found | `ErrorResponse` |
| `200 OK` (with null fields) | Non-EU registrant; `NOT_APPLICABLE` | `BindingPeriodResponse` with `bindingRuleType = NOT_APPLICABLE` |

---

### 5.2 Request / Response DTOs

All DTOs are Java `record` types in package `dk.osm2.registration.dto`.

#### `RegistrationRequest`

```java
record RegistrationRequest(
    @NotNull SchemeType schemeType,
    @NotBlank String legalName,
    @NotBlank @Size(min=2, max=2) String countryCode,
    @NotBlank String homeTaxNumber,                          // Non-EU: mandatory; EU: optional
    @NotBlank String postalAddress,
    @NotBlank @Email String contactEmail,
    @NotBlank String contactPerson,
    @NotBlank String contactPhone,
    @NotBlank String bankDetails,
    List<String> websites,                                   // nullable / empty OK
    List<String> tradingNames,                               // nullable / empty OK
    List<PriorRegistrationDto> priorRegistrations,           // nullable / empty OK
    LocalDate firstDeliveryDate,                             // nullable if no prior delivery
    LocalDate notificationDate,                              // defaults to today if null
    // EU-scheme only fields — ignored for NON_EU
    Boolean electronicInterface,
    Boolean jointRegistration,
    Boolean notEstablishedInEu,
    List<EuEstablishmentDto> fixedEstablishments,
    List<EuDispatchDto> dispatchLocations,
    List<String> otherMemberStateVatNumbers,
    EuBindingCaseType euBindingCase                         // CASE_A, CASE_B, CASE_C; required for EU scheme
)
```

> **Validation note:** For `schemeType = NON_EU`, `homeTaxNumber` is mandatory (Momsbekendtgørelsen § 116 stk. 1 nr. 2). For `schemeType = EU`, `electronicInterface` declaration is required (FR-OSS-02-011). Missing these triggers `MISSING_REQUIRED_FIELD` or `MISSING_DECLARATION` error codes. Implement custom `@NonEuSchemeValid` and `@EuSchemeValid` constraint validators — do NOT use a single `@Valid` on the record, as the mandatory field set differs by scheme type.

#### `RegistrantResponse`

```java
record RegistrantResponse(
    UUID id,
    SchemeType schemeType,
    RegistrantStatus status,
    String vatNumber,                // null until assigned
    VatNumberUsageScope vatNumberUsageScope,  // null until assigned
    String countryCode,
    String legalName,
    LocalDate bindingStart,          // null for NON_EU or case (a)
    LocalDate bindingEnd,            // null for NON_EU or case (a)
    EuBindingRuleType bindingRuleType,
    boolean transitionalUpdateOverdue,
    LocalDate transitionalUpdateSubmittedAt,
    String legalBasis                // e.g. "ML §§ 66b / MSD art. 362"
)
```

#### `ChangeNotificationRequest`

```java
record ChangeNotificationRequest(
    LocalDate changeOccurredDate,    // @NotNull — when the change occurred in the real world
    LocalDate notificationDate,      // @NotNull — when this notification is submitted
    String postalAddress,
    @Email String contactEmail,
    String contactPhone,
    String bankDetails,
    List<String> websites,
    List<String> tradingNames,
    String cessationReason           // non-null if submitting cessation notification (FR-OSS-02-025)
)
```

#### `ChangeNotificationResponse`

```java
record ChangeNotificationResponse(
    UUID registrantId,
    ChangeNotificationStatus notificationStatus,  // TIMELY or LATE_NOTIFICATION
    LocalDate changeOccurredDate,
    LocalDate notificationDate,
    String legalBasis   // "ML § 66b stk. 6 / Momsforordningen art. 57h"
)
```

#### `DeregistrationRequest`

```java
record DeregistrationRequest(
    @NotNull LocalDate notificationDate
)
```

#### `DeregistrationResponse`

```java
record DeregistrationResponse(
    UUID registrantId,
    LocalDate effectiveDate,
    boolean notificationTimely,
    boolean deferred,
    String legalBasis   // "ML § 66i / Momsforordningen art. 57g stk. 1"
)
```

#### `ExclusionRequest`

```java
record ExclusionRequest(
    @NotNull ExclusionCriterion criterion,
    @NotNull LocalDate decisionDate,
    LocalDate establishmentChangeDate  // nullable; if non-null, triggers art. 58 stk. 2 rule
)
```

#### `ExclusionResponse`

```java
record ExclusionResponse(
    UUID registrantId,
    ExclusionCriterion criterion,
    LocalDate exclusionEffectiveDate,
    LocalDate banLiftedAt,           // null unless criterion = PERSISTENT_NON_COMPLIANCE
    String legalBasis                // "ML § 66j / Momsforordningen art. 58"
)
```

#### `VatAssignmentRequest`

```java
record VatAssignmentRequest(
    String existingDanishVatNumber   // nullable; if provided, links rather than creates new
)
```

#### `ErrorResponse`

```java
record ErrorResponse(
    String errorCode,     // machine-readable code (e.g. "EXCLUSION_BAN_ACTIVE", "MISSING_REQUIRED_FIELD:bank_details")
    String message,       // human-readable explanation
    Map<String, Object> details,  // structured detail (e.g. {"banLiftedAt": "2026-06-30"})
    String legalBasis     // relevant ML § / MSD art. where applicable; null if not applicable
)
```

---

## 6. Persistence

**Package:** `dk.osm2.registration.repository`

### 6.1 JPA Repository Interfaces

```java
// Returns active registration for a registrant in a given scheme (validTo IS NULL)
interface SchemeRegistrationRepository extends JpaRepository<SchemeRegistration, UUID> {
    Optional<SchemeRegistration> findByRegistrantIdAndSchemeTypeAndValidToIsNull(UUID registrantId, SchemeType schemeType);
    List<SchemeRegistration> findByRegistrantIdAndSchemeType(UUID registrantId, SchemeType schemeType);
}

interface RegistrantRepository extends JpaRepository<Registrant, UUID> {
    Optional<Registrant> findByVatNumberAndCountryCode(String vatNumber, String countryCode);
    List<Registrant> findByStatusAndTransitionalUpdateOverdueIsTrue(RegistrantStatus status);
    // For transitional compliance job:
    @Query("""
        SELECT r FROM Registrant r
        JOIN SchemeRegistration sr ON sr.registrant.id = r.id
        WHERE sr.validFrom < :cutoffDate
          AND r.transitionalUpdateSubmittedAt IS NULL
          AND r.transitionalUpdateOverdue = false
        """)
    List<Registrant> findPreCutoffRegistrantsWithoutUpdate(@Param("cutoffDate") LocalDate cutoffDate);
}

interface ExclusionBanRepository extends JpaRepository<ExclusionBan, UUID> {
    boolean existsByRegistrantIdAndSchemeTypeAndBanLiftedAtGreaterThanEqual(
        UUID registrantId, SchemeType schemeType, LocalDate today);
}
```

### 6.2 Hibernate Envers Configuration

Entities annotated with `@Audited`:
- `Registrant` — full audit; captures all field changes including status transitions
- `SchemeRegistration` — full audit; captures `valid_from`, `valid_to` changes (critical for bitemporal queries)

Entities NOT annotated with `@Audited`:
- `ExclusionBan` — append-only; Envers overhead not justified

**`@Audited` placement:**
```java
@Entity
@Audited
@Table(name = "registrant", schema = "registration")
public class Registrant extends AuditableEntity { ... }

@Entity
@Audited
@Table(name = "scheme_registration", schema = "registration")
public class SchemeRegistration extends AuditableEntity { ... }
```

**Envers configuration** (in `application.yml`):
```yaml
spring:
  jpa:
    properties:
      hibernate:
        envers:
          audit_table_suffix: _AUD
          store_data_at_delete: true
          default_schema: registration
```

**`REVINFO` table:** Created automatically by Envers in the `registration` schema. Confirm Flyway is NOT manually managing this table (Envers owns it).

### 6.3 Flyway Migrations

| Migration | File | Content |
|---|---|---|
| V1 | `V1__init.sql` | Core tables: `registrant`, `scheme_registration`, `exclusion_ban`, `intermediary`, `principal` |
| V2 | `V2__audit_columns.sql` | `set_audit_context()` function; `created_by`, `updated_by`, `version` columns |
| **V3 (required)** | `V3__registration_status_enum.md` | Add `PENDING_VAT_NUMBER`, `CESSATION_NOTIFIED` to `registrant_status` enum. Remove `PENDING`, `SUSPENDED` if no data exists in those states. Add `vat_number_usage_scope`, `binding_rule_type`, `transitional_update_overdue`, `transitional_update_submitted_at` columns to `registrant`. |

> **V3 is not written here** — it is the implementation team's responsibility based on this spec. The team MUST verify the V1/V2 enum values before dropping `PENDING` and `SUSPENDED`.

### 6.4 JPA Column Mapping Notes

- `validFrom` and `validTo` on `SchemeRegistration` map to `DATE` columns (not `TIMESTAMPTZ`). Use `LocalDate`.
- `notificationSubmittedAt` maps to `TIMESTAMPTZ`. Use `OffsetDateTime`.
- `Registrant.bindingStart`, `bindingEnd` map to `DATE`. Use `LocalDate`.
- `Registrant.transitionalUpdateSubmittedAt` maps to `DATE`. Use `LocalDate`.
- The `registration` schema is set at the datasource level via `spring.jpa.properties.hibernate.default_schema: registration`. Entities need NOT repeat `schema =` in `@Table` if the project default is `registration`, but it is safer to be explicit.

---

## 7. Cross-Cutting Concerns

### 7.1 Audit Context

**Source:** ADR-0013, V2 migration

On every HTTP request, `AuditContextFilter` (provided by `audit-trail-commons`, `@Order(100)`) calls:
```sql
SELECT public.set_audit_context(userId, clientIp::inet, applicationName)
```

This populates PostgreSQL session variables. The registration service does NOT need to implement this — it is provided by the library auto-configuration.

`AuditorAware<String>` is auto-configured by `audit-trail-commons`. It extracts the `sub` claim from the JWT. For `demo`/`dev`/`local` profiles (permissive filter chain, no JWT), it returns `"anonymous"`.

**No custom `AuditorAware` bean.** Do not override the library-provided bean.

### 7.2 Demo / Dev Mode

**Source:** ADR-0005

When `local`, `dev`, or `demo` profile is active, `keycloak-oauth2-starter` activates `keycloakPermissiveFilterChain` — all requests are permitted without a JWT.

Under this mode:
- `SecurityContextHolder` contains no authentication
- `AuditorAware` returns `"anonymous"`
- Role-based checks in `ExclusionService` will receive an empty `SecurityContext`. The `ExclusionService` MUST handle this gracefully in non-production profiles: log a warning and permit the call (do not throw `UnauthorisedExclusionActorException` in demo mode). Use `@Profile("!demo & !local & !dev")` on the security assertion or check the active profiles via `Environment`.

### 7.3 Input Validation

- Use `@Valid` on `@RequestBody` parameters in controllers
- Implement two custom `ConstraintValidator` classes:
  - `NonEuSchemeValidator`: validates `homeTaxNumber` present when `schemeType = NON_EU`
  - `EuSchemeValidator`: validates `electronicInterface` non-null, `euBindingCase` non-null, when `schemeType = EU`
- All Jakarta constraint violations MUST produce an `ErrorResponse` with `errorCode = "MISSING_REQUIRED_FIELD:<fieldName>"` or `"MISSING_DECLARATION:<declarationName>"` — not the default Spring `MethodArgumentNotValidException` body
- Implement a `@RestControllerAdvice` `GlobalExceptionHandler` that maps:
  - `MethodArgumentNotValidException` → `ErrorResponse` (HTTP 400)
  - `RegistrantNotFoundException` → `ErrorResponse` (HTTP 404)
  - `ExclusionBanActiveException` → `ErrorResponse` (HTTP 409) with `banLiftedAt` in `details`
  - `InvalidRegistrantStatusException` → `ErrorResponse` (HTTP 409) with current status in `details`
  - `UnauthorisedExclusionActorException` → `ErrorResponse` (HTTP 403)
  - `EuBindingPeriodActiveException` → `ErrorResponse` (HTTP 409) with `bindingEnd` in `details`
  - Unhandled `Exception` → `ErrorResponse` (HTTP 500) — no stack traces in body

### 7.4 OpenAPI Documentation

Every controller method MUST carry:
```java
@Operation(summary = "...", description = "... Legal basis: ML § ...")
@ApiResponse(responseCode = "201", description = "...")
@ApiResponse(responseCode = "400", description = "...", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
// ... etc. for all response codes
```

The `legalBasis` field is MANDATORY in every success response DTO. It is informational and carries the ML § and MSD art. references for the operation performed.

### 7.5 Observability

**Source:** `application.yml` — already configured; no additional implementation required.

- Structured JSON logging via Logstash encoder (`logging.structured.format.console: logstash`)
- Prometheus metrics exposed at `/actuator/prometheus`
- OpenTelemetry traces exported to OTLP endpoint
- Health endpoint at `/actuator/health` (details shown to authorized users)

No additional instrumentation is required for this petition. Do NOT add custom metrics unless a future petition explicitly requests them.

---

## 8. Traceability Matrix

| Requirement | Specification section | Gherkin coverage |
|---|---|---|
| FR-OSS-02-001 | §4.1 `submitRegistration`, §5.1 POST | Scenario: "Non-EU scheme registration accepted for eligible applicant" |
| FR-OSS-02-002 | §5.1 POST 422 response, §5.2 custom validator | Scenario: "Non-EU scheme registration rejected when applicant has EU fixed establishment" |
| FR-OSS-02-003 | §3.2, §4.3 `computeEffectiveDate` | Scenarios: "Normal effective date", "Notification on first day of quarter" |
| FR-OSS-02-004 | §3.2 EARLY_DELIVERY_EXCEPTION | Scenarios: "Effective date is first delivery date", "Exactly on day 10" |
| FR-OSS-02-005 | §3.2 QUARTER_START (late) | Scenario: "Late notification after early delivery forfeits exception" |
| FR-OSS-02-006–008 | §3.2, §4.1, §5.1 | EU scheme early-delivery scenarios |
| FR-OSS-02-009 | §5.2 `RegistrationRequest` validation, §7.3 | Scenario: "Non-EU registration rejected when mandatory field missing" |
| FR-OSS-02-010–012 | §5.2 EU validation, `@EuSchemeValid` | EU mandatory field and declaration scenarios |
| FR-OSS-02-013–015 | §4.4 `assignVatNumber`, §3.7 | VAT number assignment and NON_EU_SCHEME_ONLY flag scenarios |
| FR-OSS-02-016 | §4.4 EU existing DK VAT number path | Scenario: "EU scheme applicant with existing Danish VAT number uses that number" |
| FR-OSS-02-017–019 | §4.4 SLA and delay notification | EU new number and delay notification scenarios |
| FR-OSS-02-020–022 | §3.3, §2.1.4, `Registrant.bindingEnd` | Binding period scenarios |
| FR-OSS-02-023–026 | §4.1 `recordChangeNotification`, §5.1 POST /changes | Change notification scenarios |
| FR-OSS-02-027–030 | §3.6, §4.1 `voluntaryDeregister`, §5.1 POST /deregister | Voluntary deregistration scenarios |
| FR-OSS-02-031–036 | §3.4–3.5, §4.2 `ExclusionService` | Forced exclusion scenarios |
| FR-OSS-02-037 | §3.1 state machine, §4.2 | Scheme switching scenarios |
| FR-OSS-02-038 | §3.8, §4.5, §5.1 POST /transitional-update | Transitional provision scenarios |

---

## 9. Open Questions (Blockers Before Implementation)

> These MUST be resolved before a build engineer begins implementation. Raise with Product Owner.

| # | Question | Impact | Source |
|---|---|---|---|
| OQ-1 | Exact VAT number format for Non-EU and new EU scheme numbers. Internal generation vs. external registry? | `VatAssignmentService.assignVatNumber` cannot be implemented without this. | FR-OSS-02-013, FR-OSS-02-017 |
| OQ-2 | `RegistrantStatus` enum reconciliation: V1 schema has `PENDING`/`SUSPENDED`. Are there any rows in these states in any environment that would block a V3 migration dropping them? | V3 Flyway migration content | V1__init.sql vs. §2.1.1 |
| OQ-3 | "Exactly 15 days before quarter end" boundary: inclusive vs. exclusive day counting. Feature file scenario implies inclusive. Confirm with legal/compliance before writing the comparator. | `computeDeregistrationEffectiveDate` off-by-one risk | §3.6 |
| OQ-4 | Delay notification channel: what mechanism does the system use to send electronic notifications (email, message queue, external notification service)? `VatAssignmentService.checkVatAssignmentSlaBreaches` needs a concrete dependency. | §4.4 SLA breach notification | FR-OSS-02-014, FR-OSS-02-019 |
| OQ-5 | Scheme-switch scenario (FR-OSS-02-037): is this a single atomic API call, or two separate calls (exclusion + registration)? If atomic, `ExclusionService` and `RegistrationService` must participate in the same transaction. | Transaction boundary design | FR-OSS-02-037 |

---

*End of OSS-02-SPEC. Every item in this document traces directly to petition OSS-02 or its outcome contract. No items have been added beyond the stated requirements.*
