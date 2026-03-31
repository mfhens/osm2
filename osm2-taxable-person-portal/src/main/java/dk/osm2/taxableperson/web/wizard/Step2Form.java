package dk.osm2.taxableperson.web.wizard;

import java.time.LocalDate;
import lombok.Data;

/**
 * Form-backing object for wizard step 2 — registration details.
 *
 * <p>Bound by Spring MVC from the POST body and persisted into {@link WizardSession}.
 */
@Data
public class Step2Form {

  private String registrantName;
  private String homeCountry;
  private String homeCountryTaxNumber;
  private String postalAddress;
  private String email;
  private String phoneNumber;
  private String bankDetails;
  private String identificationMemberState;
  private LocalDate firstDeliveryDate;
}
