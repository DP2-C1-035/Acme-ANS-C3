
package acme.features.authenticated.manager.flight;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import acme.client.controllers.AbstractGuiController;
import acme.client.controllers.GuiController;
import acme.entities.flight.Flight;
import acme.realms.manager.Manager;

@GuiController
public class AuthenticatedManagerFlightController extends AbstractGuiController<Manager, Flight> {

	// Internal state ---------------------------------------------------------

	@Autowired
	private AuthenticatedManagerCreateFlightService	createService;

	@Autowired
	private AuthenticatedManagerListFlightService	listService;

	// Constructors -----------------------------------------------------------


	@PostConstruct
	protected void initialise() {
		super.addBasicCommand("create", this.createService);
		super.addBasicCommand("list", this.listService);
	}

}
