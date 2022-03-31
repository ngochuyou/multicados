/**
 * 
 */
package multicados.internal.service.crud.security.read;

import java.util.List;

import multicados.internal.domain.DomainResource;
import multicados.internal.service.crud.security.ResultType;

/**
 * @author Ngoc Huy
 *
 */
public interface ReadMetadata<D extends DomainResource> {

	/**
	 * @return the entity type of this source
	 */
	Class<D> getEntityType();

	/**
	 * Get the representation class of this source If this source is a POJO then
	 * it's the POJO class</br>
	 * If this source is a Collection<Tuple>, returns Tuple.class</br>
	 * If this source is a Collection<POJO>, returns POJO class</br>
	 * 
	 * @return the type of this source
	 */
	Class<?> getRepresentation();

	/**
	 * @return the representation of this source
	 */
	ResultType getResultType();

	/**
	 * Named attributes to which this source is mapped
	 * 
	 * @return named attributes
	 */
	List<String> getAttributes();

	/**
	 * Set the named attributes to which this source is mapped
	 */
	void setColumns(List<String> attributes);

	/**
	 * @return whether this source contains any association
	 */
	boolean hasAssociation();

	/**
	 * @param index index of the association regarding to this source
	 * @return the {@code ReadMetadata} of the association
	 */
	ReadMetadata<?> getAssociationMetadata(String attributeName);

}
