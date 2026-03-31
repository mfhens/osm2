package dk.osm2.registration.web;

import dk.osm2.registration.exception.ExclusionBanActiveException;
import dk.osm2.registration.exception.IllegalStateTransitionException;
import dk.osm2.registration.exception.RegistrationNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

/**
 * Global exception handler for OSS-02 Registration Service.
 *
 * <p>Maps domain exceptions to RFC 7807 Problem Detail responses.
 *
 * <p>Petition: OSS-02
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 404 — Registration not found.
     */
    @ExceptionHandler(RegistrationNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(RegistrationNotFoundException ex) {
        log.warn("Registration not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("urn:osm2:registration:not-found"));
        problem.setTitle("Registration Not Found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    /**
     * 409 — Illegal state transition.
     */
    @ExceptionHandler(IllegalStateTransitionException.class)
    public ResponseEntity<ProblemDetail> handleIllegalTransition(IllegalStateTransitionException ex) {
        log.warn("Illegal state transition: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("urn:osm2:registration:illegal-state-transition"));
        problem.setTitle("Illegal State Transition");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    /**
     * 422 — Exclusion ban active; re-registration blocked.
     */
    @ExceptionHandler(ExclusionBanActiveException.class)
    public ResponseEntity<ProblemDetail> handleExclusionBan(ExclusionBanActiveException ex) {
        log.warn("Exclusion ban active: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setType(URI.create("urn:osm2:registration:exclusion-ban-active"));
        problem.setTitle("Exclusion Ban Active — EXCLUDED");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);
    }

    /**
     * 400 — Bean validation failed (missing mandatory fields).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validation error: {}", errors);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Validation failed: " + errors);
        problem.setType(URI.create("urn:osm2:registration:validation-error"));
        problem.setTitle("Validation Error");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * 400 — Generic illegal argument (e.g., unknown scheme type).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:osm2:registration:bad-request"));
        problem.setTitle("Bad Request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }
}
