/*
 * HEADER_LICENSE_OPEN_SOURCE
 */
package ch.post.it.evoting.cryptoprimitives.mixnet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import ch.post.it.evoting.cryptoprimitives.SameGroupVector;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;

/**
 * Represents the statement for the shuffle argument, consisting of a ciphertexts vector <b>C</b> and a shuffled and re-encrypted ciphertexts vector
 * <b>C'</b>.
 */
class ShuffleStatement {

	private final SameGroupVector<ElGamalMultiRecipientCiphertext, GqGroup> ciphertexts;
	private final SameGroupVector<ElGamalMultiRecipientCiphertext, GqGroup> shuffledCiphertexts;

	/**
	 * Instantiates a shuffle statement. The vector of input ciphertexts and the vector of shuffled, re-encrypted ciphertexts must comply with the
	 * following:
	 *
	 * <ul>
	 *     <li>be non null and non empty</li>
	 *     <li>both vectors must have the same size</li>
	 *     <li>ciphertexts and shuffled ciphertexts must have the same size</li>
	 *     <li>both vectors must be part of the same group</li>
	 * </ul>
	 *
	 * @param ciphertexts         <b>C</b>, the ciphertexts as a {@link SameGroupVector}. All ciphertexts must have the same size.
	 * @param shuffledCiphertexts <b>C'</b>, the shuffled and re-encrypted ciphertexts as a {@link SameGroupVector}. All shuffled ciphertexts must
	 *                            have the same size.
	 */
	ShuffleStatement(final SameGroupVector<ElGamalMultiRecipientCiphertext, GqGroup> ciphertexts,
			final SameGroupVector<ElGamalMultiRecipientCiphertext, GqGroup> shuffledCiphertexts) {

		checkNotNull(ciphertexts);
		checkNotNull(shuffledCiphertexts);

		// Dimensions checking.
		checkArgument(!ciphertexts.isEmpty(), "The ciphertexts vector can not be empty.");
		checkArgument(!shuffledCiphertexts.isEmpty(), "The shuffled ciphertexts vector can not be empty.");
		checkArgument(ciphertexts.size() == shuffledCiphertexts.size(), "The ciphertexts and shuffled ciphertexts vectors must have the same size.");
		checkArgument(ciphertexts.allEqual(ElGamalMultiRecipientCiphertext::size), "All ciphertexts must have the same size.");
		checkArgument(shuffledCiphertexts.allEqual(ElGamalMultiRecipientCiphertext::size),
				"All shuffled ciphertexts must have the same size.");
		checkArgument(ciphertexts.get(0).size() == shuffledCiphertexts.get(0).size(),
				"The ciphertexts and shuffled ciphertexts must have the same size.");

		// Cross group checking.
		checkArgument(ciphertexts.getGroup().equals(shuffledCiphertexts.getGroup()),
				"The ciphertexts and shuffled ciphertexts must be part of the same group.");

		this.ciphertexts = ciphertexts;
		this.shuffledCiphertexts = shuffledCiphertexts;
	}

	SameGroupVector<ElGamalMultiRecipientCiphertext, GqGroup> getCiphertexts() {
		return ciphertexts;
	}

	SameGroupVector<ElGamalMultiRecipientCiphertext, GqGroup> getShuffledCiphertexts() {
		return shuffledCiphertexts;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ShuffleStatement that = (ShuffleStatement) o;
		return ciphertexts.equals(that.ciphertexts) && shuffledCiphertexts.equals(that.shuffledCiphertexts);
	}

	@Override
	public int hashCode() {
		return Objects.hash(ciphertexts, shuffledCiphertexts);
	}
}