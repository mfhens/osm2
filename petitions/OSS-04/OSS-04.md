# OSS-04 — Momsangivelse og korrektioner

## Summary

Implement the complete VAT return filing system for all three OSS schemes (Non-EU, EU, Import).
A registered taxable person — or an intermediary acting on behalf of a taxable person in the Import
scheme — must be able to file electronic VAT returns for each return period, including zero returns
when there is no activity and no corrections.

The system must handle five distinct filing situations: ordinary returns, final returns upon
cessation or exclusion, special split-period situations arising from mid-period events, automated
late-return reminders, and corrections to previously filed returns within the applicable windows.

---

## Context and Motivation

OSS (One-Stop-Shop) enables EU-registered and non-EU businesses selling to consumers across EU
member states to report and pay VAT in a single identification member state rather than registering
separately in every consumption member state.

OSS-04 implements the core compliance obligation that follows registration (OSS-03): the actual
filing of VAT returns. Without this, the system cannot fulfill its primary statutory purpose.

The correction mechanism is the most legally complex part of the vejledning because it intersects
three independent temporal boundaries:
1. The pre/post-July-2021 regime split (old versus new correction rules).
2. The 3-year filing window for corrections via ordinary returns (ML § 66g stk. 2, § 66p stk. 4).
3. The post-final-return state, after which all corrections go directly to consumption member states.

A frequent implementation error is applying the payment-date ECB exchange rate instead of the
last-day-of-period ECB rate. This petition explicitly mandates the correct rate source.

**Legal basis:**
- ML §§ 66g, 66p (as amended by lov nr. 810 af 9. juni 2020)
- VAT Implementation Regulation (EU) 2019/2026, Articles 59–61a
- VAT System Directive 2006/112/EC, Articles 364–366, 369f–369h, 369s–369t, 369u
- Directive 2017/2455 and Directive 2019/1995
- Implementation Regulation (EU) 2020/194, Annex III
- Momsbekendtgørelsen §§ 116–121

**Reference documentation:**
- `docs/references/DA16.3.6-angivelse.md`
- `docs/references/DA16.3.6.1-den-ordinaere-angivelse.md`
- `docs/references/DA16.3.6.1.1-angivelsesperiode-og-frist.md`
- `docs/references/DA16.3.6.1.2-angivelsens-indhold.md`
- `docs/references/DA16.3.6.2-sidste-angivelse.md`
- `docs/references/DA16.3.6.3-saerlige-situationer.md`
- `docs/references/DA16.3.6.4-for-sene-angivelser.md`
- `docs/references/DA16.3.6.5-rettelse.md`

---

## Functional Requirements

### FR-1: Ordinary Returns — Period and Deadline

**FR-1.1 (Non-EU and EU schemes):** The system shall accept a VAT return for each calendar
quarter. The filing deadline is the last day of the calendar month following the end of the
quarter (e.g., Q1 → 30 April, Q2 → 31 July, Q3 → 31 October, Q4 → 31 January). A return must
be filed even if no qualifying supplies were made in any consumption member state during the period
(ML § 66g stk. 1).

**FR-1.2 (Import scheme):** The system shall accept a VAT return for each calendar month. The
filing deadline is the last day of the calendar month following that month (e.g., January → 28/29
February, December → 31 January). A return must be filed even if no qualifying distance sales of
goods imported from outside the EU were made during the period (ML § 66p stk. 1).

**FR-1.3 (Zero return):** When a taxable person has made no qualifying supplies in any consumption
member state and has no corrections to include from prior periods, the system shall accept and
record a zero return explicitly stating that no supplies were made in the period (VAT Regulation
Article 59a).

### FR-2: Return Content

**FR-2.1 (All schemes — mandatory fields):** Every return must include:
- The taxable person's VAT registration number assigned under the scheme.
- Per consumption member state where VAT is owed: total supply value excluding VAT, applicable
  VAT rate(s), total VAT amount per rate, total VAT amount due, and any corrections to prior
  periods (identifying: consumption member state, original period, corrected VAT amount).

**FR-2.2 (EU scheme — goods dispatched from other member states):** When goods are dispatched
or transported from a member state other than the identification member state, the return must
include, per such dispatch member state:
- The VAT registration number or tax identification number assigned by that member state (for
  intra-EU distance sales not covered by ML § 4c stk. 2).
- For ML § 4c stk. 2 (platform-facilitated supplies): the VAT or tax registration number of
  the dispatch member state, if one exists.
- All figures broken down by consumption member state (momsbekendtgørelsens § 120 stk. 2–3).

**FR-2.3 (EU scheme — fixed establishments):** When the taxable person has one or more fixed
establishments in a member state other than the identification member state from which services
are supplied, the return must include the VAT or tax registration number of each such
establishment per member state (momsbekendtgørelsens § 120 stk. 4; Directive Article 369g stk. 3).

**FR-2.4 (Import scheme — content):** The monthly Import return must include:
- The taxable person's (or intermediary's) VAT registration number(s) per momsbekendtgørelsens
  § 119 stk. 4.
- Per consumption member state where VAT is owed: total value excluding VAT of distance sales
  of goods imported from outside the EU for which VAT fell due in the period, applicable VAT
  rate(s), total VAT per rate, and total VAT due.
- Any corrections to prior periods, each specifying: consumption member state, original period,
  corrected VAT amount (ML § 66p stk. 4).

### FR-3: Currency and Rounding

**FR-3.1 (Currency — DKK):** All amounts on returns filed where Denmark is the identification
member state must be expressed in Danish kroner (DKK) (momsbekendtgørelsens §§ 120 stk. 6, 121
stk. 3).

**FR-3.2 (Currency conversion — current-period supplies):** When a supply was invoiced or
recorded in a currency other than DKK, the system shall convert using the ECB exchange rate
published on the last day of the return period. If no ECB rate is published on that day, the rate
from the next ECB publication day shall be used (momsbekendtgørelsens § 120 stk. 7, § 121 stk. 4).

**FR-3.3 (Currency conversion — corrections to prior periods):** When a correction to a prior
period is included in a subsequent ordinary return, the system shall convert using the ECB
exchange rate published on the last day of the period being corrected — not the rate for the new
filing period (momsbekendtgørelsens § 120 stk. 8, § 121 stk. 5).

**FR-3.4 (No rounding):** VAT amounts on OSS returns must not be rounded to the nearest whole
currency unit. The exact calculated amount must be filed and paid (VAT Regulation Article 60).

### FR-4: Final Return

**FR-4.1 (Taxable person — cessation or exclusion):** When a taxable person ceases to use an
OSS scheme, is excluded, or changes identification member state, the system shall require a final
return (endelig angivelse) to be filed with the identification member state that was in effect
at the time of the cessation, exclusion, or change. The final return must cover:
- All supplies made in the final period up to the effective date.
- Any returns for prior periods not yet filed (late returns).
(VAT Regulation Article 61a stk. 1)

**FR-4.2 (Intermediary — deregistration or change):** When an intermediary is removed from the
identification register or changes identification member state, the system shall require the
intermediary to file final returns on behalf of all taxable persons it represents, with the
identification member state that was in effect at the time of the change (VAT Regulation Article
61a stk. 1 as amended by Implementation Regulation 2019/2026).

### FR-5: Special Period Situations

**FR-5.1 (Mid-quarter registration — Non-EU and EU):** When the Non-EU or EU scheme applies
from the date of the taxable person's first qualifying supply (per VAT Regulation Article 57d
stk. 1, 2nd paragraph), the system shall generate a separate return obligation for that calendar
quarter. This separate return covers only the period from the date of first supply to the end
of the quarter (VAT Regulation Article 59 stk. 2).

**FR-5.2 (Mid-quarter identification state change — EU and Import):** When the identification
member state changes after the first day of a calendar quarter (EU scheme) or calendar month
(Import scheme), the system shall require separate returns — one to the old identification
member state and one to the new identification member state — each covering only the portion of
the period during which that state was the identification member state. Each return must include
the payments for the respective portion (VAT Regulation Article 59 stk. 4).

**FR-5.3 (Scheme switch within a quarter):** When a taxable person has been registered under
both the Non-EU scheme and the EU scheme during the same calendar quarter, the system shall
require a separate return per scheme, each covering only the supplies made and the period covered
by that scheme (VAT Regulation Article 59 stk. 3).

### FR-6: Late Returns and Reminders

**FR-6.1 (First reminder — identification member state):** When a return has not been filed by
its deadline, the system shall send an electronic reminder to the taxable person (or intermediary)
on the 10th calendar day after the deadline. The system shall also notify all other member states
electronically that a reminder has been issued (VAT Regulation Article 60a).

**FR-6.2 (Subsequent reminders and collection — responsibility transfer):** After the first
reminder has been issued, responsibility for any subsequent reminders and for assessing and
collecting the VAT owed passes to the relevant consumption member states. The system shall record
this responsibility transfer.

**FR-6.3 (Filing obligation unchanged):** A taxable person (or intermediary) remains obligated
to file the overdue return with the identification member state regardless of any reminders or
enforcement actions taken by consumption member states (VAT Regulation Article 60a, 3rd
paragraph).

### FR-7: Corrections

**FR-7.1 (Returns filed through June 2021 — old rules):** Corrections to returns for tax periods
up to and including the second tax period of 2021 (i.e., Q2 2021 for Non-EU/EU; June 2021 for
Import) shall be processed under the rules in force before 1 July 2021 (separate correction
return mechanics, as documented in DA16.2.5.5).

**FR-7.2 (Returns from July 2021 — correction via subsequent ordinary return):** For returns for
tax periods from the third tax period of 2021 (Q3 2021 for Non-EU/EU; July 2021 for Import)
onwards, the system shall not accept a stand-alone correction return. Corrections must be
embedded in a subsequent ordinary return, filed within 3 years of the original filing deadline
for the period being corrected. Each correction embedded in a return must specify:
- The consumption member state being corrected.
- The original tax period to which the correction relates.
- The corrected VAT amount (ML § 66g stk. 2; ML § 66p stk. 4).

**FR-7.3 (Post-final-return corrections):** Once a final return has been filed (FR-4.1 or FR-4.2),
the system shall not accept any further corrections via the OSS portal for the ceased/excluded
taxable person. All corrections to returns for that taxable person — including corrections to
returns filed before the final return — must be directed to the relevant consumption member state
and processed under that member state's own rules (VAT Regulation Article 61a stk. 1).

**FR-7.4 (Corrections beyond 3 years):** Corrections to periods whose original deadline falls
more than 3 years in the past must be submitted directly to the relevant consumption member state
under that member state's reopening (genoptagelse) rules. The system shall reject such corrections
via the OSS portal.

---

## Non-Functional Requirements

There are no non-functional requirements explicitly stated in the petition prompt. The system must
comply with the general technical standards for OSS electronic filing set by the European
Commission and Skatteforvaltningen's existing infrastructure.

---

## Constraints and Assumptions

- Denmark is the identification member state in all scenarios handled by this system.
- The ECB publishes exchange rates on all TARGET2 business days; the system must handle the case
  where the last day of a period falls on a non-publication day by looking forward to the next
  publication.
- "Ordinary return" refers to the regular periodic return; "final return" is the return for the
  last period of a taxable person's registration.
- An "intermediary" (formidler) in the Import scheme acts on behalf of one or more underlying
  taxable persons; the system must maintain the association between intermediary and represented
  taxable persons.
- The 3-year correction window is measured from the original filing deadline of the period being
  corrected, not from the filing date of the original return.
- The pre-July-2021 correction rules (FR-7.1) are legacy behaviour; the system must preserve
  them for any correction to an older period even if the correction is submitted today.

---

## Out of Scope

- **Payment processing** — collection, payment remittance to consumption member states, and
  reconciliation (covered by OSS-05).
- **Record-keeping** — the accounting register obligations under ML § 66h / § 66q (covered by
  OSS-06).
- **Registration and deregistration** — the initial registration, amendments, voluntary
  deregistration, and forced exclusion flows (covered by OSS-03).
- **Consumption member state enforcement actions** — the system records the transfer of
  responsibility (FR-6.2) but does not implement the enforcement actions themselves; those are
  conducted by the consumption member states in their own systems.
- **Non-Danish identification member state scenarios** — only scenarios where Denmark is the
  identification member state are in scope.
