package dk.osm2.authority.exception;

/**
 * Thrown when a downstream microservice call fails (non-2xx response or I/O error).
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
