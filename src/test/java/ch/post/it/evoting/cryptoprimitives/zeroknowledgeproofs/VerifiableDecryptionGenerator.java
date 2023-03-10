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
package ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs;

import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;

public class VerifiableDecryptionGenerator {
	private final GqGroup group;

	public VerifiableDecryptionGenerator(GqGroup group) {
		this.group = group;
	}

	public VerifiableDecryptions genVerifiableDecryption(int numCiphertexts, int ciphertextSize) {
		ElGamalGenerator elGamalGenerator = new ElGamalGenerator(group);
		GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> ciphertexts = elGamalGenerator
				.genRandomCiphertextVector(numCiphertexts, ciphertextSize);
		GroupVector<DecryptionProof, ZqGroup> decryptionProofs = new DecryptionProofGenerator(ZqGroup.sameOrderAs(group))
				.genDecryptionProofVector(numCiphertexts, ciphertextSize);
		return new VerifiableDecryptions(ciphertexts, decryptionProofs);
	}
}
