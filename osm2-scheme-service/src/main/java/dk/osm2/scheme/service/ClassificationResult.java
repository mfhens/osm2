package dk.osm2.scheme.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable result object inserted into the Drools session as a working-memory fact.
 *
 * <p>Fields are public so MVEL can write them directly. Rules call {@code update($res)} after
 * modifying fields so that Drools re-evaluates dependent rules.
 *
 * <p>Petition: OSS-01
 */
public class ClassificationResult {

    /** Classification outcome (e.g. {@code "ELIGIBLE"}, {@code "NO_OSS_SCHEME"}). */
    public String status;

    /** Determined scheme code (e.g. {@code "NON_EU"}, {@code "EU"}, {@code "IMPORT"}). */
    public String scheme;

    /** Determined identification member state (country name or code). */
    public String identificationMemberState;

    /** Determined consumption member state (country name or code). */
    public String consumptionMemberState;

    /** Legal basis citations. */
    public List<String> legalBasis = new ArrayList<>();

    /** Human-readable Danish explanation for non-eligible outcomes. */
    public String message;

    /** Applicable rule layers (FR-07 / DA16.3.1.3). */
    public List<String> applicableRules = new ArrayList<>();
}
