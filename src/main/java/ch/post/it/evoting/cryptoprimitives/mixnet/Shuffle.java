/*
 * Copyright 2022 Post CH Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package ch.post.it.evoting.cryptoprimitives.mixnet;

import java.util.List;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;

/**
 * Represents the result of a re-encrypting shuffle operation. It contains the re-encrypted ciphertexts, the list of exponents used for re-encryption
 * and the permutation used for shuffling.
 * <p>
 * Instances of this class are immutable.
 */
public record Shuffle(List<ElGamalMultiRecipientCiphertext> ciphertexts,
					  Permutation permutation, List<ZqElement> reEncryptionExponents) {
	public static final Shuffle EMPTY = new Shuffle(List.of(), Permutation.EMPTY, List.of());

	public Shuffle(final List<ElGamalMultiRecipientCiphertext> ciphertexts, final Permutation permutation,
			final List<ZqElement> reEncryptionExponents) {
		this.ciphertexts = List.copyOf(ciphertexts);
		this.permutation = permutation;
		this.reEncryptionExponents = List.copyOf(reEncryptionExponents);
	}

	public List<ElGamalMultiRecipientCiphertext> getCiphertexts() {
		return this.ciphertexts;
	}

	public Permutation getPermutation() {
		return permutation;
	}

	public List<ZqElement> getReEncryptionExponents() {
		return reEncryptionExponents;
	}
}
