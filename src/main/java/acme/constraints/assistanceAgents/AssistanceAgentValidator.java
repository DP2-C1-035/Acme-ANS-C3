
package acme.constraints.assistanceAgents;

import java.util.regex.Pattern;

import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Autowired;

import acme.client.components.validation.AbstractValidator;
import acme.client.components.validation.Validator;
import acme.realms.assistanceAgents.AssistanceAgent;
import acme.realms.assistanceAgents.AssistanceAgentRepository;

@Validator
public class AssistanceAgentValidator extends AbstractValidator<ValidAssistanceAgent, AssistanceAgent> {

	// Internal state ---------------------------------------------------------

	@Autowired
	private AssistanceAgentRepository repository;

	// ConstraintValidator interface ------------------------------------------


	@Override
	protected void initialise(final ValidAssistanceAgent annotation) {
		assert annotation != null;
	}

	@Override
	public boolean isValid(final AssistanceAgent assistanceAgent, final ConstraintValidatorContext context) {
		assert context != null;

		boolean result;

		if (assistanceAgent == null || assistanceAgent.getEmployeeCode() == null || assistanceAgent.getIdentity().getFullName() == null)
			super.state(context, false, "*", "javax.validation.constraints.NotNull.message");
		else {
			{
				boolean isEmployeeCodePatternCorrect;

				isEmployeeCodePatternCorrect = assistanceAgent.getEmployeeCode() != null && Pattern.matches("^[A-Z]{2,3}\\d{6}$", assistanceAgent.getEmployeeCode());

				super.state(context, isEmployeeCodePatternCorrect, "codePattern", "acme.validation.assistanceAgent.invalid-employee-code-pattern.message");

			}
			{
				String name;
				String surname;
				String employeeCode;
				boolean isEmployeeCodeCorrect;

				name = assistanceAgent.getIdentity().getName();
				surname = assistanceAgent.getIdentity().getSurname();
				employeeCode = assistanceAgent.getEmployeeCode();

				isEmployeeCodeCorrect = name.charAt(0) == employeeCode.charAt(0) && surname.charAt(0) == employeeCode.charAt(1);
				super.state(context, isEmployeeCodeCorrect, "employeeCode", "acme.validation.assistanceAgent.invalid-employee-code.message");
			}
			{
				AssistanceAgent existingAssistanceAgent;
				boolean isAssistanceAgentUnique;

				existingAssistanceAgent = this.repository.findAssitanceAgentByEmployeeCode(assistanceAgent.getEmployeeCode());
				isAssistanceAgentUnique = existingAssistanceAgent == null || assistanceAgent.getEmployeeCode().isBlank() || existingAssistanceAgent.equals(assistanceAgent);
				super.state(context, isAssistanceAgentUnique, "uniqueAssistanceAgent", "acme.validation.assistanceAgent.unique-assistance-agent.message");

			}
		}

		result = !super.hasErrors(context);

		return result;
	}

}
