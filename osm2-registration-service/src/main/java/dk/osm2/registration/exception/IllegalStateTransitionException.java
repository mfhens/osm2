package dk.osm2.registration.exception;

import dk.osm2.registration.domain.RegistrantStatus;

/**
 * Thrown when an illegal state transition is attempted on a registration.
 *
 * <p>Maps to HTTP 409 Conflict.
 * <p>Petition: OSS-02
 */
public class IllegalStateTransitionException extends RuntimeException {

    public IllegalStateTransitionException(RegistrantStatus from, RegistrantStatus to) {
        super("Illegal state transition from " + from + " to " + to);
    }

    public IllegalStateTransitionException(String message) {
        super(message);
    }
}
