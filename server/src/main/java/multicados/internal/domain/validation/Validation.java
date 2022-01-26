/**
 * 
 */
package multicados.internal.domain.validation;

import java.util.HashMap;
import java.util.Map;

import com.mysql.cj.exceptions.MysqlErrorNumbers;

import multicados.internal.helper.Result;

/**
 * @author Ngoc Huy
 *
 */
public class Validation extends Result {

	private final Map<String, Error> errors;

	private Validation(Status status) {
		super(status);
		this.errors = new HashMap<>();
	}

	public Map<String, Error> getErrors() {
		return errors;
	}

	public Validation and(Validation other) {
		status = status.and(other.status);
		errors.putAll(other.getErrors());
		return this;
	}
	
	public Validation dataTooLong(String attributeName, String message) {
		status = Status.FAILED;
		errors.put(attributeName, new Error(MysqlErrorNumbers.ER_DATA_TOO_LONG, message));
		return this;
	}

	public Validation dupKey(String attributeName, String message) {
		status = Status.FAILED;
		errors.put(attributeName, new Error(MysqlErrorNumbers.ER_DUP_ENTRY, message));
		return this;
	}

	public static Validation success() {
		return new Validation(Status.SUCCESS);
	}

	public static Validation failed() {
		return new Validation(Status.FAILED);
	}

}
