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
package ch.post.it.evoting.cryptoprimitives.mixnet;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.utils.VerificationResult;

public interface Mixnet {

	/**
	 * Shuffles (including re-encryption) and provides a Bayer-Groth argument of the shuffle.
	 * <p>
	 * Additionally to the individual arguments preconditions the following cross-argument preconditions must be met:
	 * <ul>
	 *     <li>All ciphertexts and the public key must be from the same group</li>
	 *     <li>The size of the ciphertexts must be smaller or equal to the public key size</li>
	 * </ul>
	 *
	 * @param ciphertexts C,	the collection of {@link ElGamalMultiRecipientCiphertext} to be shuffled. Must not be null and must not contain nulls.
	 *                    All elements must be from the same group and of the same size. The number of elements must be in the range [2, q - 2) where
	 *                    q is the order of the group.
	 * @param publicKey   pk, the {@link ElGamalMultiRecipientPublicKey} to be used for re-encrypting. Not null.
	 * @return the Bayer-Groth shuffle proof and the shuffled ciphertexts as a {@link VerifiableShuffle}
	 */
	VerifiableShuffle genVerifiableShuffle(final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> ciphertexts,
			final ElGamalMultiRecipientPublicKey publicKey);

	/**
	 * Verifies the correctness of a shuffle argument for the given ciphertexts and their shuffled and re-encrypted counterparts.
	 * <p>
	 * Additionally to the individual arguments preconditions the following cross-argument preconditions must be met:
	 * <ul>
	 *     <li>All ciphertexts, the shuffle argument and the public key must be from the same group</li>
	 *     <li>All ciphertexts must have the same size</li>
	 *     <li>The size of the ciphertexts must be smaller or equal to the public key size</li>
	 *     <li>The size of the ciphertext vectors must be the same</li>
	 *     <li>The ciphertext vector size must be 2 or greater and not bigger than q - 3</li>
	 * </ul>
	 *
	 * @param ciphertexts       C, the un-shuffled ciphertexts. Must not be null and not contain null elements.
	 * @param publicKey         pk, the public key used for the re-encryption. Must be non null.
	 * @return the result of the verification.
	 */
	VerificationResult verifyShuffle(final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> ciphertexts,
			final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> shuffledCiphertexts, ShuffleArgument shuffleArgument,
			final ElGamalMultiRecipientPublicKey publicKey);
}
