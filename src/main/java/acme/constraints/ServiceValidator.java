
package acme.constraints;

import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Autowired;

import acme.client.components.validation.AbstractValidator;
import acme.client.components.validation.Validator;
import acme.client.helpers.MomentHelper;
import acme.components.ServiceRepository;
import acme.entities.service.Service;

@Validator
public class ServiceValidator extends AbstractValidator<ValidService, Service> {

	@Autowired
	private ServiceRepository repository;


	@Override
	protected void initialise(final ValidService annotation) {
		assert annotation != null;
	}

	@Override
	public boolean isValid(final Service service, final ConstraintValidatorContext context) {
		assert context != null;

		boolean result;

		if (service == null || service.getPromotionCode() == null || service.getPromotionCode().length() < 2)
			super.state(context, false, "null", "javax.validation.constraints.NotNull.message");
		else {
			String promotionCode = service.getPromotionCode();
			String codeYear = promotionCode.substring(promotionCode.length() - 2);
			String currentDate = MomentHelper.getCurrentMoment().toString();
			String actualYear = currentDate.substring(currentDate.length() - 2);
			boolean uniqueService;
			Service existingservice = this.repository.findServiceByPromoCode(service.getPromotionCode());
			uniqueService = existingservice == null || existingservice.equals(service);

			super.state(context, uniqueService, "promotionalCode", "acme.validation.service.duplicated-promocode.message");
			{
				if (!codeYear.equals(actualYear))
					super.state(context, false, "promotionCode", "acme.validation.service.invalid-promotionCode-year.message");

			}
		}

		result = !super.hasErrors(context);

		return result;
	}

}
