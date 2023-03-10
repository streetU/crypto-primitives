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

import static ch.post.it.evoting.cryptoprimitives.utils.Validations.allEqual;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.GroupVectorElement;

/**
 * Value class containing the result of a shuffle argument proof.
 *
 * <p>Instances of this class are immutable.</p>
 */
@SuppressWarnings({ "java:S100", "java:S116", "java:S117", "java:S107" })
public final class ShuffleArgument implements GroupVectorElement<GqGroup>, HashableList {

	private final GroupVector<GqElement, GqGroup> c_A;
	private final GroupVector<GqElement, GqGroup> c_B;
	private final ProductArgument productArgument;
	private final MultiExponentiationArgument multiExponentiationArgument;

	private final int m;
	private final int n;
	private final int l;
	private final GqGroup group;

	private ShuffleArgument(final GroupVector<GqElement, GqGroup> c_A, final GroupVector<GqElement, GqGroup> c_B,
			final ProductArgument productArgument, final MultiExponentiationArgument multiExponentiationArgument, final int m, final int n,
			final int l, final GqGroup group) {
		this.c_A = c_A;
		this.c_B = c_B;
		this.productArgument = productArgument;
		this.multiExponentiationArgument = multiExponentiationArgument;
		this.m = m;
		this.n = n;
		this.l = l;
		this.group = group;
	}

	public GroupVector<GqElement, GqGroup> get_c_A() {
		return c_A;
	}

	public GroupVector<GqElement, GqGroup> get_c_B() {
		return c_B;
	}

	public ProductArgument getProductArgument() {
		return productArgument;
	}

	public MultiExponentiationArgument getMultiExponentiationArgument() {
		return multiExponentiationArgument;
	}

	public int get_m() {
		return m;
	}

	public int get_n() {
		return n;
	}

	public int get_l() {
		return l;
	}

	@Override
	public GqGroup getGroup() {
		return group;
	}

	@Override
	public int size() {
		return l;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final ShuffleArgument that = (ShuffleArgument) o;
		return c_A.equals(that.c_A) && c_B.equals(that.c_B) && productArgument.equals(that.productArgument) && multiExponentiationArgument
				.equals(that.multiExponentiationArgument);
	}

	@Override
	public int hashCode() {
		return Objects.hash(c_A, c_B, productArgument, multiExponentiationArgument);
	}

	@Override
	public List<? extends Hashable> toHashableForm() {
		return List.of(c_A, c_B, productArgument, multiExponentiationArgument);
	}

	public static class Builder {

		private GroupVector<GqElement, GqGroup> c_A;
		private GroupVector<GqElement, GqGroup> c_B;
		private ProductArgument productArgument;
		private MultiExponentiationArgument multiExponentiationArgument;

		public Builder with_c_A(final GroupVector<GqElement, GqGroup> c_A) {
			this.c_A = c_A;
			return this;
		}

		public Builder with_c_B(final GroupVector<GqElement, GqGroup> c_B) {
			this.c_B = c_B;
			return this;
		}

		public Builder with_productArgument(final ProductArgument productArgument) {
			this.productArgument = productArgument;
			return this;
		}

		public Builder with_multiExponentiationArgument(final MultiExponentiationArgument multiExponentiationArgument) {
			this.multiExponentiationArgument = multiExponentiationArgument;
			return this;
		}

		/**
		 * Builds the {@link ShuffleArgument}. Upon calling this method, all fields must have been set to non null values.
		 * <p>
		 * Additionally, the fields must comply with the following:
		 * <ul>
		 *     <li>cA, cB, the product and multi exponentiation arguments must belong to the same group</li>
		 *     <li>cA, cB, the product and multi exponentiation arguments must have identical dimension m</li>
		 *     <li>the product and multi exponentiation arguments must have identical dimension n</li>
		 * </ul>
		 *
		 * @return A valid Shuffle Argument.
		 */
		public ShuffleArgument build() {
			// Null checking.
			checkNotNull(this.c_A);
			checkNotNull(this.c_B);
			checkNotNull(this.productArgument);
			checkNotNull(this.multiExponentiationArgument);

			// Cross group checking.
			final List<GqGroup> gqGroups = Arrays
					.asList(this.c_A.getGroup(), this.c_B.getGroup(), this.productArgument.getGroup(), this.multiExponentiationArgument.getGroup());
			checkArgument(allEqual(gqGroups.stream(), g -> g),
					"The commitments cA, cB, the product and the multi exponentiation arguments must belong to the same group.");

			// Cross dimensions checking.
			final List<Integer> mDimensions = Arrays
					.asList(this.c_A.size(), this.c_B.size(), this.productArgument.get_m(), this.multiExponentiationArgument.get_m());
			checkArgument(allEqual(mDimensions.stream(), d -> d),
					"The commitments cA, cB and the product and multi exponentiation arguments must have the same dimension m.");

			checkArgument(this.productArgument.get_n() == this.multiExponentiationArgument.get_n(),
					"The product and multi exponentiation arguments must have the same dimension n.");

			// Build the argument.
			return new ShuffleArgument(this.c_A, this.c_B, this.productArgument, this.multiExponentiationArgument,
					productArgument.get_m(), productArgument.get_n(), multiExponentiationArgument.get_l(), productArgument.getGroup());

		}
	}
}
