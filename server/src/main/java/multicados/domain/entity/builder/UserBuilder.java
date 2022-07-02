/**
 *
 */
package multicados.domain.entity.builder;

import static java.util.Optional.ofNullable;
import static multicados.internal.helper.StringHelper.normalizeString;

import java.time.LocalDateTime;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import multicados.domain.entity.Gender;
import multicados.domain.entity.entities.User;
import multicados.internal.config.Settings;
import multicados.internal.domain.annotation.For;
import multicados.internal.domain.builder.AbstractDomainResourceBuilder;
import multicados.internal.helper.SpringHelper;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.Utils.HandledFunction;

/**
 * @author Ngoc Huy
 *
 */
@For(User.class)
public class UserBuilder extends AbstractDomainResourceBuilder<User> {

	private static final Logger logger = LoggerFactory.getLogger(UserBuilder.class);

	private static final String UNKNOWN_USER_LASTNAME = "Avocado";
	private static final String UNKNOWN_USER_FIRSTNAME = "A Lovely";

	private final PasswordEncoder passwordEncoder;
	private final String defaultUserPhotoFilename;

	public UserBuilder(Environment env, PasswordEncoder passwordEncoder) throws Exception {
		this.passwordEncoder = passwordEncoder;
		defaultUserPhotoFilename = SpringHelper.getOrThrow(env, Settings.USER_DEFAULT_PHOTO_FILENAME,
				HandledFunction.identity(), () -> new IllegalArgumentException("Unable to locate setting %s"));

		if (!logger.isDebugEnabled()) {
			return;
		}

		logger.debug(String.format("Using %s as default User photo", defaultUserPhotoFilename));
	}

	private User mandatoryBuild(User model, User persistence) {
		// we assumes identifier will always be set before
		persistence.setEmail(ofNullable(model.getEmail()).map(email -> email.trim()).orElse(null));
		persistence.setPhone(normalizeString(model.getPhone()));
		persistence.setAddress(normalizeString(model.getAddress()));
		persistence.setLastName(ofNullable(normalizeString(model.getLastName())).orElse(UNKNOWN_USER_LASTNAME));
		persistence.setFirstName(ofNullable(normalizeString(model.getFirstName())).orElse(UNKNOWN_USER_FIRSTNAME));
		persistence.setGender(ofNullable(model.getGender()).orElse(Gender.UNKNOWN));
		persistence.setPhoto(StringHelper.hasLength(model.getPhoto()) ? model.getPhoto() : defaultUserPhotoFilename);

		return persistence;
	}

	@Override
	public User buildInsertion(User resource, EntityManager entityManager) throws Exception {
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
	public User buildUpdate(User model, User persistence, EntityManager entityManger) {
		return mandatoryBuild(model, persistence);
	}

}
