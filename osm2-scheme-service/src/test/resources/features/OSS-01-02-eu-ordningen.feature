# Petition: OSS-01 — Juridisk grundlag, skemavurdering og definitioner
# Legal basis: ML §§ 66, 66a, 66d, 66m / MSD artikler 358, 358a, 369a, 369l
# Gennemførelsesforordning 282/2011 artikel 57a
# Direktiv 2017/2455 / Direktiv 2019/1995 / Lov nr. 810 af 9. juni 2020

Feature: Klassifikation under EU-ordningen
  Som Skatteforvaltningens OSS-system
  Skal systemet korrekt klassificere afgiftspligtige personer under EU-ordningen
  baseret på etableringssted, leverancetype og forsendelsesforhold
  i henhold til ML § 66d og MSD artikel 369a

  Scenario: EU-etableret person klassificeres under EU-ordningen ved ydelsesleverance
    Given en afgiftspligtig person med hjemsted for sin økonomiske virksomhed i Danmark
    And personen leverer ydelser til ikkeafgiftspligtige kunder i Sverige
    When systemet vurderer berettigelsen til OSS-særordning
    Then klassificeres personen som berettiget til "EU-ordningen"
    And hjemmel angives som "ML § 66d / MSD artikel 369a"

  Scenario: EU-ordning gælder for fjernsalg af varer inden for EU
    Given en afgiftspligtig person med hjemsted for sin økonomiske virksomhed i Danmark
    And personen foretager fjernsalg af varer til ikkeafgiftspligtige kunder i Polen
    When systemet vurderer berettigelsen til OSS-særordning
    Then klassificeres personen som berettiget til "EU-ordningen"
    And hjemmel angives som "ML § 66d / MSD artikel 369a"

  Scenario: EU-ordning gælder ikke når leverandør og kunde er i samme land
    Given en afgiftspligtig person med hjemsted for sin økonomiske virksomhed i Danmark
    And personen leverer ydelser til ikkeafgiftspligtige kunder i Danmark
    When systemet vurderer berettigelsen til OSS-særordning
    Then klassificeres personen ikke som berettiget til nogen OSS-særordning
    And systemet returnerer "Ingen OSS-særordning — standard momsregler finder anvendelse"

  Scenario: Identifikationsmedlemsland for EU-ordning er hjemstedslandet
    Given en afgiftspligtig person berettiget til EU-ordningen
    And personen har hjemsted for sin økonomiske virksomhed i Danmark
    When systemet fastlægger identifikationsmedlemslandet
    Then er identifikationsmedlemslandet "Danmark"
    And hjemmel angives som "ML § 66d, nr. 2, litra a / MSD artikel 369a"

  Scenario: Identifikationsmedlemsland for EU-ordning er landet med fast forretningssted ved manglende EU-hjemsted
    Given en afgiftspligtig person berettiget til EU-ordningen
    And personen har ikke etableret hjemsted for sin økonomiske virksomhed i EU
    And personen har ét fast forretningssted i Nederlandene
    When systemet fastlægger identifikationsmedlemslandet
    Then er identifikationsmedlemslandet "Nederlandene"
    And hjemmel angives som "ML § 66d, nr. 2, litra a / MSD artikel 369a"

  Scenario: Identifikationsmedlemsland for EU-ordning er valgt land ved multiple faste forretningssteder
    Given en afgiftspligtig person berettiget til EU-ordningen
    And personen har ikke etableret hjemsted for sin økonomiske virksomhed i EU
    And personen har faste forretningssteder i Nederlandene og Belgien
    And personen angiver Belgien som det land, hvori EU-ordningen ønskes anvendt
    When systemet fastlægger identifikationsmedlemslandet
    Then er identifikationsmedlemslandet "Belgien"
    And hjemmel angives som "ML § 66d, nr. 2, litra b / MSD artikel 369a"

  Scenario: Identifikationsmedlemsland for EU-ordning er forsendelseslandet ved ingen EU-etablering
    Given en afgiftspligtig person berettiget til EU-ordningen
    And personen har ikke etableret hjemsted for sin økonomiske virksomhed i EU
    And personen har intet fast forretningssted i EU
    And forsendelse af varer påbegyndes fra Spanien
    When systemet fastlægger identifikationsmedlemslandet
    Then er identifikationsmedlemslandet "Spanien"
    And hjemmel angives som "ML § 66d, nr. 2, litra c / MSD artikel 369a"

  Scenario: Forbrugsmedlemsland for EU-ordning ved fjernsalg af varer er modtagelseslandet
    Given en afgiftspligtig person berettiget til EU-ordningen
    And personen foretager fjernsalg af varer
    And forsendelsen af varerne til kunden afsluttes i Finland
    When systemet fastlægger forbrugsmedlemslandet
    Then er forbrugsmedlemslandet "Finland"
    And hjemmel angives som "ML § 66d, nr. 3, litra b / MSD artikel 369a"

  Scenario: Forbrugsmedlemsland for EU-ordning ved ydelser er leveringsstedet
    Given en afgiftspligtig person berettiget til EU-ordningen
    And personen leverer en ydelse der anses for at finde sted i Østrig i henhold til ML kapitel 4
    When systemet fastlægger forbrugsmedlemslandet
    Then er forbrugsmedlemslandet "Østrig"
    And hjemmel angives som "ML § 66d, nr. 3, litra a / MSD artikel 369a"

  Scenario: Forbrugsmedlemsland for EU-ordning ved varer via elektronisk grænseflade er afsendelseslandet
    Given en afgiftspligtig person berettiget til EU-ordningen
    And personen leverer varer via en elektronisk grænseflade i henhold til ML § 4c, stk. 2
    And forsendelse og transport af varerne påbegyndes og afsluttes i Belgien
    When systemet fastlægger forbrugsmedlemslandet
    Then er forbrugsmedlemslandet "Belgien"
    And hjemmel angives som "ML § 66d, nr. 3, litra c / MSD artikel 369a"
