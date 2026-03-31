# Petition: OSS-01 — Juridisk grundlag, skemavurdering og definitioner
# Legal basis: ML §§ 66, 66a, 66d, 66m / MSD artikler 358, 358a, 369a, 369l
# Gennemførelsesforordning 282/2011 artikel 57a
# Direktiv 2017/2455 / Direktiv 2019/1995 / Lov nr. 810 af 9. juni 2020

Feature: Klassifikation under Ikke-EU-ordningen
  Som Skatteforvaltningens OSS-system
  Skal systemet korrekt klassificere afgiftspligtige personer under Ikke-EU-ordningen
  baseret på etableringssted og leverancetype
  i henhold til ML § 66a og MSD artikel 358a

  Scenario: Ikke-EU-etableret person klassificeres under Ikke-EU-ordningen
    Given en afgiftspligtig person uden hjemsted for sin økonomiske virksomhed i EU
    And personen har intet fast forretningssted i EU
    And personen leverer ydelser til ikkeafgiftspligtige kunder i EU
    When systemet vurderer berettigelsen til OSS-særordning
    Then klassificeres personen som berettiget til "Ikke-EU-ordningen"
    And hjemmel angives som "ML § 66a / MSD artikel 358a"

  Scenario: Person med fast forretningssted i EU klassificeres ikke under Ikke-EU-ordningen
    Given en afgiftspligtig person uden hjemsted for sin økonomiske virksomhed i EU
    And personen har et fast forretningssted i Frankrig
    And personen leverer ydelser til ikkeafgiftspligtige kunder i andre EU-lande
    When systemet vurderer berettigelsen til OSS-særordning
    Then klassificeres personen ikke som berettiget til "Ikke-EU-ordningen"
    And klassificeres personen som berettiget til "EU-ordningen"

  Scenario: Identifikationsmedlemsland for Ikke-EU-ordning er frit valg
    Given en afgiftspligtig person berettiget til Ikke-EU-ordningen
    And personen vælger Danmark som identifikationsmedlemsland
    When systemet fastlægger identifikationsmedlemslandet
    Then er identifikationsmedlemslandet "Danmark"
    And hjemmel angives som "ML § 66a, nr. 2 / MSD artikel 358a"

  Scenario: Forbrugsmedlemsland for Ikke-EU-ordning er leveringsstedet for ydelsen
    Given en afgiftspligtig person berettiget til Ikke-EU-ordningen
    And leveringen af en ydelse anses for at finde sted i Sverige i henhold til ML kapitel 4
    When systemet fastlægger forbrugsmedlemslandet
    Then er forbrugsmedlemslandet "Sverige"
    And hjemmel angives som "ML § 66a, nr. 3 / MSD artikel 358a"
