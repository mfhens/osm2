package dk.osm2.registration.exception;

import java.util.UUID;

/**
 * Thrown when a registration cannot be found by the given ID.
 *
 * <p>Maps to HTTP 404 Not Found.
 * <p>Petition: OSS-02
 */
public class RegistrationNotFoundException extends RuntimeException {

    public RegistrationNotFoundException(UUID registrationId) {
        super("Registration not found: " + registrationId);
    }

    public RegistrationNotFoundException(String message) {
        super(message);
    }
}
