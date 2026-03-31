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


Feature: Regelhierarki og juridisk sporbarhed
  Som Skatteforvaltningens OSS-system
  Skal systemet respektere det korrekte regelhierarki
  og ledsage alle afgørelser med verificerbar juridisk hjemmel
  i henhold til DA16.3.1.3 og ML §§ 66-66u

  Scenario: Momslovens almindelige regler finder sekundær anvendelse
    Given en afgiftspligtig person tilsluttet Ikke-EU-ordningen
    And en situation er ikke udtømmende reguleret af OSS-særordningens specifikke regler
    When systemet fastslår hvilke regler der finder anvendelse
    Then finder Momslovens almindelige regler sekundært anvendelse
    And hjemmel angives som "DA16.3.1.3 — ML almindelige regler (sekundær)"

  Scenario: Opkrævningslovens regler finder tertiær anvendelse for ikke-EU-virksomheder
    Given en afgiftspligtig person etableret uden for EU tilsluttet en OSS-ordning
    And en procedure er ikke reguleret af ML eller OSS-specifik lovgivning
    When systemet fastslår hvilke regler der finder anvendelse
    Then finder Opkrævningslovens regler tertiært anvendelse
    And hjemmel angives som "DA16.3.1.3 — Opkrævningsloven (tertiær)"

  Scenario: Klassifikationsafgørelse ledsages altid af juridisk hjemmel
    Given en afgiftspligtig person vurderet af systemet
    When systemet returnerer en klassifikationsafgørelse
    Then indeholder afgørelsen reference til den relevante ML-paragraf
    And indeholder afgørelsen reference til den relevante MSD-artikel


Feature: Håndtering af utilstrækkelige oplysninger
  Som Skatteforvaltningens OSS-system
  Skal systemet afvise klassifikation og anmode om supplerende oplysninger
  når de nødvendige grundoplysninger mangler

  Scenario: Systemet kan ikke klassificere uden etableringsoplysninger
    Given en afgiftspligtig person uden angivelse af etableringsland eller etableringstype
    When systemet vurderer berettigelsen til OSS-særordning
    Then returnerer systemet "Utilstrækkelige oplysninger — etableringsland eller etableringstype mangler"
    And foretages ingen klassifikationsafgørelse

  Scenario: Systemet kan ikke klassificere Importordning uden angivet forsendelsesværdi
    Given fjernsalg af varer indført fra et land uden for EU
    And forsendelsens reelle værdi er ikke angivet
    When systemet vurderer berettigelsen til Importordningen
    Then returnerer systemet "Utilstrækkelige oplysninger — reel forsendelsesværdi skal angives"
    And foretages ingen klassifikationsafgørelse for Importordningen
