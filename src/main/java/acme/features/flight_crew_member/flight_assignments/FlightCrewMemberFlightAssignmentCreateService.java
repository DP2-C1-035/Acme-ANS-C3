
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
import acme.entities.leg.Leg;
import acme.realms.flight_crew_member.AvailabilityStatus;
import acme.realms.flight_crew_member.FlightCrewMember;

@GuiService
public class FlightCrewMemberFlightAssignmentCreateService extends AbstractGuiService<FlightCrewMember, FlightAssignment> {

	@Autowired
	private FlightCrewMemberFlightAssignmentRepository repository;


	//Cualquier miembro puede crear, independientemente de su availability status
	@Override
	public void authorise() {
		boolean status;
		String method;
		int legId;

		method = super.getRequest().getMethod();

		if (method.equals("GET"))
			status = true;
		else {
			legId = super.getRequest().getData("leg", int.class);
			Leg leg = this.repository.findLegById(legId);
			Collection<Leg> uncompletedLegs = this.repository.findUncompletedLegs(MomentHelper.getCurrentMoment());
			status = legId == 0 || uncompletedLegs.contains(leg);
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
		FlightCrewMember crewMember = flightAssignment.getFlightCrewMember();
		Leg leg = flightAssignment.getLeg();

		crewMember.getAvailabilityStatus();
		// 1. El flight crew member que se asigna debe estar AVAILABLE
		if (crewMember.getAvailabilityStatus() != AvailabilityStatus.AVAILABLE)
			super.state(false, "*", "flight-crew-member.flight-assignment.error.member-not-available");
		// 2. No puede haber solapamientos de legs (ya está asignado a otra leg simultánea)
		Collection<FlightAssignment> currentAssignments = this.repository.findFlightAssignmentsByFlightCrewMemberId(crewMember.getId());

		boolean overlaps = currentAssignments.stream().anyMatch(fa -> {
			Leg l = fa.getLeg();
			return !fa.equals(flightAssignment) && l.getScheduledDeparture().before(leg.getScheduledArrival()) && leg.getScheduledDeparture().before(l.getScheduledArrival());
		});
		super.state(!overlaps, "*", "flight-crew-member.flight-assignment.error.overlapping-legs");

		// 3. No puede haber más de un piloto ni copiloto en el mismo leg
		Collection<FlightAssignment> assignmentsInLeg = this.repository.findFlightAssignmentsByLegId(leg.getId());

		boolean dutyConflict = assignmentsInLeg.stream()
			.anyMatch(fa -> !fa.equals(flightAssignment) && fa.getFlightCrewDuty() == flightAssignment.getFlightCrewDuty() && (fa.getFlightCrewDuty() == FlightCrewDuty.PILOT || fa.getFlightCrewDuty() == FlightCrewDuty.CO_PILOT));

		if (flightAssignment.getFlightCrewDuty() == FlightCrewDuty.PILOT || flightAssignment.getFlightCrewDuty() == FlightCrewDuty.CO_PILOT)
			super.state(!dutyConflict, "flightCrewDuty", "flight-crew-member.flight-assignment.error.duty-already-assigned");

		// Validación: el leg no puede haber ocurrido ya
		boolean legHasOccurred = flightAssignment.getLeg().getScheduledArrival().before(MomentHelper.getCurrentMoment());
		super.state(!legHasOccurred, "leg", "flight-crew-member.flight-assignment.error.leg-occurred");
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
