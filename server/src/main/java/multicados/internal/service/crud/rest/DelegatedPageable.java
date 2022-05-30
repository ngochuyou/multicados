/**
 * 
 */
package multicados.internal.service.crud.rest;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * @author Ngoc Huy
 *
 */
public class DelegatedPageable implements Pageable {

	private Pageable delegatedPageable;

	public DelegatedPageable() {
		delegatedPageable = PageRequest.of(0, 10);
	}

	public DelegatedPageable(Pageable pageable) {
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
