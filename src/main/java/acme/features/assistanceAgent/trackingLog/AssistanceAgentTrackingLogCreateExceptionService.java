
package acme.features.assistanceAgent.trackingLog;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import acme.client.components.models.Dataset;
import acme.client.components.views.SelectChoices;
import acme.client.helpers.MomentHelper;
import acme.client.services.AbstractGuiService;
import acme.client.services.GuiService;
import acme.entities.claim.Claim;
import acme.entities.tracking_log.TrackingLog;
import acme.entities.tracking_log.TrackingLogIndicator;
import acme.realms.assistanceAgents.AssistanceAgent;

@GuiService
public class AssistanceAgentTrackingLogCreateExceptionService extends AbstractGuiService<AssistanceAgent, TrackingLog> {

	// Internal state ---------------------------------------------------------

	@Autowired
	private AssistanceAgentTrackingLogRepository repository;

	// AbstractService interface ----------------------------------------------


	@Override
	public void authorise() {
		boolean status;
		int masterId;
		AssistanceAgent assistanceAgent;
		Claim claim;
		Boolean exceptionalCase;

		masterId = super.getRequest().getData("masterId", int.class);
		claim = this.repository.findClaimById(masterId);
		assistanceAgent = claim == null ? null : claim.getAssistanceAgent();
		exceptionalCase = this.repository.countTrackingLogsForException(masterId) == 1;

		status = claim != null && exceptionalCase && super.getRequest().getPrincipal().hasRealm(assistanceAgent);

		super.getResponse().setAuthorised(status);
	}

	@Override
	public void load() {
		TrackingLog object;
		int masterId;
		Claim claim;
		TrackingLog trackingLog;

		masterId = super.getRequest().getData("masterId", int.class);
		claim = this.repository.findClaimById(masterId);
		trackingLog = this.repository.findTrackingLogsByClaimIdAndIndicator(masterId, TrackingLogIndicator.PENDING).stream().toList().get(0);

		object = new TrackingLog();
		object.setDraftMode(true);
		object.setClaim(claim);
		object.setResolutionPercentage(100.00);
		object.setIndicator(trackingLog.getIndicator());
		object.setLastUpdateMoment(MomentHelper.getCurrentMoment());
		object.setCreationMoment(MomentHelper.getCurrentMoment());

		super.getBuffer().addData(object);
	}

	@Override
	public void bind(final TrackingLog object) {
		super.bindObject(object, "lastUpdateMoment", "step", "resolutionPercentage", "resolution", "indicator", "creationMoment");
		object.setLastUpdateMoment(MomentHelper.getCurrentMoment());
		object.setCreationMoment(MomentHelper.getCurrentMoment());
	}

	@Override
	public void validate(final TrackingLog object) {
		if (!super.getBuffer().getErrors().hasErrors("resolution"))
			super.state(Optional.ofNullable(object.getResolution()).map(String::strip).filter(s -> !s.isEmpty()).isPresent(), "resolution", "assistanceAgent.trackingLog.form.error.resolution-not-null");
		if (!super.getBuffer().getErrors().hasErrors("indicator")) {
			Claim claim = this.repository.findClaimById(object.getClaim().getId());
			boolean sameIndicator = false;

			if (claim != null && claim.getIndicator() != null && object.getIndicator() != null)
				sameIndicator = claim.getIndicator().name().equalsIgnoreCase(object.getIndicator().name());

			super.state(sameIndicator, "indicator", "assistanceAgent.trackingLog.form.error.indicator-must-match-claim");
		}
	}

	@Override
	public void perform(final TrackingLog object) {

		object.setLastUpdateMoment(MomentHelper.getCurrentMoment());
		object.setCreationMoment(MomentHelper.getCurrentMoment());
		object.setResolutionPercentage(100.00);
		this.repository.save(object);
	}

	@Override
	public void unbind(final TrackingLog object) {
		Dataset dataset;
		SelectChoices choicesIndicator;
		choicesIndicator = SelectChoices.from(TrackingLogIndicator.class, object.getIndicator());
		dataset = super.unbindObject(object, "lastUpdateMoment", "step", "resolutionPercentage", "resolution", "indicator", "creationMoment");
		dataset.put("masterId", super.getRequest().getData("masterId", int.class));
		dataset.put("exceptionalCase", true);
		dataset.put("indicators", choicesIndicator);
		super.getResponse().addData(dataset);

	}

}
