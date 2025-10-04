/*
 * AuthenticatedFlightCrewMemberCreateService.java
 *
 * Copyright (C) 2012-2025 Rafael Corchuelo.
 *
 * In keeping with the traditional purpose of furthering education and research, it is
 * the policy of the copyright owner to permit non-commercial use and redistribution of
 * this software. It has been tested carefully, but it is not guaranteed for any particular
 * purposes. The copyright owner does not offer any warranties or representations, nor do
 * they accept any liabilities with respect to them.
 */

package acme.features.authenticated.crewMember;

import java.util.Currency;

import org.springframework.beans.factory.annotation.Autowired;

import acme.client.components.models.Dataset;
import acme.client.components.principals.Authenticated;
import acme.client.components.principals.UserAccount;
import acme.client.helpers.PrincipalHelper;
import acme.client.services.AbstractGuiService;
import acme.client.services.GuiService;
import acme.entities.airline.Airline;
import acme.realms.flight_crew_member.AvailabilityStatus;
import acme.realms.flight_crew_member.FlightCrewMember;

@GuiService
public class AuthenticatedFlightCrewMemberCreateService extends AbstractGuiService<Authenticated, FlightCrewMember> {

	// Internal state ---------------------------------------------------------

	@Autowired
	private AuthenticatedFlightCrewMemberRepository repository;

	// AbstractService<Authenticated, FlightCrewMember> -----------------------


	@Override
	public void authorise() {
		boolean status;

		// Solo usuarios autenticados sin realm de FlightCrewMember pueden crear
		status = !super.getRequest().getPrincipal().hasRealmOfType(FlightCrewMember.class);

		super.getResponse().setAuthorised(status);
	}

	@Override
	public void load() {
		FlightCrewMember object;
		int userAccountId;
		UserAccount userAccount;

		userAccountId = super.getRequest().getPrincipal().getAccountId();
		userAccount = this.repository.findUserAccountById(userAccountId);

		object = new FlightCrewMember();
		object.setUserAccount(userAccount);
		object.setAvailabilityStatus(AvailabilityStatus.AVAILABLE); // por defecto disponible

		super.getBuffer().addData(object);
	}

	@Override
	public void bind(final FlightCrewMember object) {
		assert object != null;

		// Campos del formulario
		super.bindObject(object, "employeeCode", "phoneNumber", "languageSkills", "salary", "yearsOfExperience", "availabilityStatus");

		// Recuperamos el Airline del combo
		int airlineId = super.getRequest().getData("airlineId", int.class);
		Airline airline = this.repository.findAirlineById(airlineId);
		object.setWorkingFor(airline);
	}

	@Override
	public void validate(final FlightCrewMember object) {
		assert object != null;

		// Código de empleado duplicado
		boolean duplicateCode = this.repository.existsByEmployeeCode(object.getEmployeeCode());
		super.state(!duplicateCode, "employeeCode", "authenticated.flight-crew-member.error.duplicate-code");

		// Airline obligatorio
		super.state(object.getWorkingFor() != null, "airlineId", "authenticated.flight-crew-member.error.null-airline");

		// Currency del salario válida
		if (object.getSalary() != null)
			try {
				Currency.getInstance(object.getSalary().getCurrency());
			} catch (IllegalArgumentException ex) {
				super.state(false, "salary", "administrator.service.error.invalid-currency");
			}
	}

	@Override
	public void perform(final FlightCrewMember object) {
		assert object != null;

		this.repository.save(object);
	}

	@Override
	public void unbind(final FlightCrewMember object) {
		assert object != null;

		Dataset dataset = super.unbindObject(object, "employeeCode", "phoneNumber", "languageSkills", "salary", "yearsOfExperience", "availabilityStatus");

		// Para combos de airline y availability
		var airlines = this.repository.findAllAirlines();
		var choicesAirlines = acme.client.components.views.SelectChoices.from(airlines, "iata", object.getWorkingFor());
		var choicesStatus = acme.client.components.views.SelectChoices.from(AvailabilityStatus.class, object.getAvailabilityStatus());

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
