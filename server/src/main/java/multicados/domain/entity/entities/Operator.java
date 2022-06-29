/**
 *
 */
package multicados.domain.entity.entities;

import javax.persistence.Entity;
import javax.persistence.Table;

import multicados.internal.domain.ResourceAuditor;

/**
 * @author Ngoc Huy
 *
 */
@Entity
@Table(name = "operators")
public class Operator extends User implements ResourceAuditor<String> {

}
