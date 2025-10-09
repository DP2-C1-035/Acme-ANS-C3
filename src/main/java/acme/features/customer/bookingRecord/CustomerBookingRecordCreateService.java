
package acme.features.customer.bookingRecord;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import acme.client.components.models.Dataset;
import acme.client.components.views.SelectChoices;
import acme.client.services.AbstractGuiService;
import acme.client.services.GuiService;
import acme.entities.booking.Booking;
import acme.entities.booking.BookingRecord;
import acme.entities.passenger.Passenger;
import acme.realms.customer.Customer;

@GuiService
public class CustomerBookingRecordCreateService extends AbstractGuiService<Customer, BookingRecord> {

	@Autowired
	private CustomerBookingRecordRepository repository;


	@Override
	public void authorise() {
		boolean status;
		int masterId;
		Booking booking;
		Passenger passenger;
		Collection<Passenger> passengers;
		Customer customer;

		masterId = super.getRequest().getData("masterId", int.class);
		booking = this.repository.findBookingById(masterId);

		status = booking != null && booking.isDraftMode() && super.getRequest().getPrincipal().hasRealm(booking.getCustomer());

		// ⚠️ Evita NPE si booking es null
		customer = booking == null ? null : booking.getCustomer();
		passengers = customer == null ? java.util.List.of() : this.repository.findPassengersByCustomerId(customer.getId()).stream().filter(x -> !x.isDraftMode()).toList();

		if (status && "POST".equalsIgnoreCase(super.getRequest().getMethod())) {
			Integer passengerId = super.getRequest().getData("passenger", Integer.class);
			if (passengerId != null) {
				if (passengerId != 0) {
					passenger = this.repository.findPassengerById(passengerId);
					// ⚠️ chequea passenger != null antes de contains / isDraftMode
					status = passenger != null && passengers.contains(passenger) && !passenger.isDraftMode();
				} else
					status = true;
				super.getResponse().setAuthorised(status);
			} else
				status = false;
			super.getResponse().setAuthorised(status);
		}
		super.getResponse().setAuthorised(status);
	}

	@Override
	public void load() {
		int masterId;
		Booking booking;
		BookingRecord bookingRecord = new BookingRecord();

		masterId = super.getRequest().getData("masterId", int.class);
		booking = this.repository.findBookingById(masterId);

		bookingRecord.setBooking(booking);

		super.getBuffer().addData(bookingRecord);
	}

	@Override
	public void bind(final BookingRecord bookingRecord) {
		// leer el id que viene del <select name="passenger"> y cargar la entidad
		final Integer passengerId = super.getRequest().getData("passenger", Integer.class);
		if (passengerId != null && passengerId > 0) {
			final Passenger p = this.repository.findPassengerById(passengerId);
			bookingRecord.setPassenger(p);
		} else
			bookingRecord.setPassenger(null);
	}

	@Override
	public void validate(final BookingRecord bookingRecord) {
		{
			boolean validPassenger = false;
			Passenger passenger = bookingRecord.getPassenger();

			// ⚠️ null-safety: si no llega passenger, corta con error amigo
			if (passenger == null) {
				super.state(false, "passenger", "acme.validation.booking-record.create.passenger-required.message");
				return; // evita NPEs posteriores
			}

			Customer customer = this.repository.findBookingById(bookingRecord.getBooking().getId()).getCustomer();
			Collection<Passenger> customerPassengers = this.repository.findPassengersByCustomerId(customer.getId());

			if (customerPassengers.contains(passenger))
				validPassenger = true;
			super.state(validPassenger, "passenger", "acme.validation.booking-record.create.passenger-not-from-customer.message");
		}
		{
			boolean passengerPublished;
			Passenger passenger = bookingRecord.getPassenger();

			// ⚠️ passenger no es null aquí por el return anterior
			passengerPublished = !passenger.isDraftMode();
			super.state(passengerPublished, "passenger", "acme.validation.booking-record.create.passenger-not-published.message");
		}
		// Tu comentario se mantiene
	}

	@Override
	public void perform(final BookingRecord bookingRecord) {
		this.repository.save(bookingRecord);
	}

	@Override
	public void unbind(final BookingRecord bookingRecord) {
		Dataset dataset;
		Collection<Passenger> passengers;
		Passenger selectedPassenger = bookingRecord.getPassenger();
		Collection<Passenger> passengersInBooking;
		Integer customerId;

		Booking booking = bookingRecord.getBooking();

		customerId = booking.getCustomer().getId();
		passengers = this.repository.findPassengersPublishedByCustomerId(customerId);

		if (selectedPassenger != null && !passengers.contains(selectedPassenger))
			selectedPassenger = null;

		// Excluye pasajeros ya en el booking (manteniendo tu intención)
		passengersInBooking = this.repository.findPassengersInBooking(booking.getId());
		passengers = this.repository.findPassengersPublishedByCustomerId(customerId).stream().filter(x -> !passengersInBooking.contains(x)).toList();

		SelectChoices passengerChoices = SelectChoices.from(passengers, "fullName", bookingRecord.getPassenger());

		dataset = super.unbindObject(bookingRecord, "passenger", "booking");
		dataset.put("masterId", super.getRequest().getData("masterId", int.class));

		dataset.put("passenger", passengerChoices.getSelected() != null ? passengerChoices.getSelected().getKey() : "");
		dataset.put("passengers", passengerChoices);

		dataset.put("flightRoute", bookingRecord.getBooking().getFlight().getFlightRoute());
		dataset.put("booking", booking);

		super.getResponse().addData(dataset);
	}
}
