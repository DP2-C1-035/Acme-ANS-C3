
package acme.features.assistanceAgent.trackingLog;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import acme.client.components.models.Dataset;
import acme.client.services.AbstractGuiService;
import acme.client.services.GuiService;
import acme.entities.claim.Claim;
import acme.entities.tracking_log.TrackingLog;
import acme.realms.assistanceAgents.AssistanceAgent;

@GuiService
public class AssistanceAgentTrackingLogListService extends AbstractGuiService<AssistanceAgent, TrackingLog> {

	// Internal state ---------------------------------------------------------

	@Autowired
	private AssistanceAgentTrackingLogRepository repository;

	// AbstractService interface ----------------------------------------------


	@Override
	public void authorise() {
		boolean status;
		int masterId;
		Claim claim;
		AssistanceAgent assistanceAgent;

		masterId = super.getRequest().getData("masterId", int.class);
		claim = this.repository.findClaimById(masterId);
		assistanceAgent = claim == null ? null : claim.getAssistanceAgent();
		status = claim != null && super.getRequest().getPrincipal().hasRealm(assistanceAgent);

		super.getResponse().setAuthorised(status);
	}

	@Override
	public void load() {
		Collection<TrackingLog> objects;
		int masterId;

		masterId = super.getRequest().getData("masterId", int.class);
		objects = this.repository.findTrackingLogsByClaimId(masterId);

		super.getBuffer().addData(objects);
	}

	@Override
	public void unbind(final TrackingLog object) {
		String published;
		Dataset dataset;

		dataset = super.unbindObject(object, "lastUpdateMoment", "resolutionPercentage", "indicator", "creationMoment");
		published = !object.isDraftMode() ? "Yes" : "No";
		dataset.put("published", published);

		super.getResponse().addData(dataset);
	}

	@Override
	public void unbind(final Collection<TrackingLog> object) {
		int masterId;
		Claim claim;
		Boolean noMore;
		Boolean exceptionalCase;
		Boolean greatRealm;
		Boolean hasDraftAt100;

		masterId = super.getRequest().getData("masterId", int.class);
		claim = this.repository.findClaimById(masterId);

		exceptionalCase = this.repository.countTrackingLogsForException(masterId) == 1;
		noMore = this.repository.countTrackingLogsForException(masterId) == 0;
		Long totalAt100 = this.repository.countAllTrackingLogsAt100(masterId);
		hasDraftAt100 = totalAt100 > 1;
		if (hasDraftAt100)
			exceptionalCase = false;

		greatRealm = super.getRequest().getPrincipal().hasRealm(claim.getAssistanceAgent());

		super.getResponse().addGlobal("masterId", masterId);
		super.getResponse().addGlobal("exceptionalCase", exceptionalCase);
		super.getResponse().addGlobal("noMore", noMore);
		super.getResponse().addGlobal("greatRealm", greatRealm);
	}

}
