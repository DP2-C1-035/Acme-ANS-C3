
package acme.features.assistanceAgent.claim;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import acme.client.components.models.Dataset;
import acme.client.components.views.SelectChoices;
import acme.client.services.AbstractGuiService;
import acme.client.services.GuiService;
import acme.entities.claim.Claim;
import acme.entities.claim.ClaimStatus;
import acme.entities.claim.ClaimType;
import acme.entities.leg.Leg;
import acme.entities.tracking_log.TrackingLog;
import acme.entities.tracking_log.TrackingLogIndicator;
import acme.realms.assistanceAgents.AssistanceAgent;

@GuiService
public class AssistanceAgentClaimPublishService extends AbstractGuiService<AssistanceAgent, Claim> {

	// Internal state ---------------------------------------------------------

	@Autowired
	private AssistanceAgentClaimRepository repository;

	// AbstractService interface ----------------------------------------------


	@Override
	public void authorise() {
		boolean status;
		int masterId;
		Claim claim;
		int legId;
		Leg leg;
		boolean externalRelation = true;

		if (super.getRequest().getMethod().equals("GET"))
			externalRelation = true;
		else {
			legId = super.getRequest().getData("leg", int.class);
			leg = this.repository.findLegById(legId);

			boolean isLegValid = leg != null;

			if (isLegValid) {
				boolean isLegNotDraft = !leg.isDraftMode();
				if (isLegNotDraft) {
					boolean isFlightNotDraft = !leg.getFlight().isDraftMode();
					externalRelation = isFlightNotDraft;
				} else
					externalRelation = isLegNotDraft;
			}
		}

		masterId = super.getRequest().getData("id", int.class);
		claim = this.repository.findClaimById(masterId);
		status = claim != null && claim.isDraftMode() && externalRelation && super.getRequest().getPrincipal().hasRealm(claim.getAssistanceAgent());

		super.getResponse().setAuthorised(status);
	}

	@Override
	public void load() {
		int masterId;
		Claim object;

		masterId = super.getRequest().getData("id", int.class);
		object = this.repository.findClaimById(masterId);

		super.getBuffer().addData(object);
	}

	@Override
	public void bind(final Claim object) {
		assert object != null;
		int legId;
		Leg leg;

		legId = super.getRequest().getData("leg", int.class);
		leg = this.repository.findLegById(legId);

		super.bindObject(object, "registrationMoment", "passengerEmail", "description", "type", "indicator", "leg");
		object.setLeg(leg);
	}

	@Override
	public void validate(final Claim object) {
		assert object != null;
		boolean isNotWrongLeg = true;
		Claim claim = this.repository.findClaimById(object.getId());

		if (!super.getBuffer().getErrors().hasErrors("registrationMoment")) {
			if (claim.getLeg() != null && claim.getRegistrationMoment() != null)
				isNotWrongLeg = claim.getRegistrationMoment().after(claim.getLeg().getScheduledArrival());
			super.state(isNotWrongLeg, "registrationMoment", "assistanceAgent.claim.form.error.wrong-leg-date");
		}

		if (!super.getBuffer().getErrors().hasErrors("indicator"))
			super.state(object.getIndicator() != ClaimStatus.PENDING, "indicator", "assistanceAgent.claim.form.error.indicator-must-not-be-pending");

		{
			Collection<TrackingLog> logs;
			logs = this.repository.findTrackingLogsByClaimId(claim.getId());
			boolean allLogsPending;
			allLogsPending = logs.size() == 0 || logs.stream().allMatch(l -> l.getIndicator().equals(TrackingLogIndicator.PENDING)) ? true : false;
			super.state(allLogsPending, "*", "assistanceAgent.claim.form.error.claim-published-with-pending-status");
		}
	}

	@Override
	public void perform(final Claim object) {
		assert object != null;
		object.setDraftMode(false);

		this.repository.save(object);
	}

	@Override
	public void unbind(final Claim object) {
		assert object != null;
		Dataset dataset;

		Collection<Leg> legs;
		SelectChoices choices;
		SelectChoices choicesIndicator;
		SelectChoices choicesType;

		legs = this.repository.findAllLegs();
		legs = legs.stream().filter(l -> l.isDraftMode() == false).toList();
		choices = SelectChoices.from(legs, "flightNumber", object.getLeg());
		choicesType = SelectChoices.from(ClaimType.class, object.getType());
		choicesIndicator = SelectChoices.from(ClaimStatus.class, object.getIndicator());

		dataset = super.unbindObject(object, "registrationMoment", "passengerEmail", "description", "type", "indicator", "leg", "draftMode");
		dataset.put("legs", choices);
		dataset.put("indicators", choicesIndicator);
		dataset.put("types", choicesType);

		super.getResponse().addData(dataset);
	}

}
