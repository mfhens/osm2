package dk.osm2.registration.repository;

import dk.osm2.registration.domain.ExclusionBan;
import dk.osm2.registration.domain.SchemeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link ExclusionBan} entities.
 *
 * <p>Provides query for checking active re-registration bans.
 * ML § 66d stk. 3 / § 66m stk. 3 / § 66v stk. 3
 *
 * <p>Petition: OSS-02 — FR-OSS-02-034
 */
@Repository
public interface ExclusionBanRepository extends JpaRepository<ExclusionBan, UUID> {

    /**
     * Find all active exclusion bans for a registrant in a given scheme.
     * A ban is active when ban_lifted_at is in the future.
     *
     * @param registrantId the registrant's UUID
     * @param schemeType   the OSS scheme type
     * @param today        today's date to check against ban_lifted_at
     * @return list of active bans
     */
    @Query("SELECT eb FROM ExclusionBan eb " +
           "WHERE eb.registrant.id = :registrantId " +
           "AND eb.schemeType = :schemeType " +
           "AND eb.banLiftedAt > :today")
    List<ExclusionBan> findActiveBans(
            @Param("registrantId") UUID registrantId,
            @Param("schemeType") SchemeType schemeType,
            @Param("today") LocalDate today);

    /**
     * Find active exclusion bans by homeCountryTaxNumber across registrant entities.
     * Used for re-registration ban checks where a new registrant is created.
     */
    @Query("SELECT eb FROM ExclusionBan eb " +
           "WHERE eb.registrant.homeCountryTaxNumber = :taxNumber " +
           "AND eb.schemeType = :schemeType " +
           "AND eb.banLiftedAt > :today")
    List<ExclusionBan> findActiveBansByTaxNumber(
            @Param("taxNumber") String taxNumber,
            @Param("schemeType") SchemeType schemeType,
            @Param("today") LocalDate today);

    /**
     * Find active exclusion bans by email — used for EU scheme re-registrations
     * where homeCountryTaxNumber is null.
     */
    @Query("SELECT eb FROM ExclusionBan eb " +
           "WHERE eb.registrant.email = :email " +
           "AND eb.schemeType = :schemeType " +
           "AND eb.banLiftedAt > :today")
    List<ExclusionBan> findActiveBansByEmail(
            @Param("email") String email,
            @Param("schemeType") SchemeType schemeType,
            @Param("today") LocalDate today);
}
