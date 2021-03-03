/*
 * HEADER_LICENSE_OPEN_SOURCE
 */
package ch.post.it.evoting.cryptoprimitives;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.ForwardingList;
import com.google.common.collect.ImmutableList;

import ch.post.it.evoting.cryptoprimitives.math.GroupElement;
import ch.post.it.evoting.cryptoprimitives.math.MathematicalGroup;

/**
 * Represents a vector of {@link GroupElement} belonging to the same {@link MathematicalGroup} and having the same size.
 *
 * This is effectively a decorator for the ImmutableList class.
 *
 * @param <E> the type of elements this list contains.
 * @param <G> the group type the elements of the list belong to.
 */
public class SameGroupVector<E extends GroupVectorElement<G> & Hashable, G extends MathematicalGroup<G>> extends ForwardingList<E> implements HashableList,
		RandomAccess, GroupVectorElement<G> {

	private final ImmutableList<E> elements;
	private final G group;
	private final int elementSize;

	private SameGroupVector(ImmutableList<E> elements) {
		this.elements = elements;
		this.group = elements.isEmpty() ? null : elements.get(0).getGroup();
		this.elementSize = elements.isEmpty() ? 0 : elements.get(0).size();
	}

	/**
	 * Returns a SameGroupVector of {@code elements}.
	 *
	 * @param elements the list of elements contained by this vector, which must respect the following:
	 *                 <li>the list must be non-null</li>
	 *                 <li>the list must not contain any nulls</li>
	 *                 <li>all elements must be from the same {@link MathematicalGroup} </li>
	 *                 <li>all elements must be of the same size</li>
	 */
	public static <E extends GroupVectorElement<G> & Hashable, G extends MathematicalGroup<G>> SameGroupVector<E, G> from(List<E> elements){
		//Check null values
		checkNotNull(elements);
		checkArgument(elements.stream().allMatch(Objects::nonNull), "Elements must not contain nulls");

		//Immutable copy
		ImmutableList<E> elementsCopy = ImmutableList.copyOf(elements);

		//Check same group
		checkArgument(Validations.allEqual(elementsCopy.stream(), GroupVectorElement::getGroup), "All elements must belong to the same group.");

		//Check same size
		checkArgument(Validations.allEqual(elementsCopy.stream(), GroupVectorElement::size), "All vector elements must be the same size.");

		return new SameGroupVector<>(elementsCopy);
	}

	/**
	 * Returns a SameGroupVector of {@code elements}. The elements must comply with the SameGroupVector constraints.
	 *
	 * @param elements The elements to be contained in this vector. May be empty.
	 * @param <E>      The type of the elements.
	 * @param <G>      The group of the elements.
	 * @return A SameGroupVector containing {@code elements}.
	 */
	@SafeVarargs
	public static <E extends GroupVectorElement<G> & Hashable, G extends MathematicalGroup<G>> SameGroupVector<E, G> of(final E... elements) {
		checkNotNull(elements);
		checkArgument(Arrays.stream(elements).allMatch(Objects::nonNull), "Elements must not contain nulls");

		return SameGroupVector.from(ImmutableList.copyOf(elements));
	}

	@Override
	protected List<E> delegate() {
		return this.elements;
	}

	/**
	 * @return the group all elements belong to.
	 * @throws IllegalStateException if the vector is empty.
	 */
	public G getGroup() {
		if (this.isEmpty()) {
			throw new IllegalStateException("An empty SameGroupVector does not have a group.");
		} else {
			return this.group;
		}
	}

	/**
	 * @return the size of elements. 0 if the vector is empty.
	 */
	public int getElementSize() {
		return elementSize;
	}

	/**
	 * Append a new element to this vector. Returns a new SameGroupVector.
	 *
	 * @param element The element to append. Must be non null and from the same group.
	 * @return A new SameGroupVector with the appended {@code element}.
	 */
	public SameGroupVector<E, G> append(final E element) {
		checkNotNull(element);
		checkArgument(element.getGroup().equals(this.group), "The element to append must be in the same group.");
		checkArgument(element.size() == this.elementSize, "The element to append must be the same size.");

		return new SameGroupVector<>(
				new ImmutableList.Builder<E>()
						.addAll(this.elements)
						.add(element)
						.build());
	}

	/**
	 * Prepend a new element to this vector. Returns a new SameGroupVector.
	 *
	 * @param element The element to prepend. Must be non null and from the same group.
	 * @return A new SameGroupVector with the prepended {@code element}.
	 */
	public SameGroupVector<E, G> prepend(final E element) {
		checkNotNull(element);
		checkArgument(element.getGroup().equals(this.group), "The element to prepend must be in the same group.");
		checkArgument(element.size() == this.elementSize, "The element to prepend must be the same size.");

		return new SameGroupVector<>(
				new ImmutableList.Builder<E>()
						.add(element)
						.addAll(this.elements)
						.build());
	}

	/**
	 * Validate that a property holds for all elements.
	 *
	 * @param property to check all elements against.
	 * @return true if the vector is empty or all elements are equal under this property. False otherwise.
	 */
	public boolean allEqual(Function<? super E, ?> property) {
		return Validations.allEqual(this.elements.stream(), property);
	}

	/**
	 * Transforms this vector into a matrix.
	 * <p>
	 * The elements of this vector <b><i>v</i></b> of size <i>N</i> = <i>mn</i> are rearranged into a matrix of size <i>m</i> &times; <i>n</i>, where
	 * element M<sub>i,j</sub> of the matrix corresponds to element v<sub>i + mj</sub> of the vector.
	 *
	 * @param numRows    m, the number of rows of the matrix to be created
	 * @param numColumns n, the number of columns of the matrix to be created
	 * @return a {@link SameGroupMatrix} of size m &times; n
	 */
	public SameGroupMatrix<E, G> toMatrix(final int numRows, final int numColumns) {
		checkArgument(numRows > 0, "The number of rows must be positive.");
		checkArgument(numColumns > 0, "The number of columns must be positive.");

		// Ensure N = nm
		checkArgument(this.size() == (numRows * numColumns), "The vector of ciphertexts must be decomposable into m rows and n columns.");

		// Create the matrix
		return IntStream.range(0, numRows)
				.mapToObj(i -> IntStream.range(0, numColumns)
						.mapToObj(j -> this.get(i + numRows * j))
						.collect(Collectors.toList()))
				.collect(Collectors.collectingAndThen(Collectors.toList(), SameGroupMatrix::fromRows));
	}

	/**
	 * Returns a Collector that accumulates the input elements into a SameGroupVector.
	 *
	 * @param <E> the type of elements this list contains.
	 * @return a {@code Collector} for accumulating the input elements into a SameGroupVector.
	 */
	public static <E extends GroupVectorElement<G> & Hashable, G extends MathematicalGroup<G>> Collector<E, ?, SameGroupVector<E, G>> toSameGroupVector() {
		return Collectors.collectingAndThen(toImmutableList(), SameGroupVector::from);
	}

	@Override
	public String toString() {
		return "SameGroupVector{" + "elements=" + elements + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		SameGroupVector<?, ?> that = (SameGroupVector<?, ?>) o;
		return elements.equals(that.elements);
	}

	@Override
	public int hashCode() {
		return Objects.hash(elements);
	}

	@Override
	public Spliterator<E> spliterator() {
		return this.elements.spliterator();
	}

	@Override
	public ImmutableList<? extends Hashable> toHashableForm() {
		return this.elements;

	}
}
