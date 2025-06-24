
package acme.features.authenticated.manager;

import java.util.Collection;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import acme.client.repositories.AbstractRepository;
import acme.entities.flight.Flight;

@Repository
public interface AuthenticatedManagerRepository extends AbstractRepository {

	@Query("SELECT f from Flight f where f.manager.id = :managerId")
	Collection<Flight> findFlightsByManagerId(int managerId);

}
