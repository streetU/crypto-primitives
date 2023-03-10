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
package ch.post.it.evoting.cryptoprimitives.internal.math;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;

import ch.post.it.evoting.cryptoprimitives.math.Base64;

@SuppressWarnings("java:S117")
public final class Base64Service implements Base64 {

	@Override
	public String base64Encode(final byte[] byteArray) {
		checkNotNull(byteArray);
		final byte[] B = Arrays.copyOf(byteArray, byteArray.length);
		return java.util.Base64.getEncoder().encodeToString(B);
	}

	@Override
	public byte[] base64Decode(final String string) {
		final String S = checkNotNull(string);
		try {
			// The method decode checks the given string is a valid Base64 string.
			return java.util.Base64.getDecoder().decode(S);
		} catch (final IllegalArgumentException e) {
			throw new IllegalArgumentException("The given string is not a valid Base64 string.", e);
		}
	}
}
