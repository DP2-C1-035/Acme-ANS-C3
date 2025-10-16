
package acme.features.assistanceAgent.trackingLog;

import java.util.Collection;
import java.util.Date;
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
public class AssistanceAgentTrackingLogCreateService extends AbstractGuiService<AssistanceAgent, TrackingLog> {

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
		Collection<TrackingLog> logs;
		Boolean bool;

		masterId = super.getRequest().getData("masterId", int.class);
		claim = this.repository.findClaimById(masterId);
		assistanceAgent = claim == null ? null : claim.getAssistanceAgent();
		logs = this.repository.findTrackingLogsByClaimId(masterId);
		bool = this.repository.countTrackingLogsForException(masterId) < 1;
		status = claim != null && (logs.isEmpty() || bool) && super.getRequest().getPrincipal().hasRealm(assistanceAgent);

		super.getResponse().setAuthorised(status);
	}

	@Override
	public void load() {
		TrackingLog object;
		Integer masterId;
		Claim claim;

		masterId = super.getRequest().getData("masterId", int.class);
		claim = this.repository.findClaimById(masterId);

		object = new TrackingLog();
		object.setDraftMode(true);
		object.setClaim(claim);
		object.setIndicator(TrackingLogIndicator.PENDING);
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
		Claim claim;
		claim = this.repository.findClaimById(object.getClaim().getId());
		Collection<TrackingLog> logs;
		logs = this.repository.findTrackingLogsByClaimId(claim.getId());
		if (!super.getBuffer().getErrors().hasErrors("resolution")) {
			boolean isPending = object.getIndicator() == TrackingLogIndicator.PENDING;

			boolean valid = isPending && !Optional.ofNullable(object.getResolution()).map(String::strip).filter(s -> !s.isEmpty()).isPresent()
				|| !isPending && Optional.ofNullable(object.getResolution()).map(String::strip).filter(s -> !s.isEmpty()).isPresent();

			super.state(valid, "resolution", "assistanceAgent.trackingLog.form.error.resolution-not-null");
		}
		if (!super.getBuffer().getErrors().hasErrors("creationMoment")) {
			Date creationMoment = object.getCreationMoment();
			Double percentage = object.getResolutionPercentage();
			int claimId = object.getClaim().getId();

			boolean hasSameCreationMoment = this.repository.existsTrackingLogWithCreationMoment(claimId, creationMoment);

			if (hasSameCreationMoment)
				super.state(false, "creationMoment", "assistanceAgent.trackingLog.form.error.duplicate-creation-moment");
			else {
				boolean hasNoPastInconsistencies = logs.stream().filter(log -> log.getId() != object.getId()).filter(log -> log.getCreationMoment().before(creationMoment)).allMatch(log -> {
					Double otherPercentage = log.getResolutionPercentage();
					return otherPercentage != null && percentage != null && otherPercentage < percentage;
				});

				boolean isCreationMomentValid = hasNoPastInconsistencies;

				super.state(isCreationMomentValid, "resolutionPercentage", "assistanceAgent.trackingLog.form.error.non-increasing-resolution-percentage");
			}

			Date claimRegistrationMoment;
			boolean lastUpdateMomentIsAfter;
			claimRegistrationMoment = claim.getRegistrationMoment();
			lastUpdateMomentIsAfter = object.getLastUpdateMoment().after(claimRegistrationMoment);
			super.state(lastUpdateMomentIsAfter, "creationMoment", "assistanceAgent.trackingLog.form.error.creation-moment-not-after");
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
		object.setCreationMoment(MomentHelper.getCurrentMoment());

		this.repository.save(object);
	}

	@Override
	public void unbind(final TrackingLog object) {
		Dataset dataset;

		SelectChoices choicesIndicator;

		choicesIndicator = SelectChoices.from(TrackingLogIndicator.class, object.getIndicator());

		dataset = super.unbindObject(object, "lastUpdateMoment", "step", "resolutionPercentage", "resolution", "indicator", "creationMoment");
		dataset.put("masterId", super.getRequest().getData("masterId", int.class));
		dataset.put("indicators", choicesIndicator);

		super.getResponse().addData(dataset);
	}

}
