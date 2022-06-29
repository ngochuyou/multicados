/**
 *
 */
package multicados.internal.helper;

/**
 * @author Ngoc Huy
 *
 */
public class Result {

	protected Status status;

	protected Result(Status status) {
		this.status = status;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public boolean isOk() {
		return status == Status.SUCCESS;
	}

	public enum Status {

		SUCCESS, FAILED;

		public Status and(Status other) {
			return this == SUCCESS && other == SUCCESS ? SUCCESS : FAILED;
		}

	}

}
