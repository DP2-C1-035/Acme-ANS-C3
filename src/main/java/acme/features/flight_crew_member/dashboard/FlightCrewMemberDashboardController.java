
package acme.features.flight_crew_member.dashboard;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import acme.client.controllers.AbstractGuiController;
import acme.client.controllers.GuiController;
import acme.entities.flight_crew_member.FlightCrewMember;
import acme.forms.FlightCrewMemberDashboard;

@GuiController
public class FlightCrewMemberDashboardController extends AbstractGuiController<FlightCrewMember, FlightCrewMemberDashboard> {

	@Autowired
	private FlightCrewMemberDashboardShowService showService;


	@PostConstruct
	protected void initialise() {
		super.addBasicCommand("show", this.showService);
	}

}
