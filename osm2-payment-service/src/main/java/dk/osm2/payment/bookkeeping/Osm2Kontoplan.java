package dk.osm2.payment.bookkeeping;

import dk.ufst.bookkeeping.spi.AccountType;
import dk.ufst.bookkeeping.spi.BookkeepingAccountCode;
import dk.ufst.bookkeeping.spi.Kontoplan;

/**
 * OSM2 chart of accounts for VAT One Stop Shop (OSS) bookkeeping.
 *
 * <p>Account codes align with the SKAT/UFST kontoplan for the OSS-05 payment processing component.
 * The numbering follows standard Danish public-sector accounting conventions:
 *
 * <ul>
 *   <li>1xxx — Assets
 *   <li>2xxx — Liabilities
 *   <li>3xxx — Revenue
 *   <li>5xxx — Expenses
 * </ul>
 */
public class Osm2Kontoplan implements Kontoplan {

  private enum Account implements BookkeepingAccountCode {
    RECEIVABLES("1110", "OSS VAT Tilgodehavender", AccountType.ASSET),
    INTEREST_RECEIVABLE("1120", "Inddrivelsesrente Tilgodehavende", AccountType.ASSET),
    BANK("1900", "Bank", AccountType.ASSET),
    COLLECTION_REVENUE("3110", "OSS-momsindtægt", AccountType.REVENUE),
    INTEREST_REVENUE("3120", "Inddrivelsesrente Indtægt", AccountType.REVENUE),
    WRITE_OFF_EXPENSE("5110", "Nedskrivning af tilgodehavender", AccountType.EXPENSE),
    OFFSETTING_CLEARING("2110", "Modregningskonto", AccountType.LIABILITY);

    private final String code;
    private final String name;
    private final AccountType type;

    Account(String code, String name, AccountType type) {
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

  @Override
  public BookkeepingAccountCode receivables() {
    return Account.RECEIVABLES;
  }

  @Override
  public BookkeepingAccountCode interestReceivable() {
    return Account.INTEREST_RECEIVABLE;
  }

  @Override
  public BookkeepingAccountCode bank() {
    return Account.BANK;
  }

  @Override
  public BookkeepingAccountCode collectionRevenue() {
    return Account.COLLECTION_REVENUE;
  }

  @Override
  public BookkeepingAccountCode interestRevenue() {
    return Account.INTEREST_REVENUE;
  }

  @Override
  public BookkeepingAccountCode writeOffExpense() {
    return Account.WRITE_OFF_EXPENSE;
  }

  @Override
  public BookkeepingAccountCode offsettingClearing() {
    return Account.OFFSETTING_CLEARING;
  }
}
