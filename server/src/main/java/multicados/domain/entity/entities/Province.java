/**
 *
 */
package multicados.domain.entity.entities;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import multicados.domain.entity.PermanentEntity;
import multicados.internal.domain.NamedResource;
import multicados.internal.domain.annotation.Name;

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

	@Name
	@Column(nullable = false)
	private String name;

	@OneToMany(mappedBy = District_.PROVINCE)
	private List<District> districts;

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

	public List<District> getDistricts() {
		return districts;
	}

	public void setDistricts(List<District> districts) {
		this.districts = districts;
	}

	public static final String $index = "id, active";

}
