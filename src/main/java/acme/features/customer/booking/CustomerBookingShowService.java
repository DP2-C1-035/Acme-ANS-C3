
package acme.features.customer.booking;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import acme.client.components.models.Dataset;
import acme.client.components.views.SelectChoices;
import acme.client.services.AbstractGuiService;
import acme.client.services.GuiService;
import acme.entities.booking.Booking;
import acme.entities.booking.TravelClass;
import acme.entities.flight.Flight;
import acme.realms.customer.Customer;

@GuiService
public class CustomerBookingShowService extends AbstractGuiService<Customer, Booking> {

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
		customer = null;
		if (booking != null)
			customer = booking.getCustomer();
		status = booking != null && super.getRequest().getPrincipal().hasRealm(booking.getCustomer());

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
	public void validate(final Booking booking) {
		;
	}

	@Override
	public void unbind(final Booking booking) {
		Dataset dataset;
		Collection<Flight> flights;
		SelectChoices choices;
		Flight selectedFlight;

		final Date now = Date.from(Instant.now());
		selectedFlight = booking.getFlight();

		flights = this.repository.findFlightsWithFirstLegAfter(now);

		// Reinyecta solo si tiene route
		if (selectedFlight != null && selectedFlight.getFlightRoute() != null && !flights.contains(selectedFlight)) {
			flights = new ArrayList<>(flights);
			flights.add(selectedFlight);
		}

		// Construimos la lista final: primero el seleccionado, luego únicos por route
		final Set<String> seen = new HashSet<>();
		final ArrayList<Flight> finalFlights = new ArrayList<>();

		if (selectedFlight != null && selectedFlight.getFlightRoute() != null) {
			finalFlights.add(selectedFlight);
			seen.add(selectedFlight.getFlightRoute());
		}

		for (final Flight f : flights) {
			if (selectedFlight != null && f.equals(selectedFlight))
				continue;
			final String route = f.getFlightRoute();
			if (route == null)
				continue;          // nunca meter labels nulos
			if (seen.add(route))
				finalFlights.add(f); // añade solo la primera de cada route
		}

		choices = SelectChoices.from(finalFlights, "flightRoute", selectedFlight);
		SelectChoices classChoices = SelectChoices.from(TravelClass.class, booking.getTravelClass());

		dataset = super.unbindObject(booking, "locatorCode", "travelClass", "price", "creditCardNibble", "purchaseMoment", "draftMode");

		var selectedChoice = choices.getSelected();
		dataset.put("flight", selectedChoice != null ? selectedChoice.getKey() : null);
		dataset.put("flights", choices);
		dataset.put("classes", classChoices);
		dataset.put("bookingId", booking.getId());

		super.getResponse().addData(dataset);
	}

}
