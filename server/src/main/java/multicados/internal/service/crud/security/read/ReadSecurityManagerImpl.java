/**
 * 
 */
package multicados.internal.service.crud.security.read;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AssignableTypeFilter;

import multicados.internal.config.Settings;
import multicados.internal.context.ContextManager;
import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.TypeHelper;
import multicados.internal.helper.Utils;
import multicados.internal.security.CredentialException;
import multicados.internal.service.crud.security.CRUDCredential;
import multicados.internal.service.crud.security.SecuredAttribute;
import multicados.internal.service.crud.security.UnauthorizedCredentialException;

/**
 * @author Ngoc Huy
 *
 */
public class ReadSecurityManagerImpl implements ReadSecurityManager {

	@SuppressWarnings("rawtypes")
	private final Map<Class<? extends DomainResource>, ReadSecurityNode> securityNodes;

	public ReadSecurityManagerImpl(DomainResourceContext resourceContext) throws Exception {
		ReadFailureExceptionHandler exceptionThrower = resolveFailureExceptionHandler();
		// @formatter:off
		securityNodes = Utils
			.declare(scanForContributors())
				.second(new CRUDSecurityManagerBuilderImpl(resourceContext))
			.then(this::doContribute)
				.second(resourceContext)
				.third(exceptionThrower)
			.then(this::constructConfiguredNodes)
				.second(resourceContext)
				.third(exceptionThrower)
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
			CRUDCredential credential) throws CredentialException, UnknownAttributesException {
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
		final Logger logger = LoggerFactory.getLogger(ReadSecurityManagerImpl.class);
		Map<Class<? extends DomainResource>, ReadSecurityNode> securityNodes = new HashMap<>();
		Set<SecuredAttribute> contributedAttributes = builder.getContributedAttributes();

		logger.debug("Constructing contributed nodes");

		resourceContext.getResourceGraph().forEach(node -> {
			Class<? extends DomainResource> resourceType = node.getResourceType();

			if (!shouldConstructNode(resourceType) || securityNodes.containsKey(resourceType)) {
				return;
			}
			// @formatter:off
			List<SecuredAttribute> scopedAttributes = contributedAttributes
				.stream()
				.filter(attribute -> TypeHelper.isParentOf(attribute.getOwningType(), resourceType))
				.collect(Collectors.toList());
			
			if (scopedAttributes.isEmpty()) {
				return;
			}
			
			logger.trace("Building from contributed attributes, resource type: [{}]", resourceType.getName());
			
			securityNodes.put(
					resourceType,
					new ReadSecurityNodeImpl(
							resourceType,
							resourceContext,
							scopedAttributes,
							exceptionThrower));
			// @formatter:on
		});

		return securityNodes;
	}

	@SuppressWarnings({ "rawtypes" })
	private Map<Class<? extends DomainResource>, ReadSecurityNode> constructEmptyNodes(
			Map<Class<? extends DomainResource>, ReadSecurityNode> configuredNodes,
			DomainResourceContext resourceContext, ReadFailureExceptionHandler exceptionThrower) throws Exception {
		final Logger logger = LoggerFactory.getLogger(ReadSecurityManagerImpl.class);

		logger.debug("Constructing empty nodes");

		resourceContext.getResourceGraph().forEach(node -> {
			Class<? extends DomainResource> resourceType = node.getResourceType();

			if (!shouldConstructNode(resourceType) || configuredNodes.containsKey(resourceType)) {
				return;
			}

			logger.trace("Using {} for resource type: [{}]", DefaultReadSecurityNode.class.getName(),
					resourceType.getName());
			// @formatter:off
			configuredNodes.put(
					resourceType,
					new DefaultReadSecurityNode<>(
							resourceContext.getMetadata(resourceType),
							exceptionThrower));
			// @formatter:on
		});

		return configuredNodes;
	}

	private ReadFailureExceptionHandler resolveFailureExceptionHandler() {
		final Logger logger = LoggerFactory.getLogger(ReadSecurityManagerImpl.class);

		logger.debug("Resolving {}", ReadFailureExceptionHandler.class.getName());

		Environment env = ContextManager.getBean(Environment.class);
		String configuredStrategy = env.getProperty(Settings.READ_FAILURE_EXCEPTION_THROWING_STRATEGY);

		try {
			// @formatter:off
			ExceptionThrowingStrategy strategy = Optional.ofNullable(configuredStrategy)
					.map(String::toUpperCase)
					.map(String::trim)
					.map(ExceptionThrowingStrategy::valueOf)
					.orElse(ExceptionThrowingStrategy.USE_REGISTERED_ATTRIBUTES);
			// @formatter:on
			if (strategy.equals(ExceptionThrowingStrategy.USE_REGISTERED_ATTRIBUTES)) {
				logger.debug("Using {} for read failure", UseRegisteredAttributesStrategy.class.getName());
				return new UseRegisteredAttributesStrategy();
			}

			logger.debug("Using {} for read failure", ThrowExceptionStrategy.class.getName());
			return new ThrowExceptionStrategy();
		} catch (IllegalArgumentException iae) {
			throw new IllegalArgumentException(
					String.format("Unknown read failure exception throwing strategy of [%s], expected one of %s",
							configuredStrategy, Stream.of(ExceptionThrowingStrategy.values()).map(Object::toString)
									.collect(Collectors.joining(StringHelper.COMMON_JOINER))));
		}
	}

	private CRUDSecurityManagerBuilder doContribute(List<ReadSecurityContributor> contributors,
			CRUDSecurityManagerBuilder builder) {
		LoggerFactory.getLogger(ReadSecurityManagerImpl.class).debug("Doing contributions");

		contributors.stream().forEach(contributor -> contributor.contribute(builder));

		return builder;
	}

	private List<ReadSecurityContributor> scanForContributors() throws IllegalAccessException {
		final Logger logger = LoggerFactory.getLogger(ReadSecurityManagerImpl.class);
		List<ReadSecurityContributor> contributors = new ArrayList<>();
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);

		scanner.addIncludeFilter(new AssignableTypeFilter(ReadSecurityContributor.class));

		logger.debug("Scanning for {}s", ReadSecurityContributor.class.getSimpleName());

		for (BeanDefinition beanDef : scanner.findCandidateComponents(Settings.BASE_PACKAGE)) {
			try {
				@SuppressWarnings("unchecked")
				Class<? extends ReadSecurityContributor> contributorClass = (Class<? extends ReadSecurityContributor>) Class
						.forName(beanDef.getBeanClassName());

				logger.trace("Found one {} of type [{}] in [{}]", ReadSecurityContributor.class.getSimpleName(),
						contributorClass.getName(), contributorClass.getPackageName());

				contributors.add(contributorClass.getConstructor().newInstance());
			} catch (NoSuchMethodException nsm) {
				SpringApplication.exit(ContextManager.getExitAcess().getContext());
				throw new IllegalArgumentException(String.format(
						"A non-arg constructor is required on a(n) %s instance, unable to find one in [%s]",
						ReadSecurityContributor.class.getSimpleName(), beanDef.getBeanClassName()));
			} catch (Exception any) {
				any.printStackTrace();
				SpringApplication.exit(ContextManager.getExitAcess().getContext());
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

		private final Logger logger = LoggerFactory.getLogger(CRUDSecurityManagerBuilderImpl.class);

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

		private <D extends DomainResource> Key<D> makeKey(Class<D> type, CRUDCredential credential, String name)
				throws CredentialException {
			return new Key<>(type, credential, name);
		}

		@SuppressWarnings("unchecked")
		private <D extends DomainResource> SecuredAttributeImpl<D> locateProperty(Key<D> key) {
			if (securedAttributes.containsKey(key)) {
				logger.trace(String.format("Existing entry with %s", key.toString()));

				return (SecuredAttributeImpl<D>) securedAttributes.get(key);
			}

			return null;
		}

		private <D extends DomainResource> SecuredAttributeImpl<D> putProperty(Key<D> key,
				SecuredAttributeImpl<D> attr) {
			logger.trace(String.format("New entry with %s", key.toString()));
			securedAttributes.put(key, attr);

			return attr;
		}

		private <D extends DomainResource> void setProperty(Class<D> owningType, CRUDCredential credential, String name,
				String alias) throws CredentialException {
			Key<D> key = makeKey(owningType, credential, name);
			SecuredAttributeImpl<D> attr = locateProperty(key);

			if (attr != null) {
				modifyAlias(attr, alias);
				return;
			}

			putProperty(key, new SecuredAttributeImpl<>(owningType, credential, name).setAlias(alias));
		}

		private <D extends DomainResource> void setProperty(Class<D> owningType, CRUDCredential credential, String name,
				Boolean isMasked) throws CredentialException {
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
			logger.trace(String.format("Overring alias [%s] with [%s]", attr.getAlias(), alias));
			attr.setAlias(alias);
		}

		@SuppressWarnings("rawtypes")
		private void modifyVisibility(SecuredAttributeImpl attr, Boolean isMasked) {
			logger.trace(String.format("Overring visibility [%s] with [%s]", attr.isMasked(), isMasked));
			attr.setMasked(isMasked);
		}

		private class WithTypeImpl<D extends DomainResource> implements WithType<D> {

			private final Class<D> type;

			public WithTypeImpl(Class<D> type) {
				this.type = Objects.requireNonNull(type);
				logger.debug(String.format("With type %s", type.getSimpleName()));
			}

			@Override
			public WithCredential<D> credentials(CRUDCredential... credentials) {
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
				private final CRUDCredential[] credentials;

				public WithCredentialImpl(WithType<D> owningType, CRUDCredential... credentials)
						throws CredentialException {
					this.owningType = owningType;
					this.credentials = Stream.of(requireNonNull(credentials)).map(Objects::requireNonNull)
							.toArray(CRUDCredential[]::new);

					List<String> credentialList = new ArrayList<>();

					for (CRUDCredential credential : credentials) {
						credentialList.add(credential.evaluate());
					}

					logger.debug(String.format("With Credentials[%s]",
							credentialList.stream().collect(Collectors.joining(","))));
				}

				private void removeRemainingFields(String... fields) {
					remainingFields.removeAll(List.of(fields));

					if (logger.isTraceEnabled()) {
						logger.trace(String.format("Removing [%s] from remaining fields",
								Stream.of(fields).collect(Collectors.joining(","))));
					}
				}

				@Override
				public WithAttribute<D> attributes(String... attributes) {
					removeRemainingFields(attributes);

					return new WithAttributeImpl(this, attributes);
				}

				@Override
				public WithCredential<D> credentials(CRUDCredential credential) {
					return owningType.credentials(credentials);
				}

				@Override
				public WithCredential<D> mask() {
					for (CRUDCredential crudCredential : credentials) {
						for (String attribute : context.getMetadata(type).getAttributeNames()) {
							try {
								setProperty(type, crudCredential, attribute, Boolean.TRUE);
							} catch (CredentialException any) {
								throw new IllegalArgumentException(any);
							}
						}
					}

					logger.debug("Mask all");

					return this;
				}

				@Override
				public WithCredential<D> publish() {
					for (CRUDCredential crudCredential : credentials) {
						for (String attribute : context.getMetadata(type).getAttributeNames()) {
							try {
								setProperty(type, crudCredential, attribute, Boolean.FALSE);
							} catch (CredentialException any) {
								throw new IllegalArgumentException(any);
							}
						}
					}

					logger.debug("Publish all");

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

						logger.debug(String.format("With fields %s",
								Stream.of(attributes).collect(Collectors.joining(", "))));
					}

					@Override
					public WithAttribute<D> use(String... alias) {
						if (requireNonNull(alias).length != attributes.length) {
							throw new IllegalArgumentException(String.format(
									"Alias names length and field lengths must match. Alias[%d]><[%d]Attributes",
									alias.length, attributes.length));
						}

						int n = attributes.length;

						for (CRUDCredential credential : credentials) {
							for (int i = 0; i < n; i++) {
								try {
									setProperty(type, credential, attributes[i], alias[i]);
								} catch (CredentialException any) {
									throw new IllegalArgumentException(any);
								}
							}
						}

						logger.debug(
								String.format("Using alias %s", Stream.of(alias).collect(Collectors.joining(","))));

						return this;
					}

					private WithAttribute<D> make(Boolean isMasked) {
						int n = attributes.length;

						for (CRUDCredential credential : credentials) {
							for (int i = 0; i < n; i++) {
								try {
									setProperty(type, credential, attributes[i], isMasked);
								} catch (CredentialException any) {
									throw new IllegalArgumentException(any);
								}
							}
						}

						logger.debug(isMasked ? "Mask" : "Publish");

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
					public WithCredential<D> credentials(CRUDCredential... credentials) {
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
		private final CRUDCredential credential;
		private final String name;

		private Boolean masked;
		private String alias;

		public SecuredAttributeImpl(Class<D> owningType, CRUDCredential credential, String name) {
			this.owningType = owningType;
			this.credential = credential;
			this.name = name;
		}

		@Override
		public Class<D> getOwningType() {
			return owningType;
		}

		@Override
		public CRUDCredential getCredential() {
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
		private final CRUDCredential credential;
		private final String name;

		private final int hashCode;

		public Key(Class<T> type, CRUDCredential credential, String name) throws CredentialException {
			this.type = type;
			this.credential = credential;
			this.name = name;

			int hash = 17;

			hash += 37 * type.hashCode();
			hash += credential.evaluate().hashCode();
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
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;

			Key<?> other = (Key<?>) obj;

			try {
				return credential.evaluate().equals(other.credential.evaluate()) && name.equals(other.name)
						&& type.equals(other.type);
			} catch (CredentialException any) {
				return false;
			}
		}

		@Override
		public String toString() {
			try {
				return "Key [type=" + type + ", credential=" + credential.evaluate() + ", name=" + name + ", hashCode="
						+ hashCode + "]";
			} catch (CredentialException any) {
				any.printStackTrace();
				return "Error while trying to get string";
			}
		}

	}

	private enum ExceptionThrowingStrategy {
		USE_REGISTERED_ATTRIBUTES, THROW_EXCEPTION
	}

	private abstract class AbstractReadFailureExceptionHandler implements ReadFailureExceptionHandler {

		@Override
		public void doOnUnauthorizedCredential(Class<?> resourceType, String credential) throws CredentialException {
			throw new UnauthorizedCredentialException(credential, resourceType.getName());
		}

	}

	private class UseRegisteredAttributesStrategy extends AbstractReadFailureExceptionHandler {

		private UseRegisteredAttributesStrategy() {}

		@Override
		public List<String> doOnInvalidAttributes(Class<?> resourceType, Collection<String> requestedAttributes,
				Collection<String> authorizedAttributes) {
			return new ArrayList<>(authorizedAttributes);
		}

	}

	private class ThrowExceptionStrategy extends AbstractReadFailureExceptionHandler {

		private ThrowExceptionStrategy() {};

		@Override
		public List<String> doOnInvalidAttributes(Class<?> resourceType, Collection<String> requestedAttributes,
				Collection<String> authorizedAttributes) throws UnknownAttributesException {
			throw new UnknownAttributesException(requestedAttributes, resourceType.getName());
		}

	}

	@Override
	public void summary() throws Exception {
		final Logger logger = LoggerFactory.getLogger(ReadSecurityManagerImpl.class);

		logger.trace("\n{}", securityNodes.values().stream().map(Object::toString).collect(Collectors.joining("\n")));
	}

}