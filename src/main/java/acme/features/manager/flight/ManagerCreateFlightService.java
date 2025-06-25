
package acme.features.manager.flight;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import acme.client.components.models.Dataset;
import acme.client.services.AbstractGuiService;
import acme.client.services.GuiService;
import acme.entities.flight.Flight;
import acme.realms.manager.Manager;

@GuiService
public class ManagerCreateFlightService extends AbstractGuiService<Manager, Flight> {

	@Autowired
	private ManagerFlightRepository repository;


	@Override
	public void authorise() {
		String method = super.getRequest().getMethod();
		boolean status = true;
		if (method.equals("POST")) {
			int id = super.getRequest().getData("id", int.class);
			status = id == 0;

		}

		super.getResponse().setAuthorised(status);
	}

	@Override
	public void validate(final Flight flight) {
		if (flight.getCost() != null) {
			boolean validCurrency = ManagerCreateFlightService.isValidCurrency(flight.getCost().getCurrency());
			super.state(validCurrency, "cost", "acme.validation.currency.message");
		}
	}

	@Override
	public void load() {
		Flight flight;
		Manager manager;

		manager = (Manager) super.getRequest().getPrincipal().getActiveRealm();

		flight = new Flight();
		flight.setDraftMode(true);
		flight.setManager(manager);

		super.getBuffer().addData(flight);
	}

	@Override
	public void bind(final Flight flight) {

		super.bindObject(flight, "tag", "requiresSelfTransfer", "cost", "description");

	}

	@Override
	public void perform(final Flight flight) {
		this.repository.save(flight);
	}

	@Override
	public void unbind(final Flight flight) {
		Dataset dataset;

		dataset = super.unbindObject(flight, "tag", "requiresSelfTransfer", "cost", "description");

		super.getResponse().addData(dataset);
	}

	public static boolean isValidCurrency(final String currency) {
		List<String> currencies = Arrays.asList("USD", "EUR", "JPY", "GBP", "CHF", "CAD", "AUD", "CNY", "MXN", "BRL", "RUB", "INR", "KRW", "ZAR", "SAR", "ARS", "COP", "CLP", "TRY", "EGP");
		return currencies.contains(currency.toUpperCase());
	}

}
