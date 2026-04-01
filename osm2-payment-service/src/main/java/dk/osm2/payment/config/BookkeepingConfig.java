package dk.osm2.payment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.osm2.payment.bookkeeping.DefaultCoveragePriorityAdapter;
import dk.osm2.payment.bookkeeping.FinancialEventJpaAdapter;
import dk.osm2.payment.bookkeeping.ImmudbLedgerClient;
import dk.osm2.payment.bookkeeping.LedgerEntryJpaAdapter;
import dk.osm2.payment.bookkeeping.Osm2Kontoplan;
import dk.osm2.payment.repository.FinancialEventRepository;
import dk.osm2.payment.repository.LedgerEntryRepository;
import dk.ufst.bookkeeping.engine.BookkeepingEngine;
import dk.ufst.bookkeeping.engine.BookkeepingEngineImpl;
import dk.ufst.bookkeeping.port.CoveragePriorityPort;
import dk.ufst.bookkeeping.port.FinancialEventStore;
import dk.ufst.bookkeeping.port.LedgerEntryStore;
import dk.ufst.bookkeeping.service.InterestAccrualService;
import dk.ufst.bookkeeping.service.RetroactiveCorrectionService;
import dk.ufst.bookkeeping.service.TimelineReplayService;
import dk.ufst.bookkeeping.service.impl.InterestAccrualServiceImpl;
import dk.ufst.bookkeeping.service.impl.RetroactiveCorrectionServiceImpl;
import dk.ufst.bookkeeping.service.impl.TimelineReplayServiceImpl;
import dk.ufst.bookkeeping.spi.Kontoplan;
import io.codenotary.immudb4j.ImmuClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration wiring the ufst-bookkeeping-core domain library into the payment service.
 *
 * <p>The core library is framework-agnostic (no Spring, no JPA). This class provides:
 *
 * <ul>
 *   <li>JPA port adapters ({@link LedgerEntryStore}, {@link FinancialEventStore})
 *   <li>Coverage priority adapter ({@link CoveragePriorityPort})
 *   <li>OSM2 chart of accounts ({@link Kontoplan})
 *   <li>immudb4j client for tamper-evident secondary ledger writes [ADR-0029]
 *   <li>Domain engine and services ({@link BookkeepingEngine}, {@link InterestAccrualService},
 *       {@link RetroactiveCorrectionService}, {@link TimelineReplayService})
 * </ul>
 */
@Slf4j
@Configuration
public class BookkeepingConfig {

  /** Opens an immudb session at startup and closes it gracefully on context shutdown. */
  @Bean(destroyMethod = "closeSession")
  public ImmuClient immuClient(
      @Value("${osm2.immudb.host}") String host,
      @Value("${osm2.immudb.port}") int port,
      @Value("${osm2.immudb.database}") String database,
      @Value("${osm2.immudb.username}") String username,
      @Value("${osm2.immudb.password}") String password) {

    ImmuClient client = ImmuClient.newBuilder().withServerUrl(host).withServerPort(port).build();
    client.openSession(database, username, password);
    log.info("immudb session opened: host={}:{} database={}", host, port, database);
    return client;
  }

  @Bean
  public ImmudbLedgerClient immudbLedgerClient(ImmuClient immuClient, ObjectMapper objectMapper) {
    return new ImmudbLedgerClient(immuClient, objectMapper);
  }

  @Bean
  public Kontoplan kontoplan() {
    return new Osm2Kontoplan();
  }

  @Bean
  public CoveragePriorityPort coveragePriorityPort() {
    return new DefaultCoveragePriorityAdapter();
  }

  @Bean
  public LedgerEntryStore ledgerEntryStore(
      LedgerEntryRepository repository, ImmudbLedgerClient immudbLedgerClient) {
    return new LedgerEntryJpaAdapter(repository, immudbLedgerClient);
  }

  @Bean
  public FinancialEventStore financialEventStore(FinancialEventRepository repository) {
    return new FinancialEventJpaAdapter(repository);
  }

  @Bean
  public BookkeepingEngine bookkeepingEngine(
      LedgerEntryStore ledgerEntryStore, FinancialEventStore financialEventStore) {
    return new BookkeepingEngineImpl(ledgerEntryStore, financialEventStore);
  }

  @Bean
  public InterestAccrualService interestAccrualService(FinancialEventStore financialEventStore) {
    return new InterestAccrualServiceImpl(financialEventStore);
  }

  @Bean
  public RetroactiveCorrectionService retroactiveCorrectionService(
      LedgerEntryStore ledgerEntryStore,
      FinancialEventStore financialEventStore,
      InterestAccrualService interestAccrualService,
      Kontoplan kontoplan) {
    return new RetroactiveCorrectionServiceImpl(
        ledgerEntryStore, financialEventStore, interestAccrualService, kontoplan);
  }

  @Bean
  public TimelineReplayService timelineReplayService(
      FinancialEventStore financialEventStore,
      LedgerEntryStore ledgerEntryStore,
      CoveragePriorityPort coveragePriorityPort,
      Kontoplan kontoplan) {
    return new TimelineReplayServiceImpl(
        financialEventStore, ledgerEntryStore, coveragePriorityPort, kontoplan);
  }
}
