
package acme.features.customer.booking;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import acme.client.components.models.Dataset;
import acme.client.components.views.SelectChoices;
import acme.client.helpers.MomentHelper;
import acme.client.services.AbstractGuiService;
import acme.client.services.GuiService;
import acme.entities.booking.Booking;
import acme.entities.booking.TravelClass;
import acme.entities.flight.Flight;
import acme.entities.passenger.Passenger;
import acme.realms.customer.Customer;

@GuiService
public class CustomerBookingPublishService extends AbstractGuiService<Customer, Booking> {

	@Autowired
	private CustomerBookingRepository repository;


	@Override
	public void authorise() {
		int masterId;
		int bookingId = super.getRequest().getData("id", int.class);
		Booking booking = this.repository.findBookingById(bookingId);

		Customer customer = booking == null ? null : booking.getCustomer();
		boolean status = booking != null && super.getRequest().getPrincipal().hasRealm(customer);
		if (status && "POST".equals(super.getRequest().getMethod())) {
			List<Flight> validFlights = this.repository.findPublishedFlights().stream().filter(f -> f.getScheduledDeparture().after(MomentHelper.getCurrentMoment())).toList();

			Integer flightId = super.getRequest().getData("flight", Integer.class);
			Flight flight = flightId != null && flightId != 0 ? this.repository.findFlightById(flightId) : null;

			if (flightId != null && flightId != 0 && !validFlights.contains(flight))
				status = false;
		}
		masterId = super.getRequest().getData("id", int.class);
		booking = this.repository.findBookingById(masterId);
		customer = booking == null ? null : booking.getCustomer();
		status = super.getRequest().getPrincipal().hasRealm(customer) && booking != null && booking.isDraftMode();

		super.getResponse().setAuthorised(status);
	}

	@Override
	public void load() {
		Booking booking;
		int id;

		id = super.getRequest().getData("id", int.class);
		booking = this.repository.findBookingById(id);

		super.getBuffer().addData(booking);
	}

	@Override
	public void bind(final Booking booking) {
		int flightId;
		Flight flight;

		flightId = super.getRequest().getData("flight", int.class);
		flight = this.repository.findFlightById(flightId);

		super.bindObject(booking, "locatorCode", "travelClass", "price", "creditCardNibble");
		booking.setFlight(flight);
	}

	@Override
	public void validate(final Booking booking) {
		// Validar que el número de tarjeta no sea nulo o vacío
		{
			boolean isCreditCardNibbleValid = booking.getCreditCardNibble() != null && !booking.getCreditCardNibble().trim().isEmpty();
			super.state(isCreditCardNibbleValid, "creditCardNibble", "acme.validation.booking.publish.creditCardNibble-null.message");
		}

		// Validar que haya pasajeros asociados
		{
			List<Passenger> passengers = this.repository.findPassengersByBookingId(booking.getId());
			boolean passengersAssociated = !passengers.isEmpty();
			super.state(passengersAssociated, "*", "acme.validation.booking.publish.passengers-associated.message");
		}

		// Validar que el vuelo exista
		{
			super.state(booking.getFlight() != null, "flight", "acme.validation.booking.flight.not-found.message");
		}

		// Validar que la fecha de salida del vuelo sea futura
		{
			Date moment = MomentHelper.getCurrentMoment();

			if (booking.getFlight() != null) {
				boolean flightDepartureFuture = booking.getFlight().getScheduledDeparture().after(moment);
				super.state(flightDepartureFuture, "flight", "acme.validation.booking.departure-not-in-future.message");
			}
		}

		// Validar que el booking esté en modo borrador
		{
			super.state(booking.isDraftMode(), "*", "acme.validation.booking.is-not-draft-mode.message");
		}

		// Validar que los pasajeros asociados pertenezcan al cliente
		{
			boolean passengersAssociatedAreFromCustomer = true;
			List<Passenger> bookingPassengers = this.repository.findPassengersByBookingId(booking.getId());
			Collection<Passenger> customerPassengers = this.repository.findPassengersByCustomerId(booking.getCustomer().getId());

			for (Passenger passenger : bookingPassengers)
				if (!customerPassengers.contains(passenger)) {
					passengersAssociatedAreFromCustomer = false;
					break;
				}

			super.state(passengersAssociatedAreFromCustomer, "*", "acme.validation.booking.publish.passenger-associated-not-owned.message");
		}

		// Validar que los pasajeros estén publicados
		{
			boolean passengerPublished = true;
			List<Passenger> bookingPassengers = this.repository.findPassengersByBookingId(booking.getId());

			for (Passenger passenger : bookingPassengers)
				if (passenger.isDraftMode()) {
					passengerPublished = false;
					break;
				}

			super.state(passengerPublished, "passenger", "acme.validation.booking.published.passenger-not-published.message");
		}
	}

	@Override
	public void perform(final Booking booking) {
		booking.setPurchaseMoment(MomentHelper.getCurrentMoment());
		booking.setDraftMode(false);
		this.repository.save(booking);
	}

	@Override
	public void unbind(final Booking booking) {
		Dataset dataset;
		SelectChoices choices;

		List<Flight> valid = this.repository.findPublishedFlights().stream().filter(f -> f.getScheduledDeparture().after(MomentHelper.getCurrentMoment())).toList();

		Flight current = booking.getFlight();
		Flight selected = current != null && valid.contains(current) ? current : null;

		choices = SelectChoices.from(valid, "flightRoute", selected);
		SelectChoices classChoices = SelectChoices.from(TravelClass.class, booking.getTravelClass());

		dataset = super.unbindObject(booking, "locatorCode", "purchaseMoment", "travelClass", "price", "creditCardNibble", "draftMode");

		dataset.put("flights", choices);
		dataset.put("flight", choices.getSelected() != null ? choices.getSelected().getKey() : "0");
		dataset.put("classes", classChoices);
		dataset.put("bookingId", booking.getId());

		super.getResponse().addData(dataset);
	}
}
