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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Objects;

import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.cryptoprimitives.math.GroupVectorElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;

/**
 * An exponentiation proof (e, z) composed of a hash value e and a proof element z.
 *
 * <p>Instances of this class are immutable.</p>
 */
@SuppressWarnings("java:S100")
public final class ExponentiationProof implements GroupVectorElement<ZqGroup>, HashableList {

	private final ZqElement e;
	private final ZqElement z;

	public ExponentiationProof(final ZqElement e, final ZqElement z) {
		checkNotNull(e);
		checkNotNull(z);

		checkArgument(e.getGroup().equals(z.getGroup()), "e and z must be from the same group.");

		this.e = e;
		this.z = z;
	}

	public ZqElement get_e() {
		return e;
	}

	public ZqElement get_z() {
		return z;
	}

	@Override
	public ZqGroup getGroup() {
		return e.getGroup();
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final ExponentiationProof that = (ExponentiationProof) o;
		return e.equals(that.e) && z.equals(that.z);
	}

	@Override
	public int hashCode() {
		return Objects.hash(e, z);
	}

	@Override
	public List<Hashable> toHashableForm() {
		return List.of(e, z);
	}
}
