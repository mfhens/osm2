package dk.ufst.security.oces3;

import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Spring-injectable parser for Danish OCES3 X.509 certificates.
 *
 * <p>Extracts identity fields from the certificate's RFC2253-formatted subject DN and returns an
 * {@link Oces3AuthContext} for use in SOAP interceptors or other mTLS integration points.
 *
 * <h2>Configuration</h2>
 *
 * <pre>
 * # application.yml
 * oces3:
 *   dn-field: CN   # Default. Use 'O' or a custom OID label for the creditor identifier.
 * </pre>
 *
 * <p>The property {@code oces3.dn-field} controls which DN attribute is extracted as the
 * {@code fordringshaverId} in the returned {@link Oces3AuthContext}.
 */
@Component
public class Oces3CertificateParser {

  @Value("${oces3.dn-field:CN}")
  private String dnField;

  /**
   * Parses the given X.509 certificate and extracts OCES3 identity fields.
   *
   * @param certificate the client certificate from the mTLS handshake; must not be {@code null}
   * @return an {@link Oces3AuthContext} populated from the certificate's subject and validity
   */
  public Oces3AuthContext parse(X509Certificate certificate) {
    X500Principal subject = certificate.getSubjectX500Principal();
    String dn = subject.getName(X500Principal.RFC2253);
    String fordringshaverId = extractDnField(dn, dnField);
    String cn = extractDnField(dn, "CN");
    String issuer = certificate.getIssuerX500Principal().getName(X500Principal.RFC2253);
    Instant validTo = certificate.getNotAfter().toInstant();
    String serialNumber = certificate.getSerialNumber().toString(16);
    return new Oces3AuthContext(fordringshaverId, cn, issuer, validTo, serialNumber);
  }

  private String extractDnField(String dn, String field) {
    String prefix = field + "=";
    for (String part : splitDn(dn)) {
      String trimmed = part.trim();
      if (trimmed.startsWith(prefix)) {
        return trimmed.substring(prefix.length()).trim();
      }
    }
    return "";
  }

  /**
   * Splits an RFC2253 DN string on commas that are not inside double-quoted values.
   *
   * <p>RFC2253 allows attribute values to be enclosed in double quotes, which may themselves
   * contain commas. This method tracks quote state to avoid splitting on those embedded commas.
   */
  private List<String> splitDn(String dn) {
    List<String> parts = new ArrayList<>();
    int start = 0;
    boolean inQuotes = false;
    for (int i = 0; i < dn.length(); i++) {
      char c = dn.charAt(i);
      if (c == '"') {
        inQuotes = !inQuotes;
      } else if (c == ',' && !inQuotes) {
        parts.add(dn.substring(start, i));
        start = i + 1;
      }
    }
    parts.add(dn.substring(start));
    return parts;
  }
}
