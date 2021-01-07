/*
 * HEADER_LICENSE_OPEN_SOURCE
 */
package ch.post.it.evoting.cryptoprimitives;

import static ch.post.it.evoting.cryptoprimitives.SameGroupVector.toSameGroupVector;
import static ch.post.it.evoting.cryptoprimitives.test.tools.generator.HasGroupElementGenerator.generateElementList;
import static ch.post.it.evoting.cryptoprimitives.test.tools.generator.HasGroupElementGenerator.generateElementMatrix;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.math.GroupElement;
import ch.post.it.evoting.cryptoprimitives.test.tools.TestSameGroupElement;
import ch.post.it.evoting.cryptoprimitives.test.tools.math.TestGroup;

class SameGroupMatrixTest {

	private static final int BOUND_MATRIX_SIZE = 10;
	private static TestGroup group = new TestGroup();

	private final SecureRandom secureRandom = new SecureRandom();

	private int numRows;
	private int numColumns;
	private List<List<TestSameGroupElement>> matrixElements;

	@BeforeAll
	static void setup() {
		group = new TestGroup();
	}

	@BeforeEach
	void setUp() {
		numRows = secureRandom.nextInt(10) + 1;
		numColumns = secureRandom.nextInt(10) + 1;
		matrixElements = generateElementMatrix(numRows + 1, numColumns, () -> new TestSameGroupElement(group));
	}

	@Test
	void createSameGroupMatrixWithNullValues() {
		assertThrows(NullPointerException.class, () -> SameGroupMatrix.fromRows(null));
	}

	@Test
	void createSameGroupMatrixWithNullRows() {
		final List<List<TestSameGroupElement>> nullRowMatrix = generateElementMatrix(numRows, numColumns, () -> new TestSameGroupElement(group));
		int nullIndex = secureRandom.nextInt(numRows);
		nullRowMatrix.set(nullIndex, null);
		final IllegalArgumentException exceptionFirst = assertThrows(IllegalArgumentException.class, () -> SameGroupMatrix.fromRows(nullRowMatrix));
		assertEquals("A matrix cannot contain a null row.", exceptionFirst.getMessage());
	}

	@Test
	void createSameGroupMatrixWithNullElement() {
		final List<List<TestSameGroupElement>> nullElemMatrix = generateElementMatrix(numRows, numColumns, () -> new TestSameGroupElement(group));
		int nullRowIndex = secureRandom.nextInt(numRows);
		int nullColumnIndex = secureRandom.nextInt(numColumns);
		nullElemMatrix.get(nullRowIndex).set(nullColumnIndex, null);
		final IllegalArgumentException exceptionFirst = assertThrows(IllegalArgumentException.class, () -> SameGroupMatrix.fromRows(nullElemMatrix));
		assertEquals("Elements must not contain nulls", exceptionFirst.getMessage());
	}

	@Test
	void createSameGroupMatrixWithDifferentColumnSize() {
		// Add an additional line to the matrix with less elements in the column.
		int numColumns = secureRandom.nextInt(this.numColumns);
		final List<TestSameGroupElement> lineWithSmallerColumn = generateElementList(numColumns, () -> new TestSameGroupElement(group));
		matrixElements.add(lineWithSmallerColumn);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> SameGroupMatrix.fromRows(matrixElements));
		assertEquals("All rows of the matrix must have the same number of columns.", exception.getMessage());
	}

	@Test
	void createSameGroupMatrixWithDifferentGroup() {
		final TestGroup otherGroup = new TestGroup();

		// Add an additional line to first matrix with elements from a different group.
		final List<TestSameGroupElement> differentGroupElements = generateElementList(numColumns, () -> new TestSameGroupElement(otherGroup));
		matrixElements.add(differentGroupElements);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> SameGroupMatrix.fromRows(matrixElements));
		assertEquals("All elements of the matrix must be in the same group.", exception.getMessage());
	}

	@Test
	void createSameGroupMatrixWithNoRows() {
		final List<List<TestSameGroupElement>> emptyMatrixElements = Collections.emptyList();
		SameGroupMatrix<TestSameGroupElement, TestGroup> emptyMatrix = SameGroupMatrix.fromRows(emptyMatrixElements);
		assertEquals(0, emptyMatrix.numRows());
		assertEquals(0, emptyMatrix.numColumns());
	}

	@Test
	void createSameGroupMatrixWithNoColumns() {
		final List<List<TestSameGroupElement>> emptyMatrixElements = generateElementMatrix(numRows, 0, () -> new TestSameGroupElement(group));
		SameGroupMatrix<TestSameGroupElement, TestGroup> emptyMatrix = SameGroupMatrix.fromRows(emptyMatrixElements);
		assertEquals(0, emptyMatrix.numRows());
		assertEquals(0, emptyMatrix.numColumns());
	}

	@RepeatedTest(10)
	void sizesAreCorrectForRandomMatrix() {
		int numRows = secureRandom.nextInt(BOUND_MATRIX_SIZE) + 1;
		int numColumns = secureRandom.nextInt(BOUND_MATRIX_SIZE) + 1;
		TestGroup group = new TestGroup();
		List<List<TestSameGroupElement>> matrixElements = generateElementMatrix(numRows, numColumns, () -> new TestSameGroupElement(group));
		SameGroupMatrix<TestSameGroupElement, TestGroup> matrix = SameGroupMatrix.fromRows(matrixElements);
		assertEquals(numRows, matrix.numRows());
		assertEquals(numColumns, matrix.numColumns());
	}

	@Test
	void isEmptyReturnTrueForNoRows() {
		List<List<TestSameGroupElement>> emptyMatrixElements = Collections.emptyList();
		SameGroupMatrix<TestSameGroupElement, TestGroup> emptyMatrix = SameGroupMatrix.fromRows(emptyMatrixElements);
		assertTrue(emptyMatrix.isEmpty());
	}

	@Test
	void isEmptyReturnTrueForNoColumns() {
		List<List<TestSameGroupElement>> emptyMatrixElements = Collections.singletonList(Collections.emptyList());
		SameGroupMatrix<TestSameGroupElement, TestGroup> emptyMatrix = SameGroupMatrix.fromRows(emptyMatrixElements);
		assertTrue(emptyMatrix.isEmpty());
	}

	@Test
	void getThrowsForIndexOutOfBounds() {
		int numRows = secureRandom.nextInt(BOUND_MATRIX_SIZE) + 1;
		int numColumns = secureRandom.nextInt(BOUND_MATRIX_SIZE) + 1;
		TestGroup group = new TestGroup();
		List<List<TestSameGroupElement>> matrixElements = generateElementMatrix(numRows, numColumns, () -> new TestSameGroupElement(group));
		SameGroupMatrix<TestSameGroupElement, TestGroup> matrix = SameGroupMatrix.fromRows(matrixElements);
		assertThrows(IllegalArgumentException.class, () -> matrix.get(-1, 0));
		assertThrows(IllegalArgumentException.class, () -> matrix.get(numRows, 0));
		assertThrows(IllegalArgumentException.class, () -> matrix.get(0, -1));
		assertThrows(IllegalArgumentException.class, () -> matrix.get(0, numColumns));
	}

	@RepeatedTest(10)
	void getReturnsExpectedElement() {
		int numRows = secureRandom.nextInt(BOUND_MATRIX_SIZE) + 1;
		int numColumns = secureRandom.nextInt(BOUND_MATRIX_SIZE) + 1;
		TestGroup group = new TestGroup();
		SameGroupMatrix<TestValuedElement, TestGroup> matrix = generateIncrementingMatrix(numRows, numColumns, group);
		int row = secureRandom.nextInt(numRows);
		int column = secureRandom.nextInt(numColumns);
		assertEquals(numColumns * row + column, matrix.get(row, column).getValue().intValueExact());
	}

	@RepeatedTest(10)
	void getRowReturnsExpectedRow() {
		int numRows = secureRandom.nextInt(BOUND_MATRIX_SIZE) + 1;
		int numColumns = secureRandom.nextInt(BOUND_MATRIX_SIZE) + 1;
		TestGroup group = new TestGroup();
		SameGroupMatrix<TestValuedElement, TestGroup> matrix = generateIncrementingMatrix(numRows, numColumns, group);
		int row = secureRandom.nextInt(numRows);
		List<TestValuedElement> expected = generateIncrementingRow(row * numColumns, numColumns, group);
		assertEquals(new SameGroupVector<>(expected), matrix.getRow(row));
	}

	@RepeatedTest(10)
	void getColumnReturnsExpectedColumn() {
		int numRows = secureRandom.nextInt(BOUND_MATRIX_SIZE) + 1;
		int numColumns = secureRandom.nextInt(BOUND_MATRIX_SIZE) + 1;
		TestGroup group = new TestGroup();
		SameGroupMatrix<TestValuedElement, TestGroup> matrix = generateIncrementingMatrix(numRows, numColumns, group);
		int column = secureRandom.nextInt(numColumns);
		SameGroupVector<TestValuedElement, TestGroup> expected = IntStream.range(0, numRows)
				.map(row -> row * numColumns + column)
				.mapToObj(value -> new TestValuedElement(BigInteger.valueOf(value), group))
				.collect(toSameGroupVector());
		assertEquals(expected, matrix.getColumn(column));
	}

	@RepeatedTest(10)
	void matrixFromColumnsIsMatrixFromRowsTransposed() {
		int n = secureRandom.nextInt(BOUND_MATRIX_SIZE);
		int m = secureRandom.nextInt(BOUND_MATRIX_SIZE);
		List<List<TestSameGroupElement>> rows = generateElementMatrix(n, m, () -> new TestSameGroupElement(group));
		SameGroupMatrix<TestSameGroupElement, TestGroup> expected = SameGroupMatrix.fromRows(rows);

		List<List<TestSameGroupElement>> columns =
				IntStream.range(0, m)
						.mapToObj(column ->
								rows.stream()
										.map(row -> row.get(column))
										.collect(Collectors.toList())
						).collect(Collectors.toList());
		SameGroupMatrix<TestSameGroupElement, TestGroup> actual = SameGroupMatrix.fromColumns(columns);

		assertEquals(expected, actual);
	}

	@RepeatedTest(10)
	void streamGivesElementsInCorrectOrder() {
		final int numRows = secureRandom.nextInt(BOUND_MATRIX_SIZE) + 1;
		final int numColumns = secureRandom.nextInt(BOUND_MATRIX_SIZE) + 1;
		final TestGroup group = new TestGroup();
		final List<List<TestSameGroupElement>> matrixElements = generateElementMatrix(numRows, numColumns, () -> new TestSameGroupElement(group));
		final SameGroupMatrix<TestSameGroupElement, TestGroup> matrix = SameGroupMatrix.fromRows(matrixElements);

		final int totalElements = numRows * numColumns;
		assertEquals(totalElements, matrix.stream().count());

		final List<TestSameGroupElement> flatMatrix = matrix.stream().collect(Collectors.toList());
		final int i = numRows - 1;
		final int j = numColumns - 1;
		// Index in new list is: i * numColumns + j
		assertEquals(matrix.get(0, 0), flatMatrix.get(0));
		assertEquals(matrix.get(0, j), flatMatrix.get(j));
		assertEquals(matrix.get(i, 0), flatMatrix.get(i * numColumns));
		assertEquals(matrix.get(i, j), flatMatrix.get(totalElements - 1));
	}

	@RepeatedTest(10)
	void rowStreamGivesRows() {
		final int numRows = secureRandom.nextInt(BOUND_MATRIX_SIZE) + 1;
		final int numColumns = secureRandom.nextInt(BOUND_MATRIX_SIZE) + 1;
		final TestGroup group = new TestGroup();
		final List<List<TestSameGroupElement>> matrixElements = generateElementMatrix(numRows, numColumns, () -> new TestSameGroupElement(group));
		final SameGroupMatrix<TestSameGroupElement, TestGroup> matrix = SameGroupMatrix.fromRows(matrixElements);

		assertEquals(numRows, matrix.rowStream().count());
		assertEquals(matrixElements.stream().map(SameGroupVector::new).collect(Collectors.toList()), matrix.rowStream().collect(Collectors.toList()));
	}

	@RepeatedTest(10)
	void columnStreamGivesColumns() {
		final int numRows = secureRandom.nextInt(BOUND_MATRIX_SIZE) + 1;
		final int numColumns = secureRandom.nextInt(BOUND_MATRIX_SIZE) + 1;
		final TestGroup group = new TestGroup();
		final List<List<TestSameGroupElement>> matrixElements = generateElementMatrix(numRows, numColumns, () -> new TestSameGroupElement(group));
		final SameGroupMatrix<TestSameGroupElement, TestGroup> matrix = SameGroupMatrix.fromRows(matrixElements);

		assertEquals(numColumns, matrix.columnStream().count());

		final List<List<TestSameGroupElement>> columnMatrixElements = IntStream.range(0, matrix.numColumns())
				.mapToObj(i -> matrixElements.stream().map(row -> row.get(i)).collect(Collectors.toList()))
				.collect(Collectors.toList());
		assertEquals(columnMatrixElements.stream().map(SameGroupVector::new).collect(Collectors.toList()),
				matrix.columnStream().collect(Collectors.toList()));
	}

	@Test
	void appendColumnWithInvalidParamsThrows() {
		final int numRows = secureRandom.nextInt(BOUND_MATRIX_SIZE) + 1;
		final int numColumns = secureRandom.nextInt(BOUND_MATRIX_SIZE) + 1;
		final TestGroup group = new TestGroup();
		final List<List<TestSameGroupElement>> matrixElements = generateElementMatrix(numRows, numColumns, () -> new TestSameGroupElement(group));
		final SameGroupMatrix<TestSameGroupElement, TestGroup> matrix = SameGroupMatrix.fromRows(matrixElements);

		assertThrows(NullPointerException.class, () -> matrix.appendColumn(null));

		final SameGroupVector<TestSameGroupElement, TestGroup> emptyVector = SameGroupVector.of();
		final IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
				() -> matrix.appendColumn(emptyVector));
		assertEquals(String.format("The new column size does not match size of matrix' columns. Size: %d, numRows: %d", 0, numRows),
				illegalArgumentException.getMessage());
	}

	@Test
	void appendColumnOfDifferentGroupThrows() {
		final int numRows = secureRandom.nextInt(BOUND_MATRIX_SIZE) + 1;
		final int numColumns = secureRandom.nextInt(BOUND_MATRIX_SIZE) + 1;
		final TestGroup group = new TestGroup();
		final List<List<TestSameGroupElement>> matrixElements = generateElementMatrix(numRows, numColumns, () -> new TestSameGroupElement(group));
		final SameGroupMatrix<TestSameGroupElement, TestGroup> matrix = SameGroupMatrix.fromRows(matrixElements);

		final TestGroup differentTestGroup = new TestGroup();
		final SameGroupVector<TestSameGroupElement, TestGroup> newCol = new SameGroupVector<>(
				generateElementList(numRows, () -> new TestSameGroupElement(differentTestGroup)));

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> matrix.appendColumn(newCol));
		assertEquals("The group of the new column must be equal to the matrix' group", exception.getMessage());
	}

	@RepeatedTest(10)
	void appendColumnCorrectlyAppends() {
		final int numRows = secureRandom.nextInt(BOUND_MATRIX_SIZE) + 1;
		final int numColumns = secureRandom.nextInt(BOUND_MATRIX_SIZE) + 1;
		final TestGroup group = new TestGroup();
		final List<List<TestSameGroupElement>> matrixElements = generateElementMatrix(numRows, numColumns, () -> new TestSameGroupElement(group));
		final SameGroupMatrix<TestSameGroupElement, TestGroup> matrix = SameGroupMatrix.fromRows(matrixElements);

		final SameGroupVector<TestSameGroupElement, TestGroup> newCol = new SameGroupVector<>(
				generateElementList(numRows, () -> new TestSameGroupElement(group)));
		final SameGroupMatrix<TestSameGroupElement, TestGroup> augmentedMatrix = matrix.appendColumn(newCol);

		assertEquals(numColumns + 1, augmentedMatrix.numColumns());
		assertEquals(newCol, augmentedMatrix.getColumn(numColumns));
	}

	@Test
	void prependColumnWithInvalidParamsThrows() {
		final int numRows = secureRandom.nextInt(BOUND_MATRIX_SIZE) + 1;
		final int numColumns = secureRandom.nextInt(BOUND_MATRIX_SIZE) + 1;
		final TestGroup group = new TestGroup();
		final List<List<TestSameGroupElement>> matrixElements = generateElementMatrix(numRows, numColumns, () -> new TestSameGroupElement(group));
		final SameGroupMatrix<TestSameGroupElement, TestGroup> matrix = SameGroupMatrix.fromRows(matrixElements);

		assertThrows(NullPointerException.class, () -> matrix.prependColumn(null));

		final SameGroupVector<TestSameGroupElement, TestGroup> emptyVector = SameGroupVector.of();
		final IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
				() -> matrix.prependColumn(emptyVector));
		assertEquals(String.format("The new column size does not match size of matrix' columns. Size: %d, numRows: %d", 0, numRows),
				illegalArgumentException.getMessage());
	}

	@Test
	void prependColumnOfDifferentGroupThrows() {
		final int numRows = secureRandom.nextInt(BOUND_MATRIX_SIZE) + 1;
		final int numColumns = secureRandom.nextInt(BOUND_MATRIX_SIZE) + 1;
		final TestGroup group = new TestGroup();
		final List<List<TestSameGroupElement>> matrixElements = generateElementMatrix(numRows, numColumns, () -> new TestSameGroupElement(group));
		final SameGroupMatrix<TestSameGroupElement, TestGroup> matrix = SameGroupMatrix.fromRows(matrixElements);

		final TestGroup differentTestGroup = new TestGroup();
		final SameGroupVector<TestSameGroupElement, TestGroup> newCol = new SameGroupVector<>(
				generateElementList(numRows, () -> new TestSameGroupElement(differentTestGroup)));

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> matrix.prependColumn(newCol));
		assertEquals("The group of the new column must be equal to the matrix' group", exception.getMessage());
	}

	@RepeatedTest(10)
	void prependColumnCorrectlyPrepends() {
		final int numRows = secureRandom.nextInt(BOUND_MATRIX_SIZE) + 1;
		final int numColumns = secureRandom.nextInt(BOUND_MATRIX_SIZE) + 1;
		final TestGroup group = new TestGroup();
		final List<List<TestSameGroupElement>> matrixElements = generateElementMatrix(numRows, numColumns, () -> new TestSameGroupElement(group));
		final SameGroupMatrix<TestSameGroupElement, TestGroup> matrix = SameGroupMatrix.fromRows(matrixElements);

		final SameGroupVector<TestSameGroupElement, TestGroup> newCol = new SameGroupVector<>(
				generateElementList(numRows, () -> new TestSameGroupElement(group)));
		final SameGroupMatrix<TestSameGroupElement, TestGroup> augmentedMatrix = matrix.prependColumn(newCol);

		assertEquals(numColumns + 1, augmentedMatrix.numColumns());
		assertEquals(newCol, augmentedMatrix.getColumn(0));
	}

	//***************************//
	// Utilities //
	//***************************//

	//Generate a matrix with incrementing count.
	private SameGroupMatrix<TestValuedElement, TestGroup> generateIncrementingMatrix(int numRows, int numColumns, TestGroup group) {
		List<List<TestValuedElement>> matrixElements =
				IntStream.range(0, numRows)
						.mapToObj(row -> generateIncrementingRow(numColumns * row, numColumns, group))
						.collect(Collectors.toList());
		return SameGroupMatrix.fromRows(matrixElements);
	}

	//Generate a row with incrementing number starting at start.
	private List<TestValuedElement> generateIncrementingRow(int start, int numColumns, TestGroup group) {
		return IntStream.range(0, numColumns)
				.map(column -> start + column)
				.mapToObj(BigInteger::valueOf)
				.map(value -> new TestValuedElement(value, group))
				.collect(Collectors.toList());
	}

	private static class TestValuedElement extends GroupElement<TestGroup> {
		protected TestValuedElement(BigInteger value, TestGroup group) {
			super(value, group);
		}
	}
}
