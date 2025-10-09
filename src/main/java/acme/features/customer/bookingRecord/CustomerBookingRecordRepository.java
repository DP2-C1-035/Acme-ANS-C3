
package acme.features.customer.bookingRecord;

import java.util.Collection;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import acme.client.repositories.AbstractRepository;
import acme.entities.booking.Booking;
import acme.entities.booking.BookingRecord;
import acme.entities.passenger.Passenger;

@Repository
public interface CustomerBookingRecordRepository extends AbstractRepository {

	@Query("select b from Booking b where b.id = :bookingId")
	Booking findBookingById(int bookingId);

	@Query("select b from Booking b where b.locatorCode = :locatorCode")
	Booking getBookingByLocatorCode(String locatorCode);

	@Query("select b from BookingRecord b where b.booking.id = :bookingId")
	Collection<BookingRecord> findBookingRecordsByBookingId(int bookingId);

	@Query("select b from BookingRecord b where b.id = :bookingRecordId")
	BookingRecord findBookingRecordById(int bookingRecordId);

	@Query("select p from Passenger p where p.customer.id = :customerId")
	Collection<Passenger> findPassengersByCustomerId(int customerId);

	@Query("select b.booking from BookingRecord b where b.id = :bookingRecordId")
	Booking findBookingByBookingRecordId(int bookingRecordId);

	@Query("select p from Passenger p where p.customer.id = :customerId and p.draftMode = false")
	Collection<Passenger> findPassengersPublishedByCustomerId(int customerId);

	@Query("select p from BookingRecord br join br.passenger p where br.booking.id = :bookingId")
	Collection<Passenger> findPassengersInBooking(int bookingId);

	@Query("select p from Passenger p where p.id = :passengerId")
	Passenger findPassengerById(int passengerId);

	@Query("select br from BookingRecord br where br.passenger.id = :passengerId and br.booking.id = :bookingId")
	BookingRecord findBookingRecordByPassengerBooking(int passengerId, int bookingId);

}
