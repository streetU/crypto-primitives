/*
 * Copyright 2021 Post CH Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

import ch.post.it.evoting.cryptoprimitives.GroupVector;
import ch.post.it.evoting.cryptoprimitives.TestGroupSetup;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.hashing.TestHashService;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.RandomService;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;

class ZeroKnowledgeProofServiceTest extends TestGroupSetup {

	private static final SecureRandom random = new SecureRandom();
	private static final RandomService randomService = new RandomService();

	private ZeroKnowledgeProofService zeroKnowledgeProofservice;
	private ElGamalGenerator elGamalGenerator;

	private int numCiphertexts;
	private int keyLength;
	private int ciphertextLength;
	private List<ElGamalMultiRecipientCiphertext> ciphertexts;
	private ElGamalMultiRecipientKeyPair keyPair;
	private List<String> auxiliaryInformation;

	@BeforeEach
	void setup() {
		TestHashService boundedHashService = TestHashService.create(gqGroup.getQ());
		zeroKnowledgeProofservice = new ZeroKnowledgeProofService(randomService, boundedHashService);
		elGamalGenerator = new ElGamalGenerator(gqGroup);

		final int maxLength = 10;
		numCiphertexts = random.nextInt(maxLength) + 1;
		keyLength = random.nextInt(maxLength) + 1;
		ciphertextLength = random.nextInt(keyLength) + 1;
		ciphertexts = new ArrayList<>(elGamalGenerator.genRandomCiphertextVector(numCiphertexts, ciphertextLength));
		keyPair = ElGamalMultiRecipientKeyPair.genKeyPair(gqGroup, keyLength, randomService);
		auxiliaryInformation = Arrays.asList("a", "b");
	}

	@Test
	@DisplayName("Generating verifiable messages with null arguments throws a NullPointerException")
	void genVerifiableMessagesWithNullArguments() {
		assertThrows(NullPointerException.class, () -> zeroKnowledgeProofservice.genVerifiableDecryptions(null, keyPair, auxiliaryInformation));
		assertThrows(NullPointerException.class, () -> zeroKnowledgeProofservice.genVerifiableDecryptions(ciphertexts, null, auxiliaryInformation));
		assertThrows(NullPointerException.class, () -> zeroKnowledgeProofservice.genVerifiableDecryptions(ciphertexts, keyPair, null));
	}

	@Test
	@DisplayName("Generating verifiable messages with valid arguments does not throw")
	void genVerifiableMessagesWithValidArguments() {
		assertDoesNotThrow(() -> zeroKnowledgeProofservice.genVerifiableDecryptions(ciphertexts, keyPair, ImmutableList.of()));
		assertDoesNotThrow(() -> zeroKnowledgeProofservice.genVerifiableDecryptions(ciphertexts, keyPair, auxiliaryInformation));
	}

	@Test
	@DisplayName("Generating verifiable messages with too long ciphertexts throws an IllegalArgumentException")
	void genVerifiableMessagesWithTooLongCiphertexts() {
		ciphertexts = elGamalGenerator.genRandomCiphertextVector(numCiphertexts, keyLength + 1);
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> zeroKnowledgeProofservice.genVerifiableDecryptions(ciphertexts, keyPair, auxiliaryInformation));
		assertEquals("The ciphertexts must be at most as long as the keys in the key pair.", exception.getMessage());
	}

	@Test
	@DisplayName("Generating verifiable messages with ciphertexts and keys from different groups throws an IllegalArgumentException")
	void genVerifiableMessagesWithIncompatibleGroups() {
		ciphertexts = new ElGamalGenerator(otherGqGroup).genRandomCiphertextVector(numCiphertexts, ciphertextLength);
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> zeroKnowledgeProofservice.genVerifiableDecryptions(ciphertexts, keyPair, auxiliaryInformation));
		assertEquals("The ciphertexts and the key pair must have the same group.", exception.getMessage());
	}

	@Test
	@DisplayName("Generating verifiable messages with specific values returns expected result")
	void genVerifiableMessagesWithSpecificValues() {
		BigInteger p = BigInteger.valueOf(23);
		BigInteger q = BigInteger.valueOf(11);
		BigInteger g = BigInteger.valueOf(2);
		GqGroup gqGroup = new GqGroup(p, q, g);
		ZqGroup zqGroup = ZqGroup.sameOrderAs(gqGroup);

		// BigIntegers
		BigInteger TWO = BigInteger.valueOf(2);
		BigInteger THREE = BigInteger.valueOf(3);
		BigInteger FOUR = BigInteger.valueOf(4);
		BigInteger SIX = BigInteger.valueOf(6);
		BigInteger SEVEN = BigInteger.valueOf(7);
		BigInteger EIGHT = BigInteger.valueOf(8);
		BigInteger NINE = BigInteger.valueOf(9);
		BigInteger TWELVE = BigInteger.valueOf(12);
		BigInteger THIRTEEN = BigInteger.valueOf(13);

		// ZqElements
		ZqElement zOne = ZqElement.create(BigInteger.ONE, zqGroup);
		ZqElement zTwo = ZqElement.create(TWO, zqGroup);
		ZqElement zThree = ZqElement.create(THREE, zqGroup);
		ZqElement zFour = ZqElement.create(FOUR, zqGroup);
		ZqElement zSix = ZqElement.create(SIX, zqGroup);
		ZqElement zSeven = ZqElement.create(SEVEN, zqGroup);
		ZqElement zEight = ZqElement.create(EIGHT, zqGroup);

		// GqElements
		GqElement gOne = GqElement.create(BigInteger.ONE, gqGroup);
		GqElement gTwo = GqElement.create(TWO, gqGroup);
		GqElement gFour = GqElement.create(FOUR, gqGroup);
		GqElement gEight = GqElement.create(EIGHT, gqGroup);
		GqElement gNine = GqElement.create(NINE, gqGroup);
		GqElement gTwelve = GqElement.create(TWELVE, gqGroup);
		GqElement gThirteen = GqElement.create(THIRTEEN, gqGroup);

		// Create ciphertext vector: C = ((4, 9, 1), (2, 13, 4))
		GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> ciphertexts = GroupVector.of(
				ElGamalMultiRecipientCiphertext.create(gFour, Arrays.asList(gNine, gOne)),
				ElGamalMultiRecipientCiphertext.create(gTwo, Arrays.asList(gThirteen, gFour))
		);

		// Create key pair: (pk, sk) = ((4, 8), (2, 3))
		final ElGamalMultiRecipientKeyPair keyPair = mock(ElGamalMultiRecipientKeyPair.class);
		when(keyPair.getPrivateKey()).thenReturn(new ElGamalMultiRecipientPrivateKey(Arrays.asList(zTwo, zThree)));
		when(keyPair.getPublicKey()).thenReturn(new ElGamalMultiRecipientPublicKey(Arrays.asList(gFour, gEight)));
		when(keyPair.getGroup()).thenReturn(gqGroup);

		// Create auxiliary information: iAux = ("test", "messages")
		List<String> auxiliaryInformation = Arrays.asList("test", "messages");

		// Create service
		RandomService randomService = mock(RandomService.class);
		// b = (3, 8), (2, 4)
		doReturn(GroupVector.from(Arrays.asList(zThree, zEight)), GroupVector.from(Arrays.asList(zTwo, zFour)))
				.when(randomService)
				.genRandomVector(q, 2);
		TestHashService hashService = TestHashService.create(q);
		ZeroKnowledgeProofService service = new ZeroKnowledgeProofService(randomService, hashService);

		// Create expected output
		GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> expectedCPrime = GroupVector.of(
				ElGamalMultiRecipientCiphertext.create(gFour, Arrays.asList(gTwo, gNine)),
				ElGamalMultiRecipientCiphertext.create(gTwo, Arrays.asList(gNine, gTwelve))
		);

		GroupVector<DecryptionProof, ZqGroup> expectedPi = GroupVector.of(
				new DecryptionProof(zSix, GroupVector.of(zFour, zFour)),
				new DecryptionProof(zOne, GroupVector.of(zFour, zSeven))
		);
		VerifiableDecryption expected = new VerifiableDecryption(expectedCPrime, expectedPi);

		assertEquals(expected, service.genVerifiableDecryptions(ciphertexts, keyPair, auxiliaryInformation));
	}
}