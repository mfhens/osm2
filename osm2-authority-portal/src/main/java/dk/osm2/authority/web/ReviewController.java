package dk.osm2.authority.web;

import dk.osm2.authority.client.RegistrationServiceClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/**
 * Handles the caseworker review flow for a single registration:
 * <ul>
 *   <li>Display the full detail page</li>
 *   <li>Approve with an optional VAT number</li>
 *   <li>Reject with a mandatory reason</li>
 * </ul>
 */
@Controller
@RequestMapping("/review")
public class ReviewController {

    // -----------------------------------------------------------------------
    // Form backing object
    // -----------------------------------------------------------------------

    /**
     * Form object bound to the approve form.
     * {@code vatNumber} is optional — the service will generate one if left blank.
     */
    public static class ApproveForm {
        private String vatNumber;

        public String getVatNumber() { return vatNumber; }
        public void setVatNumber(String vatNumber) { this.vatNumber = vatNumber; }
    }

    /**
     * Form object bound to the reject form.
     * {@code reason} is required — caseworkers must state grounds for rejection.
     */
    public static class RejectForm {
        private String reason;

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private final RegistrationServiceClient registrationServiceClient;

    public ReviewController(RegistrationServiceClient registrationServiceClient) {
        this.registrationServiceClient = registrationServiceClient;
    }

    // -----------------------------------------------------------------------
    // Handlers
    // -----------------------------------------------------------------------

    /**
     * {@code GET /review/{id}} — display the full registration detail.
     */
    @GetMapping("/{id}")
    public String review(@PathVariable UUID id, Model model) {
        RegistrationServiceClient.RegistrationDetail detail =
                registrationServiceClient.getRegistration(id);

        model.addAttribute("registration", detail);
        if (!model.containsAttribute("approveForm")) {
            model.addAttribute("approveForm", new ApproveForm());
        }
        if (!model.containsAttribute("rejectForm")) {
            model.addAttribute("rejectForm", new RejectForm());
        }
        return "review";
    }

    /**
     * {@code POST /review/{id}/approve} — approve the registration.
     *
     * <p>Redirects to the queue on success with a flash confirmation message.
     */
    @PostMapping("/{id}/approve")
    public String approve(
            @PathVariable UUID id,
            @ModelAttribute("approveForm") ApproveForm approveForm,
            RedirectAttributes redirectAttributes) {

        registrationServiceClient.approveRegistration(id, approveForm.getVatNumber());
        redirectAttributes.addFlashAttribute("flashMessage", "Registrering godkendt");
        redirectAttributes.addFlashAttribute("flashType", "success");
        return "redirect:/queue";
    }

    /**
     * {@code POST /review/{id}/reject} — reject the registration.
     *
     * <p>Validates that a reason is provided; if not, re-renders the review page.
     * Redirects to the queue on success with a flash confirmation message.
     */
    @PostMapping("/{id}/reject")
    public String reject(
            @PathVariable UUID id,
            @ModelAttribute("rejectForm") RejectForm rejectForm,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (rejectForm.getReason() == null || rejectForm.getReason().isBlank()) {
            // Re-fetch the detail so the page can be re-rendered with the error
            RegistrationServiceClient.RegistrationDetail detail =
                    registrationServiceClient.getRegistration(id);
            model.addAttribute("registration", detail);
            model.addAttribute("approveForm", new ApproveForm());
            model.addAttribute("rejectError", "Afvisningsgrund er påkrævet");
            return "review";
        }

        registrationServiceClient.rejectRegistration(id, rejectForm.getReason());
        redirectAttributes.addFlashAttribute("flashMessage", "Registrering afvist");
        redirectAttributes.addFlashAttribute("flashType", "warning");
        return "redirect:/queue";
    }
}
