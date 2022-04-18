/**
 * 
 */
package multicados.internal.domain;

import java.io.Closeable;

import multicados.internal.context.ContextBuilder;
import multicados.internal.context.Loggable;
import multicados.internal.domain.metadata.DomainResourceMetadata;
import multicados.internal.domain.tuplizer.DomainResourceTuplizer;

/**
 * @author Ngoc Huy
 *
 */
public interface DomainResourceContext extends ContextBuilder {

	DomainResourceGraph<DomainResource> getResourceGraph();

	<T extends DomainResource> DomainResourceMetadata<T> getMetadata(Class<T> resourceType);

	<T extends DomainResource> DomainResourceTuplizer<T> getTuplizer(Class<T> resourceType);

	public interface ObservableMetadataEntries extends Loggable, Closeable {

		<D extends DomainResource> void register(Class<D> expectingType, MetadataEntryObserver<D> observer)
				throws IllegalAccessException;

		<D extends DomainResource> void notify(DomainResourceMetadata<D> metadata);

	}

	public interface MetadataEntryObserver<D extends DomainResource> {

		void notify(DomainResourceMetadata<D> metadata);

	}

}
