
package acme.entities.flight;

import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.Valid;

import acme.client.components.basis.AbstractEntity;
import acme.client.components.datatypes.Money;
import acme.client.components.mappings.Automapped;
import acme.client.components.validation.Mandatory;
import acme.client.components.validation.Optional;
import acme.client.components.validation.ValidMoney;
import acme.client.components.validation.ValidString;
import acme.client.helpers.SpringHelper;
import acme.entities.leg.Leg;
import acme.realms.manager.Manager;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(indexes = {
	@Index(columnList = "draftMode")
})
public class Flight extends AbstractEntity {

	private static final long	serialVersionUID	= 1L;

	@Mandatory
	@ValidString(min = 1, max = 50)
	@Automapped
	private String				tag;

	@Mandatory
	@Automapped
	private boolean				requiresSelfTransfer;

	@Mandatory
	@ValidMoney(min = 0, max = 200000)
	@Automapped
	private Money				cost;

	@Optional
	@ValidString(min = 0, max = 255)
	@Automapped
	private String				description;

	@Mandatory
	@Automapped
	private boolean				draftMode;

	// Relationships ----------------------------------------------------------

	@Mandatory
	@Valid
	@ManyToOne(optional = false)
	private Manager				manager;

	//--------------------------------------------------------------------------


	@Transient
	public Leg getFirstLeg() {
		FlightRepository repository = SpringHelper.getBean(FlightRepository.class);
		List<Leg> legs = repository.findLegsByFlightIdOrderByScheduledDepartureAsc(this.getId());
		return legs.isEmpty() ? null : legs.get(0);
	}

	@Transient
	public Leg getLastLeg() {
		FlightRepository repository = SpringHelper.getBean(FlightRepository.class);
		List<Leg> legs = repository.findLegsByFlightIdOrderByScheduledArrivalDesc(this.getId());
		return legs.isEmpty() ? null : legs.get(0);
	}

	@Transient
	public Date getScheduledDeparture() {
		Leg firstLeg = this.getFirstLeg();
		return firstLeg != null ? firstLeg.getScheduledDeparture() : null;
	}

	@Transient
	public Date getScheduledArrival() {
		Leg lastLeg = this.getLastLeg();
		return lastLeg != null ? lastLeg.getScheduledArrival() : null;
	}

	@Transient
	public String getOriginCity() {
		Leg firstLeg = this.getFirstLeg();
		return firstLeg != null ? firstLeg.getDepartureAirport().getCity() : null;
	}

	@Transient
	public String getDestinationCity() {
		Leg lastLeg = this.getLastLeg();
		return lastLeg != null ? lastLeg.getArrivalAirport().getCity() : null;
	}

	@Transient
	public Integer getLayovers() {
		FlightRepository repository = SpringHelper.getBean(FlightRepository.class);
		int totalLegs = repository.getNumbersOfLegsByFlightId(this.getId()) - 1;
		if (totalLegs == -1)
			totalLegs = 0;
		return totalLegs;
	}

	@Transient
	public String getFlightRoute() {
		return this.getOriginCity() + " " + this.getScheduledDeparture() + " - " + this.getDestinationCity() + " " + this.getScheduledArrival();
	}

	public String getFlightNumber() {
		return this.tag;
	}
}
