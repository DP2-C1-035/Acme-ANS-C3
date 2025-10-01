
package acme.features.authenticated.crewMember;

import java.util.Collection;
import java.util.Currency;

import org.springframework.beans.factory.annotation.Autowired;

import acme.client.components.models.Dataset;
import acme.client.components.principals.Authenticated;
import acme.client.helpers.PrincipalHelper;
import acme.client.services.AbstractGuiService;
import acme.client.services.GuiService;
import acme.entities.airline.Airline;
import acme.realms.flight_crew_member.AvailabilityStatus;
import acme.realms.flight_crew_member.FlightCrewMember;

@GuiService
public class AuthenticatedFlightCrewMemberUpdateService extends AbstractGuiService<Authenticated, FlightCrewMember> {

	// Internal state ---------------------------------------------------------

	@Autowired
	private AuthenticatedFlightCrewMemberRepository repository;

	// AbstractService interface ----------------------------------------------


	@Override
	public void authorise() {
		// El usuario solo puede actualizar si ya tiene el rol FlightCrewMember
		boolean status = super.getRequest().getPrincipal().hasRealmOfType(FlightCrewMember.class);
		super.getResponse().setAuthorised(status);
	}

	@Override
	public void load() {
		int userAccountId = super.getRequest().getPrincipal().getAccountId();
		FlightCrewMember member = this.repository.findFlightCrewMemberByUserAccountId(userAccountId);
		super.getBuffer().addData(member);
	}

	@Override
	public void bind(final FlightCrewMember member) {
		assert member != null;

		super.bindObject(member, "employeeCode", "phoneNumber", "languageSkills", "salary", "yearsOfExperience", "availabilityStatus");

		// Airline seleccionado desde el formulario
		int airlineId = super.getRequest().getData("airlineId", int.class);
		Airline airline = this.repository.findAirlineById(airlineId);
		member.setWorkingFor(airline);
	}

	@Override
	public void validate(final FlightCrewMember member) {
		assert member != null;

		// Validar airline no nulo
		super.state(member.getWorkingFor() != null, "airlineId", "authenticated.flight-crew-member.error.null-airline");

		// Validar currency del salary
		if (member.getSalary() != null)
			try {
				Currency.getInstance(member.getSalary().getCurrency());
			} catch (IllegalArgumentException ex) {
				super.state(false, "salary", "administrator.service.error.invalid-currency");
			}
	}

	@Override
	public void perform(final FlightCrewMember member) {
		assert member != null;
		this.repository.save(member);
	}

	@Override
	public void unbind(final FlightCrewMember member) {
		assert member != null;

		Dataset dataset = super.unbindObject(member, "employeeCode", "phoneNumber", "languageSkills", "salary", "yearsOfExperience", "availabilityStatus");

		Collection<Airline> airlines = this.repository.findAllAirlines();
		var choicesAirlines = acme.client.components.views.SelectChoices.from(airlines, "iataCode", member.getWorkingFor());
		var choicesStatus = acme.client.components.views.SelectChoices.from(AvailabilityStatus.class, member.getAvailabilityStatus());

		dataset.put("airlines", choicesAirlines);
		dataset.put("airlineId", choicesAirlines.getSelected().getKey());
		dataset.put("availabilityStatuses", choicesStatus);

		super.getResponse().addData(dataset);
	}

	@Override
	public void onSuccess() {
		if (super.getRequest().getMethod().equals("POST"))
			PrincipalHelper.handleUpdate();
	}
}
