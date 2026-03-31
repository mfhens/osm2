package dk.ufst.security.oces3;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot auto-configuration for the OCES3 Certificate Parser library.
 *
 * <p>Registered via {@code
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} so that any
 * Spring Boot application that includes {@code oces3-certificate-parser} on its classpath will
 * automatically have {@link Oces3CertificateParser} available as a bean without requiring an
 * explicit {@code @ComponentScan} in the consuming application.
 */
@Configuration
@ComponentScan(basePackages = "dk.ufst.security.oces3")
public class Oces3AutoConfiguration {}
