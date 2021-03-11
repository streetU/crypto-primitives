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
package ch.post.it.evoting.cryptoprimitives.mixnet;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ch.post.it.evoting.cryptoprimitives.GroupVector;
import ch.post.it.evoting.cryptoprimitives.TestGroupSetup;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ElGamalGenerator;

class MultiExponentiationArgumentTest extends TestGroupSetup {

	private static final int DIMENSIONS_BOUND = 10;

	private static int m;
	private static int n;
	private static int l;

	private static GqElement cA0;
	private static GroupVector<GqElement, GqGroup> cBVector;
	private static GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> EVector;
	private static GroupVector<ZqElement, ZqGroup> aVector;
	private static ZqElement r;
	private static ZqElement b;
	private static ZqElement s;
	private static ZqElement tau;

	@BeforeAll
	static void setUp() {
		m = secureRandom.nextInt(DIMENSIONS_BOUND) + 1;
		n = secureRandom.nextInt(DIMENSIONS_BOUND) + 1;
		l = secureRandom.nextInt(DIMENSIONS_BOUND) + 1;

		final TestArgumentGenerator argumentGenerator = new TestArgumentGenerator(gqGroup);
		final MultiExponentiationArgument multiExponentiationArgument = argumentGenerator.genMultiExponentiationArgument(m, n, l);

		cA0 = multiExponentiationArgument.getcA0();
		cBVector = multiExponentiationArgument.getcBVector();
		EVector = multiExponentiationArgument.getEVector();
		aVector = multiExponentiationArgument.getaVector();
		r = multiExponentiationArgument.getR();
		b = multiExponentiationArgument.getB();
		s = multiExponentiationArgument.getS();
		tau = multiExponentiationArgument.getTau();
	}

	@Test
	void builtWithAllSetDoesNotThrow() {
		MultiExponentiationArgument.Builder builder = new MultiExponentiationArgument.Builder();
		assertDoesNotThrow(() -> builder
				.withcA0(cA0)
				.withcBVector(cBVector)
				.withEVector(EVector)
				.withaVector(aVector)
				.withr(r)
				.withb(b)
				.withs(s)
				.withtau(tau)
				.build()
		);
	}

	static Stream<Arguments> nullArgumentsProvider() {
		return Stream.of(
				Arguments.of(null, cBVector, EVector, aVector, r, b, s, tau),
				Arguments.of(cA0, null, EVector, aVector, r, b, s, tau),
				Arguments.of(cA0, cBVector, null, aVector, r, b, s, tau),
				Arguments.of(cA0, cBVector, EVector, null, r, b, s, tau),
				Arguments.of(cA0, cBVector, EVector, aVector, null, b, s, tau),
				Arguments.of(cA0, cBVector, EVector, aVector, r, null, s, tau),
				Arguments.of(cA0, cBVector, EVector, aVector, r, b, null, tau),
				Arguments.of(cA0, cBVector, EVector, aVector, r, b, s, null)
		);
	}

	@ParameterizedTest
	@MethodSource("nullArgumentsProvider")
	void builtWithNullFields(GqElement cA0, GroupVector<GqElement, GqGroup> cBVector,
			GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> EVector, GroupVector<ZqElement, ZqGroup> aVector, ZqElement r, ZqElement b,
			ZqElement s, ZqElement tau) {

		MultiExponentiationArgument.Builder builder = new MultiExponentiationArgument.Builder();
		builder.withcA0(cA0)
				.withcBVector(cBVector)
				.withEVector(EVector)
				.withaVector(aVector)
				.withr(r)
				.withb(b)
				.withs(s)
				.withtau(tau);
		assertThrows(NullPointerException.class, builder::build);
	}

	@Test
	void builtWithDiffGqGroup() {
		final GqElement otherGroupCA0 = otherGqGroupGenerator.genMember();

		final MultiExponentiationArgument.Builder builder = new MultiExponentiationArgument.Builder();
		builder.withcA0(otherGroupCA0)
				.withcBVector(cBVector)
				.withEVector(EVector)
				.withaVector(aVector)
				.withr(r)
				.withb(b)
				.withs(s)
				.withtau(tau);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("cA0, cBVector, EVector must belong to the same group.", exception.getMessage());
	}

	@Test
	void builtWithDiffZqGroup() {
		final ZqElement otherGroupR = otherZqGroupGenerator.genRandomZqElementMember();

		final MultiExponentiationArgument.Builder builder = new MultiExponentiationArgument.Builder();
		builder.withcA0(cA0)
				.withcBVector(cBVector)
				.withEVector(EVector)
				.withaVector(aVector)
				.withr(otherGroupR)
				.withb(b)
				.withs(s)
				.withtau(tau);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("aVector, r, b, s, tau, must belong to the same group.", exception.getMessage());
	}

	@Test
	void builtWithDiffGqGroupAndZqGroup() {
		final GroupVector<ZqElement, ZqGroup> otherGroupAVector = otherZqGroupGenerator.genRandomZqElementVector(n);
		final ZqElement otherGroupR = otherZqGroupGenerator.genRandomZqElementMember();
		final ZqElement otherGroupS = otherZqGroupGenerator.genRandomZqElementMember();
		final ZqElement otherGroupB = otherZqGroupGenerator.genRandomZqElementMember();
		final ZqElement otherGroupTau = otherZqGroupGenerator.genRandomZqElementMember();

		final MultiExponentiationArgument.Builder builder = new MultiExponentiationArgument.Builder();
		builder.withcA0(cA0)
				.withcBVector(cBVector)
				.withEVector(EVector)
				.withaVector(otherGroupAVector)
				.withr(otherGroupR)
				.withb(otherGroupB)
				.withs(otherGroupS)
				.withtau(otherGroupTau);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("GqGroup and ZqGroup of argument inputs are not compatible.", exception.getMessage());
	}

	@Test
	void builtWithDiffSizeVectors() {
		final GroupVector<GqElement, GqGroup> longerCBVector = gqGroupGenerator.genRandomGqElementVector(2 * m + 1);

		final MultiExponentiationArgument.Builder builder = new MultiExponentiationArgument.Builder();
		builder.withcA0(cA0)
				.withcBVector(longerCBVector)
				.withEVector(EVector)
				.withaVector(aVector)
				.withr(r)
				.withb(b)
				.withs(s)
				.withtau(tau);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("The vectors cB and E must have the same size.", exception.getMessage());
	}

	@Test
	void builtWithWrongSizeCBAndE() {
		final GroupVector<GqElement, GqGroup> longerCBVector = gqGroupGenerator.genRandomGqElementVector(2 * m + 1);
		final GroupVector<ElGamalMultiRecipientCiphertext, GqGroup> longerEVector = new ElGamalGenerator(gqGroup)
				.genRandomCiphertextVector(2 * m + 1, l);

		final MultiExponentiationArgument.Builder builder = new MultiExponentiationArgument.Builder();
		builder.withcA0(cA0)
				.withcBVector(longerCBVector)
				.withEVector(longerEVector)
				.withaVector(aVector)
				.withr(r)
				.withb(b)
				.withs(s)
				.withtau(tau);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);
		assertEquals("cB and E must be of size 2 * m.", exception.getMessage());
	}
}
