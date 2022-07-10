/**
 * 
 */
package multicados.internal.service;

/**
 * @author Ngoc Huy
 *
 */
public class ServicePayload<T> extends ServiceResult {

	private T body;

	public ServicePayload(ServiceResult result) {
		this(result, null);
	}

	public ServicePayload(ServiceResult result, T body) {
		super(result.getStatus());
		exception(result.getException());
		validation(result.getValidation());
		this.body = body;
	}

	public T getBody() {
		return body;
	}

	public void setBody(T body) {
		this.body = body;
	}

}
