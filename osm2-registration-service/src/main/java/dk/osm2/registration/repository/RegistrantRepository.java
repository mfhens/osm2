package dk.osm2.registration.repository;

import dk.osm2.registration.domain.Registrant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for {@link Registrant} entities.
 *
 * <p>Petition: OSS-02
 */
@Repository
public interface RegistrantRepository extends JpaRepository<Registrant, UUID> {
}
