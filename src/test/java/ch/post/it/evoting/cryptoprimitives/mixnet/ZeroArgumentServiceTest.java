/*
 * HEADER_LICENSE_OPEN_SOURCE
 */
package ch.post.it.evoting.cryptoprimitives.mixnet;

import static ch.post.it.evoting.cryptoprimitives.SameGroupVector.toSameGroupVector;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ch.post.it.evoting.cryptoprimitives.HashService;
import ch.post.it.evoting.cryptoprimitives.SameGroupMatrix;
import ch.post.it.evoting.cryptoprimitives.SameGroupVector;
import ch.post.it.evoting.cryptoprimitives.TestGroupSetup;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.random.RandomService;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.Generators;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ZqGroupGenerator;
import ch.post.it.evoting.cryptoprimitives.test.tools.serialization.JsonData;
import ch.post.it.evoting.cryptoprimitives.test.tools.serialization.TestParameters;

@DisplayName("A ZeroArgumentService")
class ZeroArgumentServiceTest extends TestGroupSetup {

	private static final BigInteger ZERO = BigInteger.valueOf(0);
	private static final BigInteger ONE = BigInteger.valueOf(1);
	private static final BigInteger TWO = BigInteger.valueOf(2);
	private static final BigInteger THREE = BigInteger.valueOf(3);
	private static final BigInteger FOUR = BigInteger.valueOf(4);
	private static final BigInteger FIVE = BigInteger.valueOf(5);
	private static final BigInteger SIX = BigInteger.valueOf(6);
	private static final BigInteger SEVEN = BigInteger.valueOf(7);
	private static final BigInteger EIGHT = BigInteger.valueOf(8);
	private static final BigInteger NINE = BigInteger.valueOf(9);
	private static final BigInteger TEN = BigInteger.valueOf(10);
	private static final BigInteger ELEVEN = BigInteger.valueOf(11);
	private static final int KEY_ELEMENTS_NUMBER = 10;
	private static final SecureRandom secureRandom = new SecureRandom();

	private static ZeroArgumentService zeroArgumentService;
	private static CommitmentKey commitmentKey;
	private static ElGamalMultiRecipientPublicKey publicKey;
	private static ElGamalGenerator elGamalGenerator;
	private static RandomService randomService;
	private static MixnetHashService hashService;

	@BeforeAll
	static void setUpAll() throws Exception {
		// Generate publicKey and commitmentKey.
		final CommitmentKeyGenerator commitmentKeyGenerator = new CommitmentKeyGenerator(gqGroup);
		commitmentKey = commitmentKeyGenerator.genCommitmentKey(KEY_ELEMENTS_NUMBER);

		elGamalGenerator = new ElGamalGenerator(gqGroup);
		publicKey = elGamalGenerator.genRandomPublicKey(KEY_ELEMENTS_NUMBER);

		// Init services.
		randomService = new RandomService();
		hashService = TestHashService.create(BigInteger.ZERO, gqGroup.getQ());

		zeroArgumentService = new ZeroArgumentService(publicKey, commitmentKey, randomService, hashService);
	}

	@Test
	@DisplayName("constructed with any null parameter throws NullPointerException")
	void constructNullParams() {
		assertAll(
				() -> assertThrows(NullPointerException.class,
						() -> new ZeroArgumentService(null, commitmentKey, randomService, hashService)),
				() -> assertThrows(NullPointerException.class,
						() -> new ZeroArgumentService(publicKey, null, randomService, hashService)),
				() -> assertThrows(NullPointerException.class,
						() -> new ZeroArgumentService(publicKey, commitmentKey, null, hashService)),
				() -> assertThrows(NullPointerException.class,
						() -> new ZeroArgumentService(publicKey, commitmentKey, randomService, null))
		);
	}

	@Test
	@DisplayName("constructed with keys from different groups throws IllegalArgumentException")
	void constructDiffGroupKeys() {
		// Create public key from other group.
		final ElGamalMultiRecipientPublicKey otherPublicKey = new ElGamalGenerator(otherGqGroup).genRandomPublicKey(KEY_ELEMENTS_NUMBER);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new ZeroArgumentService(otherPublicKey, commitmentKey, randomService, hashService));
		assertEquals("The public and commitment keys are not from the same group.", exception.getMessage());
	}

	@Nested
	@DisplayName("computeDVector")
	class ComputeDVectorTest {

		private static final int RANDOM_UPPER_BOUND = 10;

		private int n;
		private int m;
		private SameGroupMatrix<ZqElement, ZqGroup> firstMatrix;
		private SameGroupMatrix<ZqElement, ZqGroup> secondMatrix;
		private ZqElement y;

		@BeforeEach
		void setUp() {
			n = secureRandom.nextInt(RANDOM_UPPER_BOUND) + 1;
			m = secureRandom.nextInt(RANDOM_UPPER_BOUND) + 1;
			firstMatrix = zqGroupGenerator.genRandomZqElementMatrix(n, m + 1);
			secondMatrix = zqGroupGenerator.genRandomZqElementMatrix(n, m + 1);
			y = ZqElement.create(randomService.genRandomInteger(zqGroup.getQ()), zqGroup);
		}

		@Test
		@DisplayName("with any null parameter throws NullPointerException")
		void computeDVectorNullParams() {
			final SameGroupMatrix<ZqElement, ZqGroup> emptyMatrix = SameGroupMatrix.fromRows(Collections.emptyList());

			assertAll(
					() -> assertThrows(NullPointerException.class, () -> zeroArgumentService.computeDVector(null, secondMatrix, y)),
					() -> assertThrows(NullPointerException.class, () -> zeroArgumentService.computeDVector(firstMatrix, null, y)),
					() -> assertThrows(NullPointerException.class, () -> zeroArgumentService.computeDVector(firstMatrix, secondMatrix, null)),
					() -> assertThrows(NullPointerException.class, () -> zeroArgumentService.computeDVector(null, emptyMatrix, y)),
					() -> assertThrows(NullPointerException.class, () -> zeroArgumentService.computeDVector(emptyMatrix, null, y)),
					() -> assertThrows(NullPointerException.class, () -> zeroArgumentService.computeDVector(emptyMatrix, emptyMatrix, null))
			);
		}

		@Test
		@DisplayName("with matrices having unequal number of rows throws IllegalArgumentException")
		void computeDVectorDifferentSizeLines() {
			// Generate a first matrix with an additional row.
			final SameGroupMatrix<ZqElement, ZqGroup> otherMatrix = zqGroupGenerator.genRandomZqElementMatrix(n + 1, m + 1);

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> zeroArgumentService.computeDVector(otherMatrix, secondMatrix, y));
			assertEquals("The two matrices must have the same number of rows.", exception.getMessage());

			// With empty matrices.
			final SameGroupMatrix<ZqElement, ZqGroup> emptyMatrix = SameGroupMatrix.fromRows(Collections.emptyList());
			final IllegalArgumentException exceptionSecondEmpty = assertThrows(IllegalArgumentException.class,
					() -> zeroArgumentService.computeDVector(otherMatrix, emptyMatrix, y));
			assertEquals("The two matrices must have the same number of rows.", exceptionSecondEmpty.getMessage());

			final IllegalArgumentException exceptionFirstEmpty = assertThrows(IllegalArgumentException.class,
					() -> zeroArgumentService.computeDVector(emptyMatrix, secondMatrix, y));
			assertEquals("The two matrices must have the same number of rows.", exceptionFirstEmpty.getMessage());
		}

		@Test
		@DisplayName("with matrices with different number of columns throws IllegalArgumentException")
		void computeDVectorDifferentSizeColumns() {
			final SameGroupMatrix<ZqElement, ZqGroup> otherFirstMatrix = zqGroupGenerator.genRandomZqElementMatrix(n, m);
			final SameGroupMatrix<ZqElement, ZqGroup> otherSecondMatrix = zqGroupGenerator.genRandomZqElementMatrix(n, m + 1);

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> zeroArgumentService.computeDVector(otherFirstMatrix, otherSecondMatrix, y));
			assertEquals("The two matrices must have the same number of columns.", exception.getMessage());
		}

		@Test
		@DisplayName("with second matrix having elements of different group than the first matrix throws IllegalArgumentException")
		void computeDVectorMatricesDifferentGroup() {
			// Get a second matrix in a different ZqGroup.
			final SameGroupMatrix<ZqElement, ZqGroup> differentGroupSecondMatrix = otherZqGroupGenerator.genRandomZqElementMatrix(n, m + 1);

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> zeroArgumentService.computeDVector(firstMatrix, differentGroupSecondMatrix, y));
			assertEquals("The elements of both matrices must be in the same group.", exception.getMessage());
		}

		@Test
		@DisplayName("with y from a different group throws IllegalArgumentException")
		void computeDVectorYDifferentGroup() {
			// Create a y value from a different group.
			final ZqElement differentGroupY = ZqElement.create(randomService.genRandomInteger(otherZqGroup.getQ()), otherZqGroup);

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> zeroArgumentService.computeDVector(firstMatrix, secondMatrix, differentGroupY));
			assertEquals("The value y must be in the same group as the elements of the matrices.", exception.getMessage());
		}

		@RepeatedTest(100)
		@DisplayName("with random values gives expected d vector length")
		void computeDVectorTest() {
			assertEquals(2 * m + 1, zeroArgumentService.computeDVector(firstMatrix, secondMatrix, y).size());
		}

		@Test
		@DisplayName("with empty matrices gives empty result vector")
		void computeDVectorEmptyMatrices() {
			final SameGroupVector<ZqElement, ZqGroup> emptyD = SameGroupVector.of();
			final SameGroupMatrix<ZqElement, ZqGroup> firstEmptyMatrix = SameGroupMatrix.fromRows(Collections.emptyList());
			final SameGroupMatrix<ZqElement, ZqGroup> secondEmptyMatrix = SameGroupMatrix.fromRows(Collections.emptyList());

			assertEquals(emptyD, zeroArgumentService.computeDVector(firstEmptyMatrix, secondEmptyMatrix, y));
		}

		@Test
		@DisplayName("with matrices with empty columns gives empty result vector")
		void computeDVectorEmptyColumns() {
			final SameGroupVector<ZqElement, ZqGroup> emptyD = SameGroupVector.of();
			final SameGroupMatrix<ZqElement, ZqGroup> firstEmptyMatrix = SameGroupMatrix.fromColumns(Collections.emptyList());
			final SameGroupMatrix<ZqElement, ZqGroup> secondEmptyMatrix = SameGroupMatrix.fromColumns(Collections.emptyList());

			assertEquals(emptyD, zeroArgumentService.computeDVector(firstEmptyMatrix, secondEmptyMatrix, y));
		}

		@Test
		@DisplayName("with simple values gives expected result")
		void computeDVectorSimpleValuesTest() {
			// Small Zq group.
			final ZqGroup group = new ZqGroup(ELEVEN);

			// Construct the two matrices and value y.
			final List<ZqElement> a0 = asList(ZqElement.create(ZERO, group), ZqElement.create(TWO, group));
			final List<ZqElement> a1 = asList(ZqElement.create(FOUR, group), ZqElement.create(SIX, group));
			final List<ZqElement> b0 = asList(ZqElement.create(ONE, group), ZqElement.create(THREE, group));
			final List<ZqElement> b1 = asList(ZqElement.create(FIVE, group), ZqElement.create(SEVEN, group));
			final SameGroupMatrix<ZqElement, ZqGroup> firstMatrix = SameGroupMatrix.fromRows(asList(a0, a1));
			final SameGroupMatrix<ZqElement, ZqGroup> secondMatrix = SameGroupMatrix.fromRows(asList(b0, b1));
			final ZqElement y = ZqElement.create(EIGHT, group);

			// Expected d vector.
			final SameGroupVector<ZqElement, ZqGroup> expected = SameGroupVector.of(
					ZqElement.create(TEN, group), ZqElement.create(ONE, group), ZqElement.create(ZERO, group));

			assertEquals(expected, zeroArgumentService.computeDVector(firstMatrix, secondMatrix, y));
		}
	}

	@Nested
	@DisplayName("starMap")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class StarMapTest {

		private static final int RANDOM_UPPER_BOUND = 10;

		private int n;
		private SameGroupVector<ZqElement, ZqGroup> firstVector;
		private SameGroupVector<ZqElement, ZqGroup> secondVector;
		private ZqElement y;

		@BeforeEach
		void setUp() {
			n = secureRandom.nextInt(RANDOM_UPPER_BOUND) + 1;
			firstVector = zqGroupGenerator.genRandomZqElementVector(n);
			secondVector = zqGroupGenerator.genRandomZqElementVector(n);
			y = ZqElement.create(randomService.genRandomInteger(zqGroup.getQ()), zqGroup);
		}

		@Test
		@DisplayName("with any null parameter throws NullPointerException")
		void starMapNullParams() {
			final SameGroupVector<ZqElement, ZqGroup> emptyVector = SameGroupVector.of();

			assertAll(
					() -> assertThrows(NullPointerException.class, () -> zeroArgumentService.starMap(null, secondVector, y)),
					() -> assertThrows(NullPointerException.class, () -> zeroArgumentService.starMap(firstVector, null, y)),
					() -> assertThrows(NullPointerException.class, () -> zeroArgumentService.starMap(firstVector, secondVector, null)),
					() -> assertThrows(NullPointerException.class, () -> zeroArgumentService.starMap(null, emptyVector, y)),
					() -> assertThrows(NullPointerException.class, () -> zeroArgumentService.starMap(emptyVector, null, y)),
					() -> assertThrows(NullPointerException.class, () -> zeroArgumentService.starMap(emptyVector, emptyVector, null))
			);
		}

		@Test
		@DisplayName("with vectors of different size throws IllegalArgumentException")
		void starMapVectorsDifferentSize() {
			final SameGroupVector<ZqElement, ZqGroup> secondVector = zqGroupGenerator.genRandomZqElementVector(n + 1);

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> zeroArgumentService.starMap(firstVector, secondVector, y));
			assertEquals("The provided vectors must have the same size.", exception.getMessage());

			// With empty vectors.
			final SameGroupVector<ZqElement, ZqGroup> emptyVector = SameGroupVector.of();
			final IllegalArgumentException exceptionSecondEmpty = assertThrows(IllegalArgumentException.class,
					() -> zeroArgumentService.starMap(firstVector, emptyVector, y));
			assertEquals("The provided vectors must have the same size.", exceptionSecondEmpty.getMessage());

			final IllegalArgumentException exceptionFirstEmpty = assertThrows(IllegalArgumentException.class,
					() -> zeroArgumentService.starMap(emptyVector, secondVector, y));
			assertEquals("The provided vectors must have the same size.", exceptionFirstEmpty.getMessage());
		}

		@Test
		@DisplayName("with second vector elements of different group than the first vector throws IllegalArgumentException")
		void starMapVectorsDifferentGroup() {
			// Second vector from different group.
			final SameGroupVector<ZqElement, ZqGroup> secondVector = otherZqGroupGenerator.genRandomZqElementVector(n);

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> zeroArgumentService.starMap(firstVector, secondVector, y));
			assertEquals("The elements of both vectors must be in the same group.", exception.getMessage());
		}

		@Test
		@DisplayName("constructed with value y from different group throws IllegalArgumentException")
		void starMapYDifferentGroup() {
			// Get another y from a different group.
			final ZqElement differentGroupY = otherZqGroupGenerator.genRandomZqElementMember();

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> zeroArgumentService.starMap(firstVector, secondVector, differentGroupY));
			assertEquals("The value y must be in the same group as the vectors elements", exception.getMessage());
		}

		@Test
		@DisplayName("with empty vectors returns identity")
		void starMapEmptyVectors() {
			final SameGroupVector<ZqElement, ZqGroup> firstVector = SameGroupVector.of();
			final SameGroupVector<ZqElement, ZqGroup> secondVector = SameGroupVector.of();

			assertEquals(zqGroup.getIdentity(), zeroArgumentService.starMap(firstVector, secondVector, y));
		}

		@Test
		@DisplayName("with simple values gives expected result")
		void starMapTestSimpleValues() {
			// Small ZpGroup.
			final ZqGroup group = new ZqGroup(ELEVEN);

			// Construct the two vectors and value y.
			final SameGroupVector<ZqElement, ZqGroup> firstVector = SameGroupVector.of(
					ZqElement.create(TWO, group), ZqElement.create(SIX, group));
			final SameGroupVector<ZqElement, ZqGroup> secondVector = SameGroupVector.of(
					ZqElement.create(THREE, group), ZqElement.create(SEVEN, group));
			final ZqElement y = ZqElement.create(EIGHT, group);

			// Expected starMap result.
			final ZqElement expected = ZqElement.create(EIGHT, group);

			assertEquals(expected, zeroArgumentService.starMap(firstVector, secondVector, y));
		}

		@ParameterizedTest
		@MethodSource("starMapRealValuesProvider")
		@DisplayName("with real values gives expected result")
		void starMapRealValues(final SameGroupVector<ZqElement, ZqGroup> firstVector, final SameGroupVector<ZqElement, ZqGroup> secondVector,
				final ZqElement y, final ZqElement expectedOutput, final String description) {

			final ZqElement actualOutput = zeroArgumentService.starMap(firstVector, secondVector, y);

			assertEquals(actualOutput, expectedOutput, String.format("assertion failed for: %s", description));
		}

		Stream<Arguments> starMapRealValuesProvider() {
			final List<TestParameters> parametersList = TestParameters.fromResource("/mixnet/bilinearMap.json");

			return parametersList.stream().parallel().map(testParameters -> {
				// Context.
				final JsonData context = testParameters.getContext();
				final BigInteger q = context.get("q", BigInteger.class);

				final ZqGroup zqGroup = new ZqGroup(q);

				// Inputs.
				final JsonData input = testParameters.getInput();

				final BigInteger[] aVector = input.get("a", BigInteger[].class);
				final SameGroupVector<ZqElement, ZqGroup> firstVector = Arrays.stream(aVector)
						.map(bi -> ZqElement.create(bi, zqGroup))
						.collect(toSameGroupVector());

				final BigInteger[] bVector = input.get("b", BigInteger[].class);
				final SameGroupVector<ZqElement, ZqGroup> secondVector = Arrays.stream(bVector)
						.map(bi -> ZqElement.create(bi, zqGroup))
						.collect(toSameGroupVector());

				final BigInteger yValue = input.get("y", BigInteger.class);
				final ZqElement y = ZqElement.create(yValue, zqGroup);

				// Output.
				final JsonData output = testParameters.getOutput();
				final BigInteger outputValue = output.get("value", BigInteger.class);
				final ZqElement expectedOutput = ZqElement.create(outputValue, zqGroup);

				return Arguments.of(firstVector, secondVector, y, expectedOutput, testParameters.getDescription());
			});
		}
	}

	@Nested
	@DisplayName("getZeroArgument")
	class GetZeroArgument {

		private int m;
		private int n;
		private ZeroStatement zeroStatement;
		private ZeroWitness zeroWitness;

		@BeforeEach
		void setUp() {
			ZeroArgumentService zeroArgumentService = new ZeroArgumentService(publicKey, commitmentKey, randomService, hashService);
			ZeroArgumentTestData testData = new ZeroArgumentTestData(commitmentKey, zeroArgumentService);
			zeroStatement = testData.getZeroStatement();
			zeroWitness = testData.getZeroWitness();
			m = testData.getM();
			n = testData.getN();
		}

		@Test
		@DisplayName("with valid statement and witness does not throw")
		void getZeroArgValidStatementAndWitness() {
			ZeroArgumentService zeroArgumentService = new ZeroArgumentService(publicKey, commitmentKey, randomService, hashService);
			ZeroArgumentTestData testData = new ZeroArgumentTestData(commitmentKey, zeroArgumentService);
			ZeroStatement zeroStatement = testData.getZeroStatement();
			ZeroWitness zeroWitness = testData.getZeroWitness();

			final ZeroArgumentService otherZeroArgumentService = testData.getZeroArgumentService();

			assertDoesNotThrow(() -> otherZeroArgumentService.getZeroArgument(zeroStatement, zeroWitness));
		}

		@Test
		@DisplayName("with any null parameter throws NullPointerException")
		void getZeroArgNullParams() {
			assertThrows(NullPointerException.class, () -> zeroArgumentService.getZeroArgument(null, zeroWitness));
			assertThrows(NullPointerException.class, () -> zeroArgumentService.getZeroArgument(zeroStatement, null));
		}

		@Test
		@DisplayName("with commitments and exponents of different size throws IllegalArgumentException ")
		void getZeroArgDiffComExp() {
			// Create another witness with an additional element.
			final SameGroupMatrix<ZqElement, ZqGroup> matrixA = zqGroupGenerator.genRandomZqElementMatrix(n, m + 1);
			final SameGroupMatrix<ZqElement, ZqGroup> matrixB = zqGroupGenerator.genRandomZqElementMatrix(n, m + 1);
			final SameGroupVector<ZqElement, ZqGroup> exponentsR = zqGroupGenerator.genRandomZqElementVector(m + 1);
			final SameGroupVector<ZqElement, ZqGroup> exponentsS = zqGroupGenerator.genRandomZqElementVector(m + 1);

			final ZeroWitness addElemZeroWitness = new ZeroWitness(matrixA, matrixB, exponentsR, exponentsS);

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> zeroArgumentService.getZeroArgument(zeroStatement, addElemZeroWitness));
			assertEquals("The statement commitments must have the same size as the witness exponents.", exception.getMessage());
		}

		@Test
		@DisplayName("with y and exponents of different group throws IllegalArgumentException")
		void getZeroArgDiffGroupYAndExponents() {
			// Create another witness in another group.
			final ZqGroupGenerator otherZqGroupGenerator = new ZqGroupGenerator(GroupTestData.getDifferentZqGroup(zqGroup));
			final SameGroupMatrix<ZqElement, ZqGroup> matrixA = otherZqGroupGenerator.genRandomZqElementMatrix(n, m);
			final SameGroupMatrix<ZqElement, ZqGroup> matrixB = otherZqGroupGenerator.genRandomZqElementMatrix(n, m);
			final SameGroupVector<ZqElement, ZqGroup> exponentsR = otherZqGroupGenerator.genRandomZqElementVector(m);
			final SameGroupVector<ZqElement, ZqGroup> exponentsS = otherZqGroupGenerator.genRandomZqElementVector(m);

			final ZeroWitness otherZqGroupZeroWitness = new ZeroWitness(matrixA, matrixB, exponentsR, exponentsS);

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> zeroArgumentService.getZeroArgument(zeroStatement, otherZqGroupZeroWitness));
			assertEquals("The statement y and witness exponents must be part of the same group.", exception.getMessage());

		}

		@Test
		@DisplayName("with Ca commitments not equal to commitment matrix of A throws IllegalArgumentException")
		void getZeroArgDiffCaCommitments() {
			final SameGroupVector<GqElement, GqGroup> commitmentsA = zeroStatement.getCommitmentsA();

			// Generate a different commitment.
			final SameGroupVector<GqElement, GqGroup> otherCommitments = Generators
					.genWhile(() -> gqGroupGenerator.genRandomGqElementVector(m), commitments -> commitments.equals(commitmentsA));

			final SameGroupVector<GqElement, GqGroup> commitmentsB = zeroStatement.getCommitmentsB();
			final ZeroStatement otherStatement = new ZeroStatement(otherCommitments, commitmentsB, zeroStatement.getY());

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> zeroArgumentService.getZeroArgument(otherStatement, zeroWitness));
			assertEquals("The statement's Ca commitments must be equal to the witness' commitment matrix A.", exception.getMessage());
		}

		@Test
		@DisplayName("with Cb commitments not equal to commitment matrix of B throws IllegalArgumentException")
		void getZeroArgDiffCbCommitments() {
			final SameGroupVector<GqElement, GqGroup> commitmentsB = zeroStatement.getCommitmentsB();

			// Generate a different commitment.
			SameGroupVector<GqElement, GqGroup> otherCommitments = Generators
					.genWhile(() -> gqGroupGenerator.genRandomGqElementVector(m), commitments -> commitments.equals(commitmentsB));

			final SameGroupVector<GqElement, GqGroup> commitmentsA = zeroStatement.getCommitmentsA();
			final ZeroStatement otherStatement = new ZeroStatement(commitmentsA, otherCommitments, zeroStatement.getY());

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> zeroArgumentService.getZeroArgument(otherStatement, zeroWitness));
			assertEquals("The statement's Cb commitments must be equal to the witness' commitment matrix B.", exception.getMessage());
		}

		@Test
		@DisplayName("with starMap sum not equal to 0 throws IllegalArgumentException")
		void getZeroArgStarMapNotZero() {
			// Create a simple witness.
			final SameGroupMatrix<ZqElement, ZqGroup> matrixA = SameGroupMatrix
					.fromRows(Collections.singletonList(Collections.singletonList(ZqElement.create(ONE, zqGroup))));
			final SameGroupMatrix<ZqElement, ZqGroup> matrixB = SameGroupMatrix
					.fromRows(Collections.singletonList(Collections.singletonList(ZqElement.create(ONE, zqGroup))));
			final SameGroupVector<ZqElement, ZqGroup> exponentsR = SameGroupVector.of(ZqElement.create(ONE, zqGroup));
			final SameGroupVector<ZqElement, ZqGroup> exponentsS = SameGroupVector.of(ZqElement.create(ONE, zqGroup));
			final ZeroWitness otherWitness = new ZeroWitness(matrixA, matrixB, exponentsR, exponentsS);

			// Derive statement from it.
			final SameGroupVector<GqElement, GqGroup> commitmentsA = CommitmentService
					.getCommitmentMatrix(matrixA, exponentsR, commitmentKey);
			final SameGroupVector<GqElement, GqGroup> commitmentsB = CommitmentService
					.getCommitmentMatrix(matrixB, exponentsS, commitmentKey);
			// Fix y to 1 so the starMap gives 1 (because A and B are 1).
			final ZqElement y = ZqElement.create(ONE, zqGroup);
			final ZeroStatement otherStatement = new ZeroStatement(commitmentsA, commitmentsB, y);

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> zeroArgumentService.getZeroArgument(otherStatement, otherWitness));
			assertEquals("The sum of the starMap operations between the witness's matrices columns is not equal to 0.", exception.getMessage());
		}

		@Test
		@DisplayName("with simple values gives expected result")
		void getZeroArgSimpleValues() {
			// Groups.
			final GqGroup simpleGqGroup = new GqGroup(ELEVEN, FIVE, THREE);
			final ZqGroup simpleZqGroup = ZqGroup.sameOrderAs(simpleGqGroup);

			// Statement.
			final SameGroupVector<GqElement, GqGroup> commitmentsA = SameGroupVector.of(
					GqElement.create(FIVE, simpleGqGroup), GqElement.create(THREE, simpleGqGroup), GqElement.create(FOUR, simpleGqGroup));
			final SameGroupVector<GqElement, GqGroup> commitmentsB = SameGroupVector.of(
					GqElement.create(FOUR, simpleGqGroup), GqElement.create(NINE, simpleGqGroup), GqElement.create(NINE, simpleGqGroup));
			final ZqElement y = ZqElement.create(TWO, simpleZqGroup);

			final ZeroStatement simpleZeroStatement = new ZeroStatement(commitmentsA, commitmentsB, y);

			// Witness.
			final SameGroupMatrix<ZqElement, ZqGroup> simpleMatrixA = SameGroupMatrix.fromRows(asList(
					asList(ZqElement.create(TWO, simpleZqGroup), ZqElement.create(ZERO, simpleZqGroup), ZqElement.create(FOUR, simpleZqGroup)),
					asList(ZqElement.create(TWO, simpleZqGroup), ZqElement.create(FOUR, simpleZqGroup), ZqElement.create(FOUR, simpleZqGroup))));
			final SameGroupMatrix<ZqElement, ZqGroup> simpleMatrixB = SameGroupMatrix.fromRows(asList(
					asList(ZqElement.create(THREE, simpleZqGroup), ZqElement.create(TWO, simpleZqGroup), ZqElement.create(ONE, simpleZqGroup)),
					asList(ZqElement.create(ZERO, simpleZqGroup), ZqElement.create(ZERO, simpleZqGroup), ZqElement.create(ZERO, simpleZqGroup))));
			final SameGroupVector<ZqElement, ZqGroup> simpleExponentsR = SameGroupVector.of(
					ZqElement.create(THREE, simpleZqGroup), ZqElement.create(FOUR, simpleZqGroup), ZqElement.create(ZERO, simpleZqGroup));
			final SameGroupVector<ZqElement, ZqGroup> simpleExponentsS = SameGroupVector.of(
					ZqElement.create(ONE, simpleZqGroup), ZqElement.create(TWO, simpleZqGroup), ZqElement.create(FOUR, simpleZqGroup));

			final ZeroWitness simpleZeroWitness = new ZeroWitness(simpleMatrixA, simpleMatrixB, simpleExponentsR, simpleExponentsS);

			// Argument.
			final GqElement cA0 = GqElement.create(FIVE, simpleGqGroup);
			final GqElement cBm = GqElement.create(ONE, simpleGqGroup);
			final SameGroupVector<GqElement, GqGroup> cd = SameGroupVector.of(
					GqElement.create(FOUR, simpleGqGroup), GqElement.create(FOUR, simpleGqGroup), GqElement.create(NINE, simpleGqGroup),
					GqElement.create(NINE, simpleGqGroup), GqElement.create(ONE, simpleGqGroup), GqElement.create(THREE, simpleGqGroup),
					GqElement.create(ONE, simpleGqGroup));
			final SameGroupVector<ZqElement, ZqGroup> aPrime = SameGroupVector.of(
					ZqElement.create(TWO, simpleZqGroup), ZqElement.create(ZERO, simpleZqGroup));
			final SameGroupVector<ZqElement, ZqGroup> bPrime = SameGroupVector.of(
					ZqElement.create(ONE, simpleZqGroup), ZqElement.create(ONE, simpleZqGroup));
			final ZqElement rPrime = ZqElement.create(ONE, simpleZqGroup);
			final ZqElement sPrime = ZqElement.create(FOUR, simpleZqGroup);
			final ZqElement tPrime = ZqElement.create(ONE, simpleZqGroup);

			final ZeroArgument.Builder zeroArgumentBuilder = new ZeroArgument.Builder();
			zeroArgumentBuilder
					.withCA0(cA0)
					.withCBm(cBm)
					.withCd(cd)
					.withAPrime(aPrime)
					.withBPrime(bPrime)
					.withRPrime(rPrime)
					.withSPrime(sPrime)
					.withTPrime(tPrime);
			final ZeroArgument simpleZeroArgument = zeroArgumentBuilder.build();

			// PublicKey and commitmentKey.
			final GqElement h = GqElement.create(NINE, simpleGqGroup);
			final List<GqElement> g = asList(GqElement.create(FOUR, simpleGqGroup), GqElement.create(NINE, simpleGqGroup));
			final CommitmentKey simpleCommitmentKey = new CommitmentKey(h, g);

			final List<GqElement> pkElements = asList(GqElement.create(FOUR, simpleGqGroup), GqElement.create(FOUR, simpleGqGroup));
			final ElGamalMultiRecipientPublicKey simplePublicKey = new ElGamalMultiRecipientPublicKey(pkElements);

			// Mock random elements. There are 13 values to mock:
			// a0=(1,3) bm=(2,1) r0=4 sm=0 t=(0,1,3,4,2,1,2)
			final RandomService randomServiceMock = mock(RandomService.class);
			doReturn(ONE, THREE, TWO, ONE, FOUR, ZERO, ZERO, ONE, THREE, FOUR, TWO, ONE, TWO).when(randomServiceMock)
					.genRandomInteger(simpleZqGroup.getQ());

			// Mock the hashService.
			final MixnetHashService hashServiceMock = TestHashService.create(BigInteger.ZERO, simpleGqGroup.getQ());

			final ZeroArgumentService simpleZeroArgumentService = new ZeroArgumentService(simplePublicKey, simpleCommitmentKey, randomServiceMock,
					hashServiceMock);

			// Verification.
			final ZeroArgument zeroArgument = simpleZeroArgumentService.getZeroArgument(simpleZeroStatement, simpleZeroWitness);
			verify(randomServiceMock, times(13)).genRandomInteger(simpleZqGroup.getQ());

			assertEquals(simpleZeroArgument, zeroArgument);
		}
	}

	@Nested
	@DisplayName("VerifyZeroArgument")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class VerifyZeroArgument {

		@RepeatedTest(10)
		void verifyZeroArgumentTest() {
			ZeroArgumentService zeroArgumentService = new ZeroArgumentService(publicKey, commitmentKey, randomService, hashService);
			ZeroArgumentTestData testData = new ZeroArgumentTestData(commitmentKey, zeroArgumentService);
			ZeroArgumentService verifyZeroArgumentService = testData.getZeroArgumentService();
			ZeroStatement statement = testData.getZeroStatement();
			ZeroWitness witness = testData.getZeroWitness();

			ZeroArgument zeroArgument = verifyZeroArgumentService.getZeroArgument(statement, witness);

			assertTrue(verifyZeroArgumentService.verifyZeroArgument(statement, zeroArgument));
		}

		@Test
		void testNullInputParameters() {
			ZeroArgument zeroArgument = mock(ZeroArgument.class);
			ZeroStatement zeroStatement = mock(ZeroStatement.class);

			assertThrows(NullPointerException.class, () -> zeroArgumentService.verifyZeroArgument(zeroStatement, null));
			assertThrows(NullPointerException.class, () -> zeroArgumentService.verifyZeroArgument(null, zeroArgument));
		}

		@Test
		void testInputParameterGroupSizes() {
			ZeroArgument zeroArgument = mock(ZeroArgument.class, Mockito.RETURNS_DEEP_STUBS);
			ZeroStatement zeroStatement = mock(ZeroStatement.class, Mockito.RETURNS_DEEP_STUBS);

			when(zeroArgument.getCd().getGroup()).thenReturn(gqGroup);
			when(zeroStatement.getCommitmentsA().getGroup()).thenReturn(gqGroup);

			when(zeroArgument.getCd().size()).thenReturn(1);
			when(zeroStatement.getCommitmentsA().size()).thenReturn(2);

			IllegalArgumentException invalidMException = assertThrows(IllegalArgumentException.class,
					() -> zeroArgumentService.verifyZeroArgument(zeroStatement, zeroArgument));
			assertEquals("The m of the statement should be equal to the m of the argument (2m+1)", invalidMException.getMessage());

		}

		@Test
		void testInputParameterGroupMembership() {
			ZeroArgument zeroArgument = mock(ZeroArgument.class, Mockito.RETURNS_DEEP_STUBS);
			ZeroStatement otherGroupStatement = mock(ZeroStatement.class, Mockito.RETURNS_DEEP_STUBS);

			when(zeroArgument.getCd().getGroup()).thenReturn(gqGroup);
			when(otherGroupStatement.getCommitmentsA().getGroup()).thenReturn(GroupTestData.getDifferentGqGroup(gqGroup));

			IllegalArgumentException wrongGroupException = assertThrows(IllegalArgumentException.class,
					() -> zeroArgumentService.verifyZeroArgument(otherGroupStatement, zeroArgument));
			assertEquals("Statement and argument do not share the same group", wrongGroupException.getMessage());

		}

		@ParameterizedTest
		@MethodSource("verifyZeroArgumentRealValuesProvider")
		@DisplayName("with real values gives expected result")
		void verifyZeroArgumentRealValues(final ElGamalMultiRecipientPublicKey publicKey, final CommitmentKey commitmentKey,
				final ZeroStatement zeroStatement, final ZeroArgument zeroArgument, final boolean expectedOutput, String description)
				throws NoSuchAlgorithmException {
			HashService hashService = new HashService(MessageDigest.getInstance("SHA-256"));
			MixnetHashService mixnetHashService = new MixnetHashService(hashService, publicKey.getGroup().getQ().bitLength());
			final ZeroArgumentService service = new ZeroArgumentService(publicKey, commitmentKey, randomService, mixnetHashService);
			assertEquals(expectedOutput, service.verifyZeroArgument(zeroStatement, zeroArgument),
					String.format("assertion failed for: %s", description));
		}

		Stream<Arguments> verifyZeroArgumentRealValuesProvider() {
			final List<TestParameters> parametersList = TestParameters.fromResource("/mixnet/verify-za-argument.json");

			return parametersList.stream().parallel().map(testParameters -> {
				// Context.
				final JsonData context = testParameters.getContext();

				final BigInteger p = context.get("p", BigInteger.class);
				final BigInteger q = context.get("q", BigInteger.class);
				final BigInteger g = context.get("g", BigInteger.class);

				final GqGroup gqGroup = new GqGroup(p, q, g);
				final ZqGroup zqGroup = new ZqGroup(q);

				final BigInteger[] pkValues = context.get("pk", BigInteger[].class);
				final List<GqElement> keyElements = Arrays.stream(pkValues)
						.map(bi -> GqElement.create(bi, gqGroup))
						.collect(toList());
				final ElGamalMultiRecipientPublicKey publicKey = new ElGamalMultiRecipientPublicKey(keyElements);

				final BigInteger hValue = context.getJsonData("ck").get("h", BigInteger.class);
				final BigInteger[] gValues = context.getJsonData("ck").get("g", BigInteger[].class);
				final GqElement h = GqElement.create(hValue, gqGroup);
				final List<GqElement> gElements = Arrays.stream(gValues)
						.map(bi -> GqElement.create(bi, gqGroup))
						.collect(toList());
				final CommitmentKey commitmentKey = new CommitmentKey(h, gElements);

				// Inputs.
				final JsonData input = testParameters.getInput();
				ZeroStatement zeroStatement = parseZeroStatement(gqGroup, zqGroup, input);
				ZeroArgument zeroArgument = parseZeroArgument(gqGroup, zqGroup, input);

				// Output.
				final JsonData output = testParameters.getOutput();
				final boolean outputValue = output.get("verif_result", Boolean.class);

				return Arguments.of(publicKey, commitmentKey, zeroStatement, zeroArgument, outputValue, testParameters.getDescription());
			});
		}

		private ZeroStatement parseZeroStatement(GqGroup gqGroup, ZqGroup zqGroup, JsonData input) {
			final JsonData zeroStatementJsonData = input.getJsonData("statement");
			final BigInteger[] cAValues = zeroStatementJsonData.get("c_a", BigInteger[].class);
			final BigInteger[] cBValues = zeroStatementJsonData.get("c_b", BigInteger[].class);
			final BigInteger yValue = zeroStatementJsonData.get("y", BigInteger.class);

			final SameGroupVector<GqElement, GqGroup> cA = Arrays.stream(cAValues)
					.map(bi -> GqElement.create(bi, gqGroup))
					.collect(toSameGroupVector());
			final SameGroupVector<GqElement, GqGroup> cB = Arrays.stream(cBValues)
					.map(bi -> GqElement.create(bi, gqGroup))
					.collect(toSameGroupVector());
			final ZqElement y = ZqElement.create(yValue, zqGroup);

			return new ZeroStatement(cA, cB, y);
		}

		private ZeroArgument parseZeroArgument(GqGroup gqGroup, ZqGroup zqGroup, JsonData input) {
			final JsonData zeroArgumentJsonData = input.getJsonData("argument");
			final BigInteger cA0Value = zeroArgumentJsonData.get("c_a0", BigInteger.class);
			final BigInteger cBmValue = zeroArgumentJsonData.get("c_bm", BigInteger.class);
			final BigInteger[] cdValues = zeroArgumentJsonData.get("c_d", BigInteger[].class);
			final BigInteger[] aValues = zeroArgumentJsonData.get("a", BigInteger[].class);
			final BigInteger[] bValues = zeroArgumentJsonData.get("b", BigInteger[].class);
			final BigInteger rValue = zeroArgumentJsonData.get("r", BigInteger.class);
			final BigInteger sValue = zeroArgumentJsonData.get("s", BigInteger.class);
			final BigInteger tValue = zeroArgumentJsonData.get("t", BigInteger.class);

			final GqElement cA0 = GqElement.create(cA0Value, gqGroup);
			final GqElement cBm = GqElement.create(cBmValue, gqGroup);
			final SameGroupVector<GqElement, GqGroup> cd = Arrays.stream(cdValues)
					.map(bi -> GqElement.create(bi, gqGroup))
					.collect(toSameGroupVector());
			final SameGroupVector<ZqElement, ZqGroup> aPrime = Arrays.stream(aValues)
					.map(bi -> ZqElement.create(bi, zqGroup))
					.collect(toSameGroupVector());
			final SameGroupVector<ZqElement, ZqGroup> bPrime = Arrays.stream(bValues)
					.map(bi -> ZqElement.create(bi, zqGroup))
					.collect(toSameGroupVector());
			final ZqElement r = ZqElement.create(rValue, zqGroup);
			final ZqElement s = ZqElement.create(sValue, zqGroup);
			final ZqElement t = ZqElement.create(tValue, zqGroup);

			return new ZeroArgument.Builder()
					.withCA0(cA0)
					.withCBm(cBm)
					.withCd(cd)
					.withAPrime(aPrime)
					.withBPrime(bPrime)
					.withRPrime(r)
					.withSPrime(s)
					.withTPrime(t)
					.build();
		}

	}
}
