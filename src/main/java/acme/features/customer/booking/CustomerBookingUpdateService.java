
package acme.features.customer.booking;

import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import acme.client.components.models.Dataset;
import acme.client.components.views.SelectChoices;
import acme.client.helpers.MomentHelper;
import acme.client.services.AbstractGuiService;
import acme.client.services.GuiService;
import acme.components.ExchangeRate;
import acme.entities.booking.Booking;
import acme.entities.booking.TravelClass;
import acme.entities.flight.Flight;
import acme.realms.customer.Customer;

@GuiService
public class CustomerBookingUpdateService extends AbstractGuiService<Customer, Booking> {

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
			final Date now = Date.from(Instant.now());

			if (booking.getFlight() == null)
				super.state(false, "flight", "acme.validation.booking.flight.not-found.message");
			else if (booking.getFlight().getScheduledDeparture() == null)
				super.state(false, "flight", "acme.validation.booking.flight.date-null.message");
			else {
				boolean future = booking.getFlight().getScheduledDeparture().after(now);
				super.state(future, "flight", "acme.validation.booking.departure-not-in-future.message");
			}
		}

		if (booking.getPrice() != null && booking.getPrice().getCurrency() != null) {
			boolean validCurrency = ExchangeRate.isValidCurrency(booking.getPrice().getCurrency());
			super.state(validCurrency, "price", "acme.validation.currency.message");
		}

		super.state(booking.isDraftMode(), "*", "acme.validation.booking.is-not-draft-mode.message");
	}

	@Override
	public void perform(final Booking booking) {
		booking.setPurchaseMoment(MomentHelper.getCurrentMoment());
		this.repository.save(booking);
	}

	@Override
	public void unbind(final Booking booking) {
		Dataset dataset;
		final Date now = Date.from(Instant.now());

		Collection<Flight> flights = this.repository.findFlightsWithFirstLegAfter(now);

		List<Flight> validFlights = flights.stream().filter(f -> f.getFlightRoute() != null).filter(f -> f.getScheduledDeparture() != null).toList();

		Flight selectedFlight = booking.getFlight();
		if (selectedFlight != null && !validFlights.contains(selectedFlight))
			selectedFlight = null;

		SelectChoices choices = SelectChoices.from(validFlights, "flightRoute", selectedFlight);
		SelectChoices classChoices = SelectChoices.from(TravelClass.class, booking.getTravelClass());

		dataset = super.unbindObject(booking, "locatorCode", "purchaseMoment", "travelClass", "price", "creditCardNibble", "draftMode");
		dataset.put("flight", choices.getSelected() != null ? choices.getSelected().getKey() : "0");
		dataset.put("flights", choices);
		dataset.put("classes", classChoices);
		dataset.put("travelClass", classChoices.getSelected() != null ? classChoices.getSelected().getKey() : null);
		dataset.put("bookingId", booking.getId());

		super.getResponse().addData(dataset);
	}

}
