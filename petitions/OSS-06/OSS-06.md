# OSS-06 — Regnskab og dokumentation

**Petition ID:** OSS-06  
**Status:** Draft  
**Created:** 2026-03-30  
**System:** osm2 — One Stop Moms (OSS) VAT system, Skatteforvaltningen  
**Legal basis:** ML §§ 66h, 66r; Momsforordningen artikel 63c (affattet ved gennemførelsesforordning (EU) 2019/2026); Momssystemdirektivet artikler 369, 369k, 369x; Direktiv 2017/2455; Direktiv 2019/1995  

---

## 1. Summary

Implement the record-keeping and audit trail requirements for all three OSS schemes: Ikke-EU-ordningen, EU-ordningen, and Importordningen. The system must maintain sufficiently detailed electronic records of all covered transactions and make them immediately accessible on demand — per individual transaction, not batch-only — to both the identification member state (Denmark) and each consumption member state independently. Records must be retained for 10 calendar years from the end of the year in which the transaction occurred.

For the Import scheme, the obligation applies independently to both the taxable person and to any intermediary acting on their behalf. An intermediary must maintain separate records for each taxable person they represent.

The access model is dual and scoped: Denmark (identification member state) has cross-scheme visibility across all consumption member states; each consumption member state can only access records where it is the consumption member state. These access permissions are independent — neither state's access depends on the other.

Records must remain accessible for the full 10-year period even after deregistration.

---

## 2. Context and Motivation

Record-keeping is the foundation of OSS compliance verification. Without correct, immediately accessible records, consumption member states cannot independently verify whether OSS returns (OSS-04) correctly account for their share of VAT. The 10-year retention requirement aligns with the general EU tax audit window and is enforced identically across all three schemes.

The critical design constraint introduced by this petition is the "immediately accessible" requirement in Momsforordningen artikel 63c stk. 3: records must be made available electronically *straks* (immediately) and *for each individual transaction* — not as a batch export. This rules out cold/archival storage as the primary store for any records still within the 10-year retention window. The system must be capable of on-demand, per-item record retrieval at any point during the 10-year period, including after the taxable person or intermediary has deregistered.

The dual-access model (identification member state + each consumption member state as independent actors) implies that the data model must be able to partition record visibility by consumption member state without requiring cross-state coordination. This has direct implications for the data model established in OSS-02 (registrations) and OSS-04 (returns).

A 20-day reminder mechanism is mandated: if records are not submitted within 20 days of an electronic request, the identification member state must remind the taxable person or intermediary and electronically notify all affected consumption member states.

OSS-06 establishes the record-keeping domain. Its data model must be compatible with the return and payment data from OSS-04 and OSS-05, but it is an independent obligation — record-keeping requirements exist even where no return has yet been filed for a period.

---

## 3. Actors

| Actor | Description |
|---|---|
| **Taxable person** | Legal or natural person registered under the Non-EU, EU, or Import scheme; bears primary record-keeping obligation |
| **Intermediary** | Third party authorised to act on behalf of a taxable person under the Import scheme; bears independent record-keeping obligation for each represented taxable person |
| **Skatteforvaltningen** | Danish Tax Administration; acts as identification member state; has full cross-CMS record access and issues reminders |
| **Consumption member state (CMS)** | EU member state where VAT is due; has scoped read-only access to records where it is the consumption member state |
| **osm2 record store** | The system's persistent, immediately-queryable record store for OSS transaction records |

---

## 4. Functional Requirements

### 4.1 Record-Keeping Obligation — Ikke-EU-ordningen and EU-ordningen (ML § 66h)

**FR-OSS-06-001:** A taxable person using the Non-EU scheme or EU scheme SHALL maintain electronic records of all transactions covered by the respective scheme.

**FR-OSS-06-002:** Records SHALL be sufficiently detailed for consumption member state tax authorities to verify that the VAT return (momsangivelse) is correct.

**FR-OSS-06-003:** Records SHALL be made electronically accessible on demand to both the identification member state (Denmark) and to each consumption member state independently.

**FR-OSS-06-004:** Records SHALL be retained for **10 calendar years** from the end of the calendar year in which the transaction occurred (ML § 66h).

### 4.2 Required Record Fields — Ikke-EU-ordningen and EU-ordningen (Momsforordningen artikel 63c stk. 1)

**FR-OSS-06-005:** Each transaction record for the Non-EU scheme and the EU scheme SHALL contain at minimum the following fields:

| Field | Description |
|---|---|
| a) Consumption member state | The EU member state to which the goods or services are supplied |
| b) Supply type and quantity | Type of service, or description and quantity of goods supplied |
| c) Supply date | Date on which the supply of goods or services took place |
| d) Taxable amount and currency | The taxable amount expressed in the applicable currency |
| e) Taxable amount adjustments | Any subsequent increase or decrease of the taxable amount |
| f) VAT rate | The VAT rate applied to the supply |
| g) VAT amount due and currency | The VAT amount payable, in the applicable currency |
| h) Payment date and amount | Date and amount of payments received |
| i) Advance payments | Any advance payments received prior to supply |
| j) Invoice information | If an invoice was issued: invoice number and date (Momssystemdirektivet artikel 369) |
| k) Customer location evidence | For services: information used to establish where the customer is established, domiciled, or habitually resident; for goods: where dispatch or transport begins and ends |
| l) Return documentation | Documentation for any returns of goods, including taxable amount and VAT rate applied |

**FR-OSS-06-006:** For the EU scheme only, each transaction record SHALL additionally include:

| Field | Description |
|---|---|
| m) Dispatch member state | The EU member state from which goods are dispatched or transported to the customer |
| n) Fixed establishment details | Details of any fixed establishment used in connection with the supply (where applicable) |

### 4.3 Record-Keeping Obligation — Importordningen (ML § 66r)

**FR-OSS-06-007:** A taxable person using the Import scheme SHALL maintain electronic records of all transactions covered by the scheme.

**FR-OSS-06-008:** An intermediary acting on behalf of a taxable person under the Import scheme SHALL independently maintain records for **each taxable person** they represent; records for different taxable persons SHALL be stored and accessible separately.

**FR-OSS-06-009:** Import scheme records SHALL be made electronically accessible on demand to both the identification member state (Denmark) and to each consumption member state independently.

**FR-OSS-06-010:** Import scheme records SHALL be retained for **10 calendar years** from the end of the calendar year in which the transaction occurred (ML § 66r).

### 4.4 Required Record Fields — Importordningen (Momsforordningen artikel 63c stk. 2)

**FR-OSS-06-011:** Each transaction record for the Import scheme SHALL contain at minimum the following fields:

| Field | Description |
|---|---|
| a) Consumption member state | The EU member state to which goods are supplied |
| b) Description and quantity | Description and quantity of goods supplied |
| c) Supply date | Date on which the supply took place |
| d) Taxable amount and currency | The taxable amount in the applicable currency |
| e) Taxable amount adjustments | Any subsequent increase or decrease of the taxable amount |
| f) VAT rate | The VAT rate applied |
| g) VAT amount due and currency | The VAT amount payable in the applicable currency |
| h) Payment date and amount | Date and amount of payments received |
| i) Invoice information | If an invoice was issued: invoice details |
| j) Dispatch location evidence | Information used to establish where dispatch or transport begins and ends |
| k) Return documentation | Documentation for any returns of goods, including taxable amount and VAT rate |
| l) Order/transaction number | The order number or unique transaction number |
| m) Batch number | The unique batch number, if the taxable person is directly involved in the delivery |

### 4.5 Immediate Per-Item Electronic Accessibility (Momsforordningen artikel 63c stk. 3)

**FR-OSS-06-012:** Records SHALL be stored and maintained so that they can be made electronically available **immediately** and **per individual transaction** — not only as a batch or aggregate export.

**FR-OSS-06-013:** The system SHALL expose a per-transaction record retrieval interface that allows any authorised requesting authority to retrieve the complete record for a single identified transaction without requiring retrieval of records for other transactions.

### 4.6 On-Demand Request Handling and Reminder Mechanism (Momsforordningen artikel 63c stk. 3)

**FR-OSS-06-014:** When a competent authority (identification member state or consumption member state) submits an electronic request for records, the system SHALL track the request date and the response status.

**FR-OSS-06-015:** If records have not been submitted within **20 calendar days** of the request date, the identification member state SHALL issue an electronic reminder to the taxable person or intermediary.

**FR-OSS-06-016:** When a reminder is issued under FR-OSS-06-015, the identification member state SHALL electronically notify all consumption member states that are party to the outstanding request.

### 4.7 Dual-Access Model — Identification and Consumption Member States

**FR-OSS-06-017:** Denmark (identification member state) SHALL have access to all records for all transactions under all three schemes, across all consumption member states, for all taxable persons and intermediaries registered with Denmark.

**FR-OSS-06-018:** Each consumption member state SHALL have access **only** to records for transactions where that state is the consumption member state. Access by one consumption member state SHALL NOT expose records where a different state is the consumption member state.

**FR-OSS-06-019:** Access by the identification member state and access by a consumption member state SHALL be independent: neither requires the other's authorisation or coordination.

**FR-OSS-06-020:** For intermediary records under the Import scheme, the identification member state SHALL be able to access records across all taxable persons represented by a given intermediary. A consumption member state SHALL only access records for its own consumption transactions, regardless of which taxable person or intermediary the records belong to.

### 4.8 Post-Deregistration Record Retention and Accessibility

**FR-OSS-06-021:** Deregistration of a taxable person or closure of an intermediary's registration SHALL NOT cause deletion, movement to cold storage, or reduction in accessibility of records that are still within the 10-year retention period.

**FR-OSS-06-022:** Records SHALL remain immediately electronically accessible in accordance with FR-OSS-06-003 and FR-OSS-06-009 for the full 10-year retention window, even after the relevant registration has been terminated.

**FR-OSS-06-023:** The 10-year retention period is computed from the end of the **calendar year** in which the transaction occurred. A transaction occurring on any date in year Y has a retention expiry of 31 December of year Y+10.

**FR-OSS-06-024:** The system SHALL enforce automatic record deletion — or at minimum accessibility downgrade — only after the retention period has expired.

---

## 5. Non-Functional Requirements

*None stated by requestor for this petition. Downstream architecture must address the hot-storage implications of the "immediately accessible" requirement (FR-OSS-06-012) and the 10-year retention window. These represent infrastructure constraints that should be captured in the solution architecture (solution-architect).*

---

## 6. Constraints and Assumptions

- Denmark is the identification member state for all registrations in scope.
- "Immediately accessible" (straks) means the record is retrievable on-demand without manual intervention, batch processing, or archival restore. The architecture must not rely on cold storage for records within the 10-year retention window.
- "Per individual transaction" means the system must support lookup by transaction identifier, not only by time range or batch export.
- The 10-year retention period is counted from the **end of the calendar year** (31 December of year Y for a transaction in year Y), not from the transaction date itself.
- The intermediary's obligation under the Import scheme is fully independent of the taxable person's obligation; both obligations must be satisfied simultaneously and separately.
- "Consumption member state" access is scoped exclusively to that state's own transactions; the system must enforce this partition at the data layer, not only at the API layer.
- The 20-day reminder period is counted in calendar days from the date of the electronic request.
- Record-keeping obligations apply regardless of whether a return has been filed for the relevant period.
- Deregistration does not affect record retention or accessibility obligations.
- The system does not need to implement the EU cross-border notification protocol for reminders (that is handled by the EU data exchange layer); it must generate the reminder event and notification content.

---

## 7. Out of Scope

- VAT return (angivelse) filing → OSS-04
- Payment → OSS-05
- Registration lifecycle for any scheme → OSS-02, OSS-03
- EU cross-border data exchange protocols and message formats → separate concern
- Ordinary Danish bookkeeping requirements (bogføringsloven) → separate system
- Audit or inspection proceedings initiated by consumption member states → separate concern
- Appeals against record access decisions → separate concern
- Archival obligations under Rigsarkivet (Danish National Archives) → assessed separately by rigsarkivet-compliance-data-assessor

---

## 8. Key Risks

| Risk | Description | Mitigation |
|---|---|---|
| Hot-storage infrastructure cost | 10-year immediately accessible retention across all three schemes implies large persistent storage with query capability; cold archive is not sufficient | Architecture must explicitly decide on storage tier; flagged in NFR section |
| Retention expiry calculation | Off-by-one errors in year-end boundary (transaction date vs. year-end) affect legal compliance | FR-OSS-06-023 is explicit; date arithmetic verified in Gherkin scenarios |
| Intermediary data separation | Intermediary records for different taxable persons must not bleed across | FR-OSS-06-008 and FR-OSS-06-020 enforce separation; tested in Gherkin |
| CMS access isolation | A consumption member state must not be able to access another CMS's data | FR-OSS-06-018 enforces partition; access-control scenario in Gherkin |
| Post-deregistration accessibility | System may be tempted to archive/purge on deregistration; this would violate ML § 66h/66r | FR-OSS-06-021–022 prohibit this; deregistration scenario in Gherkin |
| 20-day reminder non-automation | Manual reminder processes are error-prone across many member states | FR-OSS-06-015–016 require automated tracking and reminder dispatch |
| Federated access model complexity | IMS and each CMS access independently without coordination — session/auth model must support this | Captured as architecture risk; solution-architect to address |

---

## 9. References

| Reference | Location |
|---|---|
| D.A.16.3.8 Regnskab | `docs/references/DA16.3.8-regnskab.md` |
| ML § 66h | Momslov (lov nr. 209/2024), as amended by lov nr. 810/2020 |
| ML § 66r | Momslov (lov nr. 209/2024), as inserted by lov nr. 810/2020 |
| Momssystemdirektivet artikel 369 | EU VAT Directive 2006/112/EC, as amended by Directive 2008/8/EC and 2017/2455 |
| Momssystemdirektivet artikel 369k | EU VAT Directive 2006/112/EC, as amended by Directive 2017/2455 |
| Momssystemdirektivet artikel 369x | EU VAT Directive 2006/112/EC, as inserted by Directive 2017/2455 |
| Momsforordningen artikel 63c | Council Implementing Regulation (EU) 282/2011, as amended by Implementing Regulation (EU) 2019/2026 |
| Direktiv 2017/2455 | EU OSS reform directive |
| Direktiv 2019/1995 | EU OSS extension directive |
| Gennemførelsesforordning (EU) 2019/2026 | Implementation Regulation amending Regulation 282/2011 |
