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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import ch.post.it.evoting.cryptoprimitives.internal.hashing.HashService;
import ch.post.it.evoting.cryptoprimitives.internal.securitylevel.SecurityLevelConfig;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.GqGroupGenerator;
import ch.post.it.evoting.cryptoprimitives.test.tools.serialization.JsonData;
import ch.post.it.evoting.cryptoprimitives.test.tools.serialization.TestParameters;

class CommitmentKeyServiceTest {

	private static GqGroupGenerator generator;
	private static CommitmentKeyService commitmentKeyService;
	private static GqGroup gqGroup;

	private GqElement h;
	private GroupVector<GqElement, GqGroup> gs;

	@BeforeAll
	static void setUpAll() throws NoSuchAlgorithmException {
		gqGroup = GroupTestData.getGqGroup();
		generator = new GqGroupGenerator(gqGroup);
		final HashService hashService = HashService.getInstance();
		commitmentKeyService = new CommitmentKeyService(hashService);
	}

	@BeforeEach
	void setUp() {
		h = generator.genNonIdentityNonGeneratorMember();
		gs = Stream.generate(generator::genNonIdentityNonGeneratorMember)
				.limit(10)
				.collect(GroupVector.toGroupVector());
	}

	@Test
	@DisplayName("contains the correct commitment key")
	void constructionTest() {
		final CommitmentKey commitmentKey = new CommitmentKey(h, gs);

		assertEquals(h, commitmentKey.stream().limit(1).toList().get(0));
		assertEquals(gs, commitmentKey.stream().skip(1).collect(GroupVector.toGroupVector()));
	}

	@Test
	void constructionFromNullParameterTest() {
		assertThrows(NullPointerException.class, () -> new CommitmentKey(null, gs));
		assertThrows(NullPointerException.class, () -> new CommitmentKey(h, null));
	}

	@Test
	void constructionWithEmptyListTest() {
		final GroupVector<GqElement, GqGroup> empty = GroupVector.of();
		assertThrows(IllegalArgumentException.class, () -> new CommitmentKey(h, empty));
	}

	@Test
	void constructionWithHAndGFromDifferentGroupsTest() {
		final GqGroup differentGroup = GroupTestData.getDifferentGqGroup(h.getGroup());
		final GqGroupGenerator differentGroupGenerator = new GqGroupGenerator(differentGroup);
		final GroupVector<GqElement, GqGroup> gList = Stream.generate(differentGroupGenerator::genNonIdentityNonGeneratorMember).limit(3)
				.collect(GroupVector.toGroupVector());
		assertThrows(IllegalArgumentException.class, () -> new CommitmentKey(h, gList));
	}

	@Test
	void constructionWithIdentityTest() {
		final GqElement identity = h.getGroup().getIdentity();
		final GroupVector<GqElement, GqGroup> identityVector = GroupVector.of(identity);

		assertThrows(IllegalArgumentException.class, () -> new CommitmentKey(identity, gs));
		assertThrows(IllegalArgumentException.class, () -> new CommitmentKey(h, identityVector));
	}

	@Test
	void constructionWithGeneratorTest() {
		final GqElement generator = h.getGroup().getGenerator();
		final GroupVector<GqElement, GqGroup> generatorVector = GroupVector.of(generator);

		assertThrows(IllegalArgumentException.class, () -> new CommitmentKey(generator, gs));
		assertThrows(IllegalArgumentException.class, () -> new CommitmentKey(h, generatorVector));
	}

	static Stream<Arguments> getVerifiableCommitmentKeyArgumentProvider() {
		final List<TestParameters> parametersList = TestParameters.fromResource("/mixnet/get-verifiable-commitment-key.json");

		return parametersList.stream().parallel().map(testParameters -> {
			// Context.
			final JsonData context = testParameters.getContext();
			final BigInteger p = context.get("p", BigInteger.class);
			final BigInteger q = context.get("q", BigInteger.class);
			final BigInteger g = context.get("g", BigInteger.class);

			try (final MockedStatic<SecurityLevelConfig> mockedSecurityLevel = Mockito.mockStatic(SecurityLevelConfig.class)) {
				mockedSecurityLevel.when(SecurityLevelConfig::getSystemSecurityLevel).thenReturn(testParameters.getSecurityLevel());
				final GqGroup gqGroup = new GqGroup(p, q, g);

				// Input.
				final JsonData input = testParameters.getInput();
				final int numberOfElements = input.get("k", Integer.class);

				// Output.
				final JsonData output = testParameters.getOutput();
				final GqElement h = GqElementFactory.fromValue(output.get("h", BigInteger.class), gqGroup);
				final GroupVector<GqElement, GqGroup> gVector = Arrays.stream(output.get("g", BigInteger[].class))
						.map(value -> GqElementFactory.fromValue(value, gqGroup))
						.collect(GroupVector.toGroupVector());
				final CommitmentKey expectedCommitmentKey = new CommitmentKey(h, gVector);

				return Arguments.of(numberOfElements, gqGroup, expectedCommitmentKey, testParameters.getDescription());
			}
		});
	}

	@ParameterizedTest(name = "{3}")
	@MethodSource("getVerifiableCommitmentKeyArgumentProvider")
	@DisplayName("with real values")
	void getVerifiableCommitmentKeyRealValues(final int numberOfElements, final GqGroup gqGroup, final CommitmentKey expectedCommitmentKey,
			final String description) {

		final CommitmentKey verifiableCommitmentKey = commitmentKeyService.getVerifiableCommitmentKey(numberOfElements, gqGroup);

		assertEquals(expectedCommitmentKey, verifiableCommitmentKey, String.format("assertion failed for: %s", description));
	}

	@Test
	void testGetVerifiableCommitmentKeyThrowsOnTooSmallGroup() {
		final GqGroup group = GroupTestData.getGqGroup();
		final int size = group.getQ().subtract(BigInteger.valueOf(3)).add(BigInteger.ONE).intValueExact();
		assertThrows(IllegalArgumentException.class, () -> commitmentKeyService.getVerifiableCommitmentKey(size, group));
	}

	@Test
	void testGetVerifiableCommitmentKeyNullGpGroup() {
		assertThrows(NullPointerException.class, () -> commitmentKeyService.getVerifiableCommitmentKey(1, null));
	}

	@Test
	void testGetVerifiableCommitmentKeyIncorrectNumberOfCommitmentElements() {
		IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
				() -> commitmentKeyService.getVerifiableCommitmentKey(0, gqGroup));
		assertEquals("The desired number of commitment elements must be in the range (0, q - 3]", illegalArgumentException.getMessage());

		illegalArgumentException = assertThrows(IllegalArgumentException.class,
				() -> commitmentKeyService.getVerifiableCommitmentKey(-1, gqGroup));

		assertEquals("The desired number of commitment elements must be in the range (0, q - 3]", illegalArgumentException.getMessage());
	}
}
