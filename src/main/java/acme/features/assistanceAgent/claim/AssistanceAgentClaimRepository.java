
package acme.features.assistanceAgent.claim;

import java.util.Collection;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import acme.client.repositories.AbstractRepository;
import acme.entities.claim.Claim;
import acme.entities.claim.ClaimStatus;
import acme.entities.leg.Leg;
import acme.entities.tracking_log.TrackingLog;
import acme.realms.assistanceAgents.AssistanceAgent;

@Repository
public interface AssistanceAgentClaimRepository extends AbstractRepository {

	@Query("select c from Claim c where c.assistanceAgent.id = :id")
	public Collection<Claim> findClaimsByMasterId(int id);

	@Query("select c from Claim c where c.id = :id")
	public Claim findClaimById(int id);

	@Query("SELECT l FROM Leg l WHERE l.draftMode = false AND l.flight.draftMode = false")
	public Collection<Leg> findAllLegs();

	@Query("select a from AssistanceAgent a where a.id = :id")
	public AssistanceAgent findAssitanceAgentById(int id);

	@Query("select t from TrackingLog t where t.claim.id = :id")
	public Collection<TrackingLog> findTrackingLogsByClaimId(int id);

	@Query("select count(t) = 0 from TrackingLog t where t.draftMode = true and t.claim.id = :id")
	public boolean allTrackingLogsPublishedByClaimId(int id);

	@Query("select c from Claim c where c.assistanceAgent.id = :id and c.indicator != :indicator")
	public Collection<Claim> findClaimsCompletedByMasterId(int id, ClaimStatus indicator);

	@Query("select c from Claim c where c.assistanceAgent.id = :id and c.indicator = :indicator")
	public Collection<Claim> findClaimsPendingByMasterId(int id, ClaimStatus indicator);

	@Query("select max(t.resolutionPercentage) from TrackingLog t where t.claim.id = :claimId")
	public Double findMaxResolutionPercentageByClaimId(int claimId);

	@Query("select l from Leg l where l.id = :legId")
	public Leg findLegById(int legId);
}
