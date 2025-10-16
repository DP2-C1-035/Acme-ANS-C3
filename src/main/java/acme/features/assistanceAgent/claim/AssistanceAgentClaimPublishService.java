
package acme.features.assistanceAgent.claim;

import java.util.Collection;
import java.util.Date;

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
		TrackingLog trackingLog;
		ClaimStatus indicator;
		Claim claim;
		legId = super.getRequest().getData("leg", int.class);
		leg = this.repository.findLegById(legId);
		trackingLog = this.repository.findFinalResolutionByClaimId(object.getId());
		claim = this.repository.findClaimById(object.getId());
		super.bindObject(object, "registrationMoment", "passengerEmail", "description", "type", "indicator", "leg");
		object.setLeg(leg);
		if (trackingLog != null) {
			indicator = trackingLog.getIndicator().equals(TrackingLogIndicator.ACCEPTED) ? ClaimStatus.ACCEPTED : ClaimStatus.REJECTED;
			object.setIndicator(indicator);
		} else
			object.setIndicator(claim.getIndicator());
		object.setRegistrationMoment(claim.getRegistrationMoment());

	}

	@Override
	public void validate(final Claim object) {
		assert object != null;
		boolean isNotWrongLeg = true;
		Leg leg;
		Date registrationMoment;
		Claim claim;
		claim = this.repository.findClaimById(object.getId());
		leg = object.getLeg() != null ? this.repository.findLegById(object.getLeg().getId()) : null;
		registrationMoment = object.getRegistrationMoment();
		if (!super.getBuffer().getErrors().hasErrors("registrationMoment")) {
			if (leg != null)
				isNotWrongLeg = registrationMoment.after(leg.getScheduledArrival());
			super.state(isNotWrongLeg, "registrationMoment", "assistanceAgent.claim.form.error.wrong-leg-date");
		}

		{
			Collection<TrackingLog> logs = this.repository.findTrackingLogsByClaimId(claim.getId());
			boolean hasAcceptedOrRejected = logs.stream().anyMatch(l -> l.getIndicator().equals(TrackingLogIndicator.ACCEPTED) || l.getIndicator().equals(TrackingLogIndicator.REJECTED));
			boolean canPublish = hasAcceptedOrRejected;
			super.state(canPublish, "*", "assistanceAgent.claim.form.error.claim-published-with-pending-status");
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
