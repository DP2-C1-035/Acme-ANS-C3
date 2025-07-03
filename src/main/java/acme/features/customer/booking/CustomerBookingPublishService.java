
package acme.features.customer.booking;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import acme.client.components.models.Dataset;
import acme.client.components.views.SelectChoices;
import acme.client.helpers.MomentHelper;
import acme.client.services.AbstractGuiService;
import acme.client.services.GuiService;
import acme.components.ExchangeRate;
import acme.entities.booking.Booking;
import acme.entities.flight.Flight;
import acme.entities.passenger.Passenger;
import acme.realms.customer.Customer;

@GuiService
public class CustomerBookingPublishService extends AbstractGuiService<Customer, Booking> {

	@Autowired
	private CustomerBookingRepository repository;


	@Override
	public void authorise() {
		boolean status;
		int masterId;
		Booking booking;
		Customer customer;

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
		{
			boolean iscreditCardNibbleValid = booking.getCreditCardNibble() != null && !booking.getCreditCardNibble().trim().isEmpty();

			super.state(iscreditCardNibbleValid, "creditCardNibble", "acme.validation.booking.publish.creditCardNibble-null.message");
		}
		{
			boolean passengersAssociated;
			List<Passenger> passengers = this.repository.findPassengersByBookingId(booking.getId());

			passengersAssociated = !passengers.isEmpty();

			super.state(passengersAssociated, "*", "acme.validation.booking.publish.passengers-associated.message");
		}
		{
			super.state(booking.getFlight() != null, "flight", "acme.validation.booking.flight.not-found.messasge");
		}
		{
			Date moment;
			moment = MomentHelper.getCurrentMoment();

			if (booking.getFlight() != null) {
				boolean flightDepartureFuture = booking.getFlight().getScheduledDeparture().after(moment);
				super.state(flightDepartureFuture, "flight", "acme.validation.booking.departure-not-in-future.message");
			}

		}
		if (booking.getPrice() != null && booking.getPrice().getCurrency() != null) {
			boolean validCurrency = ExchangeRate.isValidCurrency(booking.getPrice().getCurrency());
			super.state(validCurrency, "price", "acme.validation.currency.message");
		}
		{
			super.state(booking.isDraftMode(), "*", "acme.validation.booking.is-not-draft-mode.message");
		}
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
		booking.setDraftMode(false);
		this.repository.save(booking);
	}

	@Override
	public void unbind(final Booking booking) {
		Dataset dataset;
		Collection<Flight> flights;
		SelectChoices choices;
		Date moment = MomentHelper.getCurrentMoment();
		Flight selectedFlight = booking.getFlight();

		flights = this.repository.findFlightsWithFirstLegAfter(moment);

		// Asegurarse de que el vuelo seleccionado todavía es válido
		if (selectedFlight != null && !flights.contains(selectedFlight))
			selectedFlight = null;

		// Filtrar vuelos con flightRoute no nulo y sin duplicados
		Set<String> seen = new HashSet<>();
		List<Flight> validFlights = flights.stream().filter(f -> {
			try {
				return f.getFlightRoute() != null && seen.add(f.getFlightRoute());
			} catch (Exception e) {
				return false;
			}
		}).toList();

		// Crear SelectChoices
		choices = SelectChoices.from(validFlights, "flightRoute", selectedFlight);

		dataset = super.unbindObject(booking, "locatorCode", "travelClass", "price", "creditCardNibble");
		dataset.put("flight", choices.getSelected().getKey());
		dataset.put("flights", choices);

		super.getResponse().addData(dataset);
	}
}
