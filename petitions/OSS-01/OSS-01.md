# OSS-01 — Juridisk grundlag, skemavurdering og definitioner

**Program:** osm2 — One Stop Moms (OSS), anden generation
**Petition ID:** OSS-01
**Dato:** 2026-03-30
**Status:** Draft

---

## Resumé

Implementer det juridiske grundlagslag i OSS-systemet: berettigelsesbestemmelse, formelle definitioner og skemaklassifikation. Systemet skal kunne afgøre, hvilken af de tre særordninger der gælder for en afgiftspligtig person — Ikke-EU-ordningen, EU-ordningen eller Importordningen — eller om ingen gælder. Klassifikationen sker på baggrund af etableringssted, brug af formidler, type af leverancer og reel forsendelsesværdi.

---

## Kontekst og motivation

osm2 er en second-generation implementering af One Stop Moms (OSS) VAT-systemet for Skatteforvaltningen. Systemet gennemfører EU's One-Stop-Shop-særordninger for moms i henhold til ML §§ 66-66u (Momsloven), som implementerer direktiv 2017/2455 og direktiv 2019/1995.

Særordningerne giver afgiftspligtige personer mulighed for at registrere sig i ét EU-land og indberette/betale moms til alle EU's forbrugsmedlemslande via én grænseflade — og dermed undgå momsregistrering i hvert enkelt forbrugsland.

Som det første trin i enhver brugerrejse skal systemet kunne fastslå, hvilken ordning (hvis nogen) en given afgiftspligtig person er berettiget til. Alle efterfølgende flows (registrering, angivelse, betaling, regnskab) forudsætter, at denne klassifikation er korrekt foretaget.

### Historisk udvikling

| År | Hændelse | Hjemmel |
|---|---|---|
| 2003 | One Stop Moms for elektroniske ydelser fra virksomheder uden for EU | Rådets direktiv 2002/38/EF |
| 2015 | Udvidet til teleydelser og radio-/tv-spredningstjenester; ny EU-ordning indført | Direktiv 2008/8/EF, artikel 5 |
| 2021 | Udvidet til alle ydelser + fjernsalg af varer + ny Importordning | Direktiv 2017/2455 + 2019/1995; dansk: lov nr. 810/2020 |

---

## Funktionelle krav

### FR-01: Klassifikation af særordning

Systemet skal, givet et sæt oplysninger om en afgiftspligtig person og en leverance, bestemme hvilken særordning der gælder:

| Betingelse | Ordning |
|---|---|
| Afgiftspligtig person hverken har hjemsted for sin økonomiske virksomhed eller fast forretningssted i EU, og leverer ydelser til ikkeafgiftspligtige i EU | Ikke-EU-ordningen |
| Afgiftspligtig person er etableret i EU (hjemsted eller fast forretningssted), men ikke i forbrugsmedlemslandet, og leverer ydelser eller foretager fjernsalg af varer til ikkeafgiftspligtige i andre EU-lande | EU-ordningen |
| Fjernsalg af varer indført fra steder uden for EU, reel forsendelsesværdi ≤ 150 euro, og varerne er ikke punktafgiftspligtige | Importordningen |
| Ingen af ovenstående betingelser er opfyldt | Ingen OSS-særordning — standard momsregler finder anvendelse |

### FR-02: Generelle definitioner (ML § 66 / MSD artikel 358)

Systemet skal anvende følgende definitioner for alle tre ordninger, i overensstemmelse med ML § 66 og MSD artikel 358 som ændret ved direktiv 2017/2455:

- **EU-ordning:** Ordning for (a) fjernsalg af varer inden for EU, (b) levering af varer i et EU-land via elektroniske grænseflader der formidler sådanne leveringer (ML § 4c, stk. 2), og (c) ydelser leveret af afgiftspligtige personer etableret i EU men ikke i forbrugsmedlemslandet.
- **Ikke-EU-ordning:** Ordning for ydelser leveret af afgiftspligtige personer der ikke er etableret i EU.
- **Importordning:** Ordning for fjernsalg af varer indført fra steder uden for EU, undtagen punktafgiftspligtige varer, i forsendelser med en reel værdi der ikke overstiger 150 euro.
- **Afgiftsangivelse (momsangivelse):** En angivelse som indeholder de oplysninger der er nødvendige for at fastlægge det afgiftsbeløb, der skal opkræves i hvert medlemsland.

### FR-03: Definitioner — Ikke-EU-ordningen (ML § 66a / MSD artikel 358a)

Systemet skal anvende følgende ordningsspecifikke definitioner:

- **Afgiftspligtig person ikke etableret i EU:** En afgiftspligtig person som hverken har etableret hjemsted for sin økonomiske virksomhed eller har fast forretningssted i EU.
- **Identifikationsmedlemsland:** Det medlemsland som den afgiftspligtige person frit vælger at kontakte for at anmelde, hvornår den afgiftspligtige virksomhed inden for EU påbegyndes efter reglerne om Ikke-EU-ordningen.
- **Forbrugsmedlemsland:** Det medlemsland, hvor leveringen af ydelser anses for at finde sted i henhold til ML kapitel 4.

### FR-04: Definitioner — EU-ordningen (ML § 66d / MSD artikel 369a)

Systemet skal anvende følgende ordningsspecifikke definitioner:

- **Afgiftspligtig person ikke etableret i forbrugsmedlemslandet:** En afgiftspligtig person som har etableret sin økonomiske virksomhed i EU eller har et fast forretningssted i EU, men ikke i forbrugsmedlemslandenes område.

- **Identifikationsmedlemsland:** Fastlægges efter nedenstående prioriterede rækkefølge:
  - **(a)** Det land, hvor hjemstedet for den afgiftspligtige persons økonomiske virksomhed er etableret; eller, hvis hjemstedet ikke er i EU, det land, hvor et fast forretningssted er beliggende.
  - **(b)** Hvis hjemstedet ikke er i EU men der er mere end ét fast forretningssted: det af de relevante lande, som den afgiftspligtige person angiver at ville anvende.
  - **(c)** Hvis personen hverken har hjemsted i EU eller fast forretningssted: det land, hvorfra forsendelse eller transport af varerne påbegyndes. Hvis forsendelser påbegyndes fra mere end ét land, angiver personen hvilket land der er identifikationsmedlemsland.

- **Forbrugsmedlemsland:**
  - **(a)** For ydelser: det land, hvor leveringen anses for at finde sted i henhold til ML kapitel 4.
  - **(b)** For fjernsalg af varer inden for EU: det land, hvor forsendelse eller transport til kunden afsluttes.
  - **(c)** For varer leveret via elektronisk grænseflade (ML § 4c, stk. 2) med forsendelse/transport der påbegyndes og afsluttes i samme land: dette land.

### FR-05: Definitioner — Importordningen (ML § 66m / MSD artikel 369l)

Systemet skal anvende følgende ordningsspecifikke definitioner:

- **Afgiftspligtig person ikke etableret i EU:** En afgiftspligtig person som hverken har etableret hjemstedet for sin økonomiske virksomhed eller har et fast forretningssted i EU.

- **Formidler:** En person etableret i EU, udpeget af den afgiftspligtige person der foretager fjernsalg af varer indført fra steder uden for EU, til at være betalingspligtig for momsen og opfylde ordningens forpligtelser i den afgiftspligtige persons navn og på dennes vegne.

- **Identifikationsmedlemsland:** Fastlægges efter nedenstående:
  - **(a)** Hvis den afgiftspligtige person ikke er etableret i EU: det land, personen vælger at registrere sig i.
  - **(b)** Hvis den afgiftspligtige person er etableret uden for EU men har ét eller flere faste forretningssteder i EU: det land med fast forretningssted, som personen angiver at ville anvende.
  - **(c)** Hvis den afgiftspligtige person har etableret sin hjemsted for den økonomiske virksomhed i et EU-land: dette land.
  - **(d)** Hvis formidleren har etableret sin hjemsted for den økonomiske virksomhed i et EU-land: dette land.
  - **(e)** Hvis formidleren er etableret uden for EU men har ét eller flere faste forretningssteder i EU: det land med fast forretningssted, som formidleren angiver at ville anvende.

- **Forbrugsmedlemsland:** Det land, hvor forsendelse eller transport af varerne til kunden afsluttes.

### FR-06: Beløbsgrænse og udelukkelse for Importordningen

Systemet skal validere to udelukkelsesvilkår for Importordningen:

1. **Beløbsgrænse:** Reel forsendelsesværdi > 150 euro → leverancen er ikke berettiget til Importordningen.
2. **Varetype:** Punktafgiftspligtige varer → udelukket fra Importordningen uanset forsendelsesværdi.

Grænsen på 150 euro vurderes som inklusive grænse (≤ 150 euro er berettiget). Vurderingen foretages pr. forsendelse, ikke pr. vareenhed.

### FR-07: Forholdet til andre regler

Systemet skal respektere følgende regelhierarki ved alle afgørelser:

1. **OSS-særordningens specifikke regler** (ML §§ 66-66u og EU-direktivernes specialregler) — primær retskilde.
2. **Momslovens almindelige regler** — finder sekundært anvendelse på virksomheder tilsluttet en OSS-ordning, i det omfang lovgrundlaget for særordningerne ikke indeholder afvigende regler.
3. **Opkrævningslovens regler** — finder tertiært anvendelse på virksomheder uden for EU der leverer til ikkeafgiftspligtige kunder i Danmark eller foretager fjernsalg til/i Danmark, medmindre ML eller EU-forordninger fastsætter særlige regler.
4. **Forbrugsmedlemslandenes egne regler** — finder anvendelse i det omfang Gennemførelsesforordning (EU) nr. 282/2011 (som ændret ved 2019/2026) angiver dette.

### FR-08: Juridisk sporbarhed for klassifikationsafgørelser

Enhver klassifikationsafgørelse truffet af systemet skal ledsages af reference til den relevante:
- ML-paragraf (§ 66, § 66a, § 66d eller § 66m)
- MSD-artikel (artikel 358, 358a, 369a eller 369l)
- Gennemførelsesforordningsreference, hvor relevant (Gennemførelsesforordning 282/2011, artikel 57a)

---

## Ikke-funktionelle krav

Ingen ikke-funktionelle krav er specificeret i denne petition. Ydeevne, sikkerhed og tilgængelighed adresseres i efterfølgende petitioner.

---

## Juridisk grundlag

| Instrument | Reference | Rolle i OSS-01 |
|---|---|---|
| Direktiv 2008/8/EF | Artikel 5 | Grundlag for de tre ordninger |
| Direktiv 2017/2455 (EU) | Artikel 2 | Udvidelse til alle ydelser og varer; ændrer MSD art. 358, 358a, 369a, 369l |
| Direktiv 2019/1995 (EU) | Hele direktivet | Yderligere udvidelse af EU-ordning til fjernsalg |
| Forordning 904/2010 (EU) | Artikler 47a-47l | Samarbejde mellem EU-landes skattemyndigheder |
| Gennemførelsesforordning 2019/2026 (EU) | Hele | Ændrer Gennemførelsesforordning 282/2011 |
| Gennemførelsesforordning 2020/194 (EU) | Hele | Informationsudveksling om registreringsforhold og angivelsesindhold |
| Gennemførelsesforordning 282/2011 (EU) | Artikel 57a | Definitioner (identifikationsmedlemsland m.v.) |
| Momssystemdirektivet 2006/112/EF | Artikler 358, 358a, 369a, 369l | Primære EU-definitioner for alle tre ordninger |
| Momsloven (ML) | §§ 66, 66a, 66d, 66m, 66-66u | Dansk gennemførelse — primær national hjemmel |
| Lov nr. 554 af 2. juni 2014 | Hele | Første gennemførelse af ML §§ 66-66k |
| Lov nr. 810 af 9. juni 2020 | Hele | Udvidelse til ML §§ 66-66u (gælder fra 1. juli 2021) |
| Momsbekendtgørelsen | §§ 112-115 | Supplerende dansk implementering |
| Opkrævningsloven | Hele | Sekundær hjemmel for opkrævningsprocedurer |

---

## Antagelser

- Alle tre ordninger gælder fra 1. juli 2021 (ikrafttrædelsesdatoen for den udvidede OSS-ordning per lov nr. 810/2020).
- Grænsen på 150 euro vurderes på forsendelsesniveau (reel samlet forsendelsesværdi), ikke per vareenhed.
- Systemet anvender ML kapitel 4 (leveringsstedsregler) som forudsætningsforhold for fastlæggelse af forbrugsmedlemslandet for ydelsesleverancer — disse regler implementeres ikke inden for denne petition.
- Identifikationsmedlemslandsreglerne er implementeret som prioriteret beslutningslogik (ikke parallelle valgmuligheder).
- En afgiftspligtig person kan kun anvende én særordning ad gangen.

---

## Afgrænsning — hvad er IKKE i scope

| Emne | Dækkes af |
|---|---|
| Registreringsflow og anmeldelsesprocedurer (DA16.3.5) | Separat petition |
| Momsangivelse og angivelsesindhold (DA16.3.6) | Separat petition |
| Betaling og betalingsprocessering (DA16.3.7) | Separat petition |
| Regnskabsregler og record-keeping (DA16.3.8) | Separat petition |
| Tvangsframelding, udelukkelse og frivillig afmelding (DA16.3.5.6-5.7) | Separat petition |
| Konkrete leveringsstedsregler (ML kapitel 4) | Forudsætningsforhold — ikke implementeret her |
| Ydeevne, sikkerhed, tilgængelighed | Efterfølgende petitioner |

---

## Nøglereferencer

- `docs/references/DA16.3-regler-fra-2021.md`
- `docs/references/DA16.3.1-generelt-om-saerordningerne.md`
- `docs/references/DA16.3.1.1-formal.md`
- `docs/references/DA16.3.1.2-lovgrundlag.md`
- `docs/references/DA16.3.1.3-forholdet-til-andre-regler.md`
- `docs/references/DA16.3.1.4-definitioner.md`
- `docs/references/DA16.3.2-ikke-eu-ordningen.md`
- `docs/references/DA16.3.3-eu-ordningen.md`
- `docs/references/DA16.3.4-importordningen.md`
