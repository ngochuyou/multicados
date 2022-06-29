/**
 *
 */
package multicados.internal.service.crud.security.read;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_AbsoluteEven;
import multicados.internal.domain.DomainResource;
import multicados.internal.domain.DomainResourceContext;
import multicados.internal.domain.metadata.DomainResourceMetadata;
import multicados.internal.helper.CollectionHelper;
import multicados.internal.helper.StringHelper;
import multicados.internal.helper.TypeHelper;
import multicados.internal.helper.Utils;
import multicados.internal.security.CredentialException;
import multicados.internal.service.crud.security.SecuredAttribute;

/**
 * @author Ngoc Huy
 *
 */
public class ReadSecurityNodeImpl<D extends DomainResource> extends AbstractReadSecurityNode<D> {

	private final Map<String, Set<String>> authorizedAttributes;
	private final Map<String, List<String>> nonAssociationAttributes;
	private final Map<String, String> aliasesByOrigins;
	private final Map<String, String> originsByAliases;

	// @formatter:off
	public ReadSecurityNodeImpl(
			Class<D> type,
			DomainResourceContext modelContext,
			List<SecuredAttribute<D>> attributes,
			ReadFailureExceptionHandler exceptionThrower) throws Exception {
		super(modelContext.getMetadata(type), exceptionThrower);

		final List<SecuredAttribute<D>> sortedAttributes = Utils
				.declare(attributes)
				.then(this::sort)
				.get();
		final DomainResourceMetadata<D> metadata = getMetadata();

		authorizedAttributes = Utils
				.declare(sortedAttributes)
					.second(type)
				.then(this::getPublicAttributes)
				.then(this::seal)
				.then(Collections::unmodifiableMap)
				.get();
		nonAssociationAttributes = authorizedAttributes
				.entrySet()
				.stream()
				.map(entry -> Map.entry(
						entry.getKey(),
						entry.getValue()
							.stream()
							.filter(attribute -> !metadata.isAssociation(attribute))
							.collect(Collectors.toList())))
				.collect(CollectionHelper.toMap());
		aliasesByOrigins = Utils
				.declare(sortedAttributes)
					.second(modelContext.getMetadata(type))
				.then(this::getAlias)
				.then(Collections::unmodifiableMap)
				.get();
		originsByAliases = Utils
				.declare(aliasesByOrigins)
				.then(CollectionHelper::inverse)
				.then(Collections::unmodifiableMap)
				.get();
		// @formatter:on
	}

	@Override
	public Map<String, String> translate(Collection<String> attributes) {
		return attributes.stream().map(attr -> Map.entry(attr, aliasesByOrigins.get(attr)))
				.collect(CollectionHelper.toMap());
	}

	private Map<String, String> getAlias(List<SecuredAttribute<D>> attributes, DomainResourceMetadata<D> metadata) {
		final Map<String, String> aliasMap = metadata.getAttributeNames().stream()
				.map(attribute -> Map.entry(attribute, attribute))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		attributes.stream().forEach(attribute -> aliasMap.put(attribute.getName(),
				StringUtils.hasLength(attribute.getAlias()) ? attribute.getAlias() : attribute.getName()));

		return aliasMap;
	}

	private List<SecuredAttribute<D>> sort(List<SecuredAttribute<D>> attributes) {
		// @formatter:off
		return attributes.stream()
				.sorted((left, right) -> TypeHelper.isParentOf(left.getOwningType(), right.getOwningType()) ? -1 : 1)
				.collect(Collectors.toList());
		// @formatter:on
	}

	private Map<String, Set<String>> seal(Map<String, Set<String>> publicAttributes) {
		return publicAttributes.entrySet().stream()
				.map(entry -> Map.entry(entry.getKey(), Collections.unmodifiableSet(entry.getValue())))
				.collect(CollectionHelper.toMap());
	}

	private Map<String, Set<String>> getPublicAttributes(List<SecuredAttribute<D>> attributes, Class<D> owningType)
			throws CredentialException {
		final Map<String, Set<String>> publicAttributes = new HashMap<>();

		for (final SecuredAttribute<D> attribute : attributes) {
			final String name = attribute.getName();
			final boolean isMasked = Optional.ofNullable(attribute.isMasked()).orElse(true);
			final String credential = attribute.getCredential().getAuthority();

			if (credential == null) {
				throw new IllegalArgumentException(String.format("Credential was empty on property [%s]", name));
			}

			publicAttributes.putIfAbsent(credential, new HashSet<>());

			final Set<String> publicAttributesByCredential = publicAttributes.get(credential);

			if (isMasked) {
				if (publicAttributesByCredential.contains(name)) {
					publicAttributesByCredential.remove(name);
				}

				continue;
			}

			if (!publicAttributesByCredential.contains(name)) {
				publicAttributesByCredential.add(name);
			}
		}

		return publicAttributes;
	}

	@Override
	protected String getActualAttributeName(String requestedName) {
		return originsByAliases.get(requestedName);
	}

	@Override
	protected Set<String> getAuthorizedAttributes(String credentialValue) {
		return authorizedAttributes.get(credentialValue);
	}

	@Override
	protected List<String> getNonAssociationAttributes(String credentialValue) {
		return nonAssociationAttributes.get(credentialValue);
	}

	@Override
	public String toString() {
		final AsciiTable table = new AsciiTable();

		table.getRenderer().setCWC(new CWC_AbsoluteEven());
		table.addRule();
		table.addRow(null, String.format("%s<%s>", this.getClass().getSimpleName(),
				getMetadata().getResourceType().getSimpleName()));
		table.addRule();

		if (authorizedAttributes.isEmpty()) {
			return table.render();
		}

		for (final Entry<String, Set<String>> entry : authorizedAttributes.entrySet()) {
			table.addRow(entry.getKey(), StringHelper.join(StringHelper.COMMA, entry.getValue()));
		}

		table.addRule();

		return table.render();
//		// @formatter:off
//		return String.format("%s<%s>[\n\t%s\n]",
//				this.getClass().getSimpleName(), getMetadata().getResourceType().getSimpleName(),
//				authorizedAttributes.entrySet()
//					.stream()
//					.map(entry -> joinOrEmpty(entry))
//					.filter(string -> !string.equals(EMPTY_STRING))
//					.collect(Collectors.joining("\n\t")));
//		// @formatter:on
	}

}
