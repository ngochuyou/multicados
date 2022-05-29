/**
 * 
 */
package multicados.internal.service.crud.rest;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import multicados.internal.domain.DomainResource;

/**
 * @author Ngoc Huy
 *
 * @param <D>
 */
public abstract class AbstractRestQuery<D extends DomainResource> implements RestQuery<D> {

	private final Class<D> resourceType;

	private List<String> attributes;
	private PageableImpl page;

	private String associationName;

	public AbstractRestQuery(Class<D> resourceType) {
		this.resourceType = resourceType;
	}

	public Class<D> getResourceType() {
		return resourceType;
	}

	@Override
	public List<String> getAttributes() {
		return attributes;
	}

	@Override
	public void setAttributes(List<String> attributes) {
		this.attributes = attributes;
	}

	@Override
	public PageableImpl getPage() {
		return page;
	}

	@Override
	public void setPage(Pageable pageable) {
		this.page = new PageableImpl(pageable);
	}

	@Override
	public String getAssociationName() {
		return associationName;
	}

	@Override
	public void setAssociationName(String associationName) {
		this.associationName = associationName;
	}

	public static class PageableImpl implements Pageable {

		private Pageable delegatedPageable;

		public PageableImpl() {
			delegatedPageable = PageRequest.of(0, 10);
		}

		public PageableImpl(Pageable pageable) {
			delegatedPageable = pageable;
		}

		public void setSize(int size) {
			delegatedPageable = PageRequest.of(delegatedPageable.getPageNumber(), size);
		}

		public void setNum(int page) {
			delegatedPageable = PageRequest.of(page, delegatedPageable.getPageSize());
		}

		@Override
		public int getPageNumber() {
			return delegatedPageable.getPageNumber();
		}

		@Override
		public int getPageSize() {
			return delegatedPageable.getPageSize();
		}

		@Override
		public long getOffset() {
			return delegatedPageable.getOffset();
		}

		@Override
		public Sort getSort() {
			return delegatedPageable.getSort();
		}

		@Override
		public Pageable next() {
			return delegatedPageable.next();
		}

		@Override
		public Pageable previousOrFirst() {
			return delegatedPageable.previousOrFirst();
		}

		@Override
		public Pageable first() {
			return delegatedPageable.first();
		}

		@Override
		public Pageable withPage(int pageNumber) {
			return delegatedPageable.withPage(pageNumber);
		}

		@Override
		public boolean hasPrevious() {
			return delegatedPageable.hasPrevious();
		}

	}

}
