package jdk.incubator.vector;

import jdk.internal.vm.vector.VectorSupport;

/**
 * A vector of flags: which lanes take part in an operation.
 *
 * <p>It is how an {@code if} is done without branching. Instead of skipping the lanes that do not
 * meet the condition --impossible, because the instruction operates on all of them-- it is computed
 * on all and the mask decides which are written.
 *
 * <p>It is also what solves the remainder of an array: a mask that turns off the lanes that would
 * fall outside lets the last portion be processed with the same instruction as the rest.
 *
 * @since 16
 */
public abstract class VectorMask<E extends Object> extends VectorSupport.VectorMask<E> {

    /**
     * With that payload.
     *
     * <p>In the JDK this constructor is package-private, so it does not show up in the dumps. It is
     * written anyway because the superclass has no no-argument one: without it, the implicit
     * default constructor would call a {@code super()} that does not exist. The note said javac
     * then emits an invalid class file (finding #515); that finding is closed in the source javac,
     * which now rejects the class instead, but the frozen {@code bin/javac.exe} predates the fix.
     * The constructor is needed either way.
     *
     * @param payload the array of lanes
     */
    VectorMask(Object payload) {
        super(payload);
    }

    /**
     * The species of the vectors it goes with.
     *
     * @return the {@code VectorSpecies<E>}
     */
    public abstract VectorSpecies<E> vectorSpecies();

    /**
     * How many lanes the mask has.
     *
     * @return the number
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final int length() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A mask with those flags.
     *
     * @param <E> the type, in its boxed form
     * @param vectorSpecies the {@code VectorSpecies<E>}
     * @param flag the {@code boolean...}
     * @return the {@code VectorMask<E>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static <E extends Object> VectorMask<E> fromValues(VectorSpecies<E> vectorSpecies,
            boolean... flag) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A mask read from that array of flags.
     *
     * @param <E> the type, in its boxed form
     * @param vectorSpecies the {@code VectorSpecies<E>}
     * @param flags the {@code boolean[]}
     * @param i the {@code int}
     * @return the {@code VectorMask<E>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static <E extends Object> VectorMask<E> fromArray(VectorSpecies<E> vectorSpecies,
            boolean[] flags, int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A mask built from the low bits of that {@code long}, one per lane.
     *
     * @param <E> the type, in its boxed form
     * @param vectorSpecies the {@code VectorSpecies<E>}
     * @param l the {@code long}
     * @return the {@code VectorMask<E>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static <E extends Object> VectorMask<E> fromLong(VectorSpecies<E> vectorSpecies, long l
            ) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The same mask for another species of the same length.
     *
     * @param <F> the type, in its boxed form
     * @param vectorSpecies the {@code VectorSpecies<F>}
     * @return the {@code VectorMask<F>}
     */
    public abstract <F extends Object> VectorMask<F> cast(VectorSpecies<F> vectorSpecies);

    /**
     * The mask's flags together in a {@code long}, one per bit.
     *
     * @return the number
     */
    public abstract long toLong();

    /**
     * The flags in a new array.
     *
     * @return the {@code boolean[]}
     */
    public abstract boolean[] toArray();

    /**
     * Writes the flags into that array.
     *
     * @param flags the {@code boolean[]}
     * @param i the {@code int}
     */
    public abstract void intoArray(boolean[] flags, int i);

    /**
     * Whether at least one lane is set.
     *
     * @return true or false, as the case may be
     */
    public abstract boolean anyTrue();

    /**
     * Whether they are all set.
     *
     * @return true or false, as the case may be
     */
    public abstract boolean allTrue();

    /**
     * How many lanes are set.
     *
     * @return the number
     */
    public abstract int trueCount();

    /**
     * The index of the first set lane, or the lane count if there is none.
     *
     * @return the number
     */
    public abstract int firstTrue();

    /**
     * The index of the last set lane, or -1 if there is none.
     *
     * @return the number
     */
    public abstract int lastTrue();

    /**
     * The bitwise conjunction.
     *
     * @param vectorMask the {@code VectorMask<E>}
     * @return the {@code VectorMask<E>}
     */
    public abstract VectorMask<E> and(VectorMask<E> vectorMask);

    /**
     * The bitwise disjunction.
     *
     * @param vectorMask the {@code VectorMask<E>}
     * @return the {@code VectorMask<E>}
     */
    public abstract VectorMask<E> or(VectorMask<E> vectorMask);

    /**
     * The bitwise exclusive disjunction.
     *
     * @param vectorMask the {@code VectorMask<E>}
     * @return the {@code VectorMask<E>}
     */
    public abstract VectorMask<E> xor(VectorMask<E> vectorMask);

    /**
     * The conjunction with the other one inverted.
     *
     * @param vectorMask the {@code VectorMask<E>}
     * @return the {@code VectorMask<E>}
     */
    public abstract VectorMask<E> andNot(VectorMask<E> vectorMask);

    /**
     * The mask of the equal lanes.
     *
     * @param vectorMask the {@code VectorMask<E>}
     * @return the {@code VectorMask<E>}
     */
    public abstract VectorMask<E> eq(VectorMask<E> vectorMask);

    /**
     * Inverts each lane.
     *
     * @return the {@code VectorMask<E>}
     */
    public abstract VectorMask<E> not();

    /**
     * The mask of the lanes whose index still falls within the range.
     *
     * @param i the {@code int}
     * @param i2 the {@code int}
     * @return the {@code VectorMask<E>}
     */
    public abstract VectorMask<E> indexInRange(int i, int i2);

    /**
     * The mask of the lanes whose index still falls within the range.
     *
     * @param l the {@code long}
     * @param l2 the {@code long}
     * @return the {@code VectorMask<E>}
     */
    public abstract VectorMask<E> indexInRange(long l, long l2);

    /**
     * The mask seen as a vector: zero where it is unset.
     *
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> toVector();

    /**
     * Whether that lane is set.
     *
     * @param i the {@code int}
     * @return true or false, as the case may be
     */
    public abstract boolean laneIsSet(int i);

    /**
     * Checks that the mask is of that species and returns the same, already typed.
     *
     * @param <F> the type, in its boxed form
     * @param classArg the {@code Class<F>}
     * @return the {@code VectorMask<F>}
     */
    public abstract <F extends Object> VectorMask<F> check(Class<F> classArg);

    /**
     * Checks that the mask is of that species and returns the same, already typed.
     *
     * @param <F> the type, in its boxed form
     * @param vectorSpecies the {@code VectorSpecies<F>}
     * @return the {@code VectorMask<F>}
     */
    public abstract <F extends Object> VectorMask<F> check(VectorSpecies<F> vectorSpecies);

    /**
     * The flags written as a list.
     *
     * @return the text
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final String toString() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Whether the other mask has the same flags.
     *
     * @param obj the {@code Object}
     * @return true or false, as the case may be
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final boolean equals(Object obj) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The hash code.
     *
     * @return the number
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final int hashCode() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Gathers the set lanes at the start.
     *
     * @return the {@code VectorMask<E>}
     */
    public abstract VectorMask<E> compress();
}
