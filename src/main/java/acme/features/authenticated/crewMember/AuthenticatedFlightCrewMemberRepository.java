
package acme.features.authenticated.crewMember;

import java.util.Collection;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import acme.client.components.principals.UserAccount;
import acme.client.repositories.AbstractRepository;
import acme.entities.airline.Airline;
import acme.realms.flight_crew_member.FlightCrewMember;

@Repository
public interface AuthenticatedFlightCrewMemberRepository extends AbstractRepository {

	@Query("SELECT ua FROM UserAccount ua WHERE ua.id = :id")
	UserAccount findUserAccountById(@Param("id") int id);

	@Query("SELECT a FROM Airline a")
	Collection<Airline> findAllAirlines();

	@Query("SELECT a FROM Airline a WHERE a.id = :id")
	Airline findAirlineById(@Param("id") int id);

	@Query("SELECT COUNT(fcm) > 0 FROM FlightCrewMember fcm WHERE fcm.employeeCode = :code")
	boolean existsByEmployeeCode(@Param("code") String code);

	@Query("SELECT f FROM FlightCrewMember f WHERE f.userAccount.id = :id")
	FlightCrewMember findFlightCrewMemberByUserAccountId(@Param("id") int id);

}
