package dk.osm2.taxableperson.web.wizard;

import dk.osm2.taxableperson.client.RegistrationRequest;
import dk.osm2.taxableperson.client.RegistrationResponse;
import dk.osm2.taxableperson.client.RegistrationServiceClient;
import dk.osm2.taxableperson.client.SchemeClassificationRequest;
import dk.osm2.taxableperson.client.SchemeClassificationResponse;
import dk.osm2.taxableperson.client.SchemeServiceClient;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Multi-step wizard controller for OSS VAT registration.
 *
 * <pre>
 * Step 1 — Scheme classification  (GET/POST /register/step1)
 * Step 2 — Registration details   (GET/POST /register/step2)
 * Step 3 — Review                 (GET/POST /register/step3)
 * Confirm — Success               (GET /register/confirm)
 * </pre>
 *
 * <p>State is persisted across steps in {@link WizardSession} (session-scoped Spring bean).
 */
@Slf4j
@Controller
@RequestMapping("/register")
@RequiredArgsConstructor
public class WizardController {

  private final SchemeServiceClient schemeServiceClient;
  private final RegistrationServiceClient registrationServiceClient;
  private final WizardSession wizardSession;

  // =========================================================================
  // Step 1 — Scheme classification
  // =========================================================================

  @GetMapping("/step1")
  public String showStep1(Model model) {
    model.addAttribute("step1Form", new Step1Form());
    return "register/step1";
  }

  @PostMapping("/step1")
  public String processStep1(
      @ModelAttribute("step1Form") Step1Form form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes,
      Model model) {

    if (bindingResult.hasErrors()) {
      return "register/step1";
    }

    SchemeClassificationRequest classifyRequest =
        new SchemeClassificationRequest(
            form.getSupplyType(),
            form.getHomeCountry(),
            form.getConsumerCountry(),
            form.isEstablishedInEu(),
            form.getIdentificationMemberState(),
            form.getGoodsValue());

    SchemeClassificationResponse result = schemeServiceClient.classify(classifyRequest);
    log.debug(
        "Classification result: status={}, scheme={}", result.status(), result.scheme());

    wizardSession.setClassifiedScheme(result.scheme());
    wizardSession.setClassificationStatus(result.status());
    // Use identificationMemberState as a proxy for the legal basis headline
    wizardSession.setLegalBasis(result.identificationMemberState());

    if (!result.eligible()) {
      model.addAttribute("classificationResult", result);
      model.addAttribute("step1Form", form);
      return "register/step1";
    }

    return "redirect:/register/step2";
  }

  // =========================================================================
  // Step 2 — Registration details
  // =========================================================================

  @GetMapping("/step2")
  public String showStep2(Model model) {
    if (!wizardSession.isSchemeClassified()) {
      return "redirect:/register/step1";
    }
    Step2Form form = new Step2Form();
    // Pre-fill identificationMemberState from classification
    form.setIdentificationMemberState(wizardSession.getIdentificationMemberState());
    form.setHomeCountry(wizardSession.getHomeCountry());
    model.addAttribute("step2Form", form);
    model.addAttribute("classifiedScheme", wizardSession.getClassifiedScheme());
    return "register/step2";
  }

  @PostMapping("/step2")
  public String processStep2(
      @ModelAttribute("step2Form") Step2Form form,
      BindingResult bindingResult,
      Model model) {

    if (!wizardSession.isSchemeClassified()) {
      return "redirect:/register/step1";
    }

    if (bindingResult.hasErrors()) {
      model.addAttribute("classifiedScheme", wizardSession.getClassifiedScheme());
      return "register/step2";
    }

    // Persist form data into session
    wizardSession.setRegistrantName(form.getRegistrantName());
    wizardSession.setHomeCountry(form.getHomeCountry());
    wizardSession.setHomeCountryTaxNumber(form.getHomeCountryTaxNumber());
    wizardSession.setPostalAddress(form.getPostalAddress());
    wizardSession.setEmail(form.getEmail());
    wizardSession.setPhoneNumber(form.getPhoneNumber());
    wizardSession.setBankDetails(form.getBankDetails());
    wizardSession.setIdentificationMemberState(form.getIdentificationMemberState());
    wizardSession.setFirstDeliveryDate(form.getFirstDeliveryDate());

    return "redirect:/register/step3";
  }

  // =========================================================================
  // Step 3 — Review
  // =========================================================================

  @GetMapping("/step3")
  public String showStep3(Model model) {
    if (!wizardSession.isSchemeClassified()) {
      return "redirect:/register/step1";
    }
    model.addAttribute("session", wizardSession);
    return "register/step3";
  }

  @PostMapping("/step3")
  public String processStep3() {
    if (!wizardSession.isSchemeClassified()) {
      return "redirect:/register/step1";
    }

    RegistrationRequest request =
        new RegistrationRequest(
            wizardSession.getRegistrantName(),
            wizardSession.getClassifiedScheme(),
            wizardSession.getHomeCountry(),
            wizardSession.getHomeCountryTaxNumber(),
            wizardSession.getPostalAddress(),
            wizardSession.getEmail(),
            wizardSession.getPhoneNumber(),
            wizardSession.getBankDetails(),
            wizardSession.getIdentificationMemberState(),
            wizardSession.getFirstDeliveryDate(),
            // registrantId — use a placeholder UUID; overridden by the service from the JWT sub
            UUID.fromString("00000000-0000-0000-0000-000000000000"));

    RegistrationResponse response = registrationServiceClient.submitRegistration(request);
    log.info(
        "Registration submitted: id={}, status={}", response.registrationId(), response.status());

    // Store confirmation data in session for the confirm page
    wizardSession.setSubmittedRegistrationId(
        response.registrationId() != null ? response.registrationId().toString() : null);
    wizardSession.setSubmittedStatus(response.status());
    wizardSession.setSubmittedEffectiveDate(response.effectiveDate());
    wizardSession.setSubmittedVatNumber(response.vatNumber());
    if (response.legalBasis() != null) {
      wizardSession.setLegalBasis(response.legalBasis());
    }

    return "redirect:/register/confirm";
  }

  // =========================================================================
  // Confirm — Success page
  // =========================================================================

  @GetMapping("/confirm")
  public String showConfirm(Model model) {
    model.addAttribute("registrationId", wizardSession.getSubmittedRegistrationId());
    model.addAttribute("status", wizardSession.getSubmittedStatus());
    model.addAttribute("effectiveDate", wizardSession.getSubmittedEffectiveDate());
    model.addAttribute("vatNumber", wizardSession.getSubmittedVatNumber());
    model.addAttribute("legalBasis", wizardSession.getLegalBasis());
    model.addAttribute("scheme", wizardSession.getClassifiedScheme());
    return "register/confirm";
  }
}
