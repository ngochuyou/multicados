/**
 * 
 */
package multicados.internal.service;

import javax.persistence.EntityManager;

/**
 * @author Ngoc Huy
 *
 */
public class ServiceUtils {

	public static ServiceResult success(EntityManager em, boolean flushOnFinish) {
		return finish(em, ServiceResult.success(), flushOnFinish);
	}
	
	public static ServiceResult finish(EntityManager em, ServiceResult result, boolean flushOnFinish) {
		if (flushOnFinish) {
			try {
				if (result.isOk()) {
					em.flush();
					return result;
				}

				em.clear();
				return result;
			} catch (Exception any) {
				return ServiceResult.failed(any);
			}
		}

		return result;
	}
	
}
