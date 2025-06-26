
package acme.features.flight_crew_member.flight_assignments;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import acme.client.components.models.Dataset;
import acme.client.components.views.SelectChoices;
import acme.client.helpers.MomentHelper;
import acme.client.services.AbstractGuiService;
import acme.client.services.GuiService;
import acme.entities.flight_assignment.AssignmentStatus;
import acme.entities.flight_assignment.FlightAssignment;
import acme.entities.flight_assignment.FlightCrewDuty;
import acme.entities.flight_crew_member.FlightCrewMember;
import acme.entities.leg.Leg;

@GuiService
public class FlightCrewMemberFlightAssignmentCreateService extends AbstractGuiService<FlightCrewMember, FlightAssignment> {

	@Autowired
	private FlightCrewMemberFlightAssignmentRepository repository;


	@Override
	public void authorise() {
		boolean status;
		String method;
		int legId;

		FlightCrewMember flightCrewMember = (FlightCrewMember) super.getRequest().getPrincipal().getActiveRealm();
		Collection<FlightAssignment> assignments = this.repository.findFlightAssignmentsByFlightCrewMemberId(flightCrewMember.getId());
		boolean isLeadAttendant = assignments.stream().anyMatch(fa -> fa.getFlightCrewDuty() == FlightCrewDuty.LEAD_ATTENDANT);

		status = isLeadAttendant;

		if (status) {
			method = super.getRequest().getMethod();
			if (method.equals("GET"))
				status = true;
			else {
				legId = super.getRequest().getData("leg", int.class);
				Leg leg = this.repository.findLegById(legId);
				Collection<Leg> uncompletedLegs = this.repository.findUncompletedLegs(MomentHelper.getCurrentMoment());
				status = legId == 0 || uncompletedLegs.contains(leg);
			}
		}

		super.getResponse().setAuthorised(status);
	}

	@Override
	public void load() {
		FlightAssignment flightAssignment = new FlightAssignment();
		flightAssignment.setDraftMode(true);
		super.getBuffer().addData(flightAssignment);
	}

	@Override
	public void bind(final FlightAssignment flightAssignment) {
		FlightCrewMember flightCrewMember;
		int legId;
		Leg leg;

		legId = super.getRequest().getData("leg", int.class);
		leg = this.repository.findLegById(legId);
		flightCrewMember = (FlightCrewMember) super.getRequest().getPrincipal().getActiveRealm();
		flightAssignment.setLeg(leg);
		flightAssignment.setFlightCrewMember(flightCrewMember);
		flightAssignment.setLastUpdate(MomentHelper.getCurrentMoment());
		flightAssignment.setAssignmentStatus(AssignmentStatus.PENDING);
		super.bindObject(flightAssignment, "flightCrewDuty", "remarks");
	}

	@Override
	public void validate(final FlightAssignment flightAssignment) {
		;
	}

	@Override
	public void perform(final FlightAssignment flightAssignment) {
		this.repository.save(flightAssignment);
	}

	@Override
	public void unbind(final FlightAssignment flightAssignment) {
		Collection<Leg> legs;
		SelectChoices legChoices;
		SelectChoices dutyChoices;
		SelectChoices statusChoices;

		FlightCrewMember flightCrewMember;
		flightCrewMember = (FlightCrewMember) super.getRequest().getPrincipal().getActiveRealm();
		Dataset dataset;

		legs = this.repository.findUncompletedLegs(MomentHelper.getCurrentMoment());

		legChoices = SelectChoices.from(legs, "flightNumber", null);
		dutyChoices = SelectChoices.from(FlightCrewDuty.class, null);
		statusChoices = SelectChoices.from(AssignmentStatus.class, null);

		dataset = super.unbindObject(flightAssignment, "draftMode");
		dataset.put("member", flightCrewMember.getIdentity().getFullName());
		dataset.put("leg", legChoices.getSelected().getKey());
		dataset.put("legs", legChoices);
		dataset.put("flightCrewDuty", dutyChoices.getSelected().getKey());
		dataset.put("duties", dutyChoices);
		dataset.put("assignmentStatus", statusChoices);

		super.getResponse().addData(dataset);
	}
}
