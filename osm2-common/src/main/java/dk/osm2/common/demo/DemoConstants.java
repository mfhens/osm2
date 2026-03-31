package dk.osm2.common.demo;

import java.util.UUID;

/**
 * Canonical identifiers and display names for the demo dataset.
 *
 * <p>All seed SQL files and integration tests MUST use these constants so that cross-service
 * references remain consistent. UUID literals here match those in {@code
 * db/seed/V9001__seed_demo_data.sql} in each service module.
 *
 * <p>This class contains no Spring beans and is safe to reference from both production demo seeds
 * and test code.
 */
public final class DemoConstants {

  private DemoConstants() {}

  // -------------------------------------------------------------------------
  // Registrants
  // -------------------------------------------------------------------------

  /** Nina Hansen ApS — Danish company registered under the EU scheme. */
  public static final UUID REGISTRANT_EU_ID =
      UUID.fromString("11111111-1111-1111-1111-111111111111");

  public static final String REGISTRANT_EU_VAT = "DK12345678";
  public static final String REGISTRANT_EU_NAME = "Nina Hansen ApS";
  public static final String REGISTRANT_EU_COUNTRY = "DK";

  /** Nordic Konsulent AS — Norwegian company registered under the Non-EU scheme. */
  public static final UUID REGISTRANT_NON_EU_ID =
      UUID.fromString("22222222-2222-2222-2222-222222222222");

  public static final String REGISTRANT_NON_EU_VAT = "NO987654321MVA";
  public static final String REGISTRANT_NON_EU_NAME = "Nordic Konsulent AS";
  public static final String REGISTRANT_NON_EU_COUNTRY = "NO";

  /** EasyProxy GmbH — Austrian intermediary registered under the Import scheme. */
  public static final UUID REGISTRANT_IMPORT_ID =
      UUID.fromString("33333333-3333-3333-3333-333333333333");

  public static final String REGISTRANT_IMPORT_VAT = "ATU12345678";
  public static final String REGISTRANT_IMPORT_NAME = "EasyProxy GmbH";
  public static final String REGISTRANT_IMPORT_COUNTRY = "AT";

  /** QuickGoods Ltd — Import scheme principal, represented by EasyProxy. */
  public static final UUID INTERMEDIARY_ID =
      UUID.fromString("44444444-4444-4444-4444-444444444444");

  public static final UUID PRINCIPAL_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
  public static final String PRINCIPAL_VAT = "GB123456789";
  public static final String PRINCIPAL_NAME = "QuickGoods Ltd";
  public static final String PRINCIPAL_COUNTRY = "GB";

  // -------------------------------------------------------------------------
  // VAT Returns — EU scheme (Nina Hansen ApS)
  // -------------------------------------------------------------------------

  public static final UUID RETURN_EU_Q3_2023 =
      UUID.fromString("aaaa0001-0000-0000-0000-000000000000");
  public static final UUID RETURN_EU_Q4_2023 =
      UUID.fromString("aaaa0002-0000-0000-0000-000000000000");

  /** Original Q1 2024 return — superseded by a correction. */
  public static final UUID RETURN_EU_Q1_2024_ORIGINAL =
      UUID.fromString("aaaa0003-0000-0000-0000-000000000000");

  /** Correction return for Q1 2024. */
  public static final UUID RETURN_EU_Q1_2024_CORRECTION =
      UUID.fromString("aaaa0004-0000-0000-0000-000000000000");

  // -------------------------------------------------------------------------
  // VAT Returns — Non-EU scheme (Nordic Konsulent AS)
  // -------------------------------------------------------------------------

  public static final UUID RETURN_NON_EU_Q3_2023 =
      UUID.fromString("bbbb0001-0000-0000-0000-000000000000");

  /** Nil return — no taxable supplies in Q4 2023. */
  public static final UUID RETURN_NON_EU_Q4_2023_NIL =
      UUID.fromString("bbbb0002-0000-0000-0000-000000000000");

  // -------------------------------------------------------------------------
  // VAT Returns — Import scheme (EasyProxy GmbH)
  // -------------------------------------------------------------------------

  public static final UUID RETURN_IMPORT_OCT_2023 =
      UUID.fromString("cccc0001-0000-0000-0000-000000000000");
  public static final UUID RETURN_IMPORT_NOV_2023 =
      UUID.fromString("cccc0002-0000-0000-0000-000000000000");

  // -------------------------------------------------------------------------
  // Payments
  // -------------------------------------------------------------------------

  public static final UUID PAYMENT_EU_Q3 = UUID.fromString("dddd0001-0000-0000-0000-000000000000");
  public static final UUID PAYMENT_EU_Q4 = UUID.fromString("dddd0002-0000-0000-0000-000000000000");
  public static final UUID PAYMENT_EU_Q1 = UUID.fromString("dddd0003-0000-0000-0000-000000000000");
  public static final UUID PAYMENT_NON_EU = UUID.fromString("eeee0001-0000-0000-0000-000000000000");
  public static final UUID PAYMENT_IMPORT = UUID.fromString("ffff0001-0000-0000-0000-000000000000");

  // -------------------------------------------------------------------------
  // Keycloak usernames (for test assertions)
  // -------------------------------------------------------------------------

  public static final String USER_TAXABLE = "demo-taxable";
  public static final String USER_NON_EU = "demo-noneu";
  public static final String USER_INTERMEDIARY = "demo-intermediary";
  public static final String USER_CASEWORKER = "demo-caseworker";
  public static final String USER_SUPERVISOR = "demo-supervisor";
  public static final String DEMO_PASSWORD = "Demo1234!";
}
