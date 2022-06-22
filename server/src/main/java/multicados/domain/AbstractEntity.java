/**
 * 
 */
package multicados.domain;

import java.io.Serializable;

import multicados.internal.domain.Entity;

/**
 * @author Ngoc Huy
 *
 */
public abstract class AbstractEntity<T extends Serializable> implements Entity<T> {

	public static final String SHARED_TABLE_GENERATOR = "SHARED_TABLE_GENERATOR";
	public static final String SHARED_TABLE_GENERATOR_TABLENAME = "id_generators";

}
