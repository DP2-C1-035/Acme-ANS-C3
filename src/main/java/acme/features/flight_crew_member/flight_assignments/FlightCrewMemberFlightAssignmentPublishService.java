
package acme.features.flight_crew_member.flight_assignments;

import java.util.Collection;
import java.util.Date;

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
import acme.realms.flight_crew_member.FlightCrewMember;

@GuiService
public class FlightCrewMemberFlightAssignmentPublishService extends AbstractGuiService<FlightCrewMember, FlightAssignment> {

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

		status = flightAssignment != null && flightAssignment.isDraftMode() // solo se pueden publicar borradores
			&& super.getRequest().getPrincipal().hasRealm(flightCrewMember);

		super.getResponse().setAuthorised(status);
	}

	@Override
	public void load() {
		int id = super.getRequest().getData("id", int.class);
		FlightAssignment flightAssignment = this.repository.findFlightAssignmentById(id);
		super.getBuffer().addData(flightAssignment);
	}

	@Override
	public void bind(final FlightAssignment flightAssignment) {
		FlightCrewMember flightCrewMember = (FlightCrewMember) super.getRequest().getPrincipal().getActiveRealm();
		int legId = super.getRequest().getData("leg", int.class);
		Leg leg = this.repository.findLegById(legId);

		super.bindObject(flightAssignment, "flightCrewDuty", "remarks");
		flightAssignment.setFlightCrewMember(flightCrewMember);
		flightAssignment.setLeg(leg);
		flightAssignment.setLastUpdate(MomentHelper.getCurrentMoment());
	}

	@Override
	public void validate(final FlightAssignment flightAssignment) {
		if (flightAssignment.getLeg() == null)
			return;

		Leg leg = flightAssignment.getLeg();

		// 1️ Validar que el leg no haya ocurrido
		boolean legHasOccurred = leg.getScheduledArrival().before(MomentHelper.getCurrentMoment());
		super.state(!legHasOccurred, "leg", "flight-crew-member.flight-assignment.error.leg-occurred");

		// 2️ Validar que el leg esté publicado
		boolean legNotPublished = leg.isDraftMode();
		super.state(!legNotPublished, "leg", "flight-crew-member.flight-assignment.error.leg-not-published");

		// 3️ Validar que no haya más de un piloto o copiloto en el mismo leg
		Collection<FlightAssignment> assignmentsOfLeg = this.repository.findPublishedFlightAssignmentsByLegId(leg.getId());
		if (flightAssignment.getFlightCrewDuty() == FlightCrewDuty.PILOT || flightAssignment.getFlightCrewDuty() == FlightCrewDuty.CO_PILOT)
			for (FlightAssignment fa : assignmentsOfLeg) {
				boolean sameDuty = fa.getFlightCrewDuty() == flightAssignment.getFlightCrewDuty();
				if (sameDuty) {
					super.state(false, "flightCrewDuty", "flight-crew-member.flight-assignment.validation.duty.publish");
					break;
				}
			}

		// 4️ Validar que no haya solapamiento con otros vuelos publicados
		Date newDeparture = leg.getScheduledDeparture();
		Date newArrival = leg.getScheduledArrival();
		Collection<FlightAssignment> publishedAssignments = this.repository.findFlightAssignmentsByFlightCrewMemberId(flightAssignment.getFlightCrewMember().getId());

		boolean overlaps = publishedAssignments.stream().anyMatch(existing -> {
			Leg existingLeg = existing.getLeg();
			return !existing.equals(flightAssignment) && existingLeg != null && existingLeg.getScheduledDeparture().before(newArrival) && newDeparture.before(existingLeg.getScheduledArrival());
		});

		super.state(!overlaps, "leg", "flight-crew-member.flight-assignment.validation.overlapping-leg.publish");
	}

	@Override
	public void perform(final FlightAssignment flightAssignment) {
		flightAssignment.setDraftMode(false);
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
