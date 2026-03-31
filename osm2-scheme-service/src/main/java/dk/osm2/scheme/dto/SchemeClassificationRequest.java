package dk.osm2.scheme.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Input for scheme eligibility classification.
 *
 * <p>A Lombok-built Java bean so that:
 *
 * <ol>
 *   <li>Step definitions can call {@code SchemeClassificationRequest.builder()…build()}.
 *   <li>Drools MVEL can resolve properties via standard JavaBean {@code getXxx()} getters.
 * </ol>
 *
 * <p>Nested enums {@link SupplyType} and {@link EnrolledScheme} are referenced from step
 * definitions as {@code SchemeClassificationRequest.SupplyType.SERVICES} etc.
 *
 * <p>Petition: OSS-01 / ML §§ 66, 66a, 66d, 66m
 */
@Data
@Builder(builderClassName = "Builder")
@NoArgsConstructor
@AllArgsConstructor
public class SchemeClassificationRequest {

    // -----------------------------------------------------------------------
    // Nested enums — accessible as SchemeClassificationRequest.SupplyType etc.
    // -----------------------------------------------------------------------

    /**
     * Discriminates the type of supply (FR-01, FR-02, FR-05).
     *
     * <p>ADR-0031: constants are statutory — do not rename.
     */
    public enum SupplyType {
        SERVICES,
        DISTANCE_SALES_INTRA_EU,
        IMPORT_GOODS
    }

    /**
     * The scheme in which the taxable person is currently enrolled, used when the caller already
     * knows the scheme and wants ID/consumption MS determination (FR-03, FR-04, FR-05).
     *
     * <p>ADR-0031: constants are statutory — do not rename.
     */
    public enum EnrolledScheme {
        NON_EU,
        EU,
        IMPORT
    }

    // -----------------------------------------------------------------------
    // Establishment fields (FR-01, FR-03, FR-04, FR-05)
    // -----------------------------------------------------------------------

    /** True if the taxable person has its seat of economic activity inside the EU. */
    private Boolean hasEuSeatOfEconomicActivity;

    /** ISO 3166-1 alpha-2 country code of the EU seat of economic activity, if present. */
    private String seatOfEconomicActivityCountry;

    /** True if the taxable person has one or more fixed establishments inside the EU. */
    private Boolean hasFixedEstablishmentInEu;

    /** Country names/codes of all EU fixed establishments. */
    private List<String> fixedEstablishmentCountries;

    /**
     * Country the taxable person chooses as identification member state when free choice applies
     * (Non-EU: always; EU: multiple FE; Import: non-EU person without intermediary).
     */
    private String selectedIdentificationMemberState;

    // -----------------------------------------------------------------------
    // Supply fields
    // -----------------------------------------------------------------------

    /** Type of supply for which classification is requested. */
    private SupplyType supplyType;

    /** Member state where the supply is consumed / received. */
    private String consumptionMemberState;

    // -----------------------------------------------------------------------
    // Import-specific fields (FR-05, FR-06)
    // -----------------------------------------------------------------------

    /** Country from which the shipment originates. */
    private String shipmentStartCountry;

    /** True if the goods are subject to excise duty (always excluded from Import — FR-06). */
    private Boolean excisable;

    /** Intrinsic/real value of the shipment in EUR (€150 threshold — FR-06). */
    private BigDecimal shipmentValue;

    /** True if an intermediary has been designated for the Import scheme (FR-05). */
    private Boolean hasIntermediary;

    /** Country where the intermediary is established (FR-05). */
    private String intermediaryEstablishmentCountry;

    // -----------------------------------------------------------------------
    // EU electronic interface field (FR-04)
    // -----------------------------------------------------------------------

    /**
     * True when goods are supplied via an electronic interface deemed to be the supplier under ML §
     * 4c, stk. 2 / MSD art. 14a (FR-04).
     */
    private Boolean viaElectronicInterface;

    // -----------------------------------------------------------------------
    // Classification context
    // -----------------------------------------------------------------------

    /**
     * The scheme in which the person is currently enrolled — drives FR-03/FR-04/FR-05 identification
     * and consumption MS determination without re-running FR-01 eligibility.
     */
    private EnrolledScheme enrolledScheme;

    /**
     * When true, FR-07 rule-hierarchy rules fire and populate {@code applicableRules} in the result
     * (DA16.3.1.3).
     */
    private Boolean queryRuleHierarchy;
}
