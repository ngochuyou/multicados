/**
 * 
 */
package multicados.security.userdetails;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.persistence.LockModeType;
import javax.persistence.Tuple;

import org.hibernate.StatelessSession;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import multicados.domain.entity.entities.User;
import multicados.domain.entity.entities.User_;
import multicados.internal.context.ContextManager;
import multicados.internal.domain.repository.GenericRepository;
import multicados.internal.domain.repository.Selector;
import multicados.internal.helper.Utils.LazySupplier;

/**
 * @author Ngoc Huy
 *
 */
public class UserDetailsServiceImpl implements UserDetailsService {

	/**
	 * 
	 */
	private static final String USER_NOT_FOUND_TEMPLATE = "User %s not found";
	private final LazySupplier<GenericRepository> repositoryLoader;
	private final SessionFactoryImplementor sfi;
	// @formatter:off
	private static final Selector<User, Tuple> SELECTOR = (root, query, builder) -> List.of(
			root.get(User_.password).alias(User_.PASSWORD),
			root.get(User_.active).alias(User_.ACTIVE),
			root.get(User_.credentialVersion).alias(User_.CREDENTIAL_VERSION),
			root.get(User_.locked).alias(User_.LOCKED));
	// @formatter:on
	public UserDetailsServiceImpl(SessionFactoryImplementor sfi) {
		this.sfi = sfi;
		repositoryLoader = new LazySupplier<>(() -> ContextManager.getBean(GenericRepository.class));
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		try {
			StatelessSession session = sfi.openStatelessSession();
			Optional<Tuple> optionalUser = repositoryLoader.get().findById(User.class, username, SELECTOR,
					LockModeType.PESSIMISTIC_WRITE, session);

			if (optionalUser.isEmpty()) {
				throw new UsernameNotFoundException(String.format(USER_NOT_FOUND_TEMPLATE, username));
			}

			Tuple tuple = optionalUser.get();
			// @formatter:off
			return new org.springframework.security.core.userdetails.User(
					username,
					tuple.get(User_.PASSWORD, String.class),
					tuple.get(User_.ACTIVE, Boolean.class).equals(Boolean.TRUE),
					true,
					tuple.get(User_.CREDENTIAL_VERSION, LocalDateTime.class).isAfter(LocalDateTime.now()),
					tuple.get(User_.LOCKED, Boolean.class).equals(Boolean.TRUE),
					Collections.emptyList());
			// @formatter:on
		} catch (Exception any) {
			any.printStackTrace();
			throw new IllegalStateException(any);
		}

	}

}
