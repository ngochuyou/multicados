/**
 * 
 */
package multicados.security.userdetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.persistence.LockModeType;
import javax.persistence.Tuple;

import org.hibernate.StatelessSession;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import multicados.domain.entity.entities.User;
import multicados.domain.entity.entities.User_;
import multicados.internal.context.ContextManager;
import multicados.internal.domain.repository.GenericRepository;
import multicados.internal.domain.repository.Selector;
import multicados.internal.helper.Utils.LazySupplier;
import multicados.internal.security.DomainUserDetails;

/**
 * @author Ngoc Huy
 *
 */
public class UserDetailsServiceImpl implements UserDetailsService {

	/**
	 * 
	 */
	private static final String USER_NOT_FOUND_TEMPLATE = "User %s not found";
	private final LazySupplier<GenericRepository> repositorySupplier;
	private final LazySupplier<SessionFactoryImplementor> sfiSupplier;
	// @formatter:off
	private static final Selector<User, Tuple> SELECTOR = (root, query, builder) -> List.of(
			root.get(User_.password).alias(User_.PASSWORD),
			root.get(User_.active).alias(User_.ACTIVE),
			root.get(User_.credentialVersion).alias(User_.CREDENTIAL_VERSION),
			root.get(User_.locked).alias(User_.LOCKED));
	// @formatter:on
	public UserDetailsServiceImpl() {
		sfiSupplier = new LazySupplier<>(() -> ContextManager.getBean(SessionFactoryImplementor.class));
		repositorySupplier = new LazySupplier<>(() -> ContextManager.getBean(GenericRepository.class));
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		try {
			StatelessSession session = sfiSupplier.get().openStatelessSession();
			Optional<Tuple> optionalUser = repositorySupplier.get().findById(User.class, username, SELECTOR,
					LockModeType.PESSIMISTIC_WRITE, session);

			if (optionalUser.isEmpty()) {
				throw new UsernameNotFoundException(String.format(USER_NOT_FOUND_TEMPLATE, username));
			}

			Tuple tuple = optionalUser.get();
			// @formatter:off
			return new DomainUser(
					username,
					tuple.get(User_.PASSWORD, String.class),
					tuple.get(User_.ACTIVE, Boolean.class),
					!tuple.get(User_.LOCKED, Boolean.class),
					tuple.get(User_.CREDENTIAL_VERSION, LocalDateTime.class),
					Collections.emptyList());
			// @formatter:on
		} catch (Exception any) {
			any.printStackTrace();
			throw new IllegalStateException(any);
		}
	}

	public class DomainUser extends org.springframework.security.core.userdetails.User implements DomainUserDetails {

		private static final long serialVersionUID = 1L;

		private LocalDateTime version;

		// @formatter:off
		public DomainUser(
				String username,
				String password,
				boolean enabled,
				boolean accountNonLocked,
				LocalDateTime version,
				Collection<? extends GrantedAuthority> authorities) {
			super(username, password, enabled, true, true, accountNonLocked,
					authorities);
			this.version = version;
		}
		// @formatter:on
		@Override
		public LocalDateTime getVersion() {
			return version;
		}

	}

}
