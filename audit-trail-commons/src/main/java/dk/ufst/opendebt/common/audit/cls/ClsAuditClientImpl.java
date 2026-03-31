package dk.ufst.opendebt.common.audit.cls;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of CLS audit client with asynchronous batched shipping.
 *
 * <p>Events are queued and shipped in batches at configurable intervals. This minimizes latency
 * impact on API requests while ensuring audit compliance.
 *
 * <p>Enabled only when opendebt.audit.cls.enabled=true.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "opendebt.audit.cls.enabled", havingValue = "true")
public class ClsAuditClientImpl implements ClsAuditClient {

  private final BlockingQueue<ClsAuditEvent> eventQueue = new LinkedBlockingQueue<>(10000);
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private final RestTemplate restTemplate;

  @Value("${opendebt.audit.cls.endpoint:https://cls.ufst.dk/api/v1/events}")
  private String clsEndpoint;

  @Value("${opendebt.audit.cls.batch-size:100}")
  private int batchSize;

  @Value("${opendebt.audit.cls.flush-interval-ms:5000}")
  private long flushIntervalMs;

  @Value("${opendebt.audit.cls.retry-attempts:3}")
  private int retryAttempts;

  @Value("${spring.application.name:opendebt}")
  private String serviceName;

  public ClsAuditClientImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @PostConstruct
  public void init() {
    scheduler.scheduleAtFixedRate(
        this::flushBatch, flushIntervalMs, flushIntervalMs, TimeUnit.MILLISECONDS);
    log.info(
        "CLS Audit Client initialized: endpoint={}, batchSize={}, flushInterval={}ms",
        clsEndpoint,
        batchSize,
        flushIntervalMs);
  }

  @PreDestroy
  public void shutdown() {
    flush();
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void shipEvent(ClsAuditEvent event) {
    if (!eventQueue.offer(event)) {
      log.warn("CLS audit queue full, dropping event: {}", event.getEventId());
    }
  }

  @Override
  public void shipEvents(List<ClsAuditEvent> events) {
    for (ClsAuditEvent event : events) {
      shipEvent(event);
    }
  }

  @Override
  public void flush() {
    flushBatch();
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  private void flushBatch() {
    List<ClsAuditEvent> batch = new ArrayList<>(batchSize);
    eventQueue.drainTo(batch, batchSize);

    if (batch.isEmpty()) {
      return;
    }

    for (int attempt = 1; attempt <= retryAttempts; attempt++) {
      try {
        restTemplate.postForEntity(clsEndpoint, batch, Void.class);
        log.debug("Shipped {} audit events to CLS", batch.size());
        return;
      } catch (Exception e) {
        log.warn(
            "Failed to ship audit events to CLS (attempt {}/{}): {}",
            attempt,
            retryAttempts,
            e.getMessage());
        if (attempt < retryAttempts) {
          try {
            Thread.sleep(1000L * attempt);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return;
          }
        }
      }
    }

    log.error(
        "Failed to ship {} audit events to CLS after {} attempts, events lost",
        batch.size(),
        retryAttempts);
  }
}
