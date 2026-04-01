package dk.ufst.bookkeeping.support;

import dk.ufst.bookkeeping.spi.AccountType;
import dk.ufst.bookkeeping.spi.BookkeepingAccountCode;

/**
 * Minimal chart of accounts for unit tests, mirroring the production AccountCode enum in
 * opendebt-payment-service.
 */
public enum TestAccountCode implements BookkeepingAccountCode {
  RECEIVABLES("1000", "Fordringer", AccountType.ASSET),
  INTEREST_RECEIVABLE("1100", "Renter tilgodehavende", AccountType.ASSET),
  BANK("2000", "SKB Bankkonto", AccountType.ASSET),
  COLLECTION_REVENUE("3000", "Indrivelsesindtaegter", AccountType.REVENUE),
  INTEREST_REVENUE("3100", "Renteindtaegter", AccountType.REVENUE),
  WRITE_OFF_EXPENSE("4000", "Tab paa fordringer", AccountType.EXPENSE),
  OFFSETTING_CLEARING("5000", "Modregning clearing", AccountType.LIABILITY);

  private final String code;
  private final String name;
  private final AccountType type;

  TestAccountCode(String code, String name, AccountType type) {
    this.code = code;
    this.name = name;
    this.type = type;
  }

  @Override
  public String getCode() {
    return code;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public AccountType getType() {
    return type;
  }
}
