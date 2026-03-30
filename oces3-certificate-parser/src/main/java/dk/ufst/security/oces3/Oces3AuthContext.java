package dk.ufst.security.oces3;

import java.time.Instant;

/**
 * Immutable value object holding the identity extracted from an OCES3 X.509 certificate.
 *
 * <ul>
 *   <li>{@code fordringshaverId} — the DN field used to identify the creditor system (configurable
 *       via {@code oces3.dn-field}, defaults to {@code CN}).
 *   <li>{@code cn} — the Common Name (CN) field from the subject DN.
 *   <li>{@code issuer} — the full RFC2253-formatted issuer DN.
 *   <li>{@code validTo} — certificate expiry as an {@link Instant}.
 *   <li>{@code serialNumber} — the certificate serial number in hexadecimal.
 * </ul>
 */
public record Oces3AuthContext(
    String fordringshaverId, String cn, String issuer, Instant validTo, String serialNumber) {}
