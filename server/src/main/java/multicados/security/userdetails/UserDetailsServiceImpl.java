/**
 * 
 */
package multicados.security.userdetails;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.persistence.LockModeType;
import javax.persistence.Tuple;

import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import multicados.domain.entity.Role;
import multicados.domain.entity.entities.User;
import multicados.domain.entity.entities.User_;
import multicados.internal.domain.repository.GenericRepository;
import multicados.internal.domain.repository.Selector;
import multicados.internal.security.DomainUserDetails;

/**
 * @author Ngoc Huy
 *
 */
//@Component
public class UserDetailsServiceImpl implements UserDetailsService {

	/**
	 * 
	 */
	private static final String USER_NOT_FOUND_TEMPLATE = "User %s not found";
	private final GenericRepository repository;
	private final SessionFactoryImplementor sessionFactory;
	// @formatter:off
	private static final Selector<User, Tuple> SELECTOR = (root, query, builder) -> List.of(
			root.get(User_.password).alias(User_.PASSWORD),
			root.get(User_.active).alias(User_.ACTIVE),
			root.get(User_.credentialVersion).alias(User_.CREDENTIAL_VERSION),
			root.get(User_.role).alias(User_.ROLE),
			root.get(User_.locked).alias(User_.LOCKED));
	// @formatter:on
	@Autowired
	public UserDetailsServiceImpl(SessionFactory sessionFactory, GenericRepository genericRepository) {
		this.sessionFactory = sessionFactory.unwrap(SessionFactoryImplementor.class);
		repository = genericRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		try {
			StatelessSession session = sessionFactory.openStatelessSession();
			Optional<Tuple> optionalUser = repository.findById(User.class, username, SELECTOR,
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
					List.of(new SimpleGrantedAuthority(tuple.get(User_.ROLE, Role.class).name())));
			// @formatter:on
		} catch (Exception any) {
			any.printStackTrace();
			throw new IllegalStateException(any);
		}
	}

	public static class DomainUser extends org.springframework.security.core.userdetails.User
			implements DomainUserDetails {

		private static final long serialVersionUID = 1L;

		private final LocalDateTime version;

		// @formatter:off
		public DomainUser(
				String username,
				String password,
				boolean enabled,
				boolean accountNonLocked,
				LocalDateTime version,
				List<? extends GrantedAuthority> authorities) {
			super(username, password, enabled, true, true, accountNonLocked,
					authorities);
			this.version = version;
		}
		// @formatter:on
		@Override
		public LocalDateTime getVersion() {
			return version;
		}

		@Override
		public GrantedAuthority getCRUDAuthority() {
			return getAuthorities().stream().findFirst().orElse(null);
		}

	}

}
