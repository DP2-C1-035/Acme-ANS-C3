
package acme.features.customer.booking;

import java.time.Instant;
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
import acme.entities.booking.TravelClass;
import acme.entities.flight.Flight;
import acme.realms.customer.Customer;

@GuiService
public class CustomerBookingCreateService extends AbstractGuiService<Customer, Booking> {

	@Autowired
	private CustomerBookingRepository repository;


	@Override
	public void authorise() {

		super.getResponse().setAuthorised(true);
	}

	@Override
	public void load() {
		Booking booking;
		Customer customer;
		customer = (Customer) super.getRequest().getPrincipal().getActiveRealm();

		booking = new Booking();
		booking.setDraftMode(true);
		booking.setCustomer(customer);
		booking.setPurchaseMoment(MomentHelper.getCurrentMoment());

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

			if (booking.getFlight() != null)
				super.state(booking.getFlight().getScheduledDeparture() != null, "flight", "acme.validation.booking.flight.date-null.message");

			if (booking.getFlight() != null) {
				boolean flightDepartureFuture = booking.getFlight().getScheduledDeparture().after(now);
				super.state(flightDepartureFuture, "flight", "acme.validation.booking.departure-not-in-future.message");
			}
			if (booking.getPrice() != null && booking.getPrice().getCurrency() != null) {
				boolean validCurrency = ExchangeRate.isValidCurrency(booking.getPrice().getCurrency());
				super.state(validCurrency, "price", "acme.validation.currency.message");
			}
		}
	}

	@Override
	public void perform(final Booking booking) {
		booking.setPurchaseMoment(MomentHelper.getCurrentMoment());
		this.repository.save(booking);
	}

	@Override
	public void unbind(final Booking booking) {
		Dataset dataset;
		Date now = MomentHelper.getCurrentMoment();

		Collection<Flight> flights = this.repository.findFlightsWithFirstLegAfter(now);
		Set<String> seen = new HashSet<>();
		List<Flight> validFlights = flights.stream().filter(f -> f.getFlightRoute() != null).filter(f -> f.getScheduledDeparture() != null && seen.add(f.getFlightRoute())).toList();

		Flight selectedFlight = booking.getFlight();
		if (selectedFlight != null && !validFlights.contains(selectedFlight))
			selectedFlight = null;

		SelectChoices flightChoices = SelectChoices.from(validFlights, "flightRoute", selectedFlight);

		SelectChoices classChoices = SelectChoices.from(TravelClass.class, booking.getTravelClass());

		dataset = super.unbindObject(booking, "locatorCode", "travelClass", "price", "creditCardNibble");
		dataset.put("flights", flightChoices);
		dataset.put("flight", flightChoices.getSelected() != null ? flightChoices.getSelected().getKey() : "0");
		dataset.put("classes", classChoices);
		dataset.put("purchaseMoment", booking.getPurchaseMoment());
		dataset.put("travelClass", classChoices.getSelected() != null ? classChoices.getSelected().getKey() : null);

		super.getResponse().addData(dataset);
	}

}
