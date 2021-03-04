/*
 * HEADER_LICENSE_OPEN_SOURCE
 */
package ch.post.it.evoting.cryptoprimitives.mixnet;

import static ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext.getCiphertext;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.google.common.collect.ImmutableList;

import ch.post.it.evoting.cryptoprimitives.SameGroupVector;
import ch.post.it.evoting.cryptoprimitives.TestGroupSetup;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientMessage;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.random.Permutation;
import ch.post.it.evoting.cryptoprimitives.random.PermutationService;
import ch.post.it.evoting.cryptoprimitives.random.RandomService;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.Generators;

@DisplayName("A ShuffleArgumentService")
class ShuffleArgumentServiceTest extends TestGroupSetup {

	private static final int KEY_ELEMENTS_NUMBER = 11;
	private static final RandomService randomService = new RandomService();
	private static final SecureRandom secureRandom = new SecureRandom();
	private static final PermutationService permutationService = new PermutationService(randomService);

	private static ElGamalGenerator elGamalGenerator;
	private static CommitmentKeyGenerator commitmentKeyGenerator;
	private static MixnetHashService hashService;

	@BeforeAll
	static void setUpAll() {
		elGamalGenerator = new ElGamalGenerator(gqGroup);
		commitmentKeyGenerator = new CommitmentKeyGenerator(gqGroup);
		hashService = TestHashService.create(BigInteger.ZERO, gqGroup.getQ());
	}

	@Nested
	@DisplayName("constructed with")
	class ConstructorTest {

		private int k;
		private ElGamalMultiRecipientPublicKey publicKey;
		private CommitmentKey commitmentKey;

		@BeforeEach
		void setUp() {
			k = secureRandom.nextInt(KEY_ELEMENTS_NUMBER - 2) + 2;

			publicKey = elGamalGenerator.genRandomPublicKey(k);
			commitmentKey = commitmentKeyGenerator.genCommitmentKey(k);
		}

		@Test
		@DisplayName("valid parameters does not throw")
		void construct() {
			assertDoesNotThrow(() -> new ShuffleArgumentService(publicKey, commitmentKey, randomService, hashService));
		}

		@Test
		@DisplayName("any null parameter throws NullPointerException")
		void constructNullParams() {
			assertAll(
					() -> assertThrows(NullPointerException.class, () -> new ShuffleArgumentService(null, commitmentKey, randomService, hashService)),
					() -> assertThrows(NullPointerException.class, () -> new ShuffleArgumentService(publicKey, null, randomService, hashService)),
					() -> assertThrows(NullPointerException.class, () -> new ShuffleArgumentService(publicKey, commitmentKey, null, hashService)),
					() -> assertThrows(NullPointerException.class, () -> new ShuffleArgumentService(publicKey, commitmentKey, randomService, null))
			);
		}

		@Test
		@DisplayName("public and commitments keys from different group throws IllegalArgumentException")
		void constructPublicCommitmentKeysDiffGroup() {
			final ElGamalMultiRecipientPublicKey otherPublicKey = new ElGamalGenerator(otherGqGroup).genRandomPublicKey(k);

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> new ShuffleArgumentService(otherPublicKey, commitmentKey, randomService, hashService));
			assertEquals("The public key and commitment key must belong to the same group.", exception.getMessage());
		}

		@Test
		@DisplayName("public and commitment keys of different size throws IllegalArgumentException")
		void constructPublicCommitmentKeysDiffSize() {
			final ElGamalMultiRecipientPublicKey longerPublicKey = elGamalGenerator.genRandomPublicKey(k + 1);

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> new ShuffleArgumentService(longerPublicKey, commitmentKey, randomService, hashService));
			assertEquals("The commitment key and public key must be of the same size.", exception.getMessage());
		}
	}

	@Nested
	@DisplayName("calling getShuffleArgument with")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class GetShuffleArgumentTest {

		private ElGamalMultiRecipientPublicKey publicKey;
		private ShuffleArgumentService shuffleArgumentService;

		private ShuffleStatement shuffleStatement;
		private ShuffleWitness shuffleWitness;
		private int m;
		private int n;
		private int N;
		private int l;

		@BeforeAll
		void setUpAll() {
			publicKey = elGamalGenerator.genRandomPublicKey(KEY_ELEMENTS_NUMBER);

			final CommitmentKey commitmentKey = commitmentKeyGenerator.genCommitmentKey(KEY_ELEMENTS_NUMBER);

			shuffleArgumentService = new ShuffleArgumentService(publicKey, commitmentKey, randomService, hashService);
		}

		@BeforeEach
		void setUp() {
			// Because the test groups are small.
			do {
				m = secureRandom.nextInt(KEY_ELEMENTS_NUMBER - 1) + 1;
				n = secureRandom.nextInt(KEY_ELEMENTS_NUMBER - 2) + 2;
			} while (BigInteger.valueOf((long) m * n).compareTo(zqGroup.getQ()) >= 0);
			N = m * n;

			// Create a witness.
			final Permutation permutation = permutationService.genPermutation(N);
			final SameGroupVector<ZqElement, ZqGroup> randomness = zqGroupGenerator.genRandomZqElementVector(N);

			shuffleWitness = new ShuffleWitness(permutation, randomness);

			// Create the corresponding statement.
			l = secureRandom.nextInt(KEY_ELEMENTS_NUMBER - 1) + 1;
			final SameGroupVector<ElGamalMultiRecipientCiphertext, GqGroup> ciphertexts = elGamalGenerator.genRandomCiphertextVector(N, l);

			final ElGamalMultiRecipientMessage ones = ElGamalMultiRecipientMessage.ones(gqGroup, l);
			final SameGroupVector<ElGamalMultiRecipientCiphertext, GqGroup> shuffledCiphertexts = IntStream.range(0, N)
					.mapToObj(i -> getCiphertext(ones, randomness.get(i), publicKey)
							.multiply(ciphertexts.get(permutation.get(i))))
					.collect(collectingAndThen(toList(), SameGroupVector::from));

			shuffleStatement = new ShuffleStatement(ciphertexts, shuffledCiphertexts);
		}

		@Test
		@DisplayName("valid parameters does not throw")
		void getShuffleArgumentTest() {
			assertDoesNotThrow(() -> shuffleArgumentService.getShuffleArgument(shuffleStatement, shuffleWitness, m, n));
		}

		@Test
		@DisplayName("any null parameter throws NullPointerException")
		void getShuffleArgumentNullParams() {
			assertThrows(NullPointerException.class, () -> shuffleArgumentService.getShuffleArgument(null, shuffleWitness, m, n));
			assertThrows(NullPointerException.class, () -> shuffleArgumentService.getShuffleArgument(shuffleStatement, null, m, n));
		}

		@Test
		@DisplayName("invalid number of rows or columns throws IllegalArgumentException")
		void getShuffleArgumentInvalidRowsCols() {
			final IllegalArgumentException rowsIllegalArgumentException = assertThrows(IllegalArgumentException.class,
					() -> shuffleArgumentService.getShuffleArgument(shuffleStatement, shuffleWitness, 0, n));
			assertEquals("The number of rows for the ciphertext matrices must be strictly positive.", rowsIllegalArgumentException.getMessage());

			final IllegalArgumentException columnsIllegalArgumentException = assertThrows(IllegalArgumentException.class,
					() -> shuffleArgumentService.getShuffleArgument(shuffleStatement, shuffleWitness, m, 0));
			assertEquals("The number of columns for the ciphertext matrices must be strictly positive.",
					columnsIllegalArgumentException.getMessage());
		}

		@Test
		@DisplayName("ciphertexts and permutation of different size throws IllegalArgumentException")
		void getShuffleArgumentCiphertextsPermutationDiffSize() {
			final Permutation longerPermutation = permutationService.genPermutation(N + 1);
			final SameGroupVector<ZqElement, ZqGroup> longerRandomness = zqGroupGenerator.genRandomZqElementVector(N + 1);
			final ShuffleWitness longerShuffleWitness = new ShuffleWitness(longerPermutation, longerRandomness);

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> shuffleArgumentService.getShuffleArgument(shuffleStatement, longerShuffleWitness, m, n));
			assertEquals("The statement ciphertexts must have the same size as the permutation.", exception.getMessage());
		}

		@Test
		@DisplayName("ciphertexts and randomness having a different group order throws IllegalArgumentException")
		void getShuffleArgumentCiphertextsRandomnessDiffOrder() {
			final SameGroupVector<ZqElement, ZqGroup> differentRandomness = otherZqGroupGenerator.genRandomZqElementVector(N);
			final ShuffleWitness differentShuffleWitness = new ShuffleWitness(this.shuffleWitness.getPermutation(), differentRandomness);

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> shuffleArgumentService.getShuffleArgument(shuffleStatement, differentShuffleWitness, m, n));
			assertEquals("The randomness group must have the order of the ciphertexts group.", exception.getMessage());
		}

		@Test
		@DisplayName("ciphertexts longer than public key throws IllegalArgumentException")
		void getShuffleArgumentCiphertextsLongerThanPublicKey() {
			final int biggerL = KEY_ELEMENTS_NUMBER + 1;
			final ElGamalMultiRecipientPublicKey longerPublicKey = elGamalGenerator.genRandomPublicKey(biggerL);
			final SameGroupVector<ElGamalMultiRecipientCiphertext, GqGroup> longerCiphertexts = elGamalGenerator
					.genRandomCiphertextVector(N, biggerL);

			final ElGamalMultiRecipientMessage ones = ElGamalMultiRecipientMessage.ones(gqGroup, biggerL);
			final SameGroupVector<ElGamalMultiRecipientCiphertext, GqGroup> shuffledCiphertexts = IntStream.range(0, N)
					.mapToObj(i -> getCiphertext(ones, shuffleWitness.getRandomness().get(i), longerPublicKey)
							.multiply(longerCiphertexts.get(shuffleWitness.getPermutation().get(i))))
					.collect(collectingAndThen(toList(), SameGroupVector::from));

			final ShuffleStatement longerCiphertextsStatement = new ShuffleStatement(longerCiphertexts, shuffledCiphertexts);

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> shuffleArgumentService.getShuffleArgument(longerCiphertextsStatement, shuffleWitness, m, n));
			assertEquals("The ciphertexts must be smaller than the public key.", exception.getMessage());
		}

		@Test
		@DisplayName("re-encrypted and shuffled ciphertexts C different C' throws IllegalArgumentException")
		void getShuffleArgumentCiphertextsShuffledCiphertextsDiff() {
			// Modify the shuffled ciphertexts by replacing its first element by a different ciphertext.
			final List<ElGamalMultiRecipientCiphertext> shuffledCiphertexts = new ArrayList<>(shuffleStatement.getShuffledCiphertexts());
			final ElGamalMultiRecipientCiphertext first = shuffledCiphertexts.get(0);
			final ElGamalMultiRecipientCiphertext otherFirst = Generators.genWhile(() -> elGamalGenerator.genRandomCiphertext(l), first::equals);
			shuffledCiphertexts.set(0, otherFirst);

			// Recreate shuffled ciphertexts and statement.
			final SameGroupVector<ElGamalMultiRecipientCiphertext, GqGroup> differentShuffledCiphertexts = SameGroupVector.from(
					shuffledCiphertexts);
			final ShuffleStatement differentShuffleStatement = new ShuffleStatement(this.shuffleStatement.getCiphertexts(),
					differentShuffledCiphertexts);

			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> shuffleArgumentService.getShuffleArgument(differentShuffleStatement, shuffleWitness, m, n));
			assertEquals(
					"The shuffled ciphertexts provided in the statement do not correspond to the re-encryption and shuffle of C under pi and rho.",
					exception.getMessage());
		}

		@Test
		@DisplayName("ciphertexts vectors not decomposable into matrices throws IllegalArgumentException")
		void getShuffleArgumentNotDecomposableCiphertexts() {
			final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
					() -> shuffleArgumentService.getShuffleArgument(shuffleStatement, shuffleWitness, m + 1, n));
			assertEquals(String.format("The ciphertexts vectors must be decomposable into m * n matrices: %d != %d * %d.", N, m + 1, n),
					exception.getMessage());
		}

		@Test
		@DisplayName("specific values returns the expected result")
		void getShuffleArgumentWithSpecificValues() {
			// Create groups
			BigInteger p = BigInteger.valueOf(23);
			BigInteger q = BigInteger.valueOf(11);
			BigInteger g = BigInteger.valueOf(2);
			GqGroup gqGroup = new GqGroup(p, q, g);
			ZqGroup zqGroup = new ZqGroup(q);

			// Create BigIntegers
			BigInteger ZERO = BigInteger.ZERO;
			BigInteger ONE = BigInteger.ONE;
			BigInteger TWO = BigInteger.valueOf(2);
			BigInteger THREE = BigInteger.valueOf(3);
			BigInteger FOUR = BigInteger.valueOf(4);
			BigInteger FIVE = BigInteger.valueOf(5);
			BigInteger SIX = BigInteger.valueOf(6);
			BigInteger SEVEN = BigInteger.valueOf(7);
			BigInteger EIGHT = BigInteger.valueOf(8);
			BigInteger NINE = BigInteger.valueOf(9);
			BigInteger TEN = BigInteger.valueOf(10);

			// Create GqElements
			GqElement gOne = gqGroup.getIdentity();
			GqElement gTwo = gqGroup.getGenerator();
			GqElement gThree = GqElement.create(THREE, gqGroup);
			GqElement gFour = GqElement.create(FOUR, gqGroup);
			GqElement gSix = GqElement.create(SIX, gqGroup);
			GqElement gEight = GqElement.create(EIGHT, gqGroup);
			GqElement gNine = GqElement.create(NINE, gqGroup);
			GqElement gTwelve = GqElement.create(BigInteger.valueOf(12), gqGroup);
			GqElement gThirteen = GqElement.create(BigInteger.valueOf(13), gqGroup);
			GqElement gSixteen = GqElement.create(BigInteger.valueOf(16), gqGroup);
			GqElement gEighteen = GqElement.create(BigInteger.valueOf(18), gqGroup);

			// Create ZqElements
			ZqElement zZero = ZqElement.create(ZERO, zqGroup);
			ZqElement zOne = ZqElement.create(ONE, zqGroup);
			ZqElement zTwo = ZqElement.create(TWO, zqGroup);
			ZqElement zThree = ZqElement.create(THREE, zqGroup);
			ZqElement zFour = ZqElement.create(FOUR, zqGroup);
			ZqElement zFive = ZqElement.create(FIVE, zqGroup);
			ZqElement zSix = ZqElement.create(SIX, zqGroup);
			ZqElement zSeven = ZqElement.create(SEVEN, zqGroup);
			ZqElement zEight = ZqElement.create(EIGHT, zqGroup);
			ZqElement zNine = ZqElement.create(NINE, zqGroup);
			ZqElement zTen = ZqElement.create(TEN, zqGroup);

			// Create the public key: pk = (8, 13, 4)
			ElGamalMultiRecipientPublicKey publicKey = new ElGamalMultiRecipientPublicKey(Arrays.asList(gEight, gThirteen, gFour));

			// Create the ciphertexts
			ElGamalMultiRecipientMessage m0 = new ElGamalMultiRecipientMessage(ImmutableList.of(gFour, gEight, gThree));
			ElGamalMultiRecipientMessage m1 = new ElGamalMultiRecipientMessage(ImmutableList.of(gThree, gSix, gFour));
			ElGamalMultiRecipientMessage m2 = new ElGamalMultiRecipientMessage(ImmutableList.of(gSixteen, gTwo, gNine));
			ElGamalMultiRecipientMessage m3 = new ElGamalMultiRecipientMessage(ImmutableList.of(gThirteen, gFour, gEighteen));

			ElGamalMultiRecipientCiphertext c0 = ElGamalMultiRecipientCiphertext.getCiphertext(m0, zFive, publicKey);
			ElGamalMultiRecipientCiphertext c1 = ElGamalMultiRecipientCiphertext.getCiphertext(m1, zSeven, publicKey);
			ElGamalMultiRecipientCiphertext c2 = ElGamalMultiRecipientCiphertext.getCiphertext(m2, zTen, publicKey);
			ElGamalMultiRecipientCiphertext c3 = ElGamalMultiRecipientCiphertext.getCiphertext(m3, zTwo, publicKey);
			// Create the vector of ciphertexts:
			// C = ({9, (18, 9, 13)}, {13, (13, 8, 9)}, {12, (2, 9, 8)}, {4, (4, 9, 12)})
			SameGroupVector<ElGamalMultiRecipientCiphertext, GqGroup> c = SameGroupVector.of(c0, c1, c2, c3);

			RandomService permutationRandomService = mock(RandomService.class);
			when(permutationRandomService.genRandomInteger(any()))
					.thenReturn(BigInteger.ONE, BigInteger.valueOf(2), BigInteger.ZERO, BigInteger.ZERO);
			// Create the permutation: pi = [1, 3, 2, 0]
			Permutation permutation = new PermutationService(permutationRandomService).genPermutation(4);
			// Create the randomness: rho = (4, 9, 3, 2)
			SameGroupVector<ZqElement, ZqGroup> rho = SameGroupVector.of(zFour, zNine, zThree, zTwo);

			ElGamalMultiRecipientMessage ones = ElGamalMultiRecipientMessage.ones(gqGroup, 3);
			// Create the vector of shuffled ciphertexts:
			// C' = ({1, (3, 6, 4)}, {1, (13, 4, 18)}, {4, (12, 16, 6)}, {13, (2, 3, 1)})
			SameGroupVector<ElGamalMultiRecipientCiphertext, GqGroup> cPrime = IntStream.range(0, 4)
					.mapToObj(i -> ElGamalMultiRecipientCiphertext.getCiphertext(ones, rho.get(i), publicKey).multiply(c.get(permutation.get(i))))
					.collect(SameGroupVector.toSameGroupVector());

			// Create the ShuffleArgumentService
			// Create the commitment key: ck = {3, (6, 13, 12)}
			CommitmentKey commitmentKey = new CommitmentKey(gThree, ImmutableList.of(gSix, gThirteen, gTwelve));
			RandomService shuffleRandomService = spy(new RandomService());
			// Shuffle: r = (3, 5), s = (7, 8)
			// Product: s = 10
			// Zero: a0 = (2, 5), bm = (1, 4), r0 = 7, sm = 3, t = (6, 2, 4, 5, 8)
			// Single: d = (4, 9), rd = 0, s0 = 1, sx = 7
			// Multi: a0 = (0, 1), r0 = 6, b = (2, 3, 7, 9), s = (10, 1, 3, 4), tau = (5, 6, 8, 7)
			doReturn(THREE, FIVE, SEVEN, EIGHT,
					TEN,
					TWO, FIVE, ONE, FOUR, SEVEN, THREE, SIX, TWO, FOUR, FIVE, EIGHT,
					FOUR, NINE, ZERO, ONE, SEVEN,
					ZERO, ONE, SIX, TWO, THREE, SEVEN, NINE, TEN, ONE, THREE, FOUR, FIVE, SIX, EIGHT, SEVEN)
					.when(shuffleRandomService).genRandomInteger(q);
			MixnetHashService shuffleHashService = TestHashService.create(BigInteger.ZERO, gqGroup.getQ());
			ShuffleArgumentService shuffleArgumentService = new ShuffleArgumentService(publicKey, commitmentKey,
					shuffleRandomService, shuffleHashService);

			// Create the statement and the witness
			ShuffleStatement statement = new ShuffleStatement(c, cPrime);
			ShuffleWitness witness = new ShuffleWitness(permutation, rho);
			ShuffleArgument actual = shuffleArgumentService.getShuffleArgument(statement, witness, 2, 2);

			// Create the expected ZeroArgument
			ZeroArgument zeroArgument = new ZeroArgument.Builder()
					.withCA0(gTwelve)
					.withCBm(gEighteen)
					.withCd(SameGroupVector.of(gEighteen, gFour, gThirteen, gOne, gFour))
					.withAPrime(SameGroupVector.of(zEight, zEight))
					.withBPrime(SameGroupVector.of(zSix, zThree))
					.withRPrime(zSeven)
					.withSPrime(zZero)
					.withTPrime(zFive)
					.build();

			// Create the expected HadamardArgument
			SameGroupVector<GqElement, GqGroup> cBhadamard = SameGroupVector.of(gSixteen, gNine);
			HadamardArgument hadamardArgument = new HadamardArgument(cBhadamard, zeroArgument);

			// Create the expected SingleValueProductArgument
			SingleValueProductArgument singleValueProductArgument = new SingleValueProductArgument.Builder()
					.withCd(gOne)
					.withCLowerDelta(gEight)
					.withCUpperDelta(gOne)
					.withATilde(SameGroupVector.of(zEight, zFive))
					.withBTilde(SameGroupVector.of(zEight, zSeven))
					.withRTilde(zSeven)
					.withSTilde(zSeven)
					.build();

			// Create the expected ProductArgument:
			// cb = 9
			// Hadamard: cB = (16, 9), Zero: cA0 = 12, cBm = 18, cd = (18, 4, 13, 1, 4), a' = (8, 8), b' = (6, 3), r' = 7, s' = 0, t' = 5
			// Single: cd = 1, cδ = 8, cΔ = 1, aTilde = (8, 5), bTilde = (8, 7), rTilde = 7, sTilde = 7
			ProductArgument productArgument = new ProductArgument(gNine, hadamardArgument, singleValueProductArgument);

			SameGroupVector<GqElement, GqGroup> cBmulti = SameGroupVector.of(gTwelve, gFour, gOne, gEight);
			SameGroupVector<ElGamalMultiRecipientCiphertext, GqGroup> eVector = SameGroupVector.of(
					ElGamalMultiRecipientCiphertext.create(gTwo, Arrays.asList(gThirteen, gTwo, gTwo)),
					ElGamalMultiRecipientCiphertext.create(gNine, Arrays.asList(gEighteen, gEighteen, gSix)),
					ElGamalMultiRecipientCiphertext.create(gNine, Arrays.asList(gFour, gThirteen, gOne)),
					ElGamalMultiRecipientCiphertext.create(gSix, Arrays.asList(gEight, gThree, gSix))
			);

			// Create the expected MultiExponentiationArgument:
			// cA0 = 1, cB = (12, 4, 1, 8), E = ({2, (13, 2, 2)}, {9, (18, 18, 6)}, {9, (4, 13, 1)}, {6, (8, 3, 6)})
			// a = (2, 4), r = 7, b = 1, s = 5, tau = 5
			MultiExponentiationArgument multiExponentiationArgument = new MultiExponentiationArgument.Builder()
					.withcA0(gOne)
					.withcBVector(cBmulti)
					.withEVector(eVector)
					.withaVector(SameGroupVector.of(zTwo, zFour))
					.withr(zSeven)
					.withb(zOne)
					.withs(zFive)
					.withtau(zFive)
					.build();

			// Create the expected output:
			// cA = (8, 2), cB = (8, 18)
			SameGroupVector<GqElement, GqGroup> cAshuffle = SameGroupVector.of(gEight, gTwo);
			SameGroupVector<GqElement, GqGroup> cBshuffle = SameGroupVector.of(gEight, gEighteen);

			ShuffleArgument expected = new ShuffleArgument.Builder()
					.withCA(cAshuffle)
					.withCB(cBshuffle)
					.withProductArgument(productArgument)
					.withMultiExponentiationArgument(multiExponentiationArgument)
					.build();

			assertEquals(expected, actual);
		}
	}

}