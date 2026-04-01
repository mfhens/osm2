package dk.ufst.bookkeeping.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Immutable value object representing one side of a double-entry ledger posting. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry {

  private UUID transactionId;
  private UUID debtId;
  private String accountCode;
  private String accountName;
  private EntryType entryType;
  private BigDecimal amount;
  private LocalDate effectiveDate;
  private LocalDate postingDate;
  private String reference;
  private String description;
  private UUID reversalOfTransactionId;
  private EntryCategory entryCategory;
}
