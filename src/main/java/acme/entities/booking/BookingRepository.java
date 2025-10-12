
package acme.entities.booking;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import acme.client.repositories.AbstractRepository;
import acme.entities.passenger.Passenger;

@Repository
public interface BookingRepository extends AbstractRepository {

	@Query("select b from Booking b where b.locatorCode = :locatorCode")
	Booking findBookingByLocatorCode(String locatorCode);

	@Query("SELECT b FROM Booking b WHERE b.locatorCode = :locatorCode AND b.id != :bookingId")
	Optional<Booking> findByLocatorCodeAndNotBookingId(String locatorCode, int bookingId);

	@Query("SELECT p FROM Passenger p WHERE p.draftMode = false AND p.customer.id = :customerId")
	Collection<Passenger> findUndraftedPassengerByCustomerId(int customerId);

	@Query("SELECT br.passenger FROM BookingRecord br WHERE br.booking.id = :bookingId")
	Collection<Passenger> findPassengersByBookingId(int bookingId);

}
