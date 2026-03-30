# oces3-certificate-parser

Standalone Maven library for parsing Danish **OCES3 X.509 certificates** used in SOAP/mTLS
e-government integrations. Handles RFC2253 DN parsing, OCES3 identity extraction, and
Spring Boot auto-wiring.

## Purpose

Danish e-government SOAP services (DUPLA, NemKonto, SKATs fordringssystem, etc.) require
mutual TLS with OCES3 certificates issued by the Danish Government's PKI. The certificate's
subject DN encodes the system identity (fordringshaver-ID, organisation, etc.).

This library:
- Splits RFC2253-formatted subject DNs correctly (including quoted values with embedded commas)
- Extracts a configurable DN field as the creditor/system identifier
- Returns an immutable `Oces3AuthContext` record for use in Spring-WS interceptors or filters
- Auto-wires as a `@Component` in any Spring Boot application

## Maven dependency

```xml
<dependency>
  <groupId>dk.ufst</groupId>
  <artifactId>oces3-certificate-parser</artifactId>
  <version>1.0</version>
</dependency>
```

This library has no transitive dependencies beyond Spring Framework (`spring-context`).

## Configuration

| Property | Default | Description |
|---|---|---|
| `oces3.dn-field` | `CN` | The DN attribute used as the `fordringshaverId` in `Oces3AuthContext` |

```yaml
# application.yml
oces3:
  dn-field: CN        # Use 'O' or another OID label if your OCES3 cert encodes the system ID differently
```

## Usage

```java
@Component
public class MyMtlsInterceptor {

  private final Oces3CertificateParser certParser;

  public MyMtlsInterceptor(Oces3CertificateParser certParser) {
    this.certParser = certParser;
  }

  public void handleRequest(X509Certificate clientCert) {
    Oces3AuthContext ctx = certParser.parse(clientCert);
    String systemId = ctx.fordringshaverId();   // e.g. "1234567890"
    String cn       = ctx.cn();                 // e.g. "MySystem PROD"
    String issuer   = ctx.issuer();             // RFC2253 issuer DN
    Instant validTo = ctx.validTo();            // expiry
    String serial   = ctx.serialNumber();       // hex serial
  }
}
```

## Installing locally

```bash
cd oces3-certificate-parser
mvn install
```

After installing, add the dependency to your project's `pom.xml` as shown above.

## Deploying to GitHub Packages

```bash
cd oces3-certificate-parser
mvn deploy
```

Configure your `~/.m2/settings.xml` with the appropriate GitHub Packages server credentials
before deploying.

## Background

Extracted from `opendebt-common` (package `dk.ufst.opendebt.common.soap`) as part of
technical backlog item **TB-042** to make the parser reusable across 5+ Danish e-government
and tax projects without pulling in the full OpenDebt common library.

Previous package: `dk.ufst.opendebt.common.soap`  
New package: `dk.ufst.security.oces3`  
Previous property key: `opendebt.soap.oces3.fordringshaver-dn-field`  
New property key: `oces3.dn-field`
