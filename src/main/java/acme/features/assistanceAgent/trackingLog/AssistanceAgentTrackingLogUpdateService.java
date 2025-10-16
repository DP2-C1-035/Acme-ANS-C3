
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
		TrackingLog trackingLog;
		Date creationMoment;
		trackingLog = this.repository.findTrackingLogById(object.getId());
		creationMoment = trackingLog.getCreationMoment();
		super.bindObject(object, "lastUpdateMoment", "step", "resolutionPercentage", "resolution", "indicator", "creationMoment");
		object.setLastUpdateMoment(MomentHelper.getCurrentMoment());
		object.setCreationMoment(creationMoment);
	}

	@Override
	public void validate(final TrackingLog object) {
		Claim claim;
		claim = this.repository.findClaimById(object.getClaim().getId());
		Collection<TrackingLog> logs;
		logs = this.repository.findTrackingLogsByClaimId(claim.getId());
		TrackingLog trackingLog;

		if (!super.getBuffer().getErrors().hasErrors("resolution")) {
			boolean isPending = object.getIndicator() == TrackingLogIndicator.PENDING;

			boolean valid = isPending && !Optional.ofNullable(object.getResolution()).map(String::strip).filter(s -> !s.isEmpty()).isPresent()
				|| !isPending && Optional.ofNullable(object.getResolution()).map(String::strip).filter(s -> !s.isEmpty()).isPresent();

			super.state(valid, "resolution", "assistanceAgent.trackingLog.form.error.resolution-not-null");
		}

		if (!super.getBuffer().getErrors().hasErrors("creationMoment")) {
			Date creationMoment = object.getCreationMoment();
			Double percentage = object.getResolutionPercentage();

			boolean hasNoPastInconsistencies = logs.stream().filter(log -> log.getId() != object.getId()).filter(log -> log.getCreationMoment().before(creationMoment)).allMatch(log -> {
				Double otherPercentage = log.getResolutionPercentage();

				if (otherPercentage == null || percentage == null)
					return true;

				if (otherPercentage < percentage)
					return true;

				if (otherPercentage.equals(100.0) && percentage.equals(100.0) && !log.isDraftMode())
					return true;

				return false;
			});

			boolean hasNoFutureInconsistencies = logs.stream().filter(log -> log.getId() != object.getId()).filter(log -> log.getCreationMoment().after(creationMoment)).allMatch(log -> {
				Double otherPercentage = log.getResolutionPercentage();

				if (otherPercentage == null || percentage == null)
					return true;

				if (otherPercentage > percentage)
					return true;

				if (otherPercentage.equals(100.0) && percentage.equals(100.0) && !log.isDraftMode())
					return true;

				return false;
			});

			boolean isCreationMomentValid = hasNoPastInconsistencies && hasNoFutureInconsistencies;

			super.state(isCreationMomentValid, "resolutionPercentage", "assistanceAgent.trackingLog.form.error.invalid-creation-moment");
		}
		if (!super.getBuffer().getErrors().hasErrors("indicator")) {
			claim = this.repository.findClaimById(object.getClaim().getId());
			boolean sameIndicator = true;
			trackingLog = this.repository.findTrackingLogById(object.getId());
			if (claim != null && !claim.isDraftMode() && !trackingLog.getIndicator().equals(TrackingLogIndicator.PENDING))
				if (claim.getIndicator() != null && object.getIndicator() != null)
					sameIndicator = claim.getIndicator().name().equalsIgnoreCase(object.getIndicator().name());
				else
					sameIndicator = false;
			super.state(sameIndicator, "indicator", "assistanceAgent.trackingLog.form.error.indicator-must-match-claim");
		}
	}

	@Override
	public void perform(final TrackingLog object) {
		Claim claim;
		TrackingLogIndicator indicator;
		indicator = object.getIndicator();
		claim = this.repository.findClaimById(object.getClaim().getId());
		if (claim.isDraftMode())
			if (object.getResolutionPercentage() == 100.0) {
				if (indicator.equals(TrackingLogIndicator.ACCEPTED))
					claim.setIndicator(ClaimStatus.ACCEPTED);
				else if (indicator.equals(TrackingLogIndicator.REJECTED))
					claim.setIndicator(ClaimStatus.REJECTED);
				else
					claim.setIndicator(ClaimStatus.PENDING);
			} else
				claim.setIndicator(ClaimStatus.PENDING);
		object.setLastUpdateMoment(MomentHelper.getCurrentMoment());

		this.repository.save(object);
	}

	@Override
	public void unbind(final TrackingLog object) {
		Dataset dataset;

		SelectChoices choicesIndicator;

		choicesIndicator = SelectChoices.from(TrackingLogIndicator.class, object.getIndicator());

		dataset = super.unbindObject(object, "lastUpdateMoment", "step", "resolutionPercentage", "resolution", "indicator", "creationMoment", "draftMode");
		dataset.put("indicators", choicesIndicator);

		super.getResponse().addData(dataset);
	}

}
