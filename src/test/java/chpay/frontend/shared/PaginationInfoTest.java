package chpay.frontend.shared;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.domain.Sort;

class PaginationInfoTest {

  @Test
  void constructor_ValidInputs_CreatesInstance() {
    PaginationInfo info = new PaginationInfo(1, 10, true, false, 100);

    assertAll(
        () -> assertEquals(1, info.getPage()),
        () -> assertEquals(10, info.getSize()),
        () -> assertTrue(info.isHasNext()),
        () -> assertFalse(info.isHasPrev()),
        () -> assertEquals(100, info.getTotalTransactions()));
  }

  @ParameterizedTest
  @CsvSource({
    "1,  10, true,  false, 100",
    "2,  20, true,  true,  150",
    "10, 5,  false, true,  48",
    "1,  5,  false, false, 3"
  })
  void constructor_VariousInputs_CreatesCorrectInstances(
      int page, int size, boolean hasNext, boolean hasPrev, long totalTransactions) {

    PaginationInfo info = new PaginationInfo(page, size, hasNext, hasPrev, totalTransactions);

    assertAll(
        () -> assertEquals(page, info.getPage()),
        () -> assertEquals(size, info.getSize()),
        () -> assertEquals(hasNext, info.isHasNext()),
        () -> assertEquals(hasPrev, info.isHasPrev()),
        () -> assertEquals(totalTransactions, info.getTotalTransactions()));
  }

  @Test
  void getters_ReturnCorrectValues() {
    int page = 2;
    int size = 15;
    boolean hasNext = true;
    boolean hasPrev = true;
    long totalTransactions = 75;

    PaginationInfo info = new PaginationInfo(page, size, hasNext, hasPrev, totalTransactions);

    assertEquals(page, info.getPage());
    assertEquals(size, info.getSize());
    assertEquals(hasNext, info.isHasNext());
    assertEquals(hasPrev, info.isHasPrev());
    assertEquals(totalTransactions, info.getTotalTransactions());
  }

  @Test
  void constructor_ZeroValues_CreatesInstance() {
    PaginationInfo info = new PaginationInfo(0, 0, false, false, 0);

    assertAll(
        () -> assertEquals(0, info.getPage()),
        () -> assertEquals(0, info.getSize()),
        () -> assertFalse(info.isHasNext()),
        () -> assertFalse(info.isHasPrev()),
        () -> assertEquals(0, info.getTotalTransactions()));
  }

  @Test
  void constructor_NegativeValues_CreatesInstance() {
    PaginationInfo info = new PaginationInfo(-1, -10, false, false, -50);

    assertAll(
        () -> assertEquals(-1, info.getPage()),
        () -> assertEquals(-10, info.getSize()),
        () -> assertFalse(info.isHasNext()),
        () -> assertFalse(info.isHasPrev()),
        () -> assertEquals(-50, info.getTotalTransactions()));
  }

  @Test
  void constructor_MaxValues_CreatesInstance() {
    PaginationInfo info =
        new PaginationInfo(Integer.MAX_VALUE, Integer.MAX_VALUE, true, true, Long.MAX_VALUE);

    assertAll(
        () -> assertEquals(Integer.MAX_VALUE, info.getPage()),
        () -> assertEquals(Integer.MAX_VALUE, info.getSize()),
        () -> assertTrue(info.isHasNext()),
        () -> assertTrue(info.isHasPrev()),
        () -> assertEquals(Long.MAX_VALUE, info.getTotalTransactions()));
  }

  @Test
  void buildPaginationInfo_SinglePageData_ReturnsCorrectInfo() {
    // Given data that fits in a single page
    PaginationInfo info = PaginationInfo.buildPaginationInfo(5, 1, 10);

    // Then
    assertAll(
        () -> assertEquals(1, info.getPage()),
        () -> assertEquals(10, info.getSize()),
        () -> assertFalse(info.isHasNext()),
        () -> assertFalse(info.isHasPrev()),
        () -> assertEquals(5, info.getTotalTransactions()));
  }

  @Test
  void buildPaginationInfo_MultiplePages_ReturnsCorrectInfo() {
    // Given data that spans multiple pages
    PaginationInfo info = PaginationInfo.buildPaginationInfo(25, 2, 10);

    // Then
    assertAll(
        () -> assertEquals(2, info.getPage()),
        () -> assertEquals(10, info.getSize()),
        () -> assertTrue(info.isHasNext()),
        () -> assertTrue(info.isHasPrev()),
        () -> assertEquals(25, info.getTotalTransactions()));
  }

  @Test
  void buildPaginationInfo_LastPage_HasNoNextPage() {
    // Given we're on the last page
    PaginationInfo info = PaginationInfo.buildPaginationInfo(25, 3, 10);

    // Then
    assertAll(
        () -> assertEquals(3, info.getPage()),
        () -> assertEquals(10, info.getSize()),
        () -> assertFalse(info.isHasNext()),
        () -> assertTrue(info.isHasPrev()),
        () -> assertEquals(25, info.getTotalTransactions()));
  }

  @Test
  void buildPaginationInfo_NegativePage_AdjustsToFirstPage() {
    // Given a negative page number
    PaginationInfo info = PaginationInfo.buildPaginationInfo(25, -1, 10);

    // Then
    assertAll(
        () -> assertEquals(1, info.getPage()),
        () -> assertEquals(10, info.getSize()),
        () -> assertTrue(info.isHasNext()),
        () -> assertFalse(info.isHasPrev()),
        () -> assertEquals(25, info.getTotalTransactions()));
  }

  @Test
  void buildPaginationInfo_ZeroPage_AdjustsToFirstPage() {
    // Given a page number of zero
    PaginationInfo info = PaginationInfo.buildPaginationInfo(25, 0, 10);

    // Then
    assertAll(
        () -> assertEquals(1, info.getPage()),
        () -> assertEquals(10, info.getSize()),
        () -> assertTrue(info.isHasNext()),
        () -> assertFalse(info.isHasPrev()),
        () -> assertEquals(25, info.getTotalTransactions()));
  }

  @Test
  void buildPaginationInfo_PageBeyondLimit_AdjustsToLastPossiblePage() {
    // Given a page number beyond the available data
    PaginationInfo info = PaginationInfo.buildPaginationInfo(25, 5, 10);

    // Then
    assertAll(
        () -> assertEquals(3, info.getPage()),
        () -> assertEquals(10, info.getSize()),
        () -> assertFalse(info.isHasNext()),
        () -> assertTrue(info.isHasPrev()),
        () -> assertEquals(25, info.getTotalTransactions()));
  }

  @ParameterizedTest
  @CsvSource({
    "1, 1, 10, false, false", // Single item
    "10, 1, 10, false, false", // Exactly one page
    "11, 1, 10, true, false", // Just over one page
    "20, 2, 10, false, true" // Middle page with no next
  })
  void buildPaginationInfo_VariousScenarios(
      long totalRows, int page, int pageSize, boolean expectedHasNext, boolean expectedHasPrev) {

    PaginationInfo info = PaginationInfo.buildPaginationInfo(totalRows, page, pageSize);

    assertAll(
        () -> assertEquals(page, info.getPage()),
        () -> assertEquals(pageSize, info.getSize()),
        () -> assertEquals(expectedHasNext, info.isHasNext()),
        () -> assertEquals(expectedHasPrev, info.isHasPrev()),
        () -> assertEquals(totalRows, info.getTotalTransactions()));
  }

  @Test
  void buildPaginationInfo_ExactDivision_HandlesCorrectly() {
    // Given data that divides exactly by page size
    PaginationInfo info = PaginationInfo.buildPaginationInfo(20, 2, 10);

    assertAll(
        () -> assertEquals(2, info.getPage()),
        () -> assertEquals(10, info.getSize()),
        () -> assertFalse(info.isHasNext()),
        () -> assertTrue(info.isHasPrev()),
        () -> assertEquals(20, info.getTotalTransactions()));
  }

  @Test
  void getSort_AscendingOrder_ReturnsAscendingSort() {
    // Given
    PaginationInfo info = new PaginationInfo(1, 10, true, false, 100);
    String sortBy = "timestamp";
    String order = "asc";

    // When
    Sort result = info.getSort(sortBy, order);

    // Then
    assertAll(
        () -> assertEquals(Sort.Direction.ASC, result.getOrderFor(sortBy).getDirection()),
        () -> assertEquals(sortBy, result.getOrderFor(sortBy).getProperty()));
  }

  @Test
  void getSort_DescendingOrder_ReturnsDescendingSort() {
    // Given
    PaginationInfo info = new PaginationInfo(1, 10, true, false, 100);
    String sortBy = "amount";
    String order = "desc";

    // When
    Sort result = info.getSort(sortBy, order);

    // Then
    assertAll(
        () -> assertEquals(Sort.Direction.DESC, result.getOrderFor(sortBy).getDirection()),
        () -> assertEquals(sortBy, result.getOrderFor(sortBy).getProperty()));
  }

  @Test
  void getSort_NonAscOrder_DefaultsToDescending() {
    // Given
    PaginationInfo info = new PaginationInfo(1, 10, true, false, 100);
    String sortBy = "description";
    String order = "invalid"; // Any value other than "asc"

    // When
    Sort result = info.getSort(sortBy, order);

    // Then
    assertAll(
        () -> assertEquals(Sort.Direction.DESC, result.getOrderFor(sortBy).getDirection()),
        () -> assertEquals(sortBy, result.getOrderFor(sortBy).getProperty()));
  }
}
