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
package ch.post.it.evoting.cryptoprimitives.hashing;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;

/**
 * Interface to be implemented by classes whose hashable form is a byte array.
 */
public interface HashableByteArray extends Hashable {

	@Override
	byte[] toHashableForm();

	/**
	 * Utility function which creates an immutable HashableByteArray whose hashable form is the provided byte array.
	 *
	 * @param byteArray the hashable form. Non-null.
	 * @return A new HashableByteArray whose hashable form is {@code byteArray}
	 */
	static HashableByteArray from(final byte[] byteArray) {
		checkNotNull(byteArray);

		// The first copy is done to avoid modification of the input affecting the HashableByteArray.
		// The second copy is done to avoid modification of the returned value to affect the HashableByteArray.
		final byte[] copy = Arrays.copyOf(byteArray, byteArray.length);
		return () ->  Arrays.copyOf(copy, copy.length);
	}
}
