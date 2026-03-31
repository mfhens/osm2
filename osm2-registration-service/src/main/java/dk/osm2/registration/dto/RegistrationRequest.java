package dk.osm2.registration.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;



/**
 * Request DTO for submitting an OSS scheme registration notification.
 *
 * <p>Field order MUST match the constructor call in {@code RegistrationSteps.buildRequest()}.
 *
 * <p>Legal basis: ML § 66b; Momsbekendtgørelsen §§ 115–117
 * <p>Petition: OSS-02 — FR-OSS-02-001, FR-OSS-02-006, FR-OSS-02-009
 *
 * <p>Ref: RegistrationSteps.buildRequest() — parameters in this exact order:
 * bankDetails, email, phoneNumber, postalAddress, homeCountryTaxNumber,
 * registrantName, scheme, homeCountry, tradingNames, bankAccountCountry,
 * identificationMemberState, firstDeliveryDate, desiredStartDate,
 * electronicInterfaceFlag, notEstablishedInEuDeclaration, existingDanishVatNumber,
 * notificationDate, hasEuEstablishment
 */
public record RegistrationRequest(

        /** Bank account details for VAT refunds (mandatory). */
        @NotBlank(message = "MISSING_REQUIRED_FIELD") String bankDetails,

        /** Contact email address (mandatory). */
        @NotBlank(message = "MISSING_REQUIRED_FIELD") String email,

        /** Contact phone number (optional). */
        String phoneNumber,

        /** Full postal address (mandatory). */
        @NotBlank(message = "MISSING_REQUIRED_FIELD") String postalAddress,

        /** Tax number in home country (optional for EU-established). */
        String homeCountryTaxNumber,

        /** Legal name of the taxable person (mandatory). */
        @NotBlank(message = "MISSING_REQUIRED_FIELD") String registrantName,

        /** Requested OSS scheme: "NON_EU", "EU", or "IMPORT" (mandatory). */
        @NotBlank(message = "MISSING_REQUIRED_FIELD") String scheme,

        /** ISO 3166-1 alpha-2 home country code (mandatory). */
        @NotBlank(message = "MISSING_REQUIRED_FIELD") String homeCountry,

        /** Trading names used by the taxable person (optional). */
        List<String> tradingNames,

        /** Country of the bank account (optional). */
        String bankAccountCountry,

        /** Identification member state — always "DK" in this system (mandatory). */
        @NotBlank(message = "MISSING_REQUIRED_FIELD") String identificationMemberState,

        /** Date of first eligible delivery (used for early-delivery exception, ML § 66b stk. 3). */
        LocalDate firstDeliveryDate,

        /** Desired registration start date (used as effective date when explicitly provided). */
        LocalDate desiredStartDate,

        /** True if the person is an electronic interface under ML § 4c stk. 2. */
        boolean electronicInterfaceFlag,

        /** True if the person declares they are not established in the EU (EU scheme §117). */
        boolean notEstablishedInEuDeclaration,

        /** Existing Danish VAT number (reused for EU scheme, ML § 66e stk. 2). */
        String existingDanishVatNumber,

        /** Notification submission date (used for effective-date calculation; defaults to today if null). */
        LocalDate notificationDate,

        /** True if the applicant has a fixed establishment in an EU member state. */
        boolean hasEuEstablishment

) {}
