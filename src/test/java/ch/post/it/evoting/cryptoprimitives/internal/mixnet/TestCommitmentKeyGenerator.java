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
package ch.post.it.evoting.cryptoprimitives.internal.mixnet;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.stream.Stream;

import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.GqGroupGenerator;

public class TestCommitmentKeyGenerator {

	private final GqGroupGenerator generator;

	TestCommitmentKeyGenerator(final GqGroup group) {
		checkNotNull(group);
		this.generator = new GqGroupGenerator(group);

	}

	/**
	 * Generate a random commitment key in the given group and of given size.
	 *
	 * @param nu the number of g elements of the key.
	 * @return a new commitment key of length ν.
	 */
	CommitmentKey genCommitmentKey(final int nu) {
		final GqElement h = generator.genNonIdentityNonGeneratorMember();
		final GroupVector<GqElement, GqGroup> gList = Stream.generate(generator::genNonIdentityNonGeneratorMember)
				.limit(nu)
				.collect(GroupVector.toGroupVector());
		return new CommitmentKey(h, gList);
	}
}
