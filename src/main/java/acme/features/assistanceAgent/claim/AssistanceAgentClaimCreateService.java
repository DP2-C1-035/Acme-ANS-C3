
package acme.features.assistanceAgent.claim;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import acme.client.components.models.Dataset;
import acme.client.components.views.SelectChoices;
import acme.client.helpers.MomentHelper;
import acme.client.services.AbstractGuiService;
import acme.client.services.GuiService;
import acme.entities.claim.Claim;
import acme.entities.claim.ClaimStatus;
import acme.entities.claim.ClaimType;
import acme.entities.leg.Leg;
import acme.realms.assistanceAgents.AssistanceAgent;

@GuiService
public class AssistanceAgentClaimCreateService extends AbstractGuiService<AssistanceAgent, Claim>

{
	// Internal state ---------------------------------------------------------

	@Autowired
	private AssistanceAgentClaimRepository repository;

	// AbstractService interface ----------------------------------------------


	@Override
	public void authorise() {
		AssistanceAgent assistanceAgent;
		boolean status = false;
		boolean bool = true;
		int legId;
		Leg leg;

		if (super.getRequest().getMethod().equals("GET"))
			bool = true;
		else {
			legId = super.getRequest().getData("leg", int.class);
			leg = this.repository.findLegById(legId);

			boolean isLegValid = leg != null;

			if (isLegValid) {
				boolean isLegNotDraft = !leg.isDraftMode();
				if (isLegNotDraft) {
					boolean isFlightNotDraft = !leg.getFlight().isDraftMode();
					bool = isFlightNotDraft;
				} else
					bool = isLegNotDraft;
			}
		}

		assistanceAgent = (AssistanceAgent) super.getRequest().getPrincipal().getActiveRealm();
		if (super.getRequest().getPrincipal().hasRealm(assistanceAgent))
			if (bool)
				status = true;

		super.getResponse().setAuthorised(status);
	}

	@Override
	public void load() {
		Claim object;
		AssistanceAgent assistanceAgent;

		assistanceAgent = (AssistanceAgent) super.getRequest().getPrincipal().getActiveRealm();

		object = new Claim();
		object.setDraftMode(true);
		object.setAssistanceAgent(assistanceAgent);
		object.setIndicator(ClaimStatus.PENDING);
		object.setRegistrationMoment(MomentHelper.getCurrentMoment());

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

		if (!super.getBuffer().getErrors().hasErrors("indicator"))
			super.state(object.getIndicator() == ClaimStatus.PENDING, "indicator", "assistanceAgent.claim.form.error.indicator-must-be-pending");

		if (!super.getBuffer().getErrors().hasErrors("registrationMoment")) {
			if (claim.getLeg() != null && claim.getRegistrationMoment() != null)
				isNotWrongLeg = claim.getRegistrationMoment().after(claim.getLeg().getScheduledArrival());
			super.state(isNotWrongLeg, "registrationMoment", "assistanceAgent.claim.form.error.wrong-leg-date");
		}
	}

	@Override
	public void perform(final Claim object) {
		assert object != null;

		object.setRegistrationMoment(MomentHelper.getCurrentMoment());

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

		dataset = super.unbindObject(object, "registrationMoment", "passengerEmail", "description", "type", "indicator", "leg");
		dataset.put("legs", choices);
		dataset.put("indicators", choicesIndicator);
		dataset.put("types", choicesType);

		super.getResponse().addData(dataset);
	}
}
