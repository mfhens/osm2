# OSS-06 — Outcome Contract: Regnskab og dokumentation

**Petition ID:** OSS-06  
**Status:** Draft  
**Created:** 2026-03-30  
**System:** osm2 — One Stop Moms (OSS) VAT system, Skatteforvaltningen  

---

## 1. Purpose

This document defines the measurable acceptance criteria for OSS-06. All criteria must be satisfied before the feature is considered complete. Each criterion is directly traceable to one or more functional requirements in OSS-06.md.

---

## 2. Definition of Done

The feature is complete when:

1. All acceptance criteria in Section 3 are verifiably satisfied.
2. Every Gherkin scenario in `OSS-06.feature` passes against the implemented system.
3. The record store supports per-item on-demand retrieval for any record within the 10-year retention window without batch processing or manual intervention.
4. Access control partitioning (FR-OSS-06-017–020) is enforced at the data layer and verified by automated tests.
5. No records within the 10-year retention window are deleted or moved to cold storage on deregistration.
6. No invented requirements have been implemented beyond those stated in OSS-06.md.

---

## 3. Acceptance Criteria

### AC-OSS-06-001 — Required record fields: Non-EU and EU schemes

**Traces to:** FR-OSS-06-005  
**Given** a taxable person is registered under the Non-EU scheme or EU scheme,  
**When** a transaction record is created,  
**Then** the record SHALL contain all 12 mandatory fields (a–l) defined in FR-OSS-06-005: consumption member state, supply type/quantity, supply date, taxable amount and currency, taxable amount adjustments, VAT rate, VAT amount and currency, payment date/amount, advance payments, invoice information (if applicable), customer location evidence, and return documentation (if applicable).  
**Failure condition:** Any field is absent, null where mandatory, or structurally non-retrievable per transaction.

---

### AC-OSS-06-002 — Additional fields: EU scheme only

**Traces to:** FR-OSS-06-006  
**Given** a taxable person is registered under the EU scheme,  
**When** a transaction record is created,  
**Then** the record SHALL additionally contain the dispatch member state (m) and fixed establishment details (n) where applicable.  
**Failure condition:** These fields are absent from EU scheme records, or incorrectly present on Non-EU scheme records.

---

### AC-OSS-06-003 — Required record fields: Import scheme (taxable person)

**Traces to:** FR-OSS-06-007, FR-OSS-06-011  
**Given** a taxable person is registered under the Import scheme,  
**When** a transaction record is created,  
**Then** the record SHALL contain all 13 mandatory fields (a–m) defined in FR-OSS-06-011: consumption member state, description/quantity, supply date, taxable amount and currency, adjustments, VAT rate, VAT amount and currency, payment date/amount, invoice information (if applicable), dispatch location evidence, return documentation (if applicable), order/transaction number, and batch number (if taxable person directly involved in delivery).  
**Failure condition:** Any field is absent or non-retrievable per transaction.

---

### AC-OSS-06-004 — Intermediary maintains separate records per taxable person

**Traces to:** FR-OSS-06-008  
**Given** an intermediary represents two or more taxable persons under the Import scheme,  
**When** records are retrieved for a specific taxable person represented by that intermediary,  
**Then** only records belonging to that taxable person SHALL be returned; records for other taxable persons represented by the same intermediary SHALL NOT appear.  
**Failure condition:** Records for taxable person A appear in a retrieval scoped to taxable person B.

---

### AC-OSS-06-005 — 10-year retention period: correct expiry calculation

**Traces to:** FR-OSS-06-004, FR-OSS-06-010, FR-OSS-06-023  
**Given** a transaction occurring on any date D in calendar year Y,  
**When** the system calculates the retention expiry for that transaction,  
**Then** the expiry date SHALL be 31 December of year Y+10.  
**Failure condition:** Expiry is calculated from transaction date D (not year-end), or year Y+10 boundary is off by one year.

---

### AC-OSS-06-006 — Per-item immediate electronic accessibility

**Traces to:** FR-OSS-06-012, FR-OSS-06-013  
**Given** a record exists for a specific transaction identified by its transaction identifier,  
**When** an authorised authority requests that record,  
**Then** the system SHALL return the complete record for that single transaction without requiring a batch export, without manual intervention, and without an archival restore step.  
**Failure condition:** Record retrieval requires batch job, manual step, or takes longer than immediate response due to archival storage.

---

### AC-OSS-06-007 — Denmark (IMS) has full cross-scheme, cross-CMS access

**Traces to:** FR-OSS-06-017  
**Given** Denmark is acting as the identification member state,  
**When** Denmark requests records for any transaction under any scheme (Non-EU, EU, Import) for any consumption member state,  
**Then** the system SHALL return those records.  
**Failure condition:** Records are inaccessible to Denmark for any scheme, consumption member state combination, or taxable person/intermediary registered with Denmark.

---

### AC-OSS-06-008 — Consumption member state access is scoped to own records only

**Traces to:** FR-OSS-06-018  
**Given** consumption member state CMS-A requests records,  
**When** those records include both transactions where CMS-A is the consumption member state and transactions where CMS-B is the consumption member state,  
**Then** the system SHALL return only the records where CMS-A is the consumption member state; records where CMS-B is the consumption member state SHALL NOT be returned.  
**Failure condition:** CMS-A can retrieve any record where it is not the designated consumption member state.

---

### AC-OSS-06-009 — IMS and CMS access is independent

**Traces to:** FR-OSS-06-019  
**Given** an active record request from a consumption member state,  
**When** the identification member state simultaneously requests the same or overlapping records,  
**Then** both requests SHALL be fulfilled independently; neither request's authorisation SHALL depend on the other.  
**Failure condition:** Either request blocks, waits for, or is rejected due to a concurrent request from the other authority type.

---

### AC-OSS-06-010 — 20-day reminder: issued when records not submitted

**Traces to:** FR-OSS-06-015  
**Given** a competent authority has submitted an electronic record request on date R,  
**When** 20 calendar days have elapsed from date R without records being submitted,  
**Then** the system SHALL issue an electronic reminder to the taxable person or intermediary.  
**Failure condition:** Reminder is not issued, is issued before 20 days, or is issued more than 20 days late.

---

### AC-OSS-06-011 — CMS notification when reminder issued

**Traces to:** FR-OSS-06-016  
**Given** a reminder has been issued to a taxable person or intermediary under AC-OSS-06-010,  
**When** the reminder is dispatched,  
**Then** the identification member state SHALL electronically notify all consumption member states that are party to the outstanding request.  
**Failure condition:** Any consumption member state party to the request is not notified, or notification occurs before the reminder is sent.

---

### AC-OSS-06-012 — Post-deregistration records remain accessible

**Traces to:** FR-OSS-06-021, FR-OSS-06-022  
**Given** a taxable person or intermediary deregisters from the scheme,  
**When** an authorised authority requests records for transactions that are still within the 10-year retention window,  
**Then** the system SHALL return those records with the same immediate per-item accessibility as before deregistration.  
**Failure condition:** Deregistration causes records to be deleted, purged, archived to cold storage, or made inaccessible in any way.

---

### AC-OSS-06-013 — Records outside retention window are not accessible

**Traces to:** FR-OSS-06-024  
**Given** a transaction record whose retention expiry (31 December of year Y+10) has passed,  
**When** any authority requests that record,  
**Then** the system SHALL not return it (it may be deleted or downgraded per the system's archival policy).  
**Failure condition:** Records are returned after the retention period has expired.

---

## 4. Failure Conditions (Summary)

Any of the following constitutes a failure of OSS-06:

- A required record field (FR-OSS-06-005, FR-OSS-06-006, FR-OSS-06-011) is absent or not independently retrievable.
- An intermediary's records for different taxable persons are not isolated.
- The 10-year retention expiry is calculated from transaction date rather than year-end.
- Per-item retrieval requires a batch export or archival restore.
- A consumption member state can access records where it is not the consumption member state.
- Denmark cannot access records for any transaction under any scheme.
- Access by one authority type depends on or blocks the other.
- A reminder is not issued at exactly the 20-day threshold.
- Consumption member states are not notified when a reminder is issued.
- Deregistration reduces accessibility of records within the retention window.
- Records outside the 10-year retention window are still returned.

---

## 5. Out-of-Scope Verification

The following are explicitly NOT verified by this outcome contract:

- Return filing correctness → OSS-04
- Payment processing → OSS-05
- Registration lifecycle → OSS-02, OSS-03
- EU cross-border data exchange protocols
- Rigsarkivet archival obligations
