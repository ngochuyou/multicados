/**
 * 
 */
package multicados.internal.domain.tuplizer;

import java.io.Serializable;
import java.lang.reflect.Member;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.tuple.entity.EntityTuplizer;

import multicados.internal.domain.Entity;

/**
 * @author Ngoc Huy
 *
 */
class HBMDelegatedSetter implements Setter {

	private final EntityTuplizer tuplizer;
	private final String propName;

	<S extends Serializable, T extends Entity<S>> HBMDelegatedSetter(Class<T> entityType, String propName,
			SessionFactoryImplementor sfi) {
		super();
		this.tuplizer = sfi.getMetamodel().entityPersister(entityType).getEntityTuplizer();
		this.propName = propName;
	}

	@Override
	public Member getMember() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void set(Object source, Object val) throws Exception {
		tuplizer.setPropertyValue(source, propName, val);
	}

}
