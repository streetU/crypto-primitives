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
package ch.post.it.evoting.cryptoprimitives;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public final class ConversionService {

	private ConversionService() {
		//Intentionally left blank
	}

	/**
	 * Converts a string to a byte array representation. StringToByteArray algorithm implementation.
	 *
	 * @param s S, the string to convert.
	 * @return the byte array representation of the string.
	 */
	public static byte[] stringToByteArray(final String s) {
		checkNotNull(s);
		return s.getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Converts a BigInteger to a byte array representation.
	 * <p>
	 * NOTE: our implementation slightly deviates from the specifications for performance reasons. Benchmarks show that our implementation is orders
	 * of magnitude faster than the pseudo-code implementation integerToByteArraySpec. Both implementations provide the same result.
	 *
	 * @param x the positive BigInteger to convert.
	 * @return the byte array representation of this BigInteger.
	 */
	public static byte[] integerToByteArray(final BigInteger x) {
		checkNotNull(x);
		checkArgument(x.compareTo(BigInteger.ZERO) >= 0);

		// BigInteger#toByteArray gives back a 2s complement representation of the value. Given that we work only with positive BigIntegers, this
		// representation is equivalent to the binary representation, except for a potential extra leading zero byte. (The presence or not of the
		// leading zero depends on the number of bits needed to represent this value).
		byte[] twosComplement = x.toByteArray();
		byte[] result;
		if (twosComplement[0] == 0 && twosComplement.length > 1) {
			result = new byte[twosComplement.length - 1];
			System.arraycopy(twosComplement, 1, result, 0, twosComplement.length - 1);
		} else {
			result = twosComplement;
		}
		return result;
	}

	/**
	 * Do not use.
	 *
	 * <p>
	 * Implements the specification IntegerToByteArray algorithm. It is used in tests to show that it is equivalent to the more performant method
	 * used.
	 *
	 * @param x the positive BigInteger to convert.
	 * @return the byte array representation of this BigInteger.
	 **/
	static byte[] integerToByteArraySpec(final BigInteger x) {
		checkNotNull(x);
		checkArgument(x.compareTo(BigInteger.ZERO) >= 0);

		if (x.compareTo(BigInteger.ZERO) == 0) {
			return new byte[1];
		}

		int bitLength = x.bitLength();
		int n = (bitLength + Byte.SIZE - 1) / Byte.SIZE;

		byte[] output = new byte[n];
		BigInteger current = x;
		for (int i = 1; i <= n; i++) {
			output[n - i] = current.byteValue();
			current = current.shiftRight(Byte.SIZE);
		}

		return output;
	}

	/**
	 * Converts a byte array to its BigInteger equivalent.
	 * <p>
	 * Uses the {@link BigInteger} implementation of the byte array to integer transformation,
	 * which is equivalent to the specification of ByteArrayToInteger.
	 *
	 * @param bytes B, the byte array to convert.
	 * @return a BigInteger corresponding to the provided byte array representation.
	 */
	public static BigInteger byteArrayToInteger(final byte[] bytes) {
		checkNotNull(bytes);
		return new BigInteger(1, bytes);
	}
}
