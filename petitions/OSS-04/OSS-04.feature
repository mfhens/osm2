# Language: en
# Petition: OSS-04 — Momsangivelse og korrektioner
# References: ML §§ 66g, 66p; VAT Regulation 2019/2026 Articles 59–61a;
#             Directive 2017/2455; Implementation Regulation 2020/194 Annex III
# Schemes: Non-EU OSS, EU OSS, Import OSS (IOSS)

Feature: Ordinary VAT return — period and filing deadline
  As a taxable person (or intermediary) registered in an OSS scheme
  with Denmark as the identification member state
  I need to file a VAT return for each mandatory return period
  So that I satisfy the periodic filing obligation under ML §§ 66g and 66p

  Background:
    Given Denmark is the identification member state
    And the taxable person holds a valid OSS registration

  Scenario: Non-EU scheme taxable person files a quarterly return on time
    Given the taxable person is registered under the Non-EU scheme
    And the return period is Q1 of a calendar year
    When the taxable person submits a return on or before 30 April of that year
    Then the system accepts the return
    And the return is recorded as filed on time for Q1

  Scenario: EU scheme taxable person files a quarterly return on time
    Given the taxable person is registered under the EU scheme
    And the return period is Q3 of a calendar year
    When the taxable person submits a return on or before 31 October of that year
    Then the system accepts the return
    And the return is recorded as filed on time for Q3

  Scenario: Import scheme taxable person files a monthly return on time
    Given the taxable person (or intermediary) is registered under the Import scheme
    And the return period is March of a calendar year
    When the taxable person submits a return on or before 30 April of that year
    Then the system accepts the return
    And the return is recorded as filed on time for March

  Scenario: Non-EU scheme return filed after the quarterly deadline is flagged as late
    Given the taxable person is registered under the Non-EU scheme
    And the return period is Q2 of a calendar year
    When the taxable person submits a return on 2 August of that year
    Then the system accepts the return
    And the return is recorded as late for Q2

  Scenario: Import scheme return filed after the monthly deadline is flagged as late
    Given the taxable person is registered under the Import scheme
    And the return period is November of a calendar year
    When the taxable person submits a return on 2 January of the following year
    Then the system accepts the return
    And the return is recorded as late for November

Feature: Ordinary VAT return — zero return
  As a taxable person who made no qualifying supplies in any consumption
  member state during a return period and has no corrections to include
  I need to be able to file a zero return
  So that I satisfy my filing obligation even when there is no activity

  Background:
    Given Denmark is the identification member state
    And the taxable person holds a valid OSS registration

  Scenario: Non-EU scheme taxable person files a zero quarterly return
    Given the taxable person is registered under the Non-EU scheme
    And the taxable person made no qualifying supplies in any consumption member state in Q2
    And the taxable person has no corrections to prior periods
    When the taxable person submits a return for Q2 stating no supplies were made
    Then the system accepts the return as a valid zero return
    And the filing obligation for Q2 is marked as satisfied

  Scenario: Import scheme intermediary files a zero monthly return on behalf of taxable person
    Given the intermediary is registered under the Import scheme
    And the represented taxable person made no qualifying distance sales in February
    And there are no corrections to prior periods
    When the intermediary submits a return for February stating no supplies were made
    Then the system accepts the return as a valid zero return
    And the filing obligation for February is marked as satisfied

Feature: Return content validation
  As the system
  I need to enforce that every submitted return contains all mandatory data fields
  So that returns forwarded to consumption member states comply with
  Implementation Regulation 2020/194 Annex III

  Background:
    Given Denmark is the identification member state

  Scenario: Return missing VAT registration number is rejected
    Given a return for Q1 under the Non-EU scheme
    When the return is submitted without a VAT registration number
    Then the system rejects the return
    And the error indicates that the VAT registration number is missing

  Scenario: Return missing total supply value for a consumption member state is rejected
    Given a return for Q1 under the EU scheme
    And the return declares supplies in France and Germany
    When the return is submitted without a total supply value for Germany
    Then the system rejects the return
    And the error indicates that the total supply value for Germany is missing

  Scenario: Return missing VAT rate for a consumption member state is rejected
    Given a return for Q1 under the EU scheme
    And the return declares supplies in Spain
    When the return is submitted without an applicable VAT rate for Spain
    Then the system rejects the return
    And the error indicates that the applicable VAT rate for Spain is missing

  Scenario: EU scheme return with dispatch from another member state missing dispatch registration number is rejected
    Given the taxable person is registered under the EU scheme
    And the return for Q2 declares goods dispatched from Poland
    When the return is submitted without a VAT or tax registration number for Poland
    Then the system rejects the return
    And the error indicates that a registration number for the dispatch member state Poland is missing

  Scenario: EU scheme return with fixed establishment missing registration number is rejected
    Given the taxable person is registered under the EU scheme
    And the taxable person has a fixed establishment in Italy from which services were supplied
    When the return for Q3 is submitted without the Italian establishment's VAT registration number
    Then the system rejects the return
    And the error indicates that the fixed establishment registration number for Italy is missing

  Scenario: Valid Non-EU scheme return with all mandatory fields is accepted
    Given a return for Q4 under the Non-EU scheme
    And the return includes the VAT registration number
    And the return declares supplies in Belgium with total value, VAT rate, VAT per rate, and total VAT
    When the return is submitted
    Then the system accepts the return

  Scenario: Valid EU scheme return including dispatch-from and fixed-establishment data is accepted
    Given a return for Q1 under the EU scheme
    And the return includes the VAT registration number
    And the return declares supplies in the Netherlands with all required financial fields
    And goods were dispatched from Austria with the Austrian tax registration number provided
    And the taxable person has a fixed establishment in Sweden with its VAT registration number provided
    When the return is submitted
    Then the system accepts the return

Feature: Currency conversion and rounding prohibition
  As the system
  I need to apply ECB exchange rates correctly and prohibit rounding
  So that returns comply with VAT Regulation Articles 60, 366, 369h, and 369u
  and momsbekendtgørelsens §§ 120–121

  Background:
    Given Denmark is the identification member state
    And the ECB publishes exchange rates on all TARGET2 business days

  Scenario: Supply invoiced in EUR is converted at ECB rate on last day of return period
    Given a Non-EU scheme return for Q1 (period ends 31 March)
    And a supply was invoiced in EUR at the rate of EUR 1 = DKK 7.46
    And the ECB published the rate EUR 1 = DKK 7.46 on 31 March
    When the return is submitted
    Then the system records the converted amount using the rate EUR 1 = DKK 7.46
    And the amount is not converted at any other date's rate

  Scenario: ECB rate not published on last day of period — next publication day rate is used
    Given an Import scheme return for February (period ends 28 February)
    And the ECB did not publish a rate on 28 February because it was a non-business day
    And the ECB published a rate on 1 March
    When the return is submitted
    Then the system applies the ECB rate from 1 March for the currency conversion

  Scenario: Correction to a prior period uses the ECB rate of the corrected period, not the new filing period
    Given a Non-EU scheme ordinary return for Q3 that embeds a correction to Q1
    And the ECB rate on the last day of Q1 (31 March) was EUR 1 = DKK 7.44
    And the ECB rate on the last day of Q3 (30 September) is EUR 1 = DKK 7.49
    When the return is submitted
    Then the system converts the Q1 correction amount using the rate EUR 1 = DKK 7.44
    And the Q3 current-period amounts are converted using the rate EUR 1 = DKK 7.49

  Scenario: VAT amount with decimal precision is stored without rounding
    Given a Non-EU scheme return for Q2
    And the computed total VAT due is DKK 1234.567
    When the return is submitted
    Then the system stores and transmits the amount as DKK 1234.567
    And the amount is not rounded to DKK 1235 or DKK 1234.57 or any other rounded value

Feature: Final return upon cessation, exclusion, or identification state change
  As a taxable person ceasing to use an OSS scheme
  I need to file a final return with the identification member state in effect at cessation
  So that all outstanding VAT obligations are settled with the correct state
  per VAT Regulation Article 61a stk. 1

  Background:
    Given Denmark is the identification member state

  Scenario: Taxable person voluntarily ceases use of Non-EU scheme and files final return
    Given the taxable person is registered under the Non-EU scheme
    And the taxable person notifies cessation effective in Q2
    When the taxable person files the final return for Q2 with Denmark
    Then the system marks the return as the final return for the Non-EU scheme
    And the final return is filed with Denmark as the identification member state

  Scenario: Final return covers all outstanding late-filed prior returns
    Given the taxable person is excluded from the EU scheme effective in Q3
    And the return for Q1 was never filed
    When the taxable person submits the final return for Q3
    Then the system requires the Q1 return to be included or separately filed before the final return is accepted
    And the final return obligation is not marked as complete until Q1 is also filed

  Scenario: Intermediary deregistration triggers final return obligations for all represented taxable persons
    Given an intermediary is registered under the Import scheme
    And the intermediary represents 3 taxable persons
    And the intermediary is removed from the identification register
    When the deregistration is processed
    Then the system creates a final return obligation for each of the 3 represented taxable persons
    And each obligation is addressed to Denmark as the identification member state at the time of deregistration

Feature: Special period situations
  As the system
  I need to generate the correct return obligations for mid-period events
  So that split-period supplies are reported to the correct identification member state(s)
  per VAT Regulation Articles 59 stk. 2–4

  Background:
    Given Denmark is the identification member state

  Scenario: Mid-quarter Non-EU registration generates a separate return for the registration quarter
    Given a taxable person registers under the Non-EU scheme
    And the scheme applies from the date of the first qualifying supply on 15 February (within Q1)
    When the system processes the registration
    Then the system generates a separate return obligation for Q1
    And the separate return covers the period from 15 February to 31 March
    And the separate return does not cover 1 January to 14 February

  Scenario: Mid-quarter EU identification state change generates two separate return obligations
    Given the taxable person is registered under the EU scheme
    And the identification member state changes from Germany to Denmark on 10 August (within Q3)
    When the system processes the identification state change
    Then the system generates a return obligation to Germany covering 1 July to 9 August
    And the system generates a return obligation to Denmark covering 10 August to 30 September
    And the two obligations are distinct and do not overlap

  Scenario: Non-EU to EU scheme switch within a quarter generates two separate scheme returns
    Given a taxable person was registered under the Non-EU scheme for the first part of Q2
    And the taxable person switched to the EU scheme on 1 May (within Q2)
    When the system processes the scheme switch
    Then the system generates a return obligation under the Non-EU scheme for 1 April to 30 April
    And the system generates a return obligation under the EU scheme for 1 May to 30 June
    And both obligations are for the same calendar quarter Q2

Feature: Late return reminders
  As the identification member state system
  I need to automatically remind taxable persons who have missed a filing deadline
  and record the transfer of collection responsibility to consumption member states
  per VAT Regulation Article 60a

  Background:
    Given Denmark is the identification member state

  Scenario: Electronic reminder sent on day 10 after missed quarterly deadline
    Given the taxable person is registered under the Non-EU scheme
    And the Q1 return deadline was 30 April
    And no return was filed by 30 April
    When the calendar reaches 10 May
    Then the system automatically sends an electronic reminder to the taxable person
    And the reminder dispatch is recorded with the timestamp of 10 May

  Scenario: System notifies other member states when reminder is issued
    Given the Q1 Non-EU return deadline was 30 April
    And no return was filed by 30 April
    When the system sends the reminder on 10 May
    Then the system electronically notifies all other EU member states that a reminder was issued
    And the notification is recorded

  Scenario: Collection responsibility transfers to consumption member states after first reminder
    Given the system sent a reminder for the Q1 Non-EU return on 10 May
    When the reminder is recorded
    Then the system records that responsibility for subsequent reminders has transferred to the consumption member states
    And the system records that responsibility for VAT collection for Q1 has transferred to the consumption member states

  Scenario: Taxable person files overdue return with identification member state after reminder
    Given a reminder was sent for the Q2 Non-EU return on 10 August
    And consumption member states have initiated collection proceedings
    When the taxable person files the Q2 return with Denmark on 25 September
    Then the system accepts the return from the identification member state portal
    And the filing obligation for Q2 is marked as satisfied

Feature: Corrections — pre-July-2021 periods
  As a taxable person needing to correct a return for a period up to and including June 2021
  I need the pre-July-2021 correction rules to apply
  So that the correction mechanics match the rules in force for those periods
  per DA16.2.5.5 and VAT Regulation Article 61 (pre-2019/2026 version)

  Background:
    Given Denmark is the identification member state

  Scenario: Correction to a Q2 2021 Non-EU return applies old correction rules
    Given the taxable person needs to correct the Q2 2021 Non-EU return
    When the taxable person initiates a correction for Q2 2021
    Then the system applies the pre-July-2021 correction rules
    And the system does not require the correction to be embedded in a subsequent ordinary return

  Scenario: Correction to a June 2021 Import return applies old correction rules
    Given the taxable person needs to correct the June 2021 Import return
    When the taxable person initiates a correction for June 2021
    Then the system applies the pre-July-2021 correction rules
    And the system does not require the correction to be embedded in a subsequent ordinary return

Feature: Corrections — post-June-2021 periods (embedded in subsequent ordinary return)
  As a taxable person needing to correct a return for a period from July 2021 onwards
  I need to embed the correction in a subsequent ordinary return within 3 years
  So that the correction complies with ML § 66g stk. 2 and ML § 66p stk. 4

  Background:
    Given Denmark is the identification member state

  Scenario: Standalone correction return for a Q3 2021 period is rejected
    Given the taxable person needs to correct the Q3 2021 Non-EU return
    When the taxable person submits a standalone correction return for Q3 2021
    Then the system rejects the submission
    And the error indicates that corrections for periods from Q3 2021 onwards must be embedded in a subsequent ordinary return

  Scenario: Correction embedded in a subsequent ordinary return within 3 years is accepted
    Given the taxable person is registered under the EU scheme
    And the original filing deadline for Q1 2022 was 30 April 2022
    And the taxable person embeds a correction to Q1 2022 in the Q2 2025 ordinary return
    And the Q2 2025 return is submitted on 31 July 2025 (within 3 years of 30 April 2022)
    When the return is submitted
    Then the system accepts the embedded correction for Q1 2022
    And the correction is recorded against Q1 2022 for the specified consumption member state

  Scenario: Correction embedded in a return but specifying the period beyond the 3-year window is rejected
    Given the taxable person is registered under the EU scheme
    And the original filing deadline for Q1 2021 was 30 April 2021
    And the taxable person attempts to embed a correction to Q1 2021 in the Q3 2024 return
    And the Q3 2024 return is submitted on 31 October 2024 (more than 3 years after 30 April 2021)
    When the return is submitted
    Then the system rejects the embedded correction for Q1 2021
    And the error indicates that the 3-year correction window has expired and the correction must go directly to the consumption member state

  Scenario: Embedded correction missing consumption member state is rejected
    Given the taxable person is registered under the Non-EU scheme
    And the taxable person embeds a correction in the Q2 2024 ordinary return
    And the correction does not specify a consumption member state
    When the return is submitted
    Then the system rejects the return
    And the error indicates that each embedded correction must specify the consumption member state

  Scenario: Embedded correction missing original tax period is rejected
    Given the taxable person is registered under the Non-EU scheme
    And the taxable person embeds a correction in the Q2 2024 ordinary return for France
    And the correction does not specify the original tax period
    When the return is submitted
    Then the system rejects the return
    And the error indicates that each embedded correction must specify the original tax period

  Scenario: Embedded correction missing corrected VAT amount is rejected
    Given the taxable person is registered under the Import scheme
    And the taxable person embeds a correction in the April 2024 ordinary return for Belgium
    And the correction specifies the period as January 2024 but does not include a corrected VAT amount
    When the return is submitted
    Then the system rejects the return
    And the error indicates that each embedded correction must specify the corrected VAT amount

Feature: Corrections — post-final-return and beyond-3-year boundary
  As the system
  I need to prevent OSS portal corrections after a final return has been filed
  or after the 3-year window has expired
  So that corrections are directed to the correct authority
  per VAT Regulation Article 61a stk. 1 and ML §§ 66g stk. 2, 66p stk. 4

  Background:
    Given Denmark is the identification member state

  Scenario: OSS portal rejects correction attempt after final return has been filed
    Given the taxable person ceased using the EU scheme
    And the final return for the EU scheme has been filed
    When the taxable person attempts to submit a correction via the OSS portal for the EU scheme
    Then the system rejects the correction attempt
    And the error indicates that corrections must be submitted directly to the relevant consumption member state following that state's own rules

  Scenario: OSS portal rejects correction to a period whose deadline was more than 3 years ago
    Given the taxable person is registered under the Non-EU scheme
    And the original filing deadline for Q2 2019 was 31 July 2019
    When the taxable person attempts to submit a correction for Q2 2019 on 1 August 2022 (more than 3 years later)
    Then the system rejects the correction
    And the error indicates that the 3-year correction window has expired and the correction must be submitted directly to the relevant consumption member state under that state's reopening rules

  Scenario: Correction within 3 years and before final return is filed is accepted via OSS portal
    Given the taxable person is registered under the Non-EU scheme
    And no final return has been filed for the Non-EU scheme
    And the original filing deadline for Q3 2023 was 31 October 2023
    And the correction is submitted on 15 September 2024 (within 3 years)
    When the taxable person embeds the correction in the Q2 2024 return
    Then the system accepts the correction via the OSS portal
