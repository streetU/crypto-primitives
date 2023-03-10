/*
 * Copyright 2022 Post CH Ltd
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
package ch.post.it.evoting.cryptoprimitives.elgamal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Streams;

import ch.post.it.evoting.cryptoprimitives.internal.math.RandomService;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;

@DisplayName("A multi-recipient key pair")
class ElGamalMultiRecipientKeyPairTest {

	private static GqGroup publicKeyGroup;
	private static ElGamalMultiRecipientKeyPair keyPair;
	private static RandomService randomSer;
	private static int numKeys;
	private static ZqGroup privateKeyGroup;

	@BeforeAll
	static void setUp() {
		BigInteger p = BigInteger.valueOf(23);
		BigInteger q = BigInteger.valueOf(11);
		BigInteger g = BigInteger.valueOf(2);

		
		publicKeyGroup = new GqGroup(p, q, g);
		privateKeyGroup = ZqGroup.sameOrderAs(publicKeyGroup);

		randomSer = new RandomService();

		numKeys = 10;
		keyPair = ElGamalMultiRecipientKeyPair.genKeyPair(publicKeyGroup, numKeys, randomSer);
	}

	@Test
	void generateFailsOnNullGroup() {
		assertThrows(NullPointerException.class, () -> ElGamalMultiRecipientKeyPair.genKeyPair(null, 10, randomSer));
	}

	@Test
	void generateFailsOnZeroLength() {
		assertThrows(IllegalArgumentException.class, () -> ElGamalMultiRecipientKeyPair.genKeyPair(publicKeyGroup, 0, randomSer));
	}

	@Test
	void generateFailsOnNegativeLength() {
		assertThrows(IllegalArgumentException.class, () -> ElGamalMultiRecipientKeyPair.genKeyPair(publicKeyGroup, -1, randomSer));
	}

	@Test
	void testThatGeneratedKeysSizesAreTheExpectedValues() {
		int numGeneratedPrivateKeys = keyPair.getPrivateKey().size();
		int numGeneratedPublicKeys = keyPair.getPublicKey().size();

		assertEquals(numKeys, numGeneratedPrivateKeys);
		assertEquals(numKeys, numGeneratedPublicKeys);
	}

	@Test
	void testThatGeneratedKeysAreMembersOfTheSpecifiedGroup() {
		assertEquals(publicKeyGroup, keyPair.getPublicKey().getGroup());
		assertEquals(privateKeyGroup, keyPair.getPrivateKey().getGroup());
	}

	@Test
	void testThatPublicKeyCorrespondsToPrivateKey() {
		assertTrue(Streams.zip(
				keyPair.getPrivateKey().stream(),
				keyPair.getPublicKey().stream(),
				(ske, pke) -> publicKeyGroup.getGenerator().exponentiate(ske).equals(pke))
				.allMatch(Boolean::booleanValue));
	}

	/**
	 * Check that the created key pair elements stay within the bounds [0, q).
	 */
	@Test
	void testThatPrivateKeyExponentsWithinBounds() {
		BigInteger p = BigInteger.valueOf(11);
		BigInteger q = BigInteger.valueOf(5);
		BigInteger g = BigInteger.valueOf(3);
		
		GqGroup smallGroup = new GqGroup(p, q, g);
		ElGamalMultiRecipientKeyPair keyPair = ElGamalMultiRecipientKeyPair.genKeyPair(smallGroup, 10 * q.intValue(), randomSer);
		keyPair.getPrivateKey().stream().forEach(sk -> {
			assertTrue(sk.getValue().compareTo(BigInteger.ZERO) >= 0);
			assertTrue(sk.getValue().compareTo(q) < 0);
		});
	}
}
