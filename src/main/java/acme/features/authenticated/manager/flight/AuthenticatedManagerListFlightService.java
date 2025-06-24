
package acme.features.authenticated.manager.flight;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import acme.client.components.models.Dataset;
import acme.client.services.AbstractGuiService;
import acme.client.services.GuiService;
import acme.entities.flight.Flight;
import acme.features.authenticated.manager.AuthenticatedManagerRepository;
import acme.realms.manager.Manager;

@GuiService
public class AuthenticatedManagerListFlightService extends AbstractGuiService<Manager, Flight> {

	@Autowired
	private AuthenticatedManagerRepository repository;


	@Override
	public void authorise() {
		super.getResponse().setAuthorised(true);
	}

	@Override
	public void load() {
		Collection<Flight> flights;
		int managerId;

		managerId = super.getRequest().getPrincipal().getActiveRealm().getId();
		flights = this.repository.findFlightsByManagerId(managerId);

		super.getBuffer().addData(flights);

	}

	@Override
	public void unbind(final Flight flight) {
		Dataset dataset;

		dataset = super.unbindObject(flight, "tag", "cost", "draftMode");
		dataset.put("originCity", flight.getOriginCity());
		dataset.put("destinationCity", flight.getDestinationCity());
		dataset.put("numberOfLayovers", flight.getLayovers());

		super.getResponse().addData(dataset);

	}

}
