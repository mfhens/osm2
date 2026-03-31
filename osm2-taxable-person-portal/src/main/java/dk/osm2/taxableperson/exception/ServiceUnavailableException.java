package dk.osm2.taxableperson.exception;

/**
 * Thrown when a downstream microservice is unreachable or returns a non-2xx response.
 *
 * <p>Caught by {@link dk.osm2.taxableperson.web.GlobalExceptionHandler} and mapped to HTTP 503.
 */
public class ServiceUnavailableException extends RuntimeException {

  public ServiceUnavailableException(String message) {
    super(message);
  }

  public ServiceUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
