package dk.ufst.bookkeeping.spi;

/**
 * Chart of accounts abstraction (kontoplan). Provides the specific accounts used by core
 * bookkeeping logic, allowing implementors to bind their own account codes.
 */
public interface Kontoplan {

  BookkeepingAccountCode receivables();

  BookkeepingAccountCode interestReceivable();

  BookkeepingAccountCode bank();

  BookkeepingAccountCode collectionRevenue();

  BookkeepingAccountCode interestRevenue();

  BookkeepingAccountCode writeOffExpense();

  BookkeepingAccountCode offsettingClearing();
}
