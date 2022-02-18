/**
 * 
 */
package multicados.domain.entity.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import multicados.domain.entity.PermanentEntity;
import multicados.internal.domain.NamedResource;

/**
 * @author Ngoc Huy
 *
 */
@Entity
@Table(name = "provinces", indexes = @Index(columnList = Province.$index))
public class Province extends PermanentEntity<Integer> implements NamedResource {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(nullable = false)
	private String name;

	@Override
	public Integer getId() {
		return id;
	}

	@Override
	public void setId(Integer id) {
		this.id = id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	public static final String $index = "id, active";

}
