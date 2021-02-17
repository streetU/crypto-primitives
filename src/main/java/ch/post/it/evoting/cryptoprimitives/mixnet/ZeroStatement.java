/*
 * HEADER_LICENSE_OPEN_SOURCE
 */
package ch.post.it.evoting.cryptoprimitives.mixnet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import ch.post.it.evoting.cryptoprimitives.SameGroupVector;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;

/**
 * Represents the statement for a zero argument, consisting of two commitments and a y value for bilinear mapping.
 */
class ZeroStatement {

	private final SameGroupVector<GqElement, GqGroup> commitmentsA;
	private final SameGroupVector<GqElement, GqGroup> commitmentsB;
	private final ZqElement y;

	/**
	 * Instantiate a zero statement. The commitments and y must comply with the following:
	 *
	 * <ul>
	 *     <li>be non null</li>
	 *     <li>commitments must be part of the same group</li>
	 *     <li>commitments must be of same length</li>
	 *     <li>value y must be part of the same group as the commitments</li>
	 * </ul>
	 *
	 * @param commitmentsA c<sub>A</sub>, a list of {@link GqElement}s.
	 * @param commitmentsB c<sub>B</sub>, a list of {@link GqElement}s.
	 * @param y            The value defining the bilinear mapping.
	 */
	ZeroStatement(final SameGroupVector<GqElement, GqGroup> commitmentsA, final SameGroupVector<GqElement, GqGroup> commitmentsB, final ZqElement y) {
		// Null checking.
		this.commitmentsA = checkNotNull(commitmentsA);
		this.commitmentsB = checkNotNull(commitmentsB);
		this.y = checkNotNull(y);

		// Cross dimension checking.
		checkArgument(this.commitmentsA.size() == this.commitmentsB.size(), "The two commitments vectors must have the same size.");

		// Cross group checking.
		if (!commitmentsA.isEmpty()) {
			final GqGroup group = this.commitmentsA.getGroup();
			checkArgument(group.equals(this.commitmentsB.getGroup()), "The two commitments must be part of the same group.");
			checkArgument(group.getQ().equals(this.y.getGroup().getQ()),
					"The y value group must be of the same order as the group of the commitments.");
		}

	}

	SameGroupVector<GqElement, GqGroup> getCommitmentsA() {
		return commitmentsA;
	}

	SameGroupVector<GqElement, GqGroup> getCommitmentsB() {
		return commitmentsB;
	}

	ZqElement getY() {
		return y;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ZeroStatement that = (ZeroStatement) o;
		return commitmentsA.equals(that.commitmentsA) && commitmentsB.equals(that.commitmentsB) && y.equals(that.y);
	}

	@Override
	public int hashCode() {
		return Objects.hash(commitmentsA, commitmentsB, y);
	}
}