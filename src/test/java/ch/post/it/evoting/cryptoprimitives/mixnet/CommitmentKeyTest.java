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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.GqGroupGenerator;

class CommitmentKeyTest {

	private static GqGroupGenerator generator;

	private GqElement h;
	private List<GqElement> gs;

	@BeforeAll
	static void setUpAll() {
		GqGroup gqGroup = GroupTestData.getGqGroup();
		generator = new GqGroupGenerator(gqGroup);
	}

	@BeforeEach
	void setUp() {
		h = generator.genNonIdentityNonGeneratorMember();
		gs = Stream.generate(generator::genNonIdentityNonGeneratorMember).limit(10).collect(Collectors.toList());
	}

	@Test
	@DisplayName("contains the correct commitment key")
	void constructionTest() {
		CommitmentKey commitmentKey = new CommitmentKey(h, gs);

		assertEquals(h, commitmentKey.stream().limit(1).collect(Collectors.toList()).get(0));
		assertEquals(gs, commitmentKey.stream().skip(1).collect(Collectors.toList()));
	}

	@Test
	void constructionFromNullParameterTest() {
		assertThrows(NullPointerException.class, () -> new CommitmentKey(null, gs));
		assertThrows(NullPointerException.class, () -> new CommitmentKey(h, null));

		List<GqElement> gList = new ArrayList<>(gs);
		gList.add(null);
		assertThrows(IllegalArgumentException.class, () -> new CommitmentKey(h, gList));
	}

	@Test
	void constructionWithEmptyListTest() {
		List<GqElement> emptyList = new LinkedList<>();
		assertThrows(IllegalArgumentException.class, () -> new CommitmentKey(h, emptyList));
	}

	@Test
	void constructionWithElementsFromDifferentGroupsTest() {
		List<GqElement> elements = new LinkedList<>(gs);
		GqGroup differentGroup = GroupTestData.getDifferentGqGroup(h.getGroup());
		GqGroupGenerator differentGroupGenerator = new GqGroupGenerator(differentGroup);
		elements.add(differentGroupGenerator.genNonIdentityNonGeneratorMember());

		assertThrows(IllegalArgumentException.class, () -> new CommitmentKey(h, elements));
	}

	@Test
	void constructionWithHAndGFromDifferentGroupsTest() {
		GqGroup differentGroup = GroupTestData.getDifferentGqGroup(h.getGroup());
		GqGroupGenerator differentGroupGenerator = new GqGroupGenerator(differentGroup);
		List<GqElement> gList = Stream.generate(differentGroupGenerator::genNonIdentityNonGeneratorMember).limit(3).collect(Collectors.toList());
		assertThrows(IllegalArgumentException.class, () -> new CommitmentKey(h, gList));
	}

	@Test
	void constructionWithIdentityTest() {
		GqElement identity = h.getGroup().getIdentity();
		List<GqElement> elementsWithIdentity = new LinkedList<>(gs);
		elementsWithIdentity.add(identity);

		assertThrows(IllegalArgumentException.class, () -> new CommitmentKey(identity, gs));
		assertThrows(IllegalArgumentException.class, () -> new CommitmentKey(h, elementsWithIdentity));
	}

	@Test
	void constructionWithGeneratorTest() {
		GqElement generator = h.getGroup().getGenerator();
		List<GqElement> elementsWithIdentity = new LinkedList<>(gs);
		elementsWithIdentity.add(generator);

		assertThrows(IllegalArgumentException.class, () -> new CommitmentKey(generator, gs));
		assertThrows(IllegalArgumentException.class, () -> new CommitmentKey(h, elementsWithIdentity));
	}

	@Test
	void getVerifiableCommitmentKey() throws NoSuchAlgorithmException {

		int numberOfCommitmentElements = 8;
		GqGroup gqGroup = GroupTestData.getGroupP59();
		CommitmentKey verifiableCommitmentKey = CommitmentKey.getVerifiableCommitmentKey(numberOfCommitmentElements, gqGroup);
		assertNotNull(verifiableCommitmentKey.getGroup());

		GqElement h = GqElement.create(BigInteger.valueOf(16), gqGroup);

		GqElement g1 = GqElement.create(BigInteger.valueOf(4), gqGroup);
		GqElement g2 = GqElement.create(BigInteger.valueOf(57), gqGroup);
		GqElement g3 = GqElement.create(BigInteger.valueOf(20), gqGroup);
		GqElement g4 = GqElement.create(BigInteger.valueOf(25), gqGroup);
		GqElement g5 = GqElement.create(BigInteger.valueOf(46), gqGroup);
		GqElement g6 = GqElement.create(BigInteger.valueOf(12), gqGroup);
		GqElement g7 = GqElement.create(BigInteger.valueOf(15), gqGroup);
		GqElement g8 = GqElement.create(BigInteger.valueOf(27), gqGroup);

		List<GqElement> gqElements = new ArrayList<>();
		gqElements.add(g1);
		gqElements.add(g2);
		gqElements.add(g3);
		gqElements.add(g4);
		gqElements.add(g5);
		gqElements.add(g6);
		gqElements.add(g7);
		gqElements.add(g8);

		CommitmentKey expectedCommitmentKey = new CommitmentKey(h, gqElements);
		assertEquals(expectedCommitmentKey, verifiableCommitmentKey);
	}

	@Test
	void testGetVerifiableCommitmentKeyNullGpGroup() {
		assertThrows(NullPointerException.class, () -> CommitmentKey.getVerifiableCommitmentKey(1, null));
	}

	@Test
	void testGetVerifiableCommitmentKeyIncorrectNumberOfCommitmentElements() {
		GqGroup gqGroup = mock(GqGroup.class);

		IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
				() -> CommitmentKey.getVerifiableCommitmentKey(0, gqGroup));
		assertEquals("The desired number of commitment elements must be greater than zero", illegalArgumentException.getMessage());

		illegalArgumentException = assertThrows(IllegalArgumentException.class,
				() -> CommitmentKey.getVerifiableCommitmentKey(-1, gqGroup));

		assertEquals("The desired number of commitment elements must be greater than zero", illegalArgumentException.getMessage());
	}
}
