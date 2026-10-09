package jdk.incubator.vector;

/**
 * The class that goes between {@link Vector} and the six concrete ones.
 *
 * <h2>Why it exists if nobody can name it</h2>
 *
 * <p>It is not public, so it cannot be written from outside the package. And yet it is
 * <strong>seen</strong>: when {@code IntVector} declares {@code IntVector slice(int)} and above
 * there are two declarations with ever wider returns, javac leaves a bridge method in {@code
 * IntVector} for each one. One of those bridges is {@code public AbstractVector slice(int)}, and it
 * is public and is in the JDK's compiled file.
 *
 * <p>That is why this class is here. The first version of this package hung the six directly from
 * {@link Vector}, on the argument that an empty class in the middle gave nobody a new method. It
 * was false: twelve public members of the six concrete classes depend on this link existing, and
 * without it they do not appear.
 *
 * <p>It declares the minimum needed for those bridges to come out: the two {@code slice}s, which
 * are the only ones the JDK narrows here instead of narrowing them only further down.
 *
 * @param <E> the lane type, in its boxed form
 */
abstract class AbstractVector<E> extends Vector<E> {

    /**
     * With that payload.
     *
     * <p>It is written even though it does nothing but delegate: {@link Vector} has no no-argument
     * constructor, and without this one the implicit default constructor would call a nonexistent
     * {@code super()}.
     *
     * @param payload the array of lanes
     */
    AbstractVector(Object payload) {
        super(payload);
    }

    /**
     * A part of the vector, starting at that lane.
     *
     * @param origin where it starts from
     * @return the piece
     */
    @Override
    public abstract AbstractVector<E> slice(int origin);

    /**
     * A part that starts in this vector and continues in the other.
     *
     * @param origin where it starts from
     * @param v1 the vector that follows
     * @return the piece
     */
    @Override
    public abstract AbstractVector<E> slice(int origin, Vector<E> v1);
}
