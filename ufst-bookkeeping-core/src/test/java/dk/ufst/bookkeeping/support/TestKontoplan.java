package dk.ufst.bookkeeping.support;

import dk.ufst.bookkeeping.spi.BookkeepingAccountCode;
import dk.ufst.bookkeeping.spi.Kontoplan;

/**
 * Deterministic Kontoplan implementation backed by TestAccountCode. Used instead of a Mockito mock
 * so that tests that depend on account-code lookups (storno filtering, interest totals, etc.)
 * behave like the real production Kontoplan.
 */
public class TestKontoplan implements Kontoplan {

  public static final TestKontoplan INSTANCE = new TestKontoplan();

  private TestKontoplan() {}

  @Override
  public BookkeepingAccountCode receivables() {
    return TestAccountCode.RECEIVABLES;
  }

  @Override
  public BookkeepingAccountCode interestReceivable() {
    return TestAccountCode.INTEREST_RECEIVABLE;
  }

  @Override
  public BookkeepingAccountCode bank() {
    return TestAccountCode.BANK;
  }

  @Override
  public BookkeepingAccountCode collectionRevenue() {
    return TestAccountCode.COLLECTION_REVENUE;
  }

  @Override
  public BookkeepingAccountCode interestRevenue() {
    return TestAccountCode.INTEREST_REVENUE;
  }

  @Override
  public BookkeepingAccountCode writeOffExpense() {
    return TestAccountCode.WRITE_OFF_EXPENSE;
  }

  @Override
  public BookkeepingAccountCode offsettingClearing() {
    return TestAccountCode.OFFSETTING_CLEARING;
  }
}
