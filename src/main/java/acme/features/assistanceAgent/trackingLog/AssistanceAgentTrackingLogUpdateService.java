
package acme.features.assistanceAgent.trackingLog;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import acme.client.components.models.Dataset;
import acme.client.components.views.SelectChoices;
import acme.client.helpers.MomentHelper;
import acme.client.services.AbstractGuiService;
import acme.client.services.GuiService;
import acme.entities.claim.Claim;
import acme.entities.claim.ClaimStatus;
import acme.entities.tracking_log.TrackingLog;
import acme.entities.tracking_log.TrackingLogIndicator;
import acme.realms.assistanceAgents.AssistanceAgent;

@GuiService
public class AssistanceAgentTrackingLogUpdateService extends AbstractGuiService<AssistanceAgent, TrackingLog> {

	// Internal state ---------------------------------------------------------

	@Autowired
	private AssistanceAgentTrackingLogRepository repository;

	// AbstractService interface ----------------------------------------------


	@Override
	public void authorise() {
		boolean status;
		int masterId;
		AssistanceAgent assistanceAgent;
		TrackingLog trackingLog;

		masterId = super.getRequest().getData("id", int.class);
		trackingLog = this.repository.findTrackingLogById(masterId);
		assistanceAgent = trackingLog == null ? null : trackingLog.getClaim().getAssistanceAgent();
		status = trackingLog != null && trackingLog.isDraftMode() && super.getRequest().getPrincipal().hasRealm(assistanceAgent);

		super.getResponse().setAuthorised(status);
	}

	@Override
	public void load() {
		int masterId;
		TrackingLog object;

		masterId = super.getRequest().getData("id", int.class);
		object = this.repository.findTrackingLogById(masterId);

		super.getBuffer().addData(object);
	}

	@Override
	public void bind(final TrackingLog object) {
		super.bindObject(object, "lastUpdateMoment", "step", "resolutionPercentage", "resolution", "indicator");
	}

	@Override
	public void validate(final TrackingLog object) {
		if (!super.getBuffer().getErrors().hasErrors("indicator")) {
			boolean bool1;
			boolean bool2;
			if (!super.getBuffer().getErrors().hasErrors("resolutionPercentage")) {
				bool1 = object.getIndicator() == TrackingLogIndicator.PENDING && object.getResolutionPercentage() < 100;
				bool2 = object.getIndicator() != TrackingLogIndicator.PENDING && object.getResolutionPercentage() == 100;
				super.state(bool1 || bool2, "indicator", "assistanceAgent.claim.form.error.indicator-in-progress");
			}
		}
		if (!super.getBuffer().getErrors().hasErrors("resolution")) {
			boolean isInProgress = object.getIndicator() == TrackingLogIndicator.PENDING;

			boolean valid = isInProgress && !Optional.ofNullable(object.getResolution()).map(String::strip).filter(s -> !s.isEmpty()).isPresent()
				|| !isInProgress && Optional.ofNullable(object.getResolution()).map(String::strip).filter(s -> !s.isEmpty()).isPresent();

			super.state(valid, "resolution", "assistanceAgent.claim.form.error.resolution-not-null");
		}
	}

	@Override
	public void perform(final TrackingLog object) {
		Claim claim;
		TrackingLogIndicator indicator;
		indicator = object.getIndicator();
		claim = this.repository.findClaimById(object.getClaim().getId());
		if (indicator.equals(TrackingLogIndicator.ACCEPTED))
			claim.setIndicator(ClaimStatus.ACCEPTED);
		else if (indicator.equals(TrackingLogIndicator.REJECTED))
			claim.setIndicator(ClaimStatus.REJECTED);
		else
			claim.setIndicator(ClaimStatus.PENDING);

		object.setLastUpdateMoment(MomentHelper.getCurrentMoment());

		this.repository.save(object);
	}

	@Override
	public void unbind(final TrackingLog object) {
		Dataset dataset;

		SelectChoices choicesIndicator;

		choicesIndicator = SelectChoices.from(TrackingLogIndicator.class, object.getIndicator());

		dataset = super.unbindObject(object, "lastUpdateMoment", "step", "resolutionPercentage", "resolution", "indicator", "draftMode");
		dataset.put("indicators", choicesIndicator);

		super.getResponse().addData(dataset);
	}

}
