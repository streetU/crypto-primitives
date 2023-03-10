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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptoprimitives.math.GroupMatrix;
import ch.post.it.evoting.cryptoprimitives.math.GroupVector;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;
import ch.post.it.evoting.cryptoprimitives.mixnet.ProductWitness;
import ch.post.it.evoting.cryptoprimitives.test.tools.data.GroupTestData;
import ch.post.it.evoting.cryptoprimitives.test.tools.generator.ZqGroupGenerator;

class ProductWitnessTest {

	private static final int MATRIX_BOUND = 10;
	private static final SecureRandom secureRandom = new SecureRandom();

	private ZqGroupGenerator generator;
	private int n;
	private int m;
	private ZqGroup zqGroup;
	private GroupMatrix<ZqElement, ZqGroup> matrix;
	private GroupVector<ZqElement, ZqGroup> exponents;

	@BeforeEach
	void setup() {
		n = secureRandom.nextInt(MATRIX_BOUND) + 1;
		m = secureRandom.nextInt(MATRIX_BOUND) + 1;
		zqGroup = GroupTestData.getZqGroup();
		generator = new ZqGroupGenerator(zqGroup);
		matrix = generator.genRandomZqElementMatrix(n, m);
		exponents = generator.genRandomZqElementVector(m);
	}

	@Test
	@DisplayName("Instantiating a ProductWitness with null arguments throws a NullPointerException")
	void constructProductWitnessWithNull() {
		assertThrows(NullPointerException.class, () -> new ProductWitness(matrix, null));
		assertThrows(NullPointerException.class, () -> new ProductWitness(null, exponents));
	}

	@Test
	@DisplayName("Instantiating a ProductWitness with exponents longer than the number of matrix columns throws an IllegalArgumentException")
	void constructProductWitnessWithTooLongExponents() {
		final GroupVector<ZqElement, ZqGroup> tooLongExponents = generator.genRandomZqElementVector(m + 1);
		final Exception exception = assertThrows(IllegalArgumentException.class, () -> new ProductWitness(matrix, tooLongExponents));
		assertEquals("The number of columns in the matrix must be equal to the number of exponents.", exception.getMessage());
	}

	@Test
	@DisplayName("Instantiating a ProductWitness with the matrix and the exponents from different groups throws an IllegalArgumentException")
	void constructProductWitnessWithMatrixAndExponentsFromDifferentGroup() {
		final ZqGroup differentZqGroup = GroupTestData.getDifferentZqGroup(zqGroup);
		final ZqGroupGenerator differentGenerator = new ZqGroupGenerator(differentZqGroup);
		final GroupVector<ZqElement, ZqGroup> differentExponents = differentGenerator.genRandomZqElementVector(m);
		final Exception exception = assertThrows(IllegalArgumentException.class, () -> new ProductWitness(matrix, differentExponents));
		assertEquals("The matrix and the exponents must belong to the same group.", exception.getMessage());
	}

	@Test
	@DisplayName("The equals method returns true if and only if the matrix and exponents are the same")
	void testEquals() {
		final ProductWitness witness1 = new ProductWitness(matrix, exponents);
		final ProductWitness witness2 = new ProductWitness(matrix, exponents);

		final ZqElement one = ZqElement.create(BigInteger.ONE, zqGroup);
		final List<ZqElement> exponentsValues = new ArrayList<>(exponents);
		ZqElement first = exponentsValues.get(0);
		first = first.add(one);
		exponentsValues.set(0, first);
		final GroupVector<ZqElement, ZqGroup> differentExponents = GroupVector.from(exponentsValues);
		final ProductWitness witness3 = new ProductWitness(matrix, differentExponents);

		final List<List<ZqElement>> matrixValues = IntStream.range(0, m)
				.mapToObj(j -> new ArrayList<>(matrix.getColumn(j)))
				.collect(Collectors.toCollection(ArrayList::new));
		first = matrixValues.get(0).get(0);
		first = first.add(one);
		matrixValues.get(0).set(0, first);
		final GroupMatrix<ZqElement, ZqGroup> differentMatrix = GroupMatrix.fromColumns(matrixValues);
		final ProductWitness witness4 = new ProductWitness(differentMatrix, exponents);

		assertAll(
				() -> assertEquals(witness1, witness2),
				() -> assertNotEquals(witness1, witness3),
				() -> assertNotEquals(witness1, witness4),
				() -> assertNotEquals(witness3, witness4)
		);
	}
}
