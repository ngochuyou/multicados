/**
 * 
 */
package multicados.domain.entity.entities.customer;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import multicados.domain.AbstractEntity;
import multicados.domain.entity.PermanentEntity;
import multicados.domain.entity.entities.Customer;
import multicados.domain.entity.entities.Customer_;

/**
 * @author Ngoc Huy
 *
 */
@Entity
@Table(name = "credential_resets")
public class CredentialReset extends PermanentEntity<UUID> {

	@Id
	@GeneratedValue(generator = "uuid")
	@GenericGenerator(name = "uuid", strategy = "uuid2")
	@Column(columnDefinition = AbstractEntity.MYSQL_UUID_COLUMN_DEFINITION)
	private UUID id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = CredentialReset_.CUSTOMER, referencedColumnName = Customer_.ID)
	private Customer customer;

	@Column(nullable = false, updatable = false, columnDefinition = MYSQL_BCRYPT_COLUMN_DEFINITION)
	private String code;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime version;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public LocalDateTime getVersion() {
		return version;
	}

	public void setVersion(LocalDateTime version) {
		this.version = version;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

}
