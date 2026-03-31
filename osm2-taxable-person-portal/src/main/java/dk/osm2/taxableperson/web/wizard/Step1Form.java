package dk.osm2.taxableperson.web.wizard;

import java.math.BigDecimal;
import lombok.Data;

/**
 * Form-backing object for wizard step 1 — scheme classification inputs.
 *
 * <p>Bound by Spring MVC from the POST body and converted to a
 * {@link dk.osm2.taxableperson.client.SchemeClassificationRequest} for the scheme-service call.
 */
@Data
public class Step1Form {

  /** Supply type: SERVICES | DISTANCE_SALES_INTRA_EU | IMPORT_GOODS */
  private String supplyType = "SERVICES";

  /** ISO 3166-1 alpha-2 home country of the taxable person. */
  private String homeCountry;

  /** True if the taxable person has an EU seat of economic activity. */
  private boolean establishedInEu;

  /** True if the taxable person is already enrolled in another OSS scheme. */
  private boolean enrolledInOtherScheme;

  /** Consumer / destination country (ISO 3166-1 alpha-2). */
  private String consumerCountry;

  /** Chosen identification member state (free choice when applicable). */
  private String identificationMemberState;

  /** Shipment value in EUR (only relevant for Import scheme). */
  private BigDecimal goodsValue;
}
