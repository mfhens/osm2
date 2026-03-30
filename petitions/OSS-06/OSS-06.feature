Feature: OSS-06 — Regnskab og dokumentation

  All three OSS schemes (Ikke-EU, EU, Import) require taxable persons and intermediaries
  to maintain sufficiently detailed electronic records of covered transactions. Records
  must be immediately accessible on demand — per individual transaction — to both the
  identification member state (Denmark) and each consumption member state independently.
  Records are retained for 10 calendar years from the end of the year in which the
  transaction occurred.

  Background:
    Given the osm2 system is operating as identification member state for Denmark

  # ---------------------------------------------------------------------------
  # Non-EU scheme and EU scheme — Required record fields
  # ---------------------------------------------------------------------------

  Scenario: Non-EU scheme transaction record contains all required fields
    Given a taxable person "Acme Global Ltd" is registered under the Non-EU scheme with Denmark
    When a transaction record is created with:
      | field                        | value                          |
      | consumption_member_state     | Germany                        |
      | supply_type                  | Electronically supplied service |
      | supply_date                  | 2025-03-15                     |
      | taxable_amount               | 1000.00                        |
      | currency                     | EUR                            |
      | vat_rate                     | 19                             |
      | vat_amount                   | 190.00                         |
      | payment_date                 | 2025-03-20                     |
      | payment_amount               | 1190.00                        |
      | customer_location_evidence   | IP address 185.33.44.55, DE    |
    Then the record is persisted with all 12 mandatory fields present
    And fields (a) through (l) as defined in Momsforordningen artikel 63c stk. 1 are non-null and retrievable per transaction

  Scenario: EU scheme transaction record contains all required fields including dispatch member state
    Given a taxable person "Nordic Goods ApS" is registered under the EU scheme with Denmark
    When a transaction record is created with:
      | field                        | value               |
      | consumption_member_state     | France              |
      | supply_type                  | Physical goods      |
      | supply_quantity              | 3                   |
      | supply_date                  | 2025-06-01          |
      | taxable_amount               | 450.00              |
      | currency                     | EUR                 |
      | vat_rate                     | 20                  |
      | vat_amount                   | 90.00               |
      | payment_date                 | 2025-06-03          |
      | payment_amount               | 540.00              |
      | dispatch_member_state        | Denmark             |
      | customer_location_evidence   | Delivery address FR |
    Then the record is persisted with all 14 mandatory fields present
    And fields (a) through (n) as defined in Momsforordningen artikel 63c stk. 1 and OSS-06 FR-OSS-06-006 are non-null and retrievable per transaction
    And the field "dispatch_member_state" contains "Denmark"

  Scenario: EU scheme record contains dispatch member state; Non-EU scheme record does not
    Given a taxable person "Alpha Ltd" is registered under the Non-EU scheme with Denmark
    And a taxable person "Beta ApS" is registered under the EU scheme with Denmark
    When transaction records are created for both taxable persons
    Then the record for "Alpha Ltd" does not contain a "dispatch_member_state" field
    And the record for "Beta ApS" contains a populated "dispatch_member_state" field

  Scenario: Transaction record captures taxable amount adjustment
    Given a taxable person "Acme Global Ltd" is registered under the Non-EU scheme with Denmark
    And a transaction record exists with taxable amount 1000.00 EUR for Germany
    When the taxable amount is subsequently reduced by 100.00 EUR due to a credit note
    Then the transaction record is updated with an adjustment entry of -100.00 EUR
    And the original taxable amount 1000.00 EUR is preserved alongside the adjustment

  Scenario: Transaction record captures return of goods with taxable amount and VAT rate
    Given a taxable person "Nordic Goods ApS" is registered under the EU scheme with Denmark
    And a transaction record exists for goods delivered to France
    When a return of goods is recorded for that transaction
    Then the transaction record contains return documentation with:
      | field             | value  |
      | taxable_amount    | 450.00 |
      | currency          | EUR    |
      | vat_rate          | 20     |

  # ---------------------------------------------------------------------------
  # Import scheme — Required record fields
  # ---------------------------------------------------------------------------

  Scenario: Import scheme taxable person transaction record contains all required fields
    Given a taxable person "Import Direct BV" is registered under the Import scheme with Denmark
    When a transaction record is created with:
      | field                       | value                          |
      | consumption_member_state    | Netherlands                    |
      | goods_description           | Ceramic tableware set          |
      | goods_quantity              | 12                             |
      | supply_date                 | 2025-09-10                     |
      | taxable_amount              | 300.00                         |
      | currency                    | EUR                            |
      | vat_rate                    | 21                             |
      | vat_amount                  | 63.00                          |
      | payment_date                | 2025-09-12                     |
      | payment_amount              | 363.00                         |
      | dispatch_start              | China                          |
      | dispatch_end                | Amsterdam, NL                  |
      | order_number                | ORD-2025-88712                 |
    Then the record is persisted with all 13 mandatory fields present
    And fields (a) through (m) as defined in Momsforordningen artikel 63c stk. 2 are non-null and retrievable per transaction

  Scenario: Import scheme record includes batch number when taxable person is directly involved in delivery
    Given a taxable person "Import Direct BV" is registered under the Import scheme with Denmark
    When a transaction record is created and the taxable person is directly involved in the delivery
    Then the record contains a non-null "batch_number" field

  Scenario: Import scheme record for taxable person who is NOT directly involved in delivery does not require batch number
    Given a taxable person "Marketplace Seller GmbH" is registered under the Import scheme with Denmark
    When a transaction record is created and the taxable person is not directly involved in the delivery
    Then the record may omit the "batch_number" field without validation error

  # ---------------------------------------------------------------------------
  # Import scheme — Intermediary obligations
  # ---------------------------------------------------------------------------

  Scenario: Intermediary maintains separate records for each represented taxable person
    Given intermediary "OSS Agent ApS" represents taxable persons "Seller One Ltd" and "Seller Two GmbH" under the Import scheme
    And transaction records exist for both taxable persons
    When records are retrieved scoped to taxable person "Seller One Ltd" via intermediary "OSS Agent ApS"
    Then only records belonging to "Seller One Ltd" are returned
    And no records belonging to "Seller Two GmbH" are present in the result

  Scenario: Intermediary records for different taxable persons are independently accessible
    Given intermediary "OSS Agent ApS" represents taxable persons "Seller One Ltd" and "Seller Two GmbH" under the Import scheme
    When records are retrieved scoped to taxable person "Seller Two GmbH" via intermediary "OSS Agent ApS"
    Then only records belonging to "Seller Two GmbH" are returned
    And no records belonging to "Seller One Ltd" are present in the result

  # ---------------------------------------------------------------------------
  # 10-year retention — Expiry calculation
  # ---------------------------------------------------------------------------

  Scenario: Retention expiry is calculated from end of calendar year, not transaction date
    Given a transaction occurs on 15 March 2025
    When the system calculates the retention expiry for that transaction
    Then the retention expiry is 31 December 2035
    And the retention expiry is NOT 15 March 2035

  Scenario: Transaction on last day of year has correct retention expiry
    Given a transaction occurs on 31 December 2024
    When the system calculates the retention expiry for that transaction
    Then the retention expiry is 31 December 2034

  Scenario: Transaction on first day of year has correct retention expiry
    Given a transaction occurs on 1 January 2024
    When the system calculates the retention expiry for that transaction
    Then the retention expiry is 31 December 2034

  # ---------------------------------------------------------------------------
  # Per-item immediate electronic accessibility
  # ---------------------------------------------------------------------------

  Scenario: Individual transaction record retrieved immediately by transaction identifier
    Given a transaction record exists with identifier "TXN-2025-00441"
    When an authorised authority requests record "TXN-2025-00441"
    Then the complete record is returned immediately
    And no batch export step is required
    And no manual intervention step is required
    And no archival restore step is required

  Scenario: Record retrieval does not require retrieval of other records
    Given transaction records exist for 1000 transactions for taxable person "Acme Global Ltd"
    When an authorised authority requests only the record with identifier "TXN-2025-00441"
    Then exactly one record is returned
    And the response does not include any other transaction records

  # ---------------------------------------------------------------------------
  # Dual-access model — Identification member state
  # ---------------------------------------------------------------------------

  Scenario: Denmark as IMS can access records for any scheme and any consumption member state
    Given transaction records exist under the Non-EU scheme for Germany
    And transaction records exist under the EU scheme for France
    And transaction records exist under the Import scheme for Netherlands
    When Denmark (as identification member state) requests all records for taxable person "Acme Global Ltd"
    Then records for Germany, France, and Netherlands are all returned
    And records from all three schemes are included

  Scenario: Denmark as IMS can access records for all taxable persons represented by an intermediary
    Given intermediary "OSS Agent ApS" represents taxable persons "Seller One Ltd" and "Seller Two GmbH"
    When Denmark (as identification member state) requests records via intermediary "OSS Agent ApS"
    Then records for both "Seller One Ltd" and "Seller Two GmbH" are returned

  # ---------------------------------------------------------------------------
  # Dual-access model — Consumption member state access scoping
  # ---------------------------------------------------------------------------

  Scenario: Consumption member state can only access records where it is the consumption member state
    Given transaction records exist for taxable person "Acme Global Ltd" with:
      | transaction_id | consumption_member_state |
      | TXN-001        | Germany                  |
      | TXN-002        | France                   |
      | TXN-003        | Germany                  |
    When Germany requests records for taxable person "Acme Global Ltd"
    Then transactions "TXN-001" and "TXN-003" are returned
    And transaction "TXN-002" is NOT returned

  Scenario: France cannot access records where Germany is the consumption member state
    Given transaction records exist for taxable person "Acme Global Ltd" with:
      | transaction_id | consumption_member_state |
      | TXN-001        | Germany                  |
      | TXN-002        | France                   |
    When France requests records for taxable person "Acme Global Ltd"
    Then only transaction "TXN-002" is returned
    And transaction "TXN-001" is NOT returned

  Scenario: Consumption member state access for Import scheme is scoped to own transactions only
    Given intermediary "OSS Agent ApS" represents taxable person "Seller One Ltd" under the Import scheme
    And transaction records exist for:
      | transaction_id | consumption_member_state |
      | IMP-001        | Netherlands              |
      | IMP-002        | Belgium                  |
    When Netherlands requests records via intermediary "OSS Agent ApS" for taxable person "Seller One Ltd"
    Then only transaction "IMP-001" is returned
    And transaction "IMP-002" is NOT returned

  # ---------------------------------------------------------------------------
  # Dual-access model — Independence of IMS and CMS access
  # ---------------------------------------------------------------------------

  Scenario: IMS and CMS can request records simultaneously without blocking
    Given transaction records exist for taxable person "Acme Global Ltd"
    When Denmark (as IMS) and Germany (as CMS) simultaneously request records for "Acme Global Ltd"
    Then both requests are fulfilled independently
    And Denmark receives records for all consumption member states
    And Germany receives only records where Germany is the consumption member state
    And neither request is rejected or delayed due to the concurrent request from the other authority

  # ---------------------------------------------------------------------------
  # 20-day reminder mechanism
  # ---------------------------------------------------------------------------

  Scenario: Reminder issued when records not submitted within 20 days of request
    Given a competent authority submitted an electronic record request on 1 April 2025
    And the taxable person has not submitted the requested records
    When 20 calendar days have elapsed since 1 April 2025
    Then the system issues an electronic reminder to the taxable person
    And the reminder is issued on 21 April 2025

  Scenario: No reminder issued before 20 days have elapsed
    Given a competent authority submitted an electronic record request on 1 April 2025
    And the taxable person has not submitted the requested records
    When 19 calendar days have elapsed since 1 April 2025
    Then no reminder has been issued

  Scenario: Consumption member states notified when reminder is issued
    Given a competent authority submitted an electronic record request on 1 April 2025 for transactions in Germany and France
    And 20 days have elapsed without the taxable person submitting records
    When the system issues a reminder to the taxable person
    Then the identification member state (Denmark) sends an electronic notification to Germany
    And the identification member state (Denmark) sends an electronic notification to France

  Scenario: No reminder issued when records are submitted within 20 days
    Given a competent authority submitted an electronic record request on 1 April 2025
    When the taxable person submits the requested records on 10 April 2025
    Then no reminder is issued

  # ---------------------------------------------------------------------------
  # Post-deregistration accessibility
  # ---------------------------------------------------------------------------

  Scenario: Records remain immediately accessible after taxable person deregisters
    Given taxable person "Acme Global Ltd" has transactions from 2023 under the Non-EU scheme
    And "Acme Global Ltd" deregisters from the scheme on 31 December 2025
    When Germany requests the 2023 transaction records for "Acme Global Ltd" on 1 January 2026
    Then the 2023 records are returned immediately with full per-item accessibility
    And no archival restore step is required

  Scenario: Deregistration does not cause deletion of records within the 10-year retention window
    Given taxable person "Acme Global Ltd" has a transaction on 15 March 2023
    And the retention expiry for that transaction is 31 December 2033
    When "Acme Global Ltd" deregisters from the scheme on 31 December 2025
    Then the transaction record is not deleted
    And the transaction record remains electronically accessible until 31 December 2033

  # ---------------------------------------------------------------------------
  # Post-retention-expiry inaccessibility
  # ---------------------------------------------------------------------------

  Scenario: Records are not accessible after the 10-year retention period has expired
    Given a transaction record with retention expiry 31 December 2024 exists in the system
    When any authority requests that record on 1 January 2025 or later
    Then the record is not returned
