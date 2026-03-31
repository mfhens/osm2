# Petition: OSS-01 — Juridisk grundlag, skemavurdering og definitioner
# Legal basis: ML §§ 66, 66a, 66d, 66m / MSD artikler 358, 358a, 369a, 369l
# Gennemførelsesforordning 282/2011 artikel 57a
# Direktiv 2017/2455 / Direktiv 2019/1995 / Lov nr. 810 af 9. juni 2020

Feature: Klassifikation under Importordningen
  Som Skatteforvaltningens OSS-system
  Skal systemet korrekt klassificere leverancer under Importordningen
  baseret på varernes oprindelse, forsendelsesværdi og varetype
  i henhold til ML § 66m og MSD artikel 369l

  Scenario: Importordning klassificeres korrekt ved forsendelsesværdi under beløbsgrænsen
    Given fjernsalg af varer indført fra Kina til en ikkeafgiftspligtig kunde i Danmark
    And forsendelsens reelle værdi er 120 euro
    And varerne er ikke punktafgiftspligtige
    When systemet vurderer berettigelsen til OSS-særordning
    Then klassificeres leverancen som berettiget til "Importordningen"
    And hjemmel angives som "ML § 66m / MSD artikel 369l"

  Scenario: Importordning klassificeres korrekt ved forsendelsesværdi præcis 150 euro
    Given fjernsalg af varer indført fra et land uden for EU
    And forsendelsens reelle værdi er præcis 150 euro
    And varerne er ikke punktafgiftspligtige
    When systemet vurderer berettigelsen til OSS-særordning
    Then klassificeres leverancen som berettiget til "Importordningen"
    And hjemmel angives som "ML § 66m / MSD artikel 369l"

  Scenario: Importordning afvises når forsendelsesværdi overstiger 150 euro
    Given fjernsalg af varer indført fra et land uden for EU
    And forsendelsens reelle værdi er 151 euro
    And varerne er ikke punktafgiftspligtige
    When systemet vurderer berettigelsen til OSS-særordning
    Then klassificeres leverancen ikke som berettiget til "Importordningen"
    And systemet returnerer "Ikke berettiget til Importordning — forsendelsesværdi overstiger 150 euro"
    And hjemmel angives som "ML § 66m / MSD artikel 369l"

  Scenario: Importordning afvises for punktafgiftspligtige varer
    Given fjernsalg af varer indført fra et land uden for EU
    And varerne er punktafgiftspligtige
    And forsendelsens reelle værdi er 100 euro
    When systemet vurderer berettigelsen til OSS-særordning
    Then klassificeres leverancen ikke som berettiget til "Importordningen"
    And systemet returnerer "Ikke berettiget til Importordning — punktafgiftspligtige varer er undtaget"
    And hjemmel angives som "ML § 66m / MSD artikel 369l"

  Scenario: Formidler udpeges for Importordning og fastlægger identifikationsmedlemsland
    Given en ikke-EU-etableret afgiftspligtig person der benytter Importordningen
    And personen udpeger en formidler etableret i Nederlandene
    When systemet fastlægger identifikationsmedlemslandet
    Then er identifikationsmedlemslandet "Nederlandene"
    And hjemmel angives som "ML § 66m, nr. 3, litra d / MSD artikel 369l"

  Scenario: Identifikationsmedlemsland for Importordning er den afgiftspligtige persons EU-etableringsland
    Given en afgiftspligtig person med hjemsted for sin økonomiske virksomhed i Irland
    And personen benytter Importordningen
    And personen har ikke udpeget en formidler
    When systemet fastlægger identifikationsmedlemslandet
    Then er identifikationsmedlemslandet "Irland"
    And hjemmel angives som "ML § 66m, nr. 3, litra c / MSD artikel 369l"

  Scenario: Identifikationsmedlemsland for Importordning er frit valg for ikke-EU-etableret person uden formidler
    Given en afgiftspligtig person uden hjemsted for sin økonomiske virksomhed i EU
    And personen har intet fast forretningssted i EU
    And personen benytter Importordningen uden formidler
    And personen vælger Frankrig som identifikationsmedlemsland
    When systemet fastlægger identifikationsmedlemslandet
    Then er identifikationsmedlemslandet "Frankrig"
    And hjemmel angives som "ML § 66m, nr. 3, litra a / MSD artikel 369l"

  Scenario: Forbrugsmedlemsland for Importordning er leveringsmodtagelseslandet
    Given en leverance under Importordningen
    And forsendelsen til kunden afsluttes i Sverige
    When systemet fastlægger forbrugsmedlemslandet
    Then er forbrugsmedlemslandet "Sverige"
    And hjemmel angives som "ML § 66m, nr. 4 / MSD artikel 369l"
