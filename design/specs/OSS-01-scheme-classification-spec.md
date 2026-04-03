# OSS-01 — Scheme Classification: Implementation Specification

| Field | Value |
|---|---|
| Specification ID | OSS-01-SPEC |
| Petition | OSS-01 |
| Outcome Contract | OSS-01-outcome-contract.md |
| Feature File | classpath:features/OSS-01 |
| Status | APPROVED FOR IMPLEMENTATION |
| Package Root | `dk.osm2.scheme` |

---

## 1. Scope and Constraints

This specification covers exactly what is stated in petition OSS-01 and its outcome contract. Nothing beyond that scope is specified here.

| Constraint | Rule |
|---|---|
| Classification is **stateless** | No database writes during `classify`. Reference tables (`scheme_type`, `eligibility_rule`) exist but are not read during stateless classification. |
| Security | `keycloak-oauth2-starter` provides `SecurityFilterChain`. Do **not** declare one manually. Do **not** add `@PreAuthorize`. |
| DTOs | Java `record` types. Do **not** extend `AuditableEntity` (that is JPA-only). |
| €150 threshold | Inclusive: `shipmentValue ≤ 150` is eligible. Exactly `150.00` = eligible (AC-08). |
| Excisable goods | Always excluded from Import regardless of shipment value (AC-09). |
| Legal basis | Every `SchemeClassificationResult` must carry at least one entry in `legalBasis`. Format: `"ML § <para> / MSD artikel <art>"`. |
| Technology | Java 21, Spring Boot 3.5.0, Drools 9.44.0.Final, Maven multi-module. |
| ADR-0031 | `SchemeType` enum codes are `NON_EU`, `EU`, `IMPORT` — statutory, do not rename. |
| ADR-0032 | Drools is the runtime engine. Catala is the formal oracle, **typechecked in CI**; it is **not** invoked by `mvn verify`. |
| ADR-0004 | API-first. All endpoints must be documented with OpenAPI annotations. |

---

## 2. Domain Model

**Package:** `dk.osm2.scheme.domain`

### 2.1 `SchemeType` enum

Represents the three OSS schemes defined in ML §§ 66–66u and MSD arts. 358–369l.

| Constant | ML Reference | MSD Reference |
|---|---|---|
| `NON_EU` | ML § 66a | MSD art. 358a |
| `EU` | ML § 66d | MSD art. 369a |
| `IMPORT` | ML § 66m | MSD art. 369l |

```
Source: FR-01, ADR-0031
```

### 2.2 `SupplyType` enum

Discriminates the type of supply provided in the classification request.

| Constant | Meaning |
|---|---|
| `SERVICES` | Supply of services |
| `DISTANCE_SALES_INTRA_EU` | Intra-EU distance sales of goods |
| `IMPORT_GOODS` | Distance sales of goods imported from outside the EU |

```
Source: FR-01, FR-02, FR-05
```

### 2.3 `ClassificationStatus` enum

The top-level outcome of a classification request.

| Constant | Meaning |
|---|---|
| `ELIGIBLE` | Taxable person is eligible for the determined scheme |
| `INELIGIBLE` | Criteria for any OSS scheme are not met (e.g., value > €150, excisable) |
| `INSUFFICIENT_INFORMATION` | Required fields are absent; classification cannot proceed |
| `NO_OSS_SCHEME` | Supply characteristics exclude OSS (e.g., same-country supplier/consumer) |

```
Source: FR-01, FR-06, AC-20, AC-21
```

---

## 3. DTOs (Java Records)

**Package:** `dk.osm2.scheme.dto`

DTOs are `record` types. They are **not** JPA entities and must **not** extend `AuditableEntity`.

### 3.1 `SchemeClassificationRequest`

```
Input to POST /api/v1/schemes/classify
Source: FR-01 through FR-08, AC-10 through AC-21
```

| Field | Type | Nullable | Description | Source |
|---|---|---|---|---|
| `hasEuSeatOfEconomicActivity` | `Boolean` | No | Whether the taxable person's seat of economic activity is in the EU | FR-01, FR-03, FR-04 |
| `seatOfEconomicActivityCountry` | `String` | Yes | ISO 3166-1 alpha-2 country code of seat; `null` if no EU seat | FR-04 (priority a) |
| `hasFixedEstablishmentInEu` | `Boolean` | No | Whether the taxable person has at least one fixed establishment in the EU | FR-01, FR-03, FR-04 |
| `fixedEstablishmentCountries` | `List<String>` | Yes | ISO country codes of all EU fixed establishments; empty or `null` if none | FR-04 (priorities b, c) |
| `supplyType` | `SupplyType` | No | Type of supply being classified | FR-01 |
| `consumptionMemberState` | `String` | Yes | ISO country code of consumption MS (place of supply per ML ch. 4 for services; shipment destination for goods) | FR-03, FR-04, FR-05 |
| `shipmentStartCountry` | `String` | Yes | ISO country code where shipment originates; used for EU scheme same-country rule (FR-04 rule c) | FR-04 |
| `excisable` | `Boolean` | Yes | Whether the goods are excisable; required when `supplyType == IMPORT_GOODS` | FR-06, AC-09 |
| `shipmentValue` | `BigDecimal` | Yes | Real shipment value in EUR; required when `supplyType == IMPORT_GOODS` | FR-06, AC-08, AC-21 |
| `hasIntermediary` | `Boolean` | Yes | Whether an intermediary has been appointed; relevant for Import scheme | FR-05 |
| `intermediaryEstablishmentCountry` | `String` | Yes | ISO country code of intermediary's EU establishment; relevant when `hasIntermediary == true` | FR-05, AC-15 |
| `selectedIdentificationMemberState` | `String` | Yes | ISO country code chosen by taxable person when free choice is available (Non-EU scheme; Import, no EU FE) | FR-03 (AC-10), FR-05 |
| `viaElectronicInterface` | `Boolean` | Yes | Whether the supply is made via an electronic interface per ML § 4c stk. 2 | FR-02, FR-04 |
| `enrolledScheme` | `SchemeType` | Yes | The scheme the taxable person is currently enrolled in; used for rule-hierarchy queries | FR-07, AC-17, AC-18 |
| `queryRuleHierarchy` | `Boolean` | Yes | When `true`, the response includes `applicableRules` describing the rule hierarchy (FR-07) | FR-07 |

**Validation invariants** (enforced before rule execution, not as Jakarta Bean Validation):

| Invariant | Condition | Result if violated |
|---|---|---|
| V-01 | `hasEuSeatOfEconomicActivity == null \|\| hasFixedEstablishmentInEu == null` | `INSUFFICIENT_INFORMATION`, message: `"Utilstrækkelige oplysninger — etableringsland eller etableringstype mangler"` (AC-20) |
| V-02 | `supplyType == IMPORT_GOODS && shipmentValue == null` | `INSUFFICIENT_INFORMATION`, message: `"Utilstrækkelige oplysninger — reel forsendelsesværdi skal angives"` (AC-21) |

---

### 3.2 `SchemeClassificationResult`

```
Output from POST /api/v1/schemes/classify
Source: FR-08, AC-01 through AC-21
```

| Field | Type | Nullable | Description | Source |
|---|---|---|---|---|
| `status` | `ClassificationStatus` | No | Overall classification outcome | FR-01, FR-06 |
| `scheme` | `SchemeType` | Yes | Applicable OSS scheme; `null` when `status` is `INELIGIBLE`, `INSUFFICIENT_INFORMATION`, or `NO_OSS_SCHEME` | FR-01 |
| `identificationMemberState` | `String` | Yes | Determined identification MS (ISO country code); `null` when no scheme | FR-03, FR-04, FR-05 |
| `consumptionMemberState` | `String` | Yes | Determined consumption MS (ISO country code); `null` when no scheme | FR-03, FR-04, FR-05 |
| `legalBasis` | `List<String>` | No | One or more legal basis references; every eligible result must have at least one entry | FR-08, AC-10 through AC-15 |
| `message` | `String` | Yes | Human-readable Danish message; required for rejection and insufficient-information outcomes | AC-20, AC-21, FR-06 |
| `applicableRules` | `List<String>` | Yes | Rule hierarchy descriptions; populated only when `queryRuleHierarchy == true` | FR-07, AC-17, AC-18 |

**`legalBasis` format**: each entry is a string with form `"ML § <paragraph> / MSD artikel <article>"`. Multiple entries are allowed (e.g., scheme basis + identification MS basis + consumption MS basis).

---

## 4. Drools Rules

**File location:** `src/main/resources/rules/scheme-classification.drl`  
**Package declaration:** `dk.osm2.scheme.rules`  
**Imports:** `dk.osm2.scheme.dto.SchemeClassificationRequest`, `dk.osm2.scheme.dto.SchemeClassificationResult`, `dk.osm2.scheme.domain.SchemeType`, `dk.osm2.scheme.domain.SupplyType`, `dk.osm2.scheme.domain.ClassificationStatus`

The Drools session is **stateful** (one session per `classify` call, disposed immediately after). The `SchemeClassificationRequest` is inserted as a fact. A `SchemeClassificationResult` object (pre-constructed, inserted before rule firing) is mutated by rules and extracted after `fireAllRules`.

### 4.1 Validation Rules

These fire first and set `status = INSUFFICIENT_INFORMATION`. No scheme or identification MS is set.

| Rule Name | Condition | Action |
|---|---|---|
| `"VAL-01-missing-establishment"` | `request.hasEuSeatOfEconomicActivity() == null \|\| request.hasFixedEstablishmentInEu() == null` | `result.status = INSUFFICIENT_INFORMATION`; `result.message = "Utilstrækkelige oplysninger — etableringsland eller etableringstype mangler"` |
| `"VAL-02-missing-shipment-value"` | `request.supplyType() == IMPORT_GOODS && request.shipmentValue() == null` | `result.status = INSUFFICIENT_INFORMATION`; `result.message = "Utilstrækkelige oplysninger — reel forsendelsesværdi skal angives"` |

```
Source: AC-20, AC-21
```

### 4.2 Scheme Eligibility Rules

These fire only when `result.status` has not already been set by a validation rule.

#### 4.2.1 Non-EU Scheme (ML § 66a / MSD art. 358a)

| Rule Name | Conditions | Actions |
|---|---|---|
| `"FR-01-non-eu-scheme"` | `hasEuSeatOfEconomicActivity == false` AND `hasFixedEstablishmentInEu == false` AND `supplyType == SERVICES` | `status = ELIGIBLE`; `scheme = NON_EU`; add `"ML § 66a / MSD artikel 358a"` to `legalBasis` |

```
Source: FR-01, FR-03
```

#### 4.2.2 EU Scheme (ML § 66d / MSD art. 369a)

"EU-established" means: `hasEuSeatOfEconomicActivity == true` OR `hasFixedEstablishmentInEu == true`.

| Rule Name | Conditions | Actions |
|---|---|---|
| `"FR-01-eu-scheme-services"` | EU-established AND `supplyType == SERVICES` AND `consumptionMemberState` is not the same as the identification MS country | `status = ELIGIBLE`; `scheme = EU`; add `"ML § 66d / MSD artikel 369a"` to `legalBasis` |
| `"FR-01-eu-scheme-distance-sales"` | EU-established AND `supplyType == DISTANCE_SALES_INTRA_EU` | `status = ELIGIBLE`; `scheme = EU`; add `"ML § 66d / MSD artikel 369a"` to `legalBasis` |
| `"FR-01-eu-scheme-electronic-interface"` | EU-established AND `supplyType == IMPORT_GOODS` AND `viaElectronicInterface == true` AND `shipmentStartCountry` equals `consumptionMemberState` | `status = ELIGIBLE`; `scheme = EU`; add `"ML § 66d / MSD artikel 369a"` to `legalBasis` |
| `"FR-01-no-oss-same-country"` | EU-established AND (`supplyType == SERVICES` OR `supplyType == DISTANCE_SALES_INTRA_EU`) AND `consumptionMemberState` equals identification MS country | `status = NO_OSS_SCHEME`; `message = "Forbrug i etableringsmedlemsstat — OSS-ordningen finder ikke anvendelse"` |
| `"FR-01-non-eu-via-eu-fe"` | `hasEuSeatOfEconomicActivity == false` AND `hasFixedEstablishmentInEu == true` AND `supplyType == SERVICES` | `status = ELIGIBLE`; `scheme = EU`; add `"ML § 66d / MSD artikel 369a"` to `legalBasis` |

```
Source: FR-01, FR-02, FR-04
```

#### 4.2.3 Import Scheme (ML § 66m / MSD art. 369l)

| Rule Name | Conditions | Actions |
|---|---|---|
| `"FR-01-import-scheme"` | `supplyType == IMPORT_GOODS` AND `shipmentValue <= 150` AND (`excisable == null \|\| excisable == false`) | `status = ELIGIBLE`; `scheme = IMPORT`; add `"ML § 66m / MSD artikel 369l"` to `legalBasis` |
| `"FR-06-import-value-exceeded"` | `supplyType == IMPORT_GOODS` AND `shipmentValue > 150` | `status = INELIGIBLE`; `message = "Forsendelsesværdi overstiger €150 — importordningen finder ikke anvendelse"` |
| `"FR-06-import-excisable"` | `supplyType == IMPORT_GOODS` AND `excisable == true` | `status = INELIGIBLE`; `message = "Punktafgiftspligtige varer er udelukket fra importordningen"` |

```
Source: FR-01, FR-06, AC-08, AC-09
Note: FR-06-import-excisable has higher salience than FR-06-import-value-exceeded;
      excisable exclusion applies regardless of shipment value.
```

### 4.3 Identification MS Rules

These fire after scheme is determined, to set `identificationMemberState` on the result.

#### Non-EU Identification MS (FR-03)

| Rule Name | Conditions | Actions |
|---|---|---|
| `"FR-03-non-eu-identification-ms"` | `result.scheme == NON_EU` AND `request.selectedIdentificationMemberState != null` | `result.identificationMemberState = request.selectedIdentificationMemberState`; add `"ML § 66a, nr. 2 / MSD artikel 358a"` to `legalBasis` |

```
Source: FR-03, AC-10
Legal basis added: "ML § 66a, nr. 2 / MSD artikel 358a"
```

#### EU Identification MS (FR-04, priority order)

Rules fire in priority order (a) → (b) → (c) → (d). Each rule has a salience reflecting its priority. Only the highest-priority applicable rule fires.

| Priority | Rule Name | Conditions | Actions |
|---|---|---|---|
| (a) | `"FR-04-eu-id-ms-seat"` | `result.scheme == EU` AND `hasEuSeatOfEconomicActivity == true` AND `seatOfEconomicActivityCountry != null` | `result.identificationMemberState = seatOfEconomicActivityCountry`; add `"ML § 66d, stk. 2, nr. 1 / MSD artikel 369a"` to `legalBasis` |
| (b) | `"FR-04-eu-id-ms-single-fe"` | `result.scheme == EU` AND `hasEuSeatOfEconomicActivity == false` AND `fixedEstablishmentCountries.size() == 1` | `result.identificationMemberState = fixedEstablishmentCountries.get(0)`; add `"ML § 66d, stk. 2, nr. 2 / MSD artikel 369a"` to `legalBasis` |
| (c) | `"FR-04-eu-id-ms-multiple-fe"` | `result.scheme == EU` AND `hasEuSeatOfEconomicActivity == false` AND `fixedEstablishmentCountries.size() > 1` AND `selectedIdentificationMemberState != null` | `result.identificationMemberState = selectedIdentificationMemberState`; add `"ML § 66d, stk. 2, nr. 3 / MSD artikel 369a"` to `legalBasis` |
| (d) | `"FR-04-eu-id-ms-departure-country"` | `result.scheme == EU` AND `hasEuSeatOfEconomicActivity == false` AND `(fixedEstablishmentCountries == null \|\| fixedEstablishmentCountries.isEmpty())` AND `shipmentStartCountry != null` | `result.identificationMemberState = shipmentStartCountry`; add `"ML § 66d, stk. 2, nr. 4 / MSD artikel 369a"` to `legalBasis` |

```
Source: FR-04, AC-11, AC-12, AC-13, AC-14
```

#### Import Identification MS (FR-05)

| Priority | Rule Name | Conditions | Actions |
|---|---|---|---|
| (a) | `"FR-05-import-id-ms-no-intermediary-no-fe"` | `result.scheme == IMPORT` AND `hasIntermediary != true` AND `hasEuSeatOfEconomicActivity == false` AND `(fixedEstablishmentCountries == null \|\| fixedEstablishmentCountries.isEmpty())` AND `selectedIdentificationMemberState != null` | `result.identificationMemberState = selectedIdentificationMemberState`; add `"ML § 66m, stk. 2 / MSD artikel 369l"` to `legalBasis` |
| (b) | `"FR-05-import-id-ms-no-intermediary-eu-fe"` | `result.scheme == IMPORT` AND `hasIntermediary != true` AND `hasFixedEstablishmentInEu == true` AND `selectedIdentificationMemberState != null` | `result.identificationMemberState = selectedIdentificationMemberState`; add `"ML § 66m, stk. 2, nr. 2 / MSD artikel 369l"` to `legalBasis` |
| (c) | `"FR-05-import-id-ms-eu-seat"` | `result.scheme == IMPORT` AND `hasIntermediary != true` AND `hasEuSeatOfEconomicActivity == true` | `result.identificationMemberState = seatOfEconomicActivityCountry`; add `"ML § 66m, stk. 2, nr. 3 / MSD artikel 369l"` to `legalBasis` |
| (d) | `"FR-05-import-id-ms-intermediary-eu-seat"` | `result.scheme == IMPORT` AND `hasIntermediary == true` AND `intermediaryEstablishmentCountry != null` | `result.identificationMemberState = intermediaryEstablishmentCountry`; add `"ML § 66m, stk. 2, nr. 4 / MSD artikel 369l"` to `legalBasis` |

```
Source: FR-05, AC-15
```

### 4.4 Consumption MS Rules

These fire after scheme is determined.

| Rule Name | Conditions | Actions |
|---|---|---|
| `"FR-03-non-eu-consumption-ms"` | `result.scheme == NON_EU` AND `request.consumptionMemberState != null` | `result.consumptionMemberState = request.consumptionMemberState` |
| `"FR-04-eu-consumption-ms-services"` | `result.scheme == EU` AND `supplyType == SERVICES` | `result.consumptionMemberState = request.consumptionMemberState` (place of supply per ML ch. 4) |
| `"FR-04-eu-consumption-ms-distance-sales"` | `result.scheme == EU` AND `supplyType == DISTANCE_SALES_INTRA_EU` | `result.consumptionMemberState = request.consumptionMemberState` (shipment destination) |
| `"FR-04-eu-consumption-ms-electronic-interface"` | `result.scheme == EU` AND `viaElectronicInterface == true` AND `supplyType == IMPORT_GOODS` | `result.consumptionMemberState = request.shipmentStartCountry` (same-country: destination = origin) |
| `"FR-05-import-consumption-ms"` | `result.scheme == IMPORT` | `result.consumptionMemberState = request.consumptionMemberState` (shipment destination) |

```
Source: FR-03, FR-04, FR-05
```

### 4.5 Rule Hierarchy Rules (FR-07)

These fire only when `request.queryRuleHierarchy == true` and `result.scheme != null`.

| Rule Name | Conditions | Actions |
|---|---|---|
| `"FR-07-rule-hierarchy-oss-first"` | `queryRuleHierarchy == true` AND `scheme != null` | Add `"ML §§ 66–66u (OSS-specifikke regler) — forrang"` to `result.applicableRules` |
| `"FR-07-rule-hierarchy-ml-general"` | `queryRuleHierarchy == true` AND `scheme != null` | Add `"ML — generelle momsregler"` to `result.applicableRules` |
| `"FR-07-rule-hierarchy-opkraevningsloven"` | `queryRuleHierarchy == true` AND `scheme != null` | Add `"Opkrævningsloven"` to `result.applicableRules` |
| `"FR-07-rule-hierarchy-consumption-ms"` | `queryRuleHierarchy == true` AND `scheme != null` | Add `"Forbrugsmedlemsstatens regler"` to `result.applicableRules` |

```
Source: FR-07, AC-17, AC-18
```

### 4.6 Rule Priority (Salience) Summary

| Priority Group | Salience Value | Rules |
|---|---|---|
| Validation | 1000 | VAL-01, VAL-02 |
| Excisable exclusion | 900 | FR-06-import-excisable |
| Value exclusion | 800 | FR-06-import-value-exceeded |
| Scheme eligibility | 500 | FR-01-* |
| Identification MS (a) | 400 | FR-04-eu-id-ms-seat, FR-05-import-id-ms-eu-seat |
| Identification MS (b) | 300 | FR-04-eu-id-ms-single-fe, FR-05-import-id-ms-no-intermediary-eu-fe |
| Identification MS (c/d) | 200 | FR-04-eu-id-ms-multiple-fe, FR-05-import-id-ms-intermediary-eu-seat |
| Identification MS (fallback) | 100 | FR-04-eu-id-ms-departure-country, FR-05-import-id-ms-no-intermediary-no-fe |
| Consumption MS | 50 | FR-03/FR-04/FR-05-consumption-ms-* |
| Rule hierarchy | 10 | FR-07-rule-hierarchy-* |

---

## 5. Drools Configuration

**Class:** `dk.osm2.scheme.config.DroolsConfig`  
**Annotation:** `@Configuration`

### 5.1 `KieContainer` bean

```
@Bean
KieContainer kieContainer()
```

- Uses `KieServices.Factory.get()` to obtain `KieServices`
- Loads from `classpath:META-INF/kmodule.xml` (standard Drools Maven convention)
- Calls `kieServices.getKieClasspathContainer()` to load all DRL files on classpath under `rules/`
- Throws `IllegalStateException` if `KieContainer` cannot be built (startup-time fail-fast)

### 5.2 `KieBase` bean

```
@Bean
KieBase kieBase(KieContainer kieContainer)
```

- Returns `kieContainer.getKieBase()`
- Used by `SchemeClassificationService` to create per-request `KieSession`

### 5.3 `kmodule.xml`

**Location:** `src/main/resources/META-INF/kmodule.xml`

```xml
<kmodule xmlns="http://www.drools.org/xsd/kmodule">
  <kbase name="SchemeClassificationBase" packages="dk.osm2.scheme.rules">
    <ksession name="SchemeClassificationSession" type="stateful"/>
  </kbase>
</kmodule>
```

```
Source: ADR-0032
```

---

## 6. Service

**Class:** `dk.osm2.scheme.service.SchemeClassificationService`

### 6.1 Constructor

```
SchemeClassificationService(KieContainer kieContainer)
```

- `KieContainer` is constructor-injected (Spring `@Service`)
- No other dependencies

### 6.2 `classify` method

```
SchemeClassificationResult classify(SchemeClassificationRequest request)
```

**Pre-rule validation** (executed before creating KieSession):

1. If `request.hasEuSeatOfEconomicActivity() == null` OR `request.hasFixedEstablishmentInEu() == null`:
   - Return `SchemeClassificationResult` with `status = INSUFFICIENT_INFORMATION`, `message = "Utilstrækkelige oplysninger — etableringsland eller etableringstype mangler"`, `legalBasis = []`
2. If `request.supplyType() == IMPORT_GOODS` AND `request.shipmentValue() == null`:
   - Return `SchemeClassificationResult` with `status = INSUFFICIENT_INFORMATION`, `message = "Utilstrækkelige oplysninger — reel forsendelsesværdi skal angives"`, `legalBasis = []`

**Rule execution sequence:**

1. Create a new `KieSession` via `kieContainer.newKieSession("SchemeClassificationSession")`
2. Construct a mutable `SchemeClassificationResult` (internal mutable holder, not the record)
3. Insert `request` as a fact
4. Insert the mutable result holder as a fact (rules mutate it)
5. Call `kieSession.fireAllRules()`
6. Extract the result holder state
7. Call `kieSession.dispose()` in a `finally` block
8. Build and return the `SchemeClassificationResult` record from extracted state

**Error handling:**

| Condition | Behaviour |
|---|---|
| `KieSession` creation fails | Propagate as `IllegalStateException` — no result returned |
| `fireAllRules()` throws | Dispose session in `finally`, propagate exception |
| Request is `null` | Throw `NullPointerException` (caller contract) |

```
Source: FR-01 through FR-08, AC-20, AC-21
```

---

## 7. REST Controller

**Class:** `dk.osm2.scheme.controller.SchemeClassificationController`  
**Annotations:** `@RestController`, `@RequestMapping("/api/v1/schemes")`

### 7.1 Endpoint

| Attribute | Value |
|---|---|
| HTTP method | `POST` |
| Path | `/classify` |
| Request body | `SchemeClassificationRequest` (JSON) |
| Response body | `SchemeClassificationResult` (JSON) |
| Success status | `200 OK` |
| Content-Type | `application/json` |

**Method signature:**

```
@PostMapping("/classify")
@Operation(summary = "Classify taxable person into OSS scheme",
           description = "Classifies based on establishment, supply type, and goods characteristics per ML §§ 66–66u and MSD arts. 358–369l.")
ResponseEntity<SchemeClassificationResult> classify(
    @Valid @RequestBody SchemeClassificationRequest request)
```

**Security:** Handled entirely by `keycloak-oauth2-starter`. Do not add `@PreAuthorize`, `@Secured`, or any `SecurityFilterChain` bean.

**OpenAPI tags:** `@Tag(name = "OSS Scheme Classification")`

```
Source: ADR-0004, FR-01, FR-08
```

---

## 8. Spring Boot Application

**Class:** `dk.osm2.scheme.SchemeServiceApplication`  
**Annotation:** `@SpringBootApplication`

```
Standard main method: SpringApplication.run(SchemeServiceApplication.class, args)
```

No additional configuration on the application class is specified by OSS-01.

---

## 9. Test Infrastructure

**Package:** `dk.osm2.scheme.steps`  
**Feature classpath:** `classpath:features/OSS-01`

### 9.1 `CucumberRunnerTest`

```
Source: OSS-01.feature (27 scenarios across 5 features)
```

| Attribute | Value |
|---|---|
| Class | `dk.osm2.scheme.CucumberRunnerTest` |
| Annotations | `@Suite`, `@IncludeEngines("cucumber")`, `@SelectClasspathResource("features/OSS-01")`, `@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "dk.osm2.scheme.steps")` |
| Engine | JUnit 5 Platform Suite |

### 9.2 `CucumberSpringConfiguration`

| Attribute | Value |
|---|---|
| Class | `dk.osm2.scheme.steps.CucumberSpringConfiguration` |
| Annotations | `@CucumberContextConfiguration`, `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` |
| Container | `@Container` Testcontainers `PostgreSQLContainer` (for reference data tables if needed by future tests) |
| Profile | Spring test profile; no production security filter chain override |

### 9.3 Step Definitions

**Class:** `dk.osm2.scheme.steps.SchemeClassificationSteps`

Step definitions must cover all 27 scenarios across the 5 features. The step definitions call the service layer directly (not the REST controller) to keep steps fast and decoupled from HTTP.

| Feature | Scenarios | Step coverage |
|---|---|---|
| Feature 1: Non-EU scheme | 4 | `hasEuSeatOfEconomicActivity = false`, `hasFixedEstablishmentInEu = false`, `supplyType = SERVICES` → `NON_EU` / `NO_OSS_SCHEME` |
| Feature 2: EU scheme | 9 | EU-established combinations, services and distance sales, same-country exclusion, electronic interface rule |
| Feature 3: Import scheme | 8 | Value boundaries (≤ 150 / > 150), excisable, intermediary identification MS |
| Feature 4: Rule hierarchy | 3 | `queryRuleHierarchy = true`, enrolled scheme, `applicableRules` content |
| Feature 5: Insufficient info | 2 | Missing `hasEuSeatOfEconomicActivity`, missing `shipmentValue` for Import |

---

## 10. Acceptance Criteria Traceability

Every AC from the outcome contract must be testable from the spec above. The table below confirms traceability.

| AC | Description (summary) | Covered by |
|---|---|---|
| AC-01 | Non-EU: no EU seat, no FE, services → NON_EU | Rule FR-01-non-eu-scheme |
| AC-02 | Non-EU: EU seat → not eligible for Non-EU | Rule FR-01-non-eu-scheme (condition not met) |
| AC-03 | EU: EU seat, services, different consumption MS → EU | Rule FR-01-eu-scheme-services |
| AC-04 | EU: EU seat, services, same consumption MS → NO_OSS | Rule FR-01-no-oss-same-country |
| AC-05 | EU: EU FE (no seat), services → EU | Rule FR-01-non-eu-via-eu-fe |
| AC-06 | EU: intra-EU distance sales, EU-established → EU | Rule FR-01-eu-scheme-distance-sales |
| AC-07 | EU: electronic interface, same shipment/consumption country → EU | Rule FR-01-eu-scheme-electronic-interface |
| AC-08 | Import: shipmentValue = 150 → ELIGIBLE | Rule FR-01-import-scheme (≤ 150 inclusive) |
| AC-09 | Import: excisable = true → INELIGIBLE regardless of value | Rule FR-06-import-excisable (salience 900) |
| AC-10 | Non-EU: identification MS = freely chosen → ML § 66a, nr. 2 | Rule FR-03-non-eu-identification-ms |
| AC-11 | EU: EU seat → identification MS = seat country (priority a) | Rule FR-04-eu-id-ms-seat |
| AC-12 | EU: no EU seat, single FE → identification MS = FE country (priority b) | Rule FR-04-eu-id-ms-single-fe |
| AC-13 | EU: no EU seat, multiple FEs → identification MS = chosen FE (priority c) | Rule FR-04-eu-id-ms-multiple-fe |
| AC-14 | EU: no EU seat, no FE → identification MS = shipment departure country (priority d) | Rule FR-04-eu-id-ms-departure-country |
| AC-15 | Import: intermediary with EU establishment → identification MS = intermediary country | Rule FR-05-import-id-ms-intermediary-eu-seat |
| AC-16 | Import: no intermediary, no EU FE → identification MS = freely chosen | Rule FR-05-import-id-ms-no-intermediary-no-fe |
| AC-17 | Rule hierarchy: OSS-specific rules have precedence over ML general rules | Rules FR-07-rule-hierarchy-* |
| AC-18 | Rule hierarchy: order is OSS → ML general → Opkrævningsloven → consumption MS | Rules FR-07-rule-hierarchy-* (ordered list) |
| AC-19 | Every result carries ML § + MSD artikel reference | `legalBasis` field; enforced in all eligibility rules |
| AC-20 | Missing establishment info → INSUFFICIENT_INFORMATION + Danish message | VAL-01, `SchemeClassificationService` pre-validation |
| AC-21 | Import goods, missing shipmentValue → INSUFFICIENT_INFORMATION + Danish message | VAL-02, `SchemeClassificationService` pre-validation |

---

## 11. Package Structure

```
dk.osm2.scheme
├── SchemeServiceApplication.java                   @SpringBootApplication
├── config/
│   └── DroolsConfig.java                           @Configuration — KieContainer, KieBase beans
├── controller/
│   └── SchemeClassificationController.java         @RestController — POST /api/v1/schemes/classify
├── domain/
│   ├── ClassificationStatus.java                   enum
│   ├── SchemeType.java                             enum (ADR-0031)
│   └── SupplyType.java                             enum
├── dto/
│   ├── SchemeClassificationRequest.java            record
│   └── SchemeClassificationResult.java             record
└── service/
    └── SchemeClassificationService.java            @Service — classify()

src/main/resources/
├── META-INF/
│   └── kmodule.xml
└── rules/
    └── scheme-classification.drl

src/test/java/dk/osm2/scheme/
├── CucumberRunnerTest.java
└── steps/
    ├── CucumberSpringConfiguration.java
    └── SchemeClassificationSteps.java

src/test/resources/features/OSS-01/
    └── *.feature                                   27 scenarios across 5 features
```

---

## 12. What This Specification Does Not Cover

The following are explicitly out of scope for OSS-01 and must not be implemented:

| Out of Scope | Reason |
|---|---|
| Database writes during classify | Classification is stateless (OSS-01 constraint) |
| `SecurityFilterChain` bean | `keycloak-oauth2-starter` handles this |
| `@PreAuthorize` / `@Secured` | Starter handles authentication/authorisation |
| UI or Thymeleaf templates | OSS-01 is API-only |
| Cross-service database calls | No cross-service integration in OSS-01 |
| `AuditableEntity` on DTOs | DTOs are records, not JPA entities |
| Catala in Maven build | ADR-0032: Catala is **not** in Maven; validation is **CI** (`catala typecheck` / tests). |
| Reference table reads during classify | `scheme_type` and `eligibility_rule` tables are not read during stateless classification |
| Any FR beyond OSS-01.md | Strict traceability enforced |

---

*Specification produced from: petition OSS-01.md, OSS-01-outcome-contract.md, OSS-01.feature (27 scenarios). Every item traces to at least one source artifact. No speculation beyond petition scope.*
