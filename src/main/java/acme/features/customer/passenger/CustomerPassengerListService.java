
package acme.features.customer.passenger;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import acme.client.components.models.Dataset;
import acme.client.services.AbstractGuiService;
import acme.client.services.GuiService;
import acme.entities.passenger.Passenger;
import acme.realms.customer.Customer;

@GuiService
public class CustomerPassengerListService extends AbstractGuiService<Customer, Passenger> {

	@Autowired
	private CustomerPassengerRepository repository;


	@Override
	public void authorise() {
		super.getResponse().setAuthorised(true);
	}

	@Override
	public void load() {
		Collection<Passenger> passengers;
		int customerId = super.getRequest().getPrincipal().getActiveRealm().getId();
		if (super.getRequest().hasData("bookingId")) {
			int bookingId = super.getRequest().getData("bookingId", int.class);
			passengers = this.repository.findPassengersByBookingAndCustomerId(bookingId, customerId);
		} else
			passengers = this.repository.findPassengersByCustomerId(customerId);

		super.getBuffer().addData(passengers);
	}

	@Override
	public void unbind(final Passenger passenger) {
		Dataset dataset;

		dataset = super.unbindObject(passenger, "fullName", "email", "passportNumber", "dateOfBirth", "draftMode");

		super.getResponse().addData(dataset);
	}
}
