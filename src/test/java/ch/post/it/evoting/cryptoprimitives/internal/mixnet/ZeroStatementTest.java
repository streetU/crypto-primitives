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

import static ch.post.it.evoting.cryptoprimitives.math.GqElement.GqElementFactory;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.internal.math.RandomService;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.mixnet.ZeroStatement;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.GqGroupGenerator;

@DisplayName("A ZeroStatement")
class ZeroStatementTest {

	private static final int RANDOM_UPPER_BOUND = 10;
	private static final SecureRandom secureRandom = new SecureRandom();
	private static final RandomService randomService = new RandomService();

	private static GqGroup gqGroup;
	private static ZqGroup zqGroup;
	private static GqGroupGenerator gqGroupGenerator;

	private int m;
	private GroupVector<GqElement, GqGroup> commitmentsA;
	private GroupVector<GqElement, GqGroup> commitmentsB;
	private ZqElement y;

	@BeforeAll
	static void setUpAll() {
		// GqGroup and corresponding ZqGroup set up.
		gqGroup = GroupTestData.getGqGroup();
		zqGroup = ZqGroup.sameOrderAs(gqGroup);
		gqGroupGenerator = new GqGroupGenerator(gqGroup);
	}

	@BeforeEach
	void setUp() {
		m = secureRandom.nextInt(RANDOM_UPPER_BOUND) + 1;
		commitmentsA = gqGroupGenerator.genRandomGqElementVector(m);
		commitmentsB = gqGroupGenerator.genRandomGqElementVector(m);
		y = ZqElement.create(randomService.genRandomInteger(zqGroup.getQ()), zqGroup);
	}

	@Test
	@DisplayName("constructed with valid parameters works as expected")
	void construct() {
		final ZeroStatement zeroStatement = new ZeroStatement(commitmentsA, commitmentsB, y);

		final GqGroup commitmentsAGroup = zeroStatement.get_c_A().get(0).getGroup();
		final GqGroup commitmentsBGroup = zeroStatement.get_c_B().get(0).getGroup();

		assertEquals(gqGroup, commitmentsAGroup);
		assertEquals(gqGroup, commitmentsBGroup);
		assertEquals(gqGroup.getQ(), zeroStatement.get_y().getGroup().getQ());
	}

	@Test
	@DisplayName("constructed with empty commitments works as expected")
	void constructEmptyCommitments() {
		final GroupVector<GqElement, GqGroup> emptyCommitmentsA = GroupVector.of();
		final GroupVector<GqElement, GqGroup> emptyCommitmentsB = GroupVector.of();

		assertThrows(IllegalArgumentException.class, () -> new ZeroStatement(emptyCommitmentsA, emptyCommitmentsB, y));
	}

	@Test
	@DisplayName("constructed with any null parameters throws NullPointerException")
	void constructNullParams() {
		final GroupVector<GqElement, GqGroup> emptyCommitmentsA = GroupVector.of();
		final GroupVector<GqElement, GqGroup> emptyCommitmentsB = GroupVector.of();

		assertAll(
				() -> assertThrows(NullPointerException.class, () -> new ZeroStatement(null, commitmentsB, y)),
				() -> assertThrows(NullPointerException.class, () -> new ZeroStatement(commitmentsA, null, y)),
				() -> assertThrows(NullPointerException.class, () -> new ZeroStatement(commitmentsA, commitmentsB, null)),
				() -> assertThrows(NullPointerException.class, () -> new ZeroStatement(null, emptyCommitmentsB, y)),
				() -> assertThrows(NullPointerException.class, () -> new ZeroStatement(emptyCommitmentsA, null, y)),
				() -> assertThrows(NullPointerException.class, () -> new ZeroStatement(emptyCommitmentsA, emptyCommitmentsB, null))
		);
	}

	@Test
	@DisplayName("constructed with commitments of different size throws IllegalArgumentException")
	void constructDiffSizeCommitments() {
		final GroupVector<GqElement, GqGroup> additionalElementCommitmentsB = commitmentsB.append(
				GqElementFactory.fromValue(BigInteger.ONE, gqGroup));

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new ZeroStatement(commitmentsA, additionalElementCommitmentsB, y));
		assertEquals("The two commitments vectors must have the same size.", exception.getMessage());
	}

	@Test
	@DisplayName("constructed with commitments from different group throws IllegalArgumentException")
	void constructDiffGroupCommitments() {
		// Generate commitmentsA from different group.
		final GqGroup differentGroup = GroupTestData.getDifferentGqGroup(gqGroup);
		final GqGroupGenerator otherGqGroupGenerator = new GqGroupGenerator(differentGroup);
		final GroupVector<GqElement, GqGroup> diffCommitmentsA = otherGqGroupGenerator.genRandomGqElementVector(m);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new ZeroStatement(diffCommitmentsA, commitmentsB, y));
		assertEquals("The two commitments must be part of the same group.", exception.getMessage());
	}

	@Test
	@DisplayName("constructed with y from a group of different order throws IllegalArgumentException")
	void constructDiffOrderGroupY() {
		final ZqGroup differentZqGroup = GroupTestData.getDifferentZqGroup(zqGroup);
		final ZqElement differentZqGroupY = ZqElement.create(randomService.genRandomInteger(differentZqGroup.getQ()), differentZqGroup);

		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new ZeroStatement(commitmentsA, commitmentsB, differentZqGroupY));
		assertEquals("The y value group must be of the same order as the group of the commitments.", exception.getMessage());
	}
}
