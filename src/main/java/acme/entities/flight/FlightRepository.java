
package acme.entities.flight;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import acme.client.repositories.AbstractRepository;
import acme.entities.leg.Leg;

@Repository
public interface FlightRepository extends AbstractRepository {

	@Query("""
		    SELECT l
		    FROM Leg l
		    WHERE l.flight.id = :flightId AND l.draftMode = false
		    ORDER BY l.scheduledDeparture ASC
		""")
	List<Leg> findLegsByFlightIdOrderByScheduledDepartureAsc(int flightId);

	@Query("""
		    SELECT l
		    FROM Leg l
		    WHERE l.flight.id = :flightId AND l.draftMode = false
		    ORDER BY l.scheduledArrival DESC
		""")
	List<Leg> findLegsByFlightIdOrderByScheduledArrivalDesc(int flightId);

	@Query("SELECT COUNT(l) FROM Leg l WHERE l.flight.id = :flightId")
	Integer getNumbersOfLegsByFlightId(int flightId);

}
