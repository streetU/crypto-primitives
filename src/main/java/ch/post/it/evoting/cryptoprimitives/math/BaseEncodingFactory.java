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
package ch.post.it.evoting.cryptoprimitives.math;

import ch.post.it.evoting.cryptoprimitives.internal.math.Base16Service;
import ch.post.it.evoting.cryptoprimitives.internal.math.Base32Service;
import ch.post.it.evoting.cryptoprimitives.internal.math.Base64Service;

public class BaseEncodingFactory {

	private BaseEncodingFactory() {
		// Intentionally left blank
	}

	public static Base16 createBase16() {
		return new Base16Service();
	}

	public static Base32 createBase32() {
		return new Base32Service();
	}

	public static Base64 createBase64() {
		return new Base64Service();
	}
}
