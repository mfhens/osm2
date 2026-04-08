package dk.osm2.authority.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Entry and placeholder routes. The layout navigation still lists sections that are not built
 * yet; those paths redirect to the implemented caseworker queue instead of 404/no-static-resource.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String root() {
        return "redirect:/queue";
    }

    @GetMapping({
        "/dashboard",
        "/registrations",
        "/returns",
        "/payments",
        "/records",
        "/exclusions"
    })
    public String navPlaceholder() {
        return "redirect:/queue";
    }
}
