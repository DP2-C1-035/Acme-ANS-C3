
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
public class AssistanceAgentTrackingLogPublishService extends AbstractGuiService<AssistanceAgent, TrackingLog> {

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
		int claimId = object.getClaim().getId();
		Claim claim = this.repository.findClaimById(claimId);
		Collection<TrackingLog> logs = this.repository.findTrackingLogsByClaimId(claimId);

		super.state(!claim.isDraftMode(), "*", "assistanceAgent.trackingLog.form.error.claim-must-be-published");

		if (!super.getBuffer().getErrors().hasErrors("resolution")) {
			boolean isPending = object.getIndicator() == TrackingLogIndicator.PENDING;
			boolean validResolution = isPending && !Optional.ofNullable(object.getResolution()).map(String::strip).filter(s -> !s.isEmpty()).isPresent()
				|| !isPending && Optional.ofNullable(object.getResolution()).map(String::strip).filter(s -> !s.isEmpty()).isPresent();
			super.state(validResolution, "resolution", "assistanceAgent.trackingLog.form.error.resolution-not-null");
		}

		if (!super.getBuffer().getErrors().hasErrors("creationMoment")) {
			Date creationMoment = object.getCreationMoment();
			Double percentage = object.getResolutionPercentage();

			boolean percentageNotNull = percentage != null;
			if (percentageNotNull) {

				boolean existsPublished100 = this.repository.countTrackingLogsForException(object.getClaim().getId()) > 0;

				if (!(percentage.equals(100.0) && existsPublished100)) {

					final Double currentPercentage = percentage;

					boolean hasNoPastInconsistencies = logs.stream().filter(log -> log.getId() != object.getId()).filter(log -> log.getCreationMoment() != null && log.getCreationMoment().before(creationMoment)).allMatch(log -> {
						Double otherPercentage = log.getResolutionPercentage();
						if (otherPercentage == null)
							return true;
						return otherPercentage < currentPercentage;
					});

					boolean hasNoFutureInconsistencies = logs.stream().filter(log -> log.getId() != object.getId()).filter(log -> log.getCreationMoment() != null && log.getCreationMoment().after(creationMoment)).allMatch(log -> {
						Double otherPercentage = log.getResolutionPercentage();
						if (otherPercentage == null)
							return true;
						return otherPercentage > currentPercentage;
					});

					boolean isCreationMomentValid = hasNoPastInconsistencies && hasNoFutureInconsistencies;
					super.state(isCreationMomentValid, "resolutionPercentage", "assistanceAgent.trackingLog.form.error.invalid-creation-moment");
				}
			}

			logs = this.repository.findTrackingLogsByClaimId(object.getClaim().getId());
			percentage = object.getResolutionPercentage();

			if (percentage != null) {
				final Double currentPercentage = percentage;
				boolean hasUnpublishedLowerLogs = logs.stream().filter(log -> log.getId() != object.getId()).filter(log -> log.isDraftMode()).filter(log -> {
					Double otherPercentage = log.getResolutionPercentage();
					return otherPercentage != null && otherPercentage < currentPercentage;
				}).findAny().isPresent();

				super.state(!hasUnpublishedLowerLogs, "*", "assistanceAgent.trackingLog.form.error.unpublished-lower-logs");
			}
		}

	}

	@Override
	public void perform(final TrackingLog object) {
		Claim claim;
		TrackingLogIndicator indicator;
		indicator = object.getIndicator();
		claim = this.repository.findClaimById(object.getClaim().getId());
		if (object.getResolutionPercentage() == 100.0)
			if (indicator.equals(TrackingLogIndicator.ACCEPTED))
				claim.setIndicator(ClaimStatus.ACCEPTED);
			else if (indicator.equals(TrackingLogIndicator.REJECTED))
				claim.setIndicator(ClaimStatus.REJECTED);
			else
				claim.setIndicator(ClaimStatus.PENDING);
		object.setDraftMode(false);
		object.setLastUpdateMoment(MomentHelper.getCurrentMoment());

		this.repository.save(claim);
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
