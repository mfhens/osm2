package dk.osm2.taxableperson.web.wizard;

import java.time.LocalDate;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

/**
 * Session-scoped bean that accumulates wizard state across the multi-step registration flow.
 *
 * <p>Populated by {@link WizardController} step handlers and consumed by the review (step 3) and
 * submission steps.
 *
 * <p>The bean is bound to the HTTP session; its lifetime matches the user's session, not the
 * request. Lombok {@code @Data} provides getters/setters required by Thymeleaf model binding.
 */
@Component
@SessionScope
@Data
public class WizardSession {

  // -------------------------------------------------------------------------
  // Step 1 — scheme classification results
  // -------------------------------------------------------------------------

  /** OSS scheme determined by the scheme-service: NON_EU | EU | IMPORT. */
  private String classifiedScheme;

  /** Legal basis citation from the scheme-service (first entry). */
  private String legalBasis;

  /** Classification status: ELIGIBLE | INELIGIBLE | NO_OSS_SCHEME | INSUFFICIENT_INFORMATION */
  private String classificationStatus;

  // -------------------------------------------------------------------------
  // Step 2 — registration form data
  // -------------------------------------------------------------------------

  private String registrantName;
  private String homeCountry;
  private String homeCountryTaxNumber;
  private String postalAddress;
  private String email;
  private String phoneNumber;
  private String bankDetails;
  private String identificationMemberState;
  private LocalDate firstDeliveryDate;

  // -------------------------------------------------------------------------
  // Step 3 — submission result
  // -------------------------------------------------------------------------

  private String submittedRegistrationId;
  private String submittedStatus;
  private LocalDate submittedEffectiveDate;
  private String submittedVatNumber;

  // -------------------------------------------------------------------------
  // Helper
  // -------------------------------------------------------------------------

  /** True when the scheme classification has been completed and a scheme is known. */
  public boolean isSchemeClassified() {
    return classifiedScheme != null && !classifiedScheme.isBlank();
  }
}
