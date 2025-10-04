
package acme.features.authenticated.crewMember;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import acme.client.components.principals.Authenticated;
import acme.client.controllers.AbstractGuiController;
import acme.client.controllers.GuiController;
import acme.realms.flight_crew_member.FlightCrewMember;

@GuiController
public class AuthenticatedFlightCrewMemberController extends AbstractGuiController<Authenticated, FlightCrewMember> {

	// Inyección con @Qualifier para evitar conflicto
	@Autowired
	@Qualifier("authenticatedFlightCrewMemberCreateService")
	private AuthenticatedFlightCrewMemberCreateService	createService;

	@Autowired
	@Qualifier("authenticatedFlightCrewMemberUpdateService")
	private AuthenticatedFlightCrewMemberUpdateService	updateService;


	@PostConstruct
	protected void initialise() {
		super.addBasicCommand("create", this.createService);
		super.addBasicCommand("update", this.updateService);
	}
}
