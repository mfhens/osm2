package dk.osm2.scheme.steps;

import dk.osm2.scheme.dto.SchemeClassificationRequest;
import dk.osm2.scheme.dto.SchemeClassificationResult;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cucumber step definitions for OSS-01 — Juridisk grundlag, skemavurdering og definitioner.
 *
 * Legal basis: ML §§ 66, 66a, 66d, 66m / MSD artikler 358, 358a, 369a, 369l
 * Petition: OSS-01
 *
 * ALL steps in this file are INTENTIONALLY FAILING until the following production
 * classes are created:
 *   - dk.osm2.scheme.dto.SchemeClassificationRequest  (Java record)
 *   - dk.osm2.scheme.dto.SchemeClassificationResult   (Java record)
 *   - POST /api/v1/schemes/classify                    (REST endpoint)
 *
 * This file compiles only once the DTOs exist. It will then run and FAIL at
 * the HTTP-assertion level because the endpoint does not yet exist. That
 * constitutes the expected RED state at BDD test-first discipline.
 *
 * Step-to-field mapping follows petitions/OSS-01/OSS-01.feature exactly.
 * Every Gherkin scenario (27 total) is covered without omission.
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class SchemeClassificationSteps {

    // -----------------------------------------------------------------------
    // Infrastructure
    // -----------------------------------------------------------------------

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * Mutable scenario state shared across Given/When/Then within one scenario.
     * Reset before each scenario by {@link #resetWorld()}.
     */
    private SchemeClassificationRequest.Builder requestBuilder;
    private ResponseEntity<SchemeClassificationResult> response;

    @Before
    public void resetWorld() {
        requestBuilder = SchemeClassificationRequest.builder();
        response = null;
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private String baseUrl() {
        return "http://localhost:" + port + "/api/v1/schemes/classify";
    }

    private void callClassifyEndpoint() {
        SchemeClassificationRequest request = requestBuilder.build();
        response = restTemplate.postForEntity(baseUrl(), request, SchemeClassificationResult.class);
        assertThat(response).as("POST /api/v1/schemes/classify must return a response").isNotNull();
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("Expected 2xx from /api/v1/schemes/classify but got %s", response.getStatusCode())
                .isTrue();
    }

    private SchemeClassificationResult result() {
        assertThat(response).as("When step must be executed before Then assertions").isNotNull();
        return response.getBody();
    }

    // -----------------------------------------------------------------------
    // Given steps — OSS-01 § Ikke-EU-ordningen (ML § 66a / MSD art. 358a)
    // -----------------------------------------------------------------------

    /**
     * Maps to: hasEuSeatOfEconomicActivity = false
     * OSS-01 scenario: Ikke-EU-etableret person klassificeres under Ikke-EU-ordningen
     */
    @Given("en afgiftspligtig person uden hjemsted for sin økonomiske virksomhed i EU")
    public void enAfgiftspligtigPersonUdenHjemstedIEU() {
        requestBuilder.hasEuSeatOfEconomicActivity(false);
    }

    /**
     * Maps to: hasFixedEstablishmentInEu = false
     * OSS-01 scenarios: Ikke-EU-ordning, Importordning (diverse)
     */
    @Given("personen har intet fast forretningssted i EU")
    public void personenHarIntetFastForretningsstedIEU() {
        requestBuilder.hasFixedEstablishmentInEu(false);
    }

    // NOTE: "personen leverer ydelser til ikkeafgiftspligtige kunder i EU" is intentionally NOT
    // a separate literal step — it would be ambiguous with the parameterised "{word}" variant below.
    // It is handled by personenLevererYdelserTilKunderI("EU") which skips consumptionMemberState
    // when the parameter equals the generic token "EU".

    /**
     * Maps to: hasFixedEstablishmentInEu = true, fixedEstablishmentCountries = [land]
     * OSS-01 scenario: Person med fast forretningssted i EU klassificeres ikke under Ikke-EU-ordningen
     */
    @Given("personen har et fast forretningssted i {word}")
    public void personenHarEtFastForretningsstedI(String land) {
        requestBuilder
                .hasFixedEstablishmentInEu(true)
                .fixedEstablishmentCountries(List.of(land));
    }

    /**
     * Maps to: supplyType = SERVICES, consumptionMemberState = "OTHER"
     * OSS-01 scenario: Person med fast forretningssted i EU
     */
    @Given("personen leverer ydelser til ikkeafgiftspligtige kunder i andre EU-lande")
    public void personenLevererYdelserTilAndreEULande() {
        requestBuilder
                .supplyType(SchemeClassificationRequest.SupplyType.SERVICES)
                .consumptionMemberState("OTHER");
    }

    /**
     * Maps to: hasEuSeatOfEconomicActivity = false, hasFixedEstablishmentInEu = false,
     *          supplyType = SERVICES, enrolledScheme = NON_EU
     * OSS-01 scenario: Identifikationsmedlemsland for Ikke-EU-ordning er frit valg
     */
    @Given("en afgiftspligtig person berettiget til Ikke-EU-ordningen")
    public void enAfgiftspligtigPersonBerettigetTilIkkeEUOrdningen() {
        requestBuilder
                .hasEuSeatOfEconomicActivity(false)
                .hasFixedEstablishmentInEu(false)
                .supplyType(SchemeClassificationRequest.SupplyType.SERVICES)
                .enrolledScheme(SchemeClassificationRequest.EnrolledScheme.NON_EU);
    }

    /**
     * Maps to: selectedIdentificationMemberState = land
     * OSS-01 scenario: Identifikationsmedlemsland for Ikke-EU-ordning er frit valg
     */
    @Given("personen vælger {word} som identifikationsmedlemsland")
    public void personenVælgerSomIdentifikationsmedlemsland(String land) {
        requestBuilder.selectedIdentificationMemberState(land);
    }

    /**
     * Maps to: supplyType = SERVICES, consumptionMemberState = land
     * OSS-01 scenario: Forbrugsmedlemsland for Ikke-EU-ordning er leveringsstedet for ydelsen
     */
    @Given("leveringen af en ydelse anses for at finde sted i {word} i henhold til ML kapitel 4")
    public void leveringenAfEnYdelseAnsesFindeSted(String land) {
        requestBuilder
                .supplyType(SchemeClassificationRequest.SupplyType.SERVICES)
                .consumptionMemberState(land);
    }

    // -----------------------------------------------------------------------
    // Given steps — EU-ordningen (ML § 66d / MSD art. 369a)
    // -----------------------------------------------------------------------

    /**
     * Maps to: hasEuSeatOfEconomicActivity = true, seatOfEconomicActivityCountry = land
     * OSS-01 scenarios: EU-ordning (various)
     */
    @Given("en afgiftspligtig person med hjemsted for sin økonomiske virksomhed i {word}")
    public void enAfgiftspligtigPersonMedHjemstedILand(String land) {
        requestBuilder
                .hasEuSeatOfEconomicActivity(true)
                .seatOfEconomicActivityCountry(land);
    }

    /**
     * Maps to: supplyType = SERVICES, consumptionMemberState = land
     * Special case: when land == "EU" (the generic token from the non-EU scenario),
     * consumptionMemberState is left unset — the step covers general EU-wide delivery.
     * OSS-01 scenarios:
     *   "personen leverer ydelser til ikkeafgiftspligtige kunder i EU"     → land = "EU" (no member state)
     *   "personen leverer ydelser til ikkeafgiftspligtige kunder i Sverige" → land = "Sverige"
     *   "personen leverer ydelser til ikkeafgiftspligtige kunder i Danmark" → land = "Danmark"
     */
    @Given("personen leverer ydelser til ikkeafgiftspligtige kunder i {word}")
    public void personenLevererYdelserTilKunderI(String land) {
        requestBuilder.supplyType(SchemeClassificationRequest.SupplyType.SERVICES);
        // "EU" is a generic token meaning "any EU member state" — no specific consumptionMemberState set.
        if (!"EU".equals(land)) {
            requestBuilder.consumptionMemberState(land);
        }
    }

    /**
     * Maps to: supplyType = DISTANCE_SALES_INTRA_EU, consumptionMemberState = land
     * OSS-01 scenario: EU-ordning gælder for fjernsalg af varer inden for EU
     */
    @Given("personen foretager fjernsalg af varer til ikkeafgiftspligtige kunder i {word}")
    public void personenFoRetagerFjernsalgAfVarerTilKunderI(String land) {
        requestBuilder
                .supplyType(SchemeClassificationRequest.SupplyType.DISTANCE_SALES_INTRA_EU)
                .consumptionMemberState(land);
    }

    /**
     * Maps to: hasEuSeatOfEconomicActivity = true, enrolledScheme = EU
     * OSS-01 scenarios: EU-ordning identifikation/forbrugsmedlem
     */
    @Given("en afgiftspligtig person berettiget til EU-ordningen")
    public void enAfgiftspligtigPersonBerettigetTilEUOrdningen() {
        requestBuilder
                .hasEuSeatOfEconomicActivity(true)
                .enrolledScheme(SchemeClassificationRequest.EnrolledScheme.EU);
    }

    /**
     * Maps to: hasEuSeatOfEconomicActivity = true, seatOfEconomicActivityCountry = land
     * OSS-01 scenario: Identifikationsmedlemsland for EU-ordning er hjemstedslandet
     */
    @Given("personen har hjemsted for sin økonomiske virksomhed i {word}")
    public void personenHarHjemstedForSinOkonomiskeVirksomhedI(String land) {
        requestBuilder
                .hasEuSeatOfEconomicActivity(true)
                .seatOfEconomicActivityCountry(land);
    }

    /**
     * Maps to: hasEuSeatOfEconomicActivity = false
     * OSS-01 scenarios: EU-ordning med multiple faste forretningssteder
     */
    @Given("personen har ikke etableret hjemsted for sin økonomiske virksomhed i EU")
    public void personenHarIkkeEtableretHjemstedIEU() {
        requestBuilder.hasEuSeatOfEconomicActivity(false);
    }

    /**
     * Maps to: hasFixedEstablishmentInEu = true, fixedEstablishmentCountries = [land]
     * OSS-01 scenario: EU-ordning — ét fast forretningssted
     */
    @Given("personen har ét fast forretningssted i {word}")
    public void personenHarEtFastForretningsstedEnkelt(String land) {
        requestBuilder
                .hasFixedEstablishmentInEu(true)
                .fixedEstablishmentCountries(List.of(land));
    }

    /**
     * Maps to: hasFixedEstablishmentInEu = true, fixedEstablishmentCountries = [land1, land2]
     * OSS-01 scenario: EU-ordning med multiple faste forretningssteder
     */
    @Given("personen har faste forretningssteder i {word} og {word}")
    public void personenHarFasteForretningsstedeIToLande(String land1, String land2) {
        requestBuilder
                .hasFixedEstablishmentInEu(true)
                .fixedEstablishmentCountries(List.of(land1, land2));
    }

    /**
     * Maps to: selectedIdentificationMemberState = land
     * OSS-01 scenario: EU-ordning — valgt land ved multiple faste forretningssteder
     */
    @Given("personen angiver {word} som det land, hvori EU-ordningen ønskes anvendt")
    public void personenAngiver(String land) {
        requestBuilder.selectedIdentificationMemberState(land);
    }

    /**
     * Maps to: shipmentStartCountry = land
     * OSS-01 scenario: EU-ordning — forsendelseslandet ved ingen EU-etablering
     */
    @Given("forsendelse af varer påbegyndes fra {word}")
    public void forsendelseAfVarerPåbegyndesFra(String land) {
        requestBuilder.shipmentStartCountry(land);
    }

    /**
     * Maps to: supplyType = DISTANCE_SALES_INTRA_EU
     * OSS-01 scenario: EU-ordning fjernsalg — forbrugsmedlemsland
     */
    @Given("personen foretager fjernsalg af varer")
    public void personenFoRetagerFjernsalgAfVarer() {
        requestBuilder.supplyType(SchemeClassificationRequest.SupplyType.DISTANCE_SALES_INTRA_EU);
    }

    /**
     * Maps to: consumptionMemberState = land
     * OSS-01 scenario: EU-ordning fjernsalg — forbrugsmedlemsland
     */
    @Given("forsendelsen af varerne til kunden afsluttes i {word}")
    public void forsendelsenAfVarerneTilKundenAfsluttesI(String land) {
        requestBuilder.consumptionMemberState(land);
    }

    /**
     * Maps to: supplyType = SERVICES, consumptionMemberState = land
     * OSS-01 scenario: EU-ordning ydelse — forbrugsmedlemsland
     */
    @Given("personen leverer en ydelse der anses for at finde sted i {word} i henhold til ML kapitel 4")
    public void personenLevererEnYdelseDerAnsesFindeSted(String land) {
        requestBuilder
                .supplyType(SchemeClassificationRequest.SupplyType.SERVICES)
                .consumptionMemberState(land);
    }

    /**
     * Maps to: viaElectronicInterface = true, supplyType = DISTANCE_SALES_INTRA_EU
     * OSS-01 scenario: EU-ordning — varer via elektronisk grænseflade
     * ML § 4c, stk. 2 / MSD art. 369a
     */
    @Given("personen leverer varer via en elektronisk grænseflade i henhold til ML § 4c, stk. 2")
    public void personenLevererVarerViaElektroniskGrænse() {
        requestBuilder
                .viaElectronicInterface(true)
                .supplyType(SchemeClassificationRequest.SupplyType.DISTANCE_SALES_INTRA_EU);
    }

    /**
     * Maps to: consumptionMemberState = land, shipmentStartCountry = land
     * OSS-01 scenario: EU-ordning — forsendelse og transport afsluttes i samme land
     */
    @Given("forsendelse og transport af varerne påbegyndes og afsluttes i {word}")
    public void forsendelseOgTransportPåbegyndesOgAfsluttesI(String land) {
        requestBuilder
                .consumptionMemberState(land)
                .shipmentStartCountry(land);
    }

    // -----------------------------------------------------------------------
    // Given steps — Importordningen (ML § 66m / MSD art. 369l)
    // -----------------------------------------------------------------------

    /**
     * Maps to: supplyType = IMPORT_GOODS, consumptionMemberState = land
     * OSS-01 scenario: Importordning klassificeres korrekt ved forsendelsesværdi under beløbsgrænsen
     * "Kina" in the step is used as the origin country in the narrative; land is the consumption country.
     */
    @Given("fjernsalg af varer indført fra Kina til en ikkeafgiftspligtig kunde i {word}")
    public void fjernsalgAfVarerIndførtFraKinaTilKundeI(String land) {
        requestBuilder
                .supplyType(SchemeClassificationRequest.SupplyType.IMPORT_GOODS)
                .consumptionMemberState(land);
    }

    /**
     * Maps to: shipmentValue = value
     * OSS-01 scenarios: Importordning — forsendelsesværdi under/lig 150 euro
     */
    @Given("forsendelsens reelle værdi er {int} euro")
    public void forsendelsenReelleVærdiEr(int value) {
        requestBuilder.shipmentValue(BigDecimal.valueOf(value));
    }

    /**
     * Maps to: shipmentValue = value (exact boundary test)
     * OSS-01 scenario: Importordning — præcis 150 euro
     */
    @Given("forsendelsens reelle værdi er præcis {int} euro")
    public void forsendelsenReelleVærdiErPræcis(int value) {
        requestBuilder.shipmentValue(BigDecimal.valueOf(value));
    }

    /**
     * Maps to: excisable = false
     * OSS-01 scenarios: Importordning — ikke punktafgiftspligtig
     */
    @Given("varerne er ikke punktafgiftspligtige")
    public void varerneErIkkePunktafgiftspligtige() {
        requestBuilder.excisable(false);
    }

    /**
     * Maps to: excisable = true
     * OSS-01 scenario: Importordning afvises for punktafgiftspligtige varer
     */
    @Given("varerne er punktafgiftspligtige")
    public void varerneErPunktafgiftspligtige() {
        requestBuilder.excisable(true);
    }

    /**
     * Maps to: supplyType = IMPORT_GOODS
     * OSS-01 scenarios: Importordning (generisk fra et land uden for EU)
     */
    @Given("fjernsalg af varer indført fra et land uden for EU")
    public void fjernsalgAfVarerIndførtFraEtLandUdenForEU() {
        requestBuilder.supplyType(SchemeClassificationRequest.SupplyType.IMPORT_GOODS);
    }

    /**
     * Maps to: hasEuSeatOfEconomicActivity = false, hasFixedEstablishmentInEu = false,
     *          enrolledScheme = IMPORT
     * OSS-01 scenario: Formidler udpeges for Importordning
     */
    @Given("en ikke-EU-etableret afgiftspligtig person der benytter Importordningen")
    public void enIkkeEUEtableretAfgiftspligtigPersonDerBenytterImportordningen() {
        requestBuilder
                .hasEuSeatOfEconomicActivity(false)
                .hasFixedEstablishmentInEu(false)
                .enrolledScheme(SchemeClassificationRequest.EnrolledScheme.IMPORT);
    }

    /**
     * Maps to: hasIntermediary = true, intermediaryEstablishmentCountry = land
     * OSS-01 scenario: Formidler udpeges for Importordning
     * ML § 66m, nr. 3, litra d / MSD art. 369l
     */
    @Given("personen udpeger en formidler etableret i {word}")
    public void personenUdpegerEnFormidlerEtableretI(String land) {
        requestBuilder
                .hasIntermediary(true)
                .intermediaryEstablishmentCountry(land);
    }

    /**
     * Maps to: enrolledScheme = IMPORT, supplyType = IMPORT_GOODS
     * OSS-01 scenario: Identifikationsmedlemsland for Importordning — EU-etableringsland
     */
    @Given("personen benytter Importordningen")
    public void personenBenytterImportordningen() {
        requestBuilder
                .enrolledScheme(SchemeClassificationRequest.EnrolledScheme.IMPORT)
                .supplyType(SchemeClassificationRequest.SupplyType.IMPORT_GOODS);
    }

    /**
     * Maps to: hasIntermediary = false
     * OSS-01 scenario: Identifikationsmedlemsland for Importordning — EU-etableringsland
     */
    @Given("personen har ikke udpeget en formidler")
    public void personenHarIkkeUdpegerEnFormidler() {
        requestBuilder.hasIntermediary(false);
    }

    /**
     * Maps to: enrolledScheme = IMPORT, supplyType = IMPORT_GOODS, hasIntermediary = false
     * OSS-01 scenario: Identifikationsmedlemsland for Importordning — frit valg
     */
    @Given("personen benytter Importordningen uden formidler")
    public void personenBenytterImportordningenUdenFormidler() {
        requestBuilder
                .enrolledScheme(SchemeClassificationRequest.EnrolledScheme.IMPORT)
                .supplyType(SchemeClassificationRequest.SupplyType.IMPORT_GOODS)
                .hasIntermediary(false);
    }

    /**
     * Maps to: enrolledScheme = IMPORT, supplyType = IMPORT_GOODS
     * OSS-01 scenario: Forbrugsmedlemsland for Importordning er leveringsmodtagelseslandet
     */
    @Given("en leverance under Importordningen")
    public void enLeveranceUnderImportordningen() {
        requestBuilder
                .enrolledScheme(SchemeClassificationRequest.EnrolledScheme.IMPORT)
                .supplyType(SchemeClassificationRequest.SupplyType.IMPORT_GOODS);
    }

    /**
     * Maps to: consumptionMemberState = land
     * OSS-01 scenario: Forbrugsmedlemsland for Importordning er leveringsmodtagelseslandet
     */
    @Given("forsendelsen til kunden afsluttes i {word}")
    public void forsendelsenTilKundenAfsluttesI(String land) {
        requestBuilder.consumptionMemberState(land);
    }

    /**
     * Maps to: shipmentValue = null
     * OSS-01 scenario: Systemet kan ikke klassificere Importordning uden angivet forsendelsesværdi
     */
    @Given("forsendelsens reelle værdi er ikke angivet")
    public void forsendelsenReelleVærdiErIkkeAngivet() {
        requestBuilder.shipmentValue(null);
    }

    // -----------------------------------------------------------------------
    // Given steps — Regelhierarki (DA16.3.1.3)
    // -----------------------------------------------------------------------

    /**
     * Maps to: enrolledScheme = NON_EU, queryRuleHierarchy = true
     * OSS-01 scenario: Momslovens almindelige regler finder sekundær anvendelse
     */
    @Given("en afgiftspligtig person tilsluttet Ikke-EU-ordningen")
    public void enAfgiftspligtigPersonTilsluttetIkkeEUOrdningen() {
        requestBuilder
                .enrolledScheme(SchemeClassificationRequest.EnrolledScheme.NON_EU)
                .queryRuleHierarchy(true);
    }

    /**
     * No additional field — implied by queryRuleHierarchy.
     * OSS-01 scenario: Momslovens almindelige regler finder sekundær anvendelse
     */
    @And("en situation er ikke udtømmende reguleret af OSS-særordningens specifikke regler")
    public void enSituationErIkkeUdtømmendReguleret() {
        // Implied by queryRuleHierarchy = true — no additional request field required.
    }

    /**
     * Maps to: hasEuSeatOfEconomicActivity = false, enrolledScheme = NON_EU, queryRuleHierarchy = true
     * OSS-01 scenario: Opkrævningslovens regler finder tertiær anvendelse
     */
    @Given("en afgiftspligtig person etableret uden for EU tilsluttet en OSS-ordning")
    public void enAfgiftspligtigPersonEtableretUdenForEUTilsluttetOSS() {
        requestBuilder
                .hasEuSeatOfEconomicActivity(false)
                .enrolledScheme(SchemeClassificationRequest.EnrolledScheme.NON_EU)
                .queryRuleHierarchy(true);
    }

    /**
     * No additional field.
     * OSS-01 scenario: Opkrævningslovens regler finder tertiær anvendelse
     */
    @And("en procedure er ikke reguleret af ML eller OSS-specifik lovgivning")
    public void enProcedureErIkkeReguleretsAfML() {
        // No additional request field required beyond queryRuleHierarchy = true.
    }

    /**
     * Maps to: hasEuSeatOfEconomicActivity = true, seatOfEconomicActivityCountry = "DK",
     *          supplyType = SERVICES, consumptionMemberState = "SE"
     * OSS-01 scenario: Klassifikationsafgørelse ledsages altid af juridisk hjemmel
     */
    @Given("en afgiftspligtig person vurderet af systemet")
    public void enAfgiftspligtigPersonVurderet() {
        requestBuilder
                .hasEuSeatOfEconomicActivity(true)
                .seatOfEconomicActivityCountry("DK")
                .supplyType(SchemeClassificationRequest.SupplyType.SERVICES)
                .consumptionMemberState("SE");
    }

    // -----------------------------------------------------------------------
    // Given steps — Utilstrækkelige oplysninger
    // -----------------------------------------------------------------------

    /**
     * Maps to: all null (empty request)
     * OSS-01 scenario: Systemet kan ikke klassificere uden etableringsoplysninger
     */
    @Given("en afgiftspligtig person uden angivelse af etableringsland eller etableringstype")
    public void enAfgiftspligtigPersonUdenAngivelseAfEtableringsland() {
        // Builder left in default state — all fields null/unset.
    }

    // -----------------------------------------------------------------------
    // When steps — API calls (all route to POST /api/v1/schemes/classify)
    // -----------------------------------------------------------------------

    /**
     * OSS-01 — when: vurderer berettigelsen til OSS-særordning
     * Petition: OSS-01 / ML § 66, 66a, 66d, 66m
     */
    @When("systemet vurderer berettigelsen til OSS-særordning")
    public void systemetVurdererBerettigelsenTilOSSSærordning() {
        callClassifyEndpoint();
    }

    /**
     * OSS-01 — when: fastlægger identifikationsmedlemslandet
     * Petition: OSS-01 / ML § 66a nr. 2, 66d nr. 2, 66m nr. 3
     */
    @When("systemet fastlægger identifikationsmedlemslandet")
    public void systemetFastlæggerIdentifikationsmedlemslandet() {
        callClassifyEndpoint();
    }

    /**
     * OSS-01 — when: fastlægger forbrugsmedlemslandet
     * Petition: OSS-01 / ML § 66a nr. 3, 66d nr. 3, 66m nr. 4
     */
    @When("systemet fastlægger forbrugsmedlemslandet")
    public void systemetFastlæggerForbrugsmedlemslandet() {
        callClassifyEndpoint();
    }

    /**
     * OSS-01 — when: fastslår hvilke regler der finder anvendelse
     * Petition: OSS-01 / DA16.3.1.3
     */
    @When("systemet fastslår hvilke regler der finder anvendelse")
    public void systemetFastslårHvilkeReglerFinderAnvendelse() {
        callClassifyEndpoint();
    }

    /**
     * OSS-01 — when: returnerer en klassifikationsafgørelse
     * Petition: OSS-01
     */
    @When("systemet returnerer en klassifikationsafgørelse")
    public void systemetReturnererEnKlassifikationsafgørelse() {
        callClassifyEndpoint();
    }

    /**
     * OSS-01 — when: vurderer berettigelsen til Importordningen
     * Petition: OSS-01 / ML § 66m
     */
    @When("systemet vurderer berettigelsen til Importordningen")
    public void systemetVurdererBerettigelsenTilImportordningen() {
        callClassifyEndpoint();
    }

    // -----------------------------------------------------------------------
    // Then steps — assertions on SchemeClassificationResult
    // -----------------------------------------------------------------------

    /**
     * OSS-01 scenario: Ikke-EU-ordningen klassificering
     * Asserts: result.scheme == "NON_EU"
     * Legal basis: ML § 66a / MSD artikel 358a
     */
    @Then("klassificeres personen som berettiget til {string}")
    public void klassificeresPersoenSomBerettiget(String expectedScheme) {
        String schemeCode = toSchemeCode(expectedScheme);
        assertThat(result().scheme())
                .as("Expected person to be classified under scheme '%s' (code '%s') — OSS-01", expectedScheme, schemeCode)
                .isEqualTo(schemeCode);
    }

    /**
     * OSS-01 scenarios: alle scenarer med juridisk hjemmel
     * Asserts: result.legalBasis contains text
     * Legal basis: ML § 66a / 66d / 66m + MSD art. 358a / 369a / 369l / DA16.3.1.3
     */
    @Then("hjemmel angives som {string}")
    public void hjemmelAngivesSom(String expectedBasis) {
        assertThat(result().legalBasis())
                .as("Expected legalBasis to contain '%s' — OSS-01 juridisk sporbarhed", expectedBasis)
                .contains(expectedBasis);
    }

    /**
     * OSS-01 scenario: Person med fast forretningssted — ikke Ikke-EU
     * Asserts: result.scheme != "NON_EU"
     */
    @Then("klassificeres personen ikke som berettiget til {string}")
    public void klassificeresPersoenIkkeSomBerettiget(String excludedScheme) {
        String schemeCode = toSchemeCode(excludedScheme);
        assertThat(result().scheme())
                .as("Expected person NOT to be classified under scheme '%s' — OSS-01", excludedScheme)
                .isNotEqualTo(schemeCode);
    }

    /**
     * OSS-01 scenario: Identifikationsmedlemsland fastlægges korrekt
     * Asserts: result.identificationMemberState == land
     * Legal basis: ML § 66a nr. 2, § 66d nr. 2, § 66m nr. 3
     */
    @Then("er identifikationsmedlemslandet {string}")
    public void erIdentifikationsmedlemslandet(String expectedCountry) {
        assertThat(result().identificationMemberState())
                .as("Expected identificationMemberState to be '%s' — OSS-01", expectedCountry)
                .isEqualTo(expectedCountry);
    }

    /**
     * OSS-01 scenario: Forbrugsmedlemsland fastlægges korrekt
     * Asserts: result.consumptionMemberState == land
     * Legal basis: ML § 66a nr. 3, § 66d nr. 3, § 66m nr. 4
     */
    @Then("er forbrugsmedlemslandet {string}")
    public void erForbrugsmedlemslandet(String expectedCountry) {
        assertThat(result().consumptionMemberState())
                .as("Expected consumptionMemberState to be '%s' — OSS-01", expectedCountry)
                .isEqualTo(expectedCountry);
    }

    /**
     * OSS-01 scenario: EU-ordning gælder ikke når leverandør og kunde er i samme land
     * Asserts: result.status == "NO_OSS_SCHEME"
     */
    @Then("klassificeres personen ikke som berettiget til nogen OSS-særordning")
    public void klassificeresPersoenIkkeSomBerettigetTilNogenOSS() {
        assertThat(result().status())
                .as("Expected status NO_OSS_SCHEME when no OSS scheme applies — OSS-01")
                .isEqualTo("NO_OSS_SCHEME");
    }

    /**
     * OSS-01 scenarios: error/rejection messages
     * Asserts: result.message == text
     */
    @Then("systemet returnerer {string}")
    public void systemetReturnerer(String expectedMessage) {
        assertThat(result().message())
                .as("Expected message '%s' — OSS-01", expectedMessage)
                .isEqualTo(expectedMessage);
    }

    /**
     * OSS-01 scenario: Importordning klassificeres korrekt
     * Asserts: result.scheme == "IMPORT"
     * Legal basis: ML § 66m / MSD art. 369l
     */
    @Then("klassificeres leverancen som berettiget til {string}")
    public void klassificeresLeverancenSomBerettiget(String expectedScheme) {
        String schemeCode = toSchemeCode(expectedScheme);
        assertThat(result().scheme())
                .as("Expected delivery to be classified under scheme '%s' (code '%s') — OSS-01", expectedScheme, schemeCode)
                .isEqualTo(schemeCode);
    }

    /**
     * OSS-01 scenarios: Importordning afvist
     * Asserts: result.scheme != "IMPORT"
     */
    @Then("klassificeres leverancen ikke som berettiget til {string}")
    public void klassificeresLeverancenIkkeSomBerettiget(String excludedScheme) {
        String schemeCode = toSchemeCode(excludedScheme);
        assertThat(result().scheme())
                .as("Expected delivery NOT to be classified under scheme '%s' — OSS-01", excludedScheme)
                .isNotEqualTo(schemeCode);
    }

    /**
     * OSS-01 scenario: Momslovens almindelige regler finder sekundær anvendelse
     * Asserts: result.applicableRules contains "ML almindelige regler (sekundær)"
     * Legal basis: DA16.3.1.3
     */
    @Then("finder Momslovens almindelige regler sekundært anvendelse")
    public void finderMomslovensAlmindeligeReglerSekundærtAnvendelse() {
        assertThat(result().applicableRules())
                .as("Expected applicableRules to contain 'ML almindelige regler (sekundær)' — OSS-01 / DA16.3.1.3")
                .contains("ML almindelige regler (sekundær)");
    }

    /**
     * OSS-01 scenario: Opkrævningslovens regler finder tertiær anvendelse
     * Asserts: result.applicableRules contains "Opkrævningsloven (tertiær)"
     * Legal basis: DA16.3.1.3
     */
    @Then("finder Opkrævningslovens regler tertiært anvendelse")
    public void finderOpkrævningslovensReglerTertiærtAnvendelse() {
        assertThat(result().applicableRules())
                .as("Expected applicableRules to contain 'Opkrævningsloven (tertiær)' — OSS-01 / DA16.3.1.3")
                .contains("Opkrævningsloven (tertiær)");
    }

    /**
     * OSS-01 scenario: Klassifikationsafgørelse ledsages altid af juridisk hjemmel — ML paragraf
     * Asserts: at least one element in result.legalBasis starts with "ML §"
     */
    @Then("indeholder afgørelsen reference til den relevante ML-paragraf")
    public void indeholderAfgørelsenReferenceMLParagraf() {
        assertThat(result().legalBasis())
                .as("Expected legalBasis to contain at least one 'ML §' reference — OSS-01 juridisk sporbarhed")
                .anyMatch(basis -> basis.startsWith("ML §"));
    }

    /**
     * OSS-01 scenario: Klassifikationsafgørelse ledsages altid af juridisk hjemmel — MSD artikel
     * Asserts: at least one element in result.legalBasis contains "MSD artikel"
     */
    @Then("indeholder afgørelsen reference til den relevante MSD-artikel")
    public void indeholderAfgørelsenReferenceMSDArtikel() {
        assertThat(result().legalBasis())
                .as("Expected legalBasis to contain at least one 'MSD artikel' reference — OSS-01 juridisk sporbarhed")
                .anyMatch(basis -> basis.contains("MSD artikel"));
    }

    /**
     * OSS-01 scenario: Utilstrækkelige oplysninger (general)
     * Asserts: result.message == text (same as systemet returnerer but phrased as returnerer systemet)
     */
    @Then("returnerer systemet {string}")
    public void returnererSystemet(String expectedMessage) {
        assertThat(result().message())
                .as("Expected message '%s' for insufficient-data scenario — OSS-01", expectedMessage)
                .isEqualTo(expectedMessage);
    }

    /**
     * OSS-01 scenario: Ingen klassifikationsafgørelse (general)
     * Asserts: result.scheme is null
     */
    @Then("foretages ingen klassifikationsafgørelse")
    public void foretagesIngenKlassifikationsafgørelse() {
        assertThat(result().scheme())
                .as("Expected scheme to be null when classification cannot be made — OSS-01")
                .isNull();
    }

    /**
     * OSS-01 scenario: Ingen klassifikationsafgørelse for Importordningen
     * Asserts: result.scheme != "IMPORT"
     */
    @Then("foretages ingen klassifikationsafgørelse for Importordningen")
    public void foretagesIngenKlassifikationsafgørelseForImportordningen() {
        assertThat(result().scheme())
                .as("Expected scheme NOT to be 'IMPORT' when import classification cannot be made — OSS-01")
                .isNotEqualTo("IMPORT");
    }

    // -----------------------------------------------------------------------
    // Utility
    // -----------------------------------------------------------------------

    /**
     * Converts the Danish display name used in Gherkin to the internal scheme code
     * used by {@link SchemeClassificationResult#scheme()}.
     *
     * OSS-01 mapping:
     *   "Ikke-EU-ordningen" → "NON_EU"
     *   "EU-ordningen"      → "EU"
     *   "Importordningen"   → "IMPORT"
     */
    private static String toSchemeCode(String displayName) {
        return switch (displayName) {
            case "Ikke-EU-ordningen" -> "NON_EU";
            case "EU-ordningen"      -> "EU";
            case "Importordningen"   -> "IMPORT";
            default -> displayName; // pass through for unexpected values (test will surface the mismatch)
        };
    }
}
