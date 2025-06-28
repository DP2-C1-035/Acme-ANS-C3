
package acme.features.assistanceAgent.claim;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import acme.client.components.models.Dataset;
import acme.client.services.AbstractGuiService;
import acme.client.services.GuiService;
import acme.entities.claim.Claim;
import acme.entities.claim.ClaimStatus;
import acme.realms.assistanceAgents.AssistanceAgent;

@GuiService
public class AssistanceAgentClaimListCompletedService extends AbstractGuiService<AssistanceAgent, Claim> {

	// Internal state ---------------------------------------------------------

	@Autowired
	private AssistanceAgentClaimRepository repository;

	// AbstractService interface ----------------------------------------------


	@Override
	public void authorise() {
		AssistanceAgent assistanceAgent;
		boolean status;

		assistanceAgent = (AssistanceAgent) super.getRequest().getPrincipal().getActiveRealm();
		status = super.getRequest().getPrincipal().hasRealm(assistanceAgent);
		super.getResponse().setAuthorised(status);
	}

	@Override
	public void load() {
		Collection<Claim> objects;
		int masterId;

		masterId = super.getRequest().getPrincipal().getActiveRealm().getId();
		objects = this.repository.findClaimsCompletedByMasterId(masterId, ClaimStatus.PENDING);

		super.getBuffer().addData(objects);
	}

	@Override
	public void unbind(final Claim object) {
		assert object != null;
		String published;
		Dataset dataset;

		dataset = super.unbindObject(object, "type", "indicator");
		published = !object.isDraftMode() ? "Yes" : "No";
		dataset.put("published", published);
		dataset.put("leg", object.getLeg().getFlightNumber());

		super.getResponse().addData(dataset);
	}

}
