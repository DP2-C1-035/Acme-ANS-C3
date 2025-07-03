
package acme.features.assistanceAgent.trackingLog;

import java.util.Collection;
import java.util.Comparator;
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
		super.bindObject(object, "lastUpdateMoment", "step", "resolutionPercentage", "resolution", "indicator");
	}

	@Override
	public void validate(final TrackingLog object) {
		{
			super.state(!object.getClaim().isDraftMode(), "*", "assistanceAgent.trackingLog.form.error.claim-must-be-published");
			super.state(this.repository.countTrackingLogsForException(object.getClaim().getId()) < 2, "*", "assistanceAgent.trackingLog.form.error.only-two-tracking-logs-100");

		}
		if (!super.getBuffer().getErrors().hasErrors("indicator")) {
			boolean bool1;
			boolean bool2;
			if (!super.getBuffer().getErrors().hasErrors("resolutionPercentage")) {
				bool1 = object.getIndicator() == TrackingLogIndicator.PENDING && object.getResolutionPercentage() < 100;
				bool2 = object.getIndicator() != TrackingLogIndicator.PENDING && object.getResolutionPercentage() == 100;
				super.state(bool1 || bool2, "indicator", "assistanceAgent.trackingLog.form.error.indicator-pending");
			}
			boolean bool3;
			ClaimStatus indicator = this.repository.findClaimById(object.getClaim().getId()).getIndicator();
			TrackingLogIndicator logIndicator = object.getIndicator();
			if (indicator.toString().equals(logIndicator.toString()))
				bool3 = true;
			else
				bool3 = false;
			super.state(bool3, "indicator", "assistanceAgent.trackingLog.form.error.indicator-not-equal-to-claim-indicator");
		}
		if (!super.getBuffer().getErrors().hasErrors("resolutionPercentage")) {
			Double maxResolutionPercentage;
			double finalMaxResolutionPercentage;
			boolean notAnyMore;

			notAnyMore = this.repository.countTrackingLogsForException(object.getClaim().getId()) == 1;
			maxResolutionPercentage = this.repository.findMaxResolutionPercentageByClaimId(object.getId(), object.getClaim().getId());
			finalMaxResolutionPercentage = maxResolutionPercentage != null ? maxResolutionPercentage : -0.01;

			if (notAnyMore)
				super.state(object.getResolutionPercentage() == 100, "resolutionPercentage", "assistanceAgent.trackingLog.form.error.must-be-100");
			else
				super.state(object.getResolutionPercentage() > finalMaxResolutionPercentage, "resolutionPercentage", "assistanceAgent.trackingLog.form.error.less-than-max-resolution-percentage");
			Collection<TrackingLog> logs = this.repository.findTrackingLogsByClaimId(object.getClaim().getId());

			Double percentage = object.getResolutionPercentage();

			boolean hasLowerDrafts = logs.stream().filter(log -> log.getId() != object.getId()).filter(log -> log.isDraftMode()).anyMatch(log -> log.getResolutionPercentage() < percentage);

			super.state(!hasLowerDrafts, "*", "assistanceAgent.trackingLog.form.error.unpublished-lower-logs");

			Optional<TrackingLog> mostRecentLog = logs.stream().filter(log -> log.getId() != object.getId()).max(Comparator.comparing(TrackingLog::getCreationMoment));

			boolean isProgressing = mostRecentLog.map(lastLog -> percentage > lastLog.getResolutionPercentage()).orElse(true);

			super.state(isProgressing, "resolutionPercentage", "assistanceAgent.trackingLog.form.error.non-increasing-resolution-percentage");
		}
		if (!super.getBuffer().getErrors().hasErrors("resolution")) {
			boolean isPending = object.getIndicator() == TrackingLogIndicator.PENDING;

			boolean valid = isPending && !Optional.ofNullable(object.getResolution()).map(String::strip).filter(s -> !s.isEmpty()).isPresent()
				|| !isPending && Optional.ofNullable(object.getResolution()).map(String::strip).filter(s -> !s.isEmpty()).isPresent();

			super.state(valid, "resolution", "assistanceAgent.trackingLog.form.error.resolution-not-null");
		}
		Claim claim;
		claim = this.repository.findClaimById(object.getClaim().getId());
		Collection<TrackingLog> logs;
		logs = this.repository.findTrackingLogsByClaimId(claim.getId());
		if (!super.getBuffer().getErrors().hasErrors("creationMoment")) {
			Date creationMoment = object.getCreationMoment();
			Double percentage = object.getResolutionPercentage();

			boolean hasNoPastInconsistencies = logs.stream().filter(log -> log.getId() != object.getId()).filter(log -> log.getCreationMoment().before(creationMoment)).allMatch(log -> log.getResolutionPercentage() < percentage);

			boolean hasNoFutureInconsistencies = logs.stream().filter(log -> log.getId() != object.getId()).filter(log -> log.getCreationMoment().after(creationMoment)).allMatch(log -> log.getResolutionPercentage() > percentage);

			boolean isCreationMomentValid = hasNoPastInconsistencies && hasNoFutureInconsistencies;

			super.state(isCreationMomentValid, "resolutionPercentage", "assistanceAgent.trackingLog.form.error.invalid-creation-moment");

			// boolean creationMomentIsAfterClaimRegistrationMoment = MomentHelper.isAfter(creationMoment, claim.getRegistrationMoment());

			//super.state(creationMomentIsAfterClaimRegistrationMoment, "creationMoment", "assistanceAgent.claim.form.error.creation-moment-not-after-registration-moment");
		}
		//if (!super.getBuffer().getErrors().hasErrors("lastUpdateMoment")) {
		//boolean lastUpdateMomentIsAfterCreationMoment = MomentHelper.isAfterOrEqual(object.getLastUpdateMoment(), object.getCreationMoment());
		//super.state(lastUpdateMomentIsAfterCreationMoment, "lastUpdateMoment", "assistanceAgent.claim.form.error.update-moment-not-after-creation-moment");
		//}
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

		dataset = super.unbindObject(object, "lastUpdateMoment", "step", "resolution", "resolutionReason", "indicator", "draftMode");
		dataset.put("indicators", choicesIndicator);

		super.getResponse().addData(dataset);
	}

}
