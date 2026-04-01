package dk.osm2.payment.bookkeeping;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.ufst.bookkeeping.domain.LedgerEntry;
import io.codenotary.immudb4j.ImmuClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Thin wrapper around immudb4j that writes ledger entries to immudb for tamper-evidence [ADR-0029].
 *
 * <p>Key format: {@code ledger:<debtId>:<transactionId>:<DEBIT|CREDIT>}<br>
 * Value: JSON-serialised {@link LedgerEntry}.
 *
 * <p>Failures are logged but never propagated — immudb is a secondary store and must not break the
 * primary PostgreSQL transaction.
 */
@Slf4j
@RequiredArgsConstructor
public class ImmudbLedgerClient {

  private final ImmuClient immuClient;
  private final ObjectMapper objectMapper;

  public void writeLedgerEntry(LedgerEntry entry) {
    String key =
        "ledger:"
            + entry.getDebtId()
            + ":"
            + entry.getTransactionId()
            + ":"
            + entry.getEntryType().name();
    try {
      byte[] value = objectMapper.writeValueAsBytes(entry);
      immuClient.set(key.getBytes(), value);
      log.debug(
          "immudb write ok: transactionId={} entryType={}", entry.getTransactionId(), entry.getEntryType());
    } catch (Exception e) {
      log.error(
          "immudb write failed for transactionId={} debtId={}: {}",
          entry.getTransactionId(),
          entry.getDebtId(),
          e.getMessage(),
          e);
      // Do not rethrow — immudb write failure must not break the primary transaction.
    }
  }
}
