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
package ch.post.it.evoting.cryptoprimitives.test.tools.generator;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class Generators {
	public static <T> T genWhile(final Supplier<T> producer, final Predicate<T> invalid) {
		T member;
		do {
			member = producer.get();
		} while (invalid.test(member));
		return member;
	}
}
