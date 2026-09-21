package jdk.incubator.vector;

/**
 * The concrete mask of this package.
 *
 * <h2>What a class that does nothing is for</h2>
 *
 * <p>{@link VectorSpecies#maskType()} has to return the class of that species' masks, and {@link
 * VectorMask} is abstract: it cannot be returned there without lying. In the JDK the answer is one
 * concrete class per combination of type and shape, all package-private. This is their equivalent:
 * package-private as well, and a single one, because without intrinsics none of them differs from
 * the others.
 *
 * <p>All its methods throw {@link UnsupportedOperationException}. It is not an oversight: a mask
 * without the machine underneath cannot answer how many lanes are set, and returning zero would be
 * a false answer that compiles and gives wrong results without warning.
 *
 * @param <E> the lane type, in its boxed form
 */
final class MaskImpl<E> extends VectorMask<E> {

    MaskImpl(Object payload) {
        super(payload);
    }

    @Override
    public VectorSpecies<E> vectorSpecies() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public <F extends Object> VectorMask<F> cast(VectorSpecies<F> vectorSpecies) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public long toLong() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public boolean[] toArray() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public void intoArray(boolean[] flags, int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public boolean anyTrue() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public boolean allTrue() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public int trueCount() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public int firstTrue() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public int lastTrue() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public VectorMask<E> and(VectorMask<E> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public VectorMask<E> or(VectorMask<E> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public VectorMask<E> xor(VectorMask<E> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public VectorMask<E> andNot(VectorMask<E> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public VectorMask<E> eq(VectorMask<E> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public VectorMask<E> not() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public VectorMask<E> indexInRange(int i, int i2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public VectorMask<E> indexInRange(long l, long l2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public Vector<E> toVector() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public boolean laneIsSet(int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public <F extends Object> VectorMask<F> check(Class<F> classArg) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public <F extends Object> VectorMask<F> check(VectorSpecies<F> vectorSpecies) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    @Override
    public VectorMask<E> compress() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }
}
