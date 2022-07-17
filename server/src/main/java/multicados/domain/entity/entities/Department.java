/**
 * 
 */
package multicados.domain.entity.entities;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import multicados.domain.entity.PermanentEntity;
import multicados.internal.domain.NamedResource;
import multicados.internal.domain.annotation.Name;

/**
 * @author Ngoc Huy
 *
 */
@Entity
@Table(name = "departments")
public class Department extends PermanentEntity<UUID> implements NamedResource {

	@Id
	@GeneratedValue(generator = "uuid")
	@GenericGenerator(name = "uuid", strategy = "uuid2")
	@Column(columnDefinition = MYSQL_UUID_COLUMN_DEFINITION)
	private UUID id;

	@Name
	@Column(nullable = false, unique = true)
	private String name;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
