/**
 * 
 */
package multicados.domain.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * @author Ngoc Huy
 *
 */
@Embeddable
public class SpanInformation implements Serializable {

	private static final long serialVersionUID = 1L;

	@Column(updatable = false)
	private LocalDateTime startTimestamp;

	private LocalDateTime endTimestamp;

	public SpanInformation() {}

	public SpanInformation(LocalDateTime startTimestamp, LocalDateTime endTimestamp) {
		this.startTimestamp = startTimestamp;
		this.endTimestamp = endTimestamp;
	}

	public LocalDateTime getStartTimestamp() {
		return startTimestamp;
	}

	public void setStartTimestamp(LocalDateTime startTimestamp) {
		this.startTimestamp = startTimestamp;
	}

	public LocalDateTime getEndTimestamp() {
		return endTimestamp;
	}

	public void setEndTimestamp(LocalDateTime endTimestamp) {
		this.endTimestamp = endTimestamp;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((endTimestamp == null) ? 0 : endTimestamp.hashCode());
		result = prime * result + ((startTimestamp == null) ? 0 : startTimestamp.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SpanInformation other = (SpanInformation) obj;
		if (endTimestamp == null) {
			if (other.endTimestamp != null)
				return false;
		} else if (!endTimestamp.equals(other.endTimestamp))
			return false;
		if (startTimestamp == null) {
			if (other.startTimestamp != null)
				return false;
		} else if (!startTimestamp.equals(other.startTimestamp))
			return false;
		return true;
	}

}
