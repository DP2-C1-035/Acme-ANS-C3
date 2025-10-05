
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
public class FlightCrewMemberFlightAssignmentUpdateService extends AbstractGuiService<FlightCrewMember, FlightAssignment> {

	@Autowired
	private FlightCrewMemberFlightAssignmentRepository repository;


	@Override
	public void authorise() {
		boolean status;
		int masterId;
		FlightAssignment flightAssignment;
		FlightCrewMember flightCrewMember;

		masterId = super.getRequest().getData("id", int.class);
		flightAssignment = this.repository.findFlightAssignmentById(masterId);
		flightCrewMember = flightAssignment == null ? null : flightAssignment.getFlightCrewMember();
		status = flightAssignment != null && flightAssignment.isDraftMode() && super.getRequest().getPrincipal().hasRealm(flightCrewMember);

		if (status) {
			String method;
			int legtId;
			method = super.getRequest().getMethod();
			if (method.equals("GET"))
				status = true;
			else {
				legtId = super.getRequest().getData("leg", int.class);
				Leg leg = this.repository.findLegById(legtId);
				Collection<Leg> uncompletedLegs = this.repository.findUncompletedLegs(MomentHelper.getCurrentMoment());
				status = legtId == 0 || uncompletedLegs.contains(leg);
			}
		}

		super.getResponse().setAuthorised(status);
	}

	@Override
	public void load() {
		FlightAssignment flightAssignment;
		int id;

		id = super.getRequest().getData("id", int.class);
		flightAssignment = this.repository.findFlightAssignmentById(id);
		super.getBuffer().addData(flightAssignment);
	}

	@Override
	public void bind(final FlightAssignment flightAssignment) {
		int legId;
		Leg leg;

		legId = super.getRequest().getData("leg", int.class);
		leg = this.repository.findLegById(legId);
		flightAssignment.setLeg(leg);
		flightAssignment.setLastUpdate(MomentHelper.getCurrentMoment());

		super.bindObject(flightAssignment, "flightCrewDuty", "remarks");

	}

	@Override
	public void validate(final FlightAssignment flightAssignment) {
		FlightCrewMember crewMember = flightAssignment.getFlightCrewMember();
		Leg leg = flightAssignment.getLeg();

		// 1. Debe estar en draftMode
		super.state(flightAssignment.isDraftMode(), "*", "flight-crew-member.flight-assignment.error.not-editable");

		// 2. El miembro debe estar AVAILABLE
		super.state(crewMember.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE, "*", "flight-crew-member.flight-assignment.error.member-not-available");

		// 3. No puede haber solapamiento de legs
		Collection<FlightAssignment> currentAssignments = this.repository.findFlightAssignmentsByFlightCrewMemberId(crewMember.getId());
		boolean overlaps = currentAssignments.stream().anyMatch(fa -> {
			Leg l = fa.getLeg();
			return fa.getId() != flightAssignment.getId() && l.getScheduledDeparture().before(leg.getScheduledArrival()) && leg.getScheduledDeparture().before(l.getScheduledArrival());
		});
		super.state(!overlaps, "*", "flight-crew-member.flight-assignment.error.overlapping-legs");

		// 4. No puede haber más de un piloto ni copiloto en el mismo leg
		Collection<FlightAssignment> assignmentsInLeg = this.repository.findFlightAssignmentsByLegId(leg.getId());
		boolean dutyConflict = assignmentsInLeg.stream()
			.anyMatch(fa -> fa.getId() != flightAssignment.getId() && fa.getFlightCrewDuty() == flightAssignment.getFlightCrewDuty() && (fa.getFlightCrewDuty() == FlightCrewDuty.PILOT || fa.getFlightCrewDuty() == FlightCrewDuty.CO_PILOT));
		if (flightAssignment.getFlightCrewDuty() == FlightCrewDuty.PILOT || flightAssignment.getFlightCrewDuty() == FlightCrewDuty.CO_PILOT)
			super.state(!dutyConflict, "flightCrewDuty", "flight-crew-member.flight-assignment.error.duty-already-assigned");

		// 5. El leg no puede haber ocurrido ya
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
		Dataset dataset;
		FlightCrewMember flightCrewMember;
		flightCrewMember = (FlightCrewMember) super.getRequest().getPrincipal().getActiveRealm();

		legs = this.repository.findUncompletedLegs(MomentHelper.getCurrentMoment());

		if (!legs.contains(flightAssignment.getLeg()))
			legChoices = SelectChoices.from(legs, "flightNumber", null);

		else
			legChoices = SelectChoices.from(legs, "flightNumber", flightAssignment.getLeg());

		dutyChoices = SelectChoices.from(FlightCrewDuty.class, flightAssignment.getFlightCrewDuty());
		statusChoices = SelectChoices.from(AssignmentStatus.class, flightAssignment.getAssignmentStatus());

		dataset = super.unbindObject(flightAssignment, "lastUpdate", "remarks", "draftMode");
		dataset.put("member", flightCrewMember.getIdentity().getFullName());
		dataset.put("leg", legChoices.getSelected().getKey());
		dataset.put("legs", legChoices);
		dataset.put("flightCrewDuty", dutyChoices.getSelected().getKey());
		dataset.put("duties", dutyChoices);
		dataset.put("assignmentStatus", statusChoices);

		super.getResponse().addData(dataset);
	}
}
