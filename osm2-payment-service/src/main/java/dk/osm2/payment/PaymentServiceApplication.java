package dk.osm2.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * OSM2 Payment Service — entry point.
 *
 * <p>Payment processing, refunds, ECB rate conversion, and CMS distribution (OSS-05). Double-entry
 * bookkeeping is handled by ufst-bookkeeping-core via JPA adapters in the {@code bookkeeping}
 * package. A tamper-evident secondary ledger copy is written to immudb on every posting [ADR-0029].
 */
@SpringBootApplication
public class PaymentServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(PaymentServiceApplication.class, args);
  }
}
