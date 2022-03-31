/**
 * 
 */
package multicados.internal.service.crud.security.read;

import multicados.internal.service.crud.security.read.ReadSecurityManagerImpl.CRUDSecurityManagerBuilder;

/**
 * @author Ngoc Huy
 *
 */
public interface ReadSecurityContributor {

	void contribute(CRUDSecurityManagerBuilder builder);

}
