package dk.osm2.taxableperson.client;

/**
 * Subset of {@code SchemeClassificationResult} returned by the scheme-service.
 *
 * <p>Fields map to JSON properties produced by
 * {@code dk.osm2.scheme.dto.SchemeClassificationResult}.
 */
public record SchemeClassificationResponse(
    /** Classification status: ELIGIBLE | INELIGIBLE | NO_OSS_SCHEME | INSUFFICIENT_INFORMATION */
    String status,

    /** Scheme code: NON_EU | EU | IMPORT — or {@code null} when not eligible. */
    String scheme,

    /** Human-readable explanation (Danish). */
    String message,

    /** First legal-basis citation, e.g. "ML § 66a / MSD artikel 358a". */
    String identificationMemberState) {

  /** Convenience helper: true when status == "ELIGIBLE". */
  public boolean eligible() {
    return "ELIGIBLE".equals(status);
  }
}
