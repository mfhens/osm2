package dk.osm2.scheme.config;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Drools 9 configuration — builds the KieContainer from the scheme-classification DRL.
 *
 * <p>The KieContainer is a singleton Spring bean. Each classification request opens a new
 * {@link org.kie.api.runtime.KieSession} (stateless per request) and disposes it in a finally
 * block — there is no shared mutable state between requests.
 *
 * <p>Sequential mode is required on Java 21 to avoid ForkJoinPool workers calling
 * Thread.setDaemon() / Thread.setPriority(), which is unsupported on virtual threads.
 *
 * <p>Petition: OSS-01 / ADR-0032 — Drools is the runtime engine.
 */
@Configuration
public class DroolsConfig {

    @Bean
    public KieContainer kieContainer() {
        // Force sequential DRL compilation: setting the threshold to -1 disables the
        // ForkJoinPool parallel path in ImmutableRuleCompilationPhase, avoiding
        // ClassLoader / ServiceLoader failures on Java 21 worker threads.
        System.setProperty("drools.parallelRulesBuildThreshold", "-1");

        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();

        kfs.write(ResourceFactory.newClassPathResource("rules/scheme-classification.drl"));

        KieBuilder kb = ks.newKieBuilder(kfs).buildAll();
        Results results = kb.getResults();

        if (results.hasMessages(Message.Level.ERROR)) {
            throw new IllegalStateException(
                    "Drools DRL build errors in scheme-classification.drl: "
                            + results.getMessages(Message.Level.ERROR));
        }

        return ks.newKieContainer(ks.getRepository().getDefaultReleaseId());
    }
}
