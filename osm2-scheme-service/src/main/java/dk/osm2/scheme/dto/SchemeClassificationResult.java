package dk.osm2.scheme.dto;

import java.util.List;

/**
 * Result of a scheme eligibility classification.
 *
 * <p>All coded fields ({@code status}, {@code scheme}) are plain {@link String} values carrying the
 * enum constant name so that Jackson serialises them as JSON strings and AssertJ can compare them
 * with string literals in step definitions.
 *
 * <p>Petition: OSS-01 / ML §§ 66, 66a, 66d, 66m
 *
 * <p>Status codes:
 *
 * <ul>
 *   <li>{@code ELIGIBLE} — person is eligible for the determined scheme
 *   <li>{@code INELIGIBLE} — excluded from all OSS schemes (e.g. excisable goods, value > €150)
 *   <li>{@code NO_OSS_SCHEME} — supply characteristics exclude OSS (e.g. same country)
 *   <li>{@code INSUFFICIENT_INFORMATION} — required fields are absent
 * </ul>
 *
 * <p>Scheme codes (ADR-0031 — statutory, do not rename):
 *
 * <ul>
 *   <li>{@code NON_EU} — Ikke-EU-ordningen (ML § 66a / MSD art. 358a)
 *   <li>{@code EU} — EU-ordningen (ML § 66d / MSD art. 369a)
 *   <li>{@code IMPORT} — Importordningen (ML § 66m / MSD art. 369l)
 * </ul>
 */
public record SchemeClassificationResult(
    /** Classification outcome code. */
    String status,

    /**
     * Determined OSS scheme code, or {@code null} when no scheme was determined (INELIGIBLE,
     * INSUFFICIENT_INFORMATION).
     */
    String scheme,

    /** Determined identification member state. */
    String identificationMemberState,

    /** Determined consumption member state. */
    String consumptionMemberState,

    /**
     * Legal basis citations. Each entry has format {@code "ML § <para> / MSD artikel <art>"}. Every
     * non-error result carries at least one entry (FR-08 / AC-26).
     */
    List<String> legalBasis,

    /** Human-readable explanation (Danish), populated for INELIGIBLE, NO_OSS_SCHEME, and errors. */
    String message,

    /**
     * Applicable rule layers, populated when {@code queryRuleHierarchy == true} (FR-07 /
     * DA16.3.1.3).
     */
    List<String> applicableRules) {}
