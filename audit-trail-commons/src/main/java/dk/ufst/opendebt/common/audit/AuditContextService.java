package dk.ufst.opendebt.common.audit;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Service for setting database audit context.
 *
 * <p>This service sets PostgreSQL session variables that are captured by audit triggers, ensuring
 * that all database modifications are attributed to the correct user, even for direct database
 * access.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditContextService {

  private static final String SET_AUDIT_CONTEXT_SQL = "SELECT set_audit_context(?, ?::inet, ?)";

  private final DataSource dataSource;

  /**
   * Sets the audit context for the current database session.
   *
   * @param userId The application user ID (from JWT/authentication)
   * @param clientIp The client's IP address
   * @param applicationName The name of the calling application
   */
  public void setAuditContext(String userId, String clientIp, String applicationName) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement stmt = connection.prepareStatement(SET_AUDIT_CONTEXT_SQL)) {

      stmt.setString(1, userId);
      stmt.setString(2, clientIp);
      stmt.setString(3, applicationName);
      stmt.execute();

      log.debug("Audit context set: user={}, ip={}, app={}", userId, clientIp, applicationName);

    } catch (SQLException e) {
      log.warn("Failed to set audit context: {}", e.getMessage());
    }
  }

  /**
   * Sets audit context from the current security context and HTTP request. Automatically extracts
   * user ID from authentication and client IP from request.
   */
  public void setAuditContextFromCurrentRequest() {
    String userId = getCurrentUserId();
    String clientIp = getClientIp();
    String applicationName = getApplicationName();

    setAuditContext(userId, clientIp, applicationName);
  }

  private String getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.isAuthenticated()) {
      return authentication.getName();
    }
    return "anonymous";
  }

  private String getClientIp() {
    ServletRequestAttributes attributes =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attributes != null) {
      HttpServletRequest request = attributes.getRequest();
      String forwardedFor = request.getHeader("X-Forwarded-For");
      if (forwardedFor != null && !forwardedFor.isEmpty()) {
        return forwardedFor.split(",")[0].trim();
      }
      return request.getRemoteAddr();
    }
    return null;
  }

  private String getApplicationName() {
    ServletRequestAttributes attributes =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attributes != null) {
      HttpServletRequest request = attributes.getRequest();
      String userAgent = request.getHeader("User-Agent");
      if (userAgent != null) {
        return userAgent.length() > 200 ? userAgent.substring(0, 200) : userAgent;
      }
    }
    return "opendebt-api";
  }
}
