package dk.ufst.bookkeeping.spi;

public interface BookkeepingAccountCode {

  String getCode();

  String getName();

  AccountType getType();
}
