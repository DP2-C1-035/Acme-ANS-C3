
package acme.features.assistanceAgent.trackingLog;

import java.util.Collection;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import acme.client.repositories.AbstractRepository;
import acme.entities.claim.Claim;
import acme.entities.tracking_log.TrackingLog;
import acme.entities.tracking_log.TrackingLogIndicator;

@Repository
public interface AssistanceAgentTrackingLogRepository extends AbstractRepository {

	@Query("select t from TrackingLog t where t.claim.id = :id")
	public Collection<TrackingLog> findTrackingLogsByClaimId(int id);

	@Query("select t from TrackingLog t where t.claim.id = :id and t.indicator != :indicator and t.draftMode = false")
	public Collection<TrackingLog> findTrackingLogsByClaimIdAndIndicator(int id, TrackingLogIndicator indicator);

	@Query("select c from Claim c where c.id = :id")
	public Claim findClaimById(int id);

	@Query("select t from TrackingLog t where t.id = :id")
	public TrackingLog findTrackingLogById(int id);

	@Query("select max(t.resolutionPercentage) from TrackingLog t where t.claim.id = :claimId and t.id != :id and t.draftMode = false")
	public Double findMaxResolutionPercentageByClaimId(int id, int claimId);

	@Query("SELECT COUNT(t) FROM TrackingLog t WHERE t.claim.id = :claimId AND t.resolutionPercentage = 100.00 AND t.draftMode = false")
	public Long countTrackingLogsForException(int claimId);

	@Query("SELECT CASE WHEN COUNT(t) = 0 THEN false WHEN COUNT(t) = COUNT(CASE WHEN t.draftMode = TRUE THEN 1 ELSE NULL END) THEN true ELSE false END FROM TrackingLog t WHERE t.claim.id = :claimId")
	public Boolean allTrackingLogsDraftMode(int claimId);

}
