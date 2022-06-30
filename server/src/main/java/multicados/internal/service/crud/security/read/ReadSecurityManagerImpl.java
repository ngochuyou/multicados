/**
 *
 */
package multicados.internal.service.crud.security.read;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.security.core.GrantedAuthority;

import multicados.internal.config.Settings;
import multicados.internal.context.ContextBuilder;
import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.DomainResourceGraphCollectors;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.TypeHelper;
import multicados.internal.helper.Utils;
import multicados.internal.security.CredentialException;
import multicados.internal.service.crud.security.SecuredAttribute;
import multicados.internal.service.crud.security.UnauthorizedCredentialException;

/**
 * @author Ngoc Huy
 *
 */
public class ReadSecurityManagerImpl extends ContextBuilder.AbstractContextBuilder implements ReadSecurityManager {

	private static final Logger logger = LoggerFactory.getLogger(ReadSecurityManagerImpl.class);

	@SuppressWarnings("rawtypes")
	private final Map<Class<? extends DomainResource>, ReadSecurityNode> securityNodes;

	@Autowired
	public ReadSecurityManagerImpl(Environment env, DomainResourceContext resourceContext)
			throws IllegalAccessException, Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Instantiating {}", ReadSecurityManagerImpl.class.getName());
		}

		final ReadFailureExceptionHandler failureHandler = resolveFailureHandler(env);
		// @formatter:off
		securityNodes = Utils
			.declare(scanForContributors())
				.second(new CRUDSecurityManagerBuilderImpl(resourceContext))
			.then(this::doContribute)
				.second(resourceContext)
				.third(failureHandler)
			.then(this::constructConfiguredNodes)
				.second(resourceContext)
				.third(failureHandler)
			.then(this::constructEmptyNodes)
			.then(Collections::unmodifiableMap)
			.get();
		// @formatter:on
	}

	@Override
	public <D extends DomainResource> Map<String, String> translate(Class<D> resourceType,
			Collection<String> attributes) {
		@SuppressWarnings("unchecked")
		ReadSecurityNode<D> readSecurityNode = securityNodes.get(resourceType);

		return readSecurityNode.translate(attributes);
	}

	@Override
	public <D extends DomainResource> List<String> check(Class<D> resourceType, Collection<String> requestedAttributes,
			GrantedAuthority credential) throws CredentialException, UnknownAttributesException {
		@SuppressWarnings("unchecked")
		ReadSecurityNode<D> readSecurityNode = securityNodes.get(resourceType);

		return readSecurityNode.check(requestedAttributes, credential);
	}

	private boolean shouldConstructNode(Class<? extends DomainResource> resourceType) {
		return !Modifier.isInterface(resourceType.getModifiers());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map<Class<? extends DomainResource>, ReadSecurityNode> constructConfiguredNodes(
			CRUDSecurityManagerBuilder builder, DomainResourceContext resourceContext,
			ReadFailureExceptionHandler exceptionThrower) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Constructing contributed nodes");
		}

		final Map<Class<? extends DomainResource>, ReadSecurityNode> securityNodes = new HashMap<>();
		final Set<SecuredAttribute> contributedAttributes = builder.getContributedAttributes();

		for (final Class<? extends DomainResource> resourceType : resourceContext.getResourceGraph()
				.collect(DomainResourceGraphCollectors.toTypesSet())) {
			if (!shouldConstructNode(resourceType)) {
				continue;
			}
			// @formatter:off
			final List<SecuredAttribute> scopedAttributes = contributedAttributes
				.stream()
				.filter(attribute -> TypeHelper.isParentOf(attribute.getOwningType(), resourceType))
				.collect(Collectors.toList());

			if (scopedAttributes.isEmpty()) {
				continue;
			}

			securityNodes.put(
					resourceType,
					new ReadSecurityNodeImpl(
							resourceType,
							resourceContext,
							scopedAttributes,
							exceptionThrower));
			// @formatter:on
		}

		return securityNodes;
	}

	@SuppressWarnings({ "rawtypes" })
	private Map<Class<? extends DomainResource>, ReadSecurityNode> constructEmptyNodes(
			Map<Class<? extends DomainResource>, ReadSecurityNode> configuredNodes,
			DomainResourceContext resourceContext, ReadFailureExceptionHandler exceptionThrower) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Constructing empty nodes");
		}

		for (final Class<? extends DomainResource> resourceType : resourceContext.getResourceGraph()
				.collect(DomainResourceGraphCollectors.toTypesSet())) {
			if (configuredNodes.containsKey(resourceType) || !shouldConstructNode(resourceType)) {
				continue;
			}
			// @formatter:off
			configuredNodes.put(
					resourceType,
					new DefaultReadSecurityNode<>(
							resourceContext.getMetadata(resourceType),
							exceptionThrower));
			// @formatter:on
		}

		return configuredNodes;
	}

	private ReadFailureExceptionHandler resolveFailureHandler(Environment env) {
		if (logger.isTraceEnabled()) {
			logger.trace("Resolving {}", ReadFailureExceptionHandler.class.getName());
		}

		final String configuredStrategy = env.getProperty(Settings.READ_FAILURE_EXCEPTION_THROWING_STRATEGY);

		try {
			// @formatter:off
			final ExceptionHandlingStrategy strategy = Optional.ofNullable(configuredStrategy)
					.map(String::toUpperCase)
					.map(String::trim)
					.map(ExceptionHandlingStrategy::valueOf)
					.orElse(ExceptionHandlingStrategy.IGNORE);
			// @formatter:on
			if (strategy.equals(ExceptionHandlingStrategy.IGNORE)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Using {} for read failure", IgnoreStrategy.class.getName());
				}

				return new IgnoreStrategy();
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Using {} for read failure", ThrowExceptionStrategy.class.getName());
			}
			return new ThrowExceptionStrategy();
		} catch (IllegalArgumentException iae) {
			throw new IllegalArgumentException(
					String.format("Unknown read failure exception throwing strategy of [%s], expected one of %s",
							configuredStrategy, StringHelper.join(Arrays.asList(ExceptionHandlingStrategy.values()))));
		}
	}

	private CRUDSecurityManagerBuilder doContribute(List<ReadSecurityContributor> contributors,
			CRUDSecurityManagerBuilder builder) {
		if (logger.isTraceEnabled()) {
			logger.trace("Doing contributions");
		}

		contributors.stream().forEach(contributor -> contributor.contribute(builder));

		return builder;
	}

	private List<ReadSecurityContributor> scanForContributors() throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Scanning for {}", ReadSecurityContributor.class.getSimpleName());
		}

		final List<ReadSecurityContributor> contributors = new ArrayList<>();
		final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
				false);

		scanner.addIncludeFilter(new AssignableTypeFilter(ReadSecurityContributor.class));

		for (final BeanDefinition beanDef : scanner.findCandidateComponents(Settings.BASE_PACKAGE)) {
			try {
				@SuppressWarnings("unchecked")
				final Class<? extends ReadSecurityContributor> contributorClass = (Class<? extends ReadSecurityContributor>) Class
						.forName(beanDef.getBeanClassName());

				if (logger.isDebugEnabled()) {
					logger.debug("Found one {} of type [{}] in [{}]", ReadSecurityContributor.class.getSimpleName(),
							contributorClass.getName(), contributorClass.getPackageName());
				}

				contributors.add(contributorClass.getConstructor().newInstance());
			} catch (NoSuchMethodException nsm) {
				throw new IllegalArgumentException(String.format(
						"A non-arg constructor is required on a(n) %s instance, unable to find one in [%s]",
						ReadSecurityContributor.class.getSimpleName(), beanDef.getBeanClassName()));
			}
		}

		return contributors;
	}

	public interface CRUDSecurityManagerBuilder {

		<D extends DomainResource> WithType<D> type(Class<D> type);

		@SuppressWarnings("rawtypes")
		Set<SecuredAttribute> getContributedAttributes();

	}

	private class CRUDSecurityManagerBuilderImpl implements CRUDSecurityManagerBuilder {

		private static final Logger logger = LoggerFactory.getLogger(CRUDSecurityManagerBuilderImpl.class);

		@SuppressWarnings("rawtypes")
		private final Map<Key, SecuredAttributeImpl> securedAttributes = new HashMap<>();
		private final DomainResourceContext context;

		public CRUDSecurityManagerBuilderImpl(DomainResourceContext context) {
			this.context = context;
		}

		@SuppressWarnings({ "rawtypes" })
		@Override
		public Set<SecuredAttribute> getContributedAttributes() {
			return securedAttributes.values().stream().collect(Collectors.toSet());
		}

		@Override
		public <D extends DomainResource> WithType<D> type(Class<D> type) {
			return new WithTypeImpl<>(type);
		}

		private <D extends DomainResource> Key<D> makeKey(Class<D> type, GrantedAuthority credential, String name)
				throws CredentialException {
			return new Key<>(type, credential, name);
		}

		@SuppressWarnings("unchecked")
		private <D extends DomainResource> SecuredAttributeImpl<D> locateProperty(Key<D> key) {
			if (securedAttributes.containsKey(key)) {
				return securedAttributes.get(key);
			}

			return null;
		}

		private <D extends DomainResource> SecuredAttributeImpl<D> putProperty(Key<D> key,
				SecuredAttributeImpl<D> attr) {
			securedAttributes.put(key, attr);

			return attr;
		}

		private <D extends DomainResource> void setProperty(Class<D> owningType, GrantedAuthority credential,
				String name, String alias) throws CredentialException {
			Key<D> key = makeKey(owningType, credential, name);
			SecuredAttributeImpl<D> attr = locateProperty(key);

			if (attr != null) {
				modifyAlias(attr, alias);
				return;
			}

			putProperty(key, new SecuredAttributeImpl<>(owningType, credential, name).setAlias(alias));
		}

		private <D extends DomainResource> void setProperty(Class<D> owningType, GrantedAuthority credential,
				String name, Boolean isMasked) throws CredentialException {
			Key<D> key = makeKey(owningType, credential, name);
			SecuredAttributeImpl<D> attr = locateProperty(key);

			if (attr != null) {
				modifyVisibility(attr, isMasked);
				return;
			}

			putProperty(key, new SecuredAttributeImpl<>(owningType, credential, name).setMasked(isMasked));
		}

		@SuppressWarnings("rawtypes")
		private void modifyAlias(SecuredAttributeImpl attr, String alias) {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Overring alias [%s] with [%s]", attr.getAlias(), alias));
			}

			attr.setAlias(alias);
		}

		@SuppressWarnings("rawtypes")
		private void modifyVisibility(SecuredAttributeImpl attr, Boolean isMasked) {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Overring visibility [%s] with [%s]", attr.isMasked(), isMasked));
			}

			attr.setMasked(isMasked);
		}

		private class WithTypeImpl<D extends DomainResource> implements WithType<D> {

			private final Class<D> type;

			public WithTypeImpl(Class<D> type) {
				this.type = Objects.requireNonNull(type);

				if (logger.isTraceEnabled()) {
					logger.trace(String.format("With type %s", type.getSimpleName()));
				}
			}

			@Override
			public WithCredential<D> credentials(GrantedAuthority... credentials) {
				try {
					return new WithCredentialImpl(this, credentials);
				} catch (CredentialException any) {
					throw new IllegalArgumentException(any);
				}
			}

			private class WithCredentialImpl implements WithCredential<D> {

				private final Set<String> remainingFields = new HashSet<>(
						context.getMetadata(type).getAttributeNames());

				private final WithType<D> owningType;
				private final GrantedAuthority[] credentials;

				public WithCredentialImpl(WithType<D> owningType, GrantedAuthority... credentials)
						throws CredentialException {
					this.owningType = owningType;
					this.credentials = Stream.of(requireNonNull(credentials)).map(Objects::requireNonNull)
							.toArray(GrantedAuthority[]::new);

					List<String> credentialList = new ArrayList<>();

					for (GrantedAuthority credential : credentials) {
						credentialList.add(credential.getAuthority());
					}

					if (logger.isTraceEnabled()) {
						logger.trace(String.format("With Credentials[%s]",
								credentialList.stream().collect(Collectors.joining(","))));
					}
				}

				private void removeFromRemainingFields(String... fields) {
					remainingFields.removeAll(List.of(fields));
				}

				@Override
				public WithAttribute<D> attributes(String... attributes) {
					removeFromRemainingFields(attributes);
					return new WithAttributeImpl(this, attributes);
				}

				@Override
				public WithAttribute<D> but(String... attributes) {
					Set<String> excludedFields = new HashSet<>(List.of(attributes));

					return attributes(remainingFields.stream().filter(field -> !excludedFields.contains(field))
							.toArray(String[]::new));
				}

				@Override
				public WithCredential<D> credentials(GrantedAuthority... credentials) {
					return owningType.credentials(credentials);
				}

				@Override
				public WithCredential<D> mask() {
					for (GrantedAuthority crudCredential : credentials) {
						for (String attribute : context.getMetadata(type).getAttributeNames()) {
							try {
								setProperty(type, crudCredential, attribute, Boolean.TRUE);
							} catch (CredentialException any) {
								throw new IllegalArgumentException(any);
							}
						}
					}
					
					if (logger.isTraceEnabled()) {
						logger.trace("Mask all");
					}

					return this;
				}

				@Override
				public WithCredential<D> publish() {
					for (GrantedAuthority crudCredential : credentials) {
						for (String attribute : context.getMetadata(type).getAttributeNames()) {
							try {
								setProperty(type, crudCredential, attribute, Boolean.FALSE);
							} catch (CredentialException any) {
								throw new IllegalArgumentException(any);
							}
						}
					}

					if (logger.isTraceEnabled()) {
						logger.trace("Publish all");
					}

					return this;
				}

				@Override
				public <E extends DomainResource> WithType<E> type(Class<E> type) {
					return CRUDSecurityManagerBuilderImpl.this.type(type);
				}

				private class WithAttributeImpl implements WithAttribute<D> {

					private final WithCredential<D> owningCredentials;
					private final String[] attributes;

					public WithAttributeImpl(WithCredential<D> onwingCredentials, String... attributes) {
						this.owningCredentials = onwingCredentials;
						this.attributes = Stream.of(requireNonNull(attributes)).map(Objects::requireNonNull)
								.toArray(String[]::new);

						if (logger.isTraceEnabled()) {
							logger.trace(String.format("With fields %s",
									Stream.of(attributes).collect(Collectors.joining(", "))));
						}
					}

					@Override
					public WithAttribute<D> use(String... alias) {
						if (requireNonNull(alias).length != attributes.length) {
							throw new IllegalArgumentException(String.format(
									"Alias names length and field lengths must match. Alias[%d]><[%d]Attributes",
									alias.length, attributes.length));
						}

						int n = attributes.length;

						for (GrantedAuthority credential : credentials) {
							for (int i = 0; i < n; i++) {
								try {
									setProperty(type, credential, attributes[i], alias[i]);
								} catch (CredentialException any) {
									throw new IllegalArgumentException(any);
								}
							}
						}

						if (logger.isTraceEnabled()) {
							logger.trace(
									String.format("Using alias %s", Stream.of(alias).collect(Collectors.joining(","))));
						}

						return this;
					}

					private WithAttribute<D> make(Boolean isMasked) {
						int n = attributes.length;

						for (GrantedAuthority credential : credentials) {
							for (int i = 0; i < n; i++) {
								try {
									setProperty(type, credential, attributes[i], isMasked);
								} catch (CredentialException any) {
									throw new IllegalArgumentException(any);
								}
							}
						}

						if (logger.isTraceEnabled()) {
							logger.trace(isMasked ? "Mask" : "Publish");
						}

						return this;
					}

					@Override
					public WithAttribute<D> publish() {
						return make(Boolean.FALSE);
					}

					@Override
					public WithAttribute<D> mask() {
						return make(Boolean.TRUE);
					}

					@Override
					public WithAttribute<D> others() {
						return owningCredentials.attributes(remainingFields.toArray(String[]::new));
					}

					@Override
					public WithAttribute<D> attributes(String... attributes) {
						return owningCredentials.attributes(attributes);
					}

					@Override
					public WithCredential<D> credentials(GrantedAuthority... credentials) {
						return owningType.credentials(credentials);
					}

					@Override
					public <E extends DomainResource> WithType<E> type(Class<E> type) {
						return CRUDSecurityManagerBuilderImpl.this.type(type);
					}
				}
			}
		}
	}

	private class SecuredAttributeImpl<D extends DomainResource> implements SecuredAttribute<D> {

		private final Class<D> owningType;
		private final GrantedAuthority credential;
		private final String name;

		private Boolean masked;
		private String alias;

		public SecuredAttributeImpl(Class<D> owningType, GrantedAuthority credential, String name) {
			this.owningType = owningType;
			this.credential = credential;
			this.name = name;
		}

		@Override
		public Class<D> getOwningType() {
			return owningType;
		}

		@Override
		public GrantedAuthority getCredential() {
			return credential;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getAlias() {
			return alias;
		}

		@Override
		public boolean isMasked() {
			return Optional.ofNullable(masked).orElse(true);
		}

		public SecuredAttributeImpl<D> setMasked(Boolean masked) {
			this.masked = masked;
			return this;
		}

		public SecuredAttributeImpl<D> setAlias(String alias) {
			this.alias = alias;
			return this;
		}

	}

	private class Key<T extends DomainResource> {

		private final Class<T> type;
		private final GrantedAuthority credential;
		private final String name;

		private final int hashCode;

		public Key(Class<T> type, GrantedAuthority credential, String name) throws CredentialException {
			this.type = type;
			this.credential = credential;
			this.name = name;

			int hash = 17;

			hash += 37 * type.hashCode();
			hash += credential.getAuthority().hashCode();
			hash += 37 * name.hashCode();

			hashCode = hash;
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if ((obj == null) || (getClass() != obj.getClass()))
				return false;

			Key<?> other = (Key<?>) obj;

			try {
				return credential.getAuthority().equals(other.credential.getAuthority()) && name.equals(other.name)
						&& type.equals(other.type);
			} catch (Exception any) {
				any.printStackTrace();
				return false;
			}
		}

		@Override
		public String toString() {
			return "Key [type=" + type + ", credential=" + credential.getAuthority() + ", name=" + name + ", hashCode="
					+ hashCode + "]";
		}

	}

	private enum ExceptionHandlingStrategy {
		IGNORE, THROW_EXCEPTION
	}

	private abstract class AbstractReadFailureExceptionHandler implements ReadFailureExceptionHandler {

		@Override
		public void doOnUnauthorizedCredential(Class<?> resourceType, String credential) throws CredentialException {
			throw new UnauthorizedCredentialException(credential, resourceType.getName());
		}

	}

	private class IgnoreStrategy extends AbstractReadFailureExceptionHandler {

		private IgnoreStrategy() {}

		@Override
		public void doOnUnauthorizedAttribute(Class<?> resourceType, String credential,
				List<String> unauthorizedAttributeNames) throws UnknownAttributesException {}

	}

	private class ThrowExceptionStrategy extends AbstractReadFailureExceptionHandler {

		private ThrowExceptionStrategy() {}

		@Override
		public void doOnUnauthorizedAttribute(Class<?> resourceType, String credential,
				List<String> unauthorizedAttributeNames) throws UnknownAttributesException {
			throw new UnknownAttributesException(unauthorizedAttributeNames);
		}

	}

	@Override
	public void summary() {
		if (logger.isDebugEnabled()) {
			logger.debug("\n{}",
					securityNodes.values().stream().map(Object::toString).collect(Collectors.joining("\n\n")));
		}
	}

}
