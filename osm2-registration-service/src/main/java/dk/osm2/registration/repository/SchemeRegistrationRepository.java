package dk.osm2.registration.repository;

import dk.osm2.registration.domain.SchemeRegistration;
import dk.osm2.registration.domain.SchemeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link SchemeRegistration} entities.
 *
 * <p>Provides custom queries for finding active registrations and checking exclusion bans.
 *
 * <p>Petition: OSS-02
 */
@Repository
public interface SchemeRegistrationRepository extends JpaRepository<SchemeRegistration, UUID> {

    /**
     * Find all registrations for a given registrant.
     *
     * @param registrantId the registrant's UUID
     * @return all scheme registrations for this registrant
     */
    List<SchemeRegistration> findByRegistrantId(UUID registrantId);

    /**
     * Find the currently active registration for a registrant in a given scheme.
     * Active means: valid_to IS NULL (or in the future).
     *
     * @param registrantId the registrant's UUID
     * @param schemeType   the OSS scheme type
     * @return active registrations
     */
    @Query("SELECT sr FROM SchemeRegistration sr " +
           "WHERE sr.registrant.id = :registrantId " +
           "AND sr.schemeType = :schemeType " +
           "AND sr.validTo IS NULL")
    List<SchemeRegistration> findActiveByRegistrantAndScheme(
            @Param("registrantId") UUID registrantId,
            @Param("schemeType") SchemeType schemeType);
}
