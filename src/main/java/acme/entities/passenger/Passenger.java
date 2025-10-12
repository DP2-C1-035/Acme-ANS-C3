
package acme.entities.passenger;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;

import acme.client.components.basis.AbstractEntity;
import acme.client.components.mappings.Automapped;
import acme.client.components.validation.Mandatory;
import acme.client.components.validation.Optional;
import acme.client.components.validation.ValidMoment;
import acme.client.components.validation.ValidString;
import acme.constraints.ValidPassenger;
import acme.realms.customer.Customer;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@ValidPassenger
@Table(indexes = {
	@Index(columnList = "customer_id, draftMode")
})
public class Passenger extends AbstractEntity {

	private static final long	serialVersionUID	= 1L;

	@Mandatory
	@ValidString(max = 255)
	@Automapped
	private String				fullName;

	@Mandatory
	@Email(message = "Invalid email format")
	@Automapped
	private String				email;

	@Mandatory
	@Pattern(regexp = "^[A-Z0-9]{6,9}$", message = "Passport number must be between 6 and 9 characters long with uppercase letters and digits")
	@Automapped
	private String				passportNumber;

	@Mandatory
	@ValidMoment(past = true)
	@Temporal(TemporalType.DATE)
	@Automapped
	private Date				dateOfBirth;

	@Optional
	@ValidString(max = 50)
	@Automapped
	private String				specialNeeds;  // Optional field for special needs

	@Mandatory
	@Automapped
	private boolean				draftMode;

	@Mandatory
	@Valid
	@ManyToOne(optional = false)
	private Customer			customer;
}
