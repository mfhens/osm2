package dk.osm2.authority.web;

import dk.osm2.authority.client.RegistrationServiceClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Displays the caseworker processing queue: all registrations in
 * {@code PENDING_VAT_NUMBER} status, ordered for SLA tracking.
 */
@Controller
@RequestMapping("/queue")
public class QueueController {

    private final RegistrationServiceClient registrationServiceClient;

    public QueueController(RegistrationServiceClient registrationServiceClient) {
        this.registrationServiceClient = registrationServiceClient;
    }

    /**
     * {@code GET /queue} — renders the pending-registrations table.
     *
     * <p>The template uses {@code now} together with each registration's
     * {@code effectiveDate} to colour-code SLA status in the view.
     */
    @GetMapping
    public String queue(Model model) {
        List<RegistrationServiceClient.RegistrationItem> registrations =
                registrationServiceClient.listPendingRegistrations();

        model.addAttribute("registrations", registrations);
        model.addAttribute("now", LocalDateTime.now());
        return "queue";
    }
}
