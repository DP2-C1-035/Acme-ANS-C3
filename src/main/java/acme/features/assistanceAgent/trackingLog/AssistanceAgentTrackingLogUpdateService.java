
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
		super.bindObject(object, "lastUpdateMoment", "step", "resolutionPercentage", "resolution", "indicator", "creationMoment");
	}

	@Override
	public void validate(final TrackingLog object) {
		Claim claim;
		claim = this.repository.findClaimById(object.getClaim().getId());
		Collection<TrackingLog> logs;
		logs = this.repository.findTrackingLogsByClaimId(claim.getId());
		if (!super.getBuffer().getErrors().hasErrors("indicator")) {
			boolean bool1;
			boolean bool2;

			if (!super.getBuffer().getErrors().hasErrors("resolutionPercentage")) {
				bool1 = object.getIndicator() == TrackingLogIndicator.PENDING && object.getResolutionPercentage() < 100;
				bool2 = object.getIndicator() != TrackingLogIndicator.PENDING && object.getResolutionPercentage() == 100;
				super.state(bool1 || bool2, "indicator", "assistanceAgent.trackingLog.form.error.indicator-in-progress");
			}
		}
		if (!super.getBuffer().getErrors().hasErrors("resolution")) {
			boolean isPending = object.getIndicator() == TrackingLogIndicator.PENDING;

			boolean valid = isPending && !Optional.ofNullable(object.getResolution()).map(String::strip).filter(s -> !s.isEmpty()).isPresent()
				|| !isPending && Optional.ofNullable(object.getResolution()).map(String::strip).filter(s -> !s.isEmpty()).isPresent();

			super.state(valid, "resolution", "assistanceAgent.trackingLog.form.error.resolution-not-null");
		}
		if (!super.getBuffer().getErrors().hasErrors("resolutionpercentage")) {
			Double percentage = object.getResolutionPercentage();
			Optional<TrackingLog> mostRecentLog = logs.stream().filter(log -> log.getId() != object.getId()).max(Comparator.comparing(TrackingLog::getCreationMoment));

			boolean isProgressing = mostRecentLog.map(lastLog -> percentage > lastLog.getResolutionPercentage()).orElse(true); // Si es el primer log, se acepta cualquier porcentaje

			super.state(isProgressing, "resolutionpercentage", "assistanceAgent.trackingLog.form.error.non-increasing-resolution-percentage");
		}
		if (!super.getBuffer().getErrors().hasErrors("creationMoment")) {
			Date creationMoment = object.getCreationMoment();
			Double percentage = object.getResolutionPercentage();

			boolean hasNoPastInconsistencies = logs.stream().filter(log -> log.getId() != object.getId()).filter(log -> log.getCreationMoment().before(creationMoment)).allMatch(log -> log.getResolutionPercentage() < percentage);

			boolean hasNoFutureInconsistencies = logs.stream().filter(log -> log.getId() != object.getId()).filter(log -> log.getCreationMoment().after(creationMoment)).allMatch(log -> log.getResolutionPercentage() > percentage);

			boolean isCreationMomentValid = hasNoPastInconsistencies && hasNoFutureInconsistencies;

			super.state(isCreationMomentValid, "creationMoment", "assistanceAgent.trackingLog.form.error.invalid-creation-moment");

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
