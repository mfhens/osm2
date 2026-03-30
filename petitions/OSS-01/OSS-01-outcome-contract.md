# OSS-01 — Outcome Contract

**Petition:** OSS-01
**Titel:** Juridisk grundlag, skemavurdering og definitioner
**Dato:** 2026-03-30
**Status:** Draft

---

## Definition of Done

Implementationen er fuldstændig, når alle nedenstående acceptkriterier er opfyldt og verificeret af automatiserede test. Ingen spekulative regler er implementeret — al adfærd er direkte sporbar til ML §§ 66, 66a, 66d, 66m eller MSD-artiklerne 358, 358a, 369a, 369l.

---

## Acceptkriterier

### AC-01: Ikke-EU-ordning klassificeres korrekt

**Givet** en afgiftspligtig person der hverken har hjemsted for sin økonomiske virksomhed eller fast forretningssted i EU
**Og** personen leverer ydelser til ikkeafgiftspligtige kunder i et EU-land
**Så** klassificeres personen som berettiget til Ikke-EU-ordningen
**Og** hjemmel ML § 66a / MSD artikel 358a angives i afgørelsen

### AC-02: Ikke-EU-ordning klassificeres ikke, når fast forretningssted foreligger i EU

**Givet** en afgiftspligtig person uden hjemsted for sin økonomiske virksomhed i EU
**Og** personen har ét eller flere faste forretningssteder i EU
**Og** personen leverer ydelser til ikkeafgiftspligtige kunder i EU
**Så** klassificeres personen ikke under Ikke-EU-ordningen
**Og** klassificeres personen som potentielt berettiget til EU-ordningen

### AC-03: EU-ordning klassificeres korrekt for ydelsesleverancer

**Givet** en afgiftspligtig person etableret i et EU-land
**Og** personen leverer ydelser til ikkeafgiftspligtige kunder i et andet EU-land
**Og** personen ikke er etableret i forbrugsmedlemslandet
**Så** klassificeres personen som berettiget til EU-ordningen
**Og** hjemmel ML § 66d / MSD artikel 369a angives i afgørelsen

### AC-04: EU-ordning klassificeres korrekt for fjernsalg af varer inden for EU

**Givet** en afgiftspligtig person etableret i et EU-land
**Og** personen foretager fjernsalg af varer til ikkeafgiftspligtige kunder i et andet EU-land
**Så** klassificeres personen som berettiget til EU-ordningen
**Og** hjemmel ML § 66d / MSD artikel 369a angives i afgørelsen

### AC-05: EU-ordning gælder ikke, når leverandør og kunde befinder sig i samme land

**Givet** en afgiftspligtig person etableret i Danmark
**Og** personen leverer ydelser til ikkeafgiftspligtige kunder i Danmark
**Så** returnerer systemet "Ingen OSS-særordning — standard momsregler finder anvendelse"
**Og** foretages ingen OSS-klassifikation

### AC-06: Importordning klassificeres korrekt inden for beløbsgrænse

**Givet** fjernsalg af varer indført fra et sted uden for EU
**Og** forsendelsens reelle værdi er 150 euro eller derunder
**Og** varerne er ikke punktafgiftspligtige
**Så** klassificeres leverancen som berettiget til Importordningen
**Og** hjemmel ML § 66m / MSD artikel 369l angives i afgørelsen

### AC-07: Importordning afvises ved overskridelse af beløbsgrænse

**Givet** fjernsalg af varer indført fra et sted uden for EU
**Og** forsendelsens reelle værdi overstiger 150 euro
**Så** returnerer systemet "Ikke berettiget til Importordning — forsendelsesværdi overstiger 150 euro"
**Og** hjemmel ML § 66m / MSD artikel 369l angives i afgørelsen

### AC-08: Grænseværdi på præcis 150 euro giver ret til Importordning

**Givet** fjernsalg af varer indført fra et sted uden for EU
**Og** forsendelsens reelle værdi er præcis 150 euro
**Og** varerne er ikke punktafgiftspligtige
**Så** klassificeres leverancen som berettiget til Importordningen (grænsen er inklusive)

### AC-09: Importordning afvises for punktafgiftspligtige varer

**Givet** fjernsalg af varer indført fra et sted uden for EU
**Og** varerne er punktafgiftspligtige
**Og** forsendelsens reelle værdi er 150 euro eller derunder
**Så** returnerer systemet "Ikke berettiget til Importordning — punktafgiftspligtige varer er undtaget"
**Og** hjemmel ML § 66m / MSD artikel 369l angives i afgørelsen

### AC-10: Identifikationsmedlemsland for Ikke-EU-ordning er frit valg

**Givet** en afgiftspligtig person berettiget til Ikke-EU-ordningen
**Og** personen vælger et specifikt EU-land som identifikationsmedlemsland
**Så** accepterer systemet dette valg uden yderligere betingelser
**Og** hjemmel ML § 66a, nr. 2 / MSD artikel 358a angives

### AC-11: Identifikationsmedlemsland for EU-ordning fastlægges efter hjemstedsregel

**Givet** en afgiftspligtig person berettiget til EU-ordningen
**Og** personen har hjemsted for sin økonomiske virksomhed i ét EU-land
**Så** er identifikationsmedlemslandet det land, hvor hjemstedet er etableret
**Og** hjemmel ML § 66d, nr. 2, litra a / MSD artikel 369a angives

### AC-12: Identifikationsmedlemsland for EU-ordning fastlægges ved manglende EU-hjemsted men ét fast forretningssted

**Givet** en afgiftspligtig person berettiget til EU-ordningen
**Og** personen ikke har hjemsted for sin økonomiske virksomhed i EU
**Og** personen har ét fast forretningssted i ét EU-land
**Så** er identifikationsmedlemslandet det land, hvor det faste forretningssted er beliggende
**Og** hjemmel ML § 66d, nr. 2, litra a / MSD artikel 369a angives

### AC-13: Identifikationsmedlemsland for EU-ordning fastlægges ved multiple faste forretningssteder

**Givet** en afgiftspligtig person berettiget til EU-ordningen
**Og** personen ikke har hjemsted for sin økonomiske virksomhed i EU
**Og** personen har faste forretningssteder i mere end ét EU-land
**Og** personen angiver et specifikt land som det valgte identifikationsmedlemsland
**Så** er identifikationsmedlemslandet det angivne land
**Og** hjemmel ML § 66d, nr. 2, litra b / MSD artikel 369a angives

### AC-14: Identifikationsmedlemsland for EU-ordning fastlægges som forsendelsesland ved ingen EU-etablering

**Givet** en afgiftspligtig person berettiget til EU-ordningen
**Og** personen ikke har hjemsted for sin økonomiske virksomhed i EU
**Og** personen ikke har noget fast forretningssted i EU
**Og** forsendelse af varer påbegyndes fra ét EU-land
**Så** er identifikationsmedlemslandet det land, hvorfra forsendelsen påbegyndes
**Og** hjemmel ML § 66d, nr. 2, litra c / MSD artikel 369a angives

### AC-15: Formidler kan udpeges for Importordningen

**Givet** en ikke-EU-etableret afgiftspligtig person der benytter Importordningen
**Og** personen udpeger en EU-etableret formidler
**Så** registrerer systemet formidlerens EU-etableringsland som identifikationsmedlemsland
**Og** hjemmel ML § 66m, nr. 3, litra d / MSD artikel 369l angives

### AC-16: Forbrugsmedlemsland for fjernsalg (EU-ordning og Importordning) er modtagelseslandet

**Givet** fjernsalg af varer under EU-ordningen eller Importordningen
**Og** forsendelse eller transport til kunden afsluttes i et specifikt EU-land
**Så** er forbrugsmedlemslandet det land, hvor forsendelsen afsluttes
**Og** hjemmel ML § 66d, nr. 3, litra b (EU-ordning) eller ML § 66m, nr. 4 (Importordning) angives

### AC-17: Momslovens almindelige regler finder sekundær anvendelse

**Givet** en afgiftspligtig person tilsluttet en OSS-ordning
**Og** en situation er ikke udtømmende reguleret af OSS-særordningens specifikke regler
**Så** finder Momslovens almindelige regler sekundært anvendelse
**Og** systemet dokumenterer regelhierarkiet (ML § 66-66u → ML almindelige regler)

### AC-18: Opkrævningslovens regler finder tertiær anvendelse for ikke-EU-virksomheder

**Givet** en afgiftspligtig person etableret uden for EU tilsluttet en OSS-ordning
**Og** en procedure er hverken reguleret af OSS-reglerne eller ML's særlige regler
**Så** finder Opkrævningslovens regler tertiært anvendelse

### AC-19: Klassifikationsafgørelse ledsages af juridisk hjemmel

**Givet** at systemet foretager en klassifikationsafgørelse
**Så** indeholder afgørelsen reference til den relevante ML-paragraf
**Og** indeholder afgørelsen reference til den relevante MSD-artikel
**Og** er hjemlen direkte sporbar til en af: ML § 66, § 66a, § 66d, § 66m

### AC-20: Klassifikation afvises ved utilstrækkelige etableringsoplysninger

**Givet** en afgiftspligtig person uden angivelse af etableringsland eller etableringstype
**Så** returnerer systemet "Utilstrækkelige oplysninger — etableringsland eller etableringstype mangler"
**Og** foretages ingen klassifikationsafgørelse

### AC-21: Klassifikation under Importordning afvises uden angivet forsendelsesværdi

**Givet** fjernsalg af varer indført fra steder uden for EU
**Og** forsendelsens reelle værdi er ikke angivet
**Så** returnerer systemet "Utilstrækkelige oplysninger — reel forsendelsesværdi skal angives"
**Og** foretages ingen Importordning-klassifikation

---

## Fejlbetingelser og kanttilfælde

| Scenarie | Forventet systemrespons |
|---|---|
| Manglende etableringsoplysninger | Afvis med anmodning om supplerende oplysninger |
| Forsendelsesværdi præcis 150 euro | Berettiget til Importordning (inklusive grænse) |
| Forsendelsesværdi ikke angivet | Afvis Importordning-klassifikation — kræv reel forsendelsesværdi |
| Afgiftspligtig person etableret både i og uden for EU | EU-etableringen er afgørende — EU-ordning gælder |
| Fjernsalg til kunde i samme land som leverandøren | Ingen OSS-særordning — standard ML-regler |
| Formidler udpeget, men ikke EU-etableret | Afvis formidler-tilknytning — formidler skal være EU-etableret |

---

## Succeskriterier

Implementationen er succesfuld, når:

1. Alle 21 acceptkriterier er grønne i den automatiserede testpakke.
2. Juridisk hjemmel angives for enhver klassifikationsafgørelse og er direkte sporbar til ML §§ 66, 66a, 66d eller 66m og den tilsvarende MSD-artikel.
3. Implementationen er fri for spekulative regler — al adfærd er direkte begrundet i acceptkriterier fra denne petition.
4. Downstream petitioner (registrering, angivelse, betaling) kan forbruge klassifikationsresultatet som verificeret forudsætning.

---

## Fejlbetingelse (hvornår er implementationen IKKE acceptabel)

- Implementationen klassificerer forkert (fx EU-etableret virksomhed under Ikke-EU-ordning).
- Forsendelsesgrænsen på 150 euro behandles som eksklusiv (> 150 i stedet for ≥ 151).
- Punktafgiftspligtige varer accepteres under Importordningen.
- Klassifikationsafgørelse returneres uden juridisk hjemmel.
- Systemet anvender spekulative regler der ikke er begrundet i ML §§ 66-66u eller MSD artiklerne 358, 358a, 369a, 369l.
