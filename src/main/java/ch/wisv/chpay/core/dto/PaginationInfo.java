package ch.wisv.chpay.core.dto;

import lombok.Getter;
import org.springframework.data.domain.Sort;

/***
 * Class to be used by the frontend to distinguish the pagination page and properties
 */
@Getter
public class PaginationInfo {
  private final long page;
  private final long size;
  private final boolean hasNext;
  private final boolean hasPrev;
  private final long totalTransactions;

  public PaginationInfo(
      long page, long size, boolean hasNext, boolean hasPrev, long totalTransactions) {
    this.page = page;
    this.size = size;
    this.hasNext = hasNext;
    this.hasPrev = hasPrev;
    this.totalTransactions = totalTransactions;
  }

  /***
   *Method to return the state of the pagination buttons for the frontend
   * @param rowsSize number of rows
   * @param page which page to get
   * @param pageSize the size of every page
   * @return a object of class PaginationInfo holding information for the page, size and state of pagination buttons
   */
  public static PaginationInfo buildPaginationInfo(long rowsSize, int page, int pageSize) {
    if (pageSize <= 0) {
      return null;
    }
    // check if the page number is in limits
    if (page <= 0) {
      page = 1;
    }
    if (page * pageSize > rowsSize) {
      page = Math.toIntExact(Math.ceilDivExact(rowsSize, pageSize));
    }
    // check if the first page is fetched
    boolean hasPrevPage = page > 1;
    // check if the last page is fetched
    boolean hasNextPage = page * pageSize < rowsSize;
    return new PaginationInfo(page, pageSize, hasNextPage, hasPrevPage, rowsSize);
  }

  /**
   * Returns a Sort object to specify sorting behavior for a query.
   *
   * @param sortBy the field name by which the sorting should be performed
   * @param order the sorting order, can be "asc" for ascending or any other value for descending
   * @return a Sort object configured with the given parameters
   */
  public Sort getSort(String sortBy, String order) {
    if (order.equals("asc")) {
      return Sort.by(Sort.Direction.ASC, sortBy);
    }
    return Sort.by(Sort.Direction.DESC, sortBy);
  }
}
