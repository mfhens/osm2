package dk.osm2.taxableperson.web;

import dk.osm2.taxableperson.exception.ServiceUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

/**
 * Global exception handler for the Taxable Person Portal.
 *
 * <p>Maps {@link ServiceUnavailableException} to HTTP 503 and any other uncaught exception to
 * HTTP 500, rendering the {@code error/generic} Thymeleaf template in both cases.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ServiceUnavailableException.class)
  @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
  public ModelAndView handleServiceUnavailable(
      ServiceUnavailableException ex, HttpServletRequest request) {
    log.error("Service unavailable on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
    ModelAndView mav = new ModelAndView("error/generic");
    mav.addObject("statusCode", 503);
    mav.addObject("statusText", "Service Unavailable");
    mav.addObject("message", "A required backend service is temporarily unavailable. Please try again later.");
    mav.addObject("detail", ex.getMessage());
    return mav;
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ModelAndView handleGeneral(Exception ex, HttpServletRequest request) {
    log.error("Unhandled exception on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
    ModelAndView mav = new ModelAndView("error/generic");
    mav.addObject("statusCode", 500);
    mav.addObject("statusText", "Internal Server Error");
    mav.addObject("message", "An unexpected error occurred. Please contact support if the problem persists.");
    mav.addObject("detail", ex.getMessage());
    return mav;
  }
}
