package dk.osm2.authority.web;

import dk.osm2.authority.exception.ServiceUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Translates unhandled exceptions into user-facing error pages.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles failures when a downstream microservice is unavailable or returns
     * a non-2xx response. Renders {@code error/generic.html} with HTTP 503.
     */
    @ExceptionHandler(ServiceUnavailableException.class)
    public ModelAndView handleServiceUnavailable(ServiceUnavailableException ex) {
        ModelAndView mav = new ModelAndView("error/generic");
        mav.setStatus(HttpStatus.SERVICE_UNAVAILABLE);
        mav.addObject("errorTitle", "Tjeneste utilgængelig");
        mav.addObject("errorMessage",
                "En eller flere bagvedliggende tjenester kunne ikke nås. "
                + "Prøv igen om lidt, eller kontakt systemadministratoren.");
        mav.addObject("errorDetail", ex.getMessage());
        return mav;
    }

    /**
     * Generic fallback for any other unhandled runtime exception.
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneric(Exception ex) {
        ModelAndView mav = new ModelAndView("error/generic");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        mav.addObject("errorTitle", "Uventet fejl");
        mav.addObject("errorMessage",
                "Der opstod en uventet fejl. Kontakt systemadministratoren.");
        mav.addObject("errorDetail", ex.getMessage());
        return mav;
    }
}
