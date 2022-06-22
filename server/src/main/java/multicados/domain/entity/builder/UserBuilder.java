/**
 * 
 */
package multicados.domain.entity.builder;

import static java.util.Optional.ofNullable;
import static multicados.internal.helper.StringHelper.normalizeString;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.EntityManager;

import org.springframework.security.crypto.password.PasswordEncoder;

import multicados.domain.entity.Gender;
import multicados.domain.entity.entities.User;
import multicados.internal.domain.For;
import multicados.internal.domain.builder.AbstractDomainResourceBuilder;

/**
 * @author Ngoc Huy
 *
 */
@For(User.class)
public class UserBuilder extends AbstractDomainResourceBuilder<User> {

	private static final String UNKNOWN_USER_LASTNAME = "Avocado";
	private static final String UNKNOWN_USER_FIRSTNAME = "A Lovely";

	private final PasswordEncoder passwordEncoder;

	public UserBuilder(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}

	private User mandatoryBuild(User target, User model) {
		// we assumes identifier will always be set before
		target.setEmail(ofNullable(model.getEmail()).map(email -> email.trim()).orElse(null));
		target.setPhone(normalizeString(model.getPhone()));
		target.setAddress(normalizeString(model.getAddress()));
		target.setLastName(ofNullable(normalizeString(model.getLastName())).orElse(UNKNOWN_USER_LASTNAME));
		target.setFirstName(ofNullable(normalizeString(model.getFirstName())).orElse(UNKNOWN_USER_FIRSTNAME));
		target.setGender(ofNullable(model.getGender()).orElse(Gender.UNKNOWN));

		return target;
	}

	@Override
	public User buildInsertion(Serializable id, User resource, EntityManager entityManager) throws Exception {
		mandatoryBuild(resource, resource);

		resource.setCredentialVersion(LocalDateTime.now());
		resource.setLocked(Boolean.TRUE);
		resource.setPassword(resource.getPassword() == null && true/*
																	 * || model.getPassword().length() <
																	 * _User.MINIMUM_PASSWORD_LENGTH
																	 */
				? null
				: passwordEncoder.encode(resource.getPassword()));

		return resource;
	}

	@Override
	public User buildUpdate(Serializable id, User model, User resource, EntityManager entityManger) {
		return mandatoryBuild(resource, resource);
	}

}
