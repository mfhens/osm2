package dk.ufst.opendebt.common.audit;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter that automatically sets database audit context for each request.
 *
 * <p>This filter runs after authentication and sets the PostgreSQL session variables used by audit
 * triggers to capture who is making database changes.
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class AuditContextFilter extends OncePerRequestFilter {

  private final AuditContextService auditContextService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    try {
      auditContextService.setAuditContextFromCurrentRequest();
    } catch (Exception e) {
      log.warn("Failed to set audit context for request: {}", e.getMessage());
    }

    filterChain.doFilter(request, response);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith("/actuator/")
        || path.startsWith("/swagger-ui/")
        || path.startsWith("/api-docs");
  }
}
