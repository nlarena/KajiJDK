package jdk.incubator.vector;

import java.lang.foreign.MemorySegment;
import java.nio.ByteOrder;
import jdk.internal.vm.vector.VectorSupport;

/**
 * A vector of values of the same type, to operate on all of them at once.
 *
 * <h2>What problem it solves</h2>
 *
 * <p>A modern processor can add eight integers in a single instruction. A Java loop that adds one
 * at a time wastes seven eighths of that capacity, and the compiler only sometimes guesses that it
 * can vectorise it. This API is the way of asking for it explicitly.
 *
 * <h2>The program does not choose the vector's shape</h2>
 *
 * <p>The machine chooses it: a processor with AVX-512 has 512-bit registers and another one 128.
 * That is why the length is not a fixed number but {@link VectorSpecies#length}, and why loops are
 * written with {@link VectorSpecies#loopBound} instead of with a constant step.
 *
 * <p>Writing the step by hand is the error that makes code work on one machine and break on
 * another.
 *
 * <h2>State in this library</h2>
 *
 * <p>The API is declared whole, with JDK 25's exact signatures, and any code that uses it compiles
 * against it. What is missing is the <strong>intrinsics</strong>: each vector operation exists for
 * the VM to replace it with a machine instruction, and without that replacement there is nothing
 * left to run.
 *
 * <p>That is why the operations throw {@link UnsupportedOperationException} instead of computing in
 * a scalar loop. A scalar fallback would be a useful lie: it would give the right result, much
 * slower than the loop the user has just replaced, that is the opposite of what this API promises.
 *
 * <p>The parts that <strong>do</strong> work are the ones that do not depend on the machine: {@link
 * VectorMath}, {@link Float16} and the metadata of {@link VectorSpecies}.
 *
 * @since 16
 */
public abstract class Vector<E extends Object> extends VectorSupport.Vector<E> {

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
    Vector(Object payload) {
        super(payload);
    }

    /**
     * The species of this vector: its lane type and its shape.
     *
     * @return the {@code VectorSpecies<E>}
     */
    public abstract VectorSpecies<E> species();

    /**
     * The type of the lanes.
     *
     * @return the {@code Class<E>}
     */
    public abstract Class<E> elementType();

    /**
     * The size of a lane, in bits.
     *
     * @return the number
     */
    public abstract int elementSize();

    /**
     * The shape of this vector.
     *
     * @return the {@code VectorShape}
     */
    public abstract VectorShape shape();

    /**
     * How many lanes it has.
     *
     * @return the number
     */
    public abstract int length();

    /**
     * The size of the whole vector, in bits.
     *
     * @return the number
     */
    public abstract int bitSize();

    /**
     * The size of the whole vector, in bytes.
     *
     * @return the number
     */
    public abstract int byteSize();

    /**
     * Applies that operator to each lane.
     *
     * @param unary the {@code VectorOperators.Unary}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> lanewise(VectorOperators.Unary unary);

    /**
     * Applies that operator to each lane.
     *
     * @param unary the {@code VectorOperators.Unary}
     * @param vectorMask the {@code VectorMask<E>}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> lanewise(VectorOperators.Unary unary, VectorMask<E> vectorMask);

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param vector the {@code Vector<E>}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> lanewise(VectorOperators.Binary binary, Vector<E> vector);

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param vector the {@code Vector<E>}
     * @param vectorMask the {@code VectorMask<E>}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> lanewise(VectorOperators.Binary binary, Vector<E> vector,
            VectorMask<E> vectorMask);

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param l the {@code long}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> lanewise(VectorOperators.Binary binary, long l);

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param l the {@code long}
     * @param vectorMask the {@code VectorMask<E>}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> lanewise(VectorOperators.Binary binary, long l,
            VectorMask<E> vectorMask);

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param vector the {@code Vector<E>}
     * @param vector2 the {@code Vector<E>}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> lanewise(VectorOperators.Ternary ternary, Vector<E> vector,
            Vector<E> vector2);

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param vector the {@code Vector<E>}
     * @param vector2 the {@code Vector<E>}
     * @param vectorMask the {@code VectorMask<E>}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> lanewise(VectorOperators.Ternary ternary, Vector<E> vector,
            Vector<E> vector2, VectorMask<E> vectorMask);

    /**
     * Adds lane by lane.
     *
     * @param vector the {@code Vector<E>}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> add(Vector<E> vector);

    /**
     * Adds lane by lane.
     *
     * @param vector the {@code Vector<E>}
     * @param vectorMask the {@code VectorMask<E>}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> add(Vector<E> vector, VectorMask<E> vectorMask);

    /**
     * Subtracts lane by lane.
     *
     * @param vector the {@code Vector<E>}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> sub(Vector<E> vector);

    /**
     * Subtracts lane by lane.
     *
     * @param vector the {@code Vector<E>}
     * @param vectorMask the {@code VectorMask<E>}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> sub(Vector<E> vector, VectorMask<E> vectorMask);

    /**
     * Multiplies lane by lane.
     *
     * @param vector the {@code Vector<E>}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> mul(Vector<E> vector);

    /**
     * Multiplies lane by lane.
     *
     * @param vector the {@code Vector<E>}
     * @param vectorMask the {@code VectorMask<E>}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> mul(Vector<E> vector, VectorMask<E> vectorMask);

    /**
     * Divides lane by lane.
     *
     * @param vector the {@code Vector<E>}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> div(Vector<E> vector);

    /**
     * Divides lane by lane.
     *
     * @param vector the {@code Vector<E>}
     * @param vectorMask the {@code VectorMask<E>}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> div(Vector<E> vector, VectorMask<E> vectorMask);

    /**
     * The negation of each lane.
     *
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> neg();

    /**
     * The absolute value of each lane.
     *
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> abs();

    /**
     * The smaller of each pair of lanes.
     *
     * @param vector the {@code Vector<E>}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> min(Vector<E> vector);

    /**
     * The larger of each pair of lanes.
     *
     * @param vector the {@code Vector<E>}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> max(Vector<E> vector);

    /**
     * Like {@code reduceLanes}, but the result is returned as a {@code long}.
     *
     * @param associative the {@code VectorOperators.Associative}
     * @return the number
     */
    public abstract long reduceLanesToLong(VectorOperators.Associative associative);

    /**
     * Like {@code reduceLanes}, but the result is returned as a {@code long}.
     *
     * @param associative the {@code VectorOperators.Associative}
     * @param vectorMask the {@code VectorMask<E>}
     * @return the number
     */
    public abstract long reduceLanesToLong(VectorOperators.Associative associative,
            VectorMask<E> vectorMask);

    /**
     * The mask of the lanes that pass that test.
     *
     * @param test the {@code VectorOperators.Test}
     * @return the {@code VectorMask<E>}
     */
    public abstract VectorMask<E> test(VectorOperators.Test test);

    /**
     * The mask of the lanes that pass that test.
     *
     * @param test the {@code VectorOperators.Test}
     * @param vectorMask the {@code VectorMask<E>}
     * @return the {@code VectorMask<E>}
     */
    public abstract VectorMask<E> test(VectorOperators.Test test, VectorMask<E> vectorMask);

    /**
     * The mask of the equal lanes.
     *
     * @param vector the {@code Vector<E>}
     * @return the {@code VectorMask<E>}
     */
    public abstract VectorMask<E> eq(Vector<E> vector);

    /**
     * The mask of the lanes that are less.
     *
     * @param vector the {@code Vector<E>}
     * @return the {@code VectorMask<E>}
     */
    public abstract VectorMask<E> lt(Vector<E> vector);

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param vector the {@code Vector<E>}
     * @return the {@code VectorMask<E>}
     */
    public abstract VectorMask<E> compare(VectorOperators.Comparison comparison, Vector<E> vector);

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param vector the {@code Vector<E>}
     * @param vectorMask the {@code VectorMask<E>}
     * @return the {@code VectorMask<E>}
     */
    public abstract VectorMask<E> compare(VectorOperators.Comparison comparison, Vector<E> vector,
            VectorMask<E> vectorMask);

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param l the {@code long}
     * @return the {@code VectorMask<E>}
     */
    public abstract VectorMask<E> compare(VectorOperators.Comparison comparison, long l);

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param l the {@code long}
     * @param vectorMask the {@code VectorMask<E>}
     * @return the {@code VectorMask<E>}
     */
    public abstract VectorMask<E> compare(VectorOperators.Comparison comparison, long l,
            VectorMask<E> vectorMask);

    /**
     * Blends two vectors, taking from one or the other according to the mask.
     *
     * @param vector the {@code Vector<E>}
     * @param vectorMask the {@code VectorMask<E>}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> blend(Vector<E> vector, VectorMask<E> vectorMask);

    /**
     * Blends two vectors, taking from one or the other according to the mask.
     *
     * @param l the {@code long}
     * @param vectorMask the {@code VectorMask<E>}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> blend(long l, VectorMask<E> vectorMask);

    /**
     * Adds to each lane its own index multiplied by that step.
     *
     * @param i the {@code int}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> addIndex(int i);

    /**
     * A vector starting at that lane.
     *
     * @param i the {@code int}
     * @param vector the {@code Vector<E>}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> slice(int i, Vector<E> vector);

    /**
     * A vector starting at that lane.
     *
     * @param i the {@code int}
     * @param vector the {@code Vector<E>}
     * @param vectorMask the {@code VectorMask<E>}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> slice(int i, Vector<E> vector, VectorMask<E> vectorMask);

    /**
     * A vector starting at that lane.
     *
     * @param i the {@code int}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> slice(int i);

    /**
     * The inverse of {@code slice}: puts the lanes back in their place.
     *
     * @param i the {@code int}
     * @param vector the {@code Vector<E>}
     * @param i2 the {@code int}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> unslice(int i, Vector<E> vector, int i2);

    /**
     * The inverse of {@code slice}: puts the lanes back in their place.
     *
     * @param i the {@code int}
     * @param vector the {@code Vector<E>}
     * @param i2 the {@code int}
     * @param vectorMask the {@code VectorMask<E>}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> unslice(int i, Vector<E> vector, int i2, VectorMask<E> vectorMask);

    /**
     * The inverse of {@code slice}: puts the lanes back in their place.
     *
     * @param i the {@code int}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> unslice(int i);

    /**
     * Rearranges the lanes according to that shuffle.
     *
     * @param vectorShuffle the {@code VectorShuffle<E>}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> rearrange(VectorShuffle<E> vectorShuffle);

    /**
     * Rearranges the lanes according to that shuffle.
     *
     * @param vectorShuffle the {@code VectorShuffle<E>}
     * @param vectorMask the {@code VectorMask<E>}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> rearrange(VectorShuffle<E> vectorShuffle, VectorMask<E> vectorMask);

    /**
     * Rearranges the lanes according to that shuffle.
     *
     * @param vectorShuffle the {@code VectorShuffle<E>}
     * @param vector the {@code Vector<E>}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> rearrange(VectorShuffle<E> vectorShuffle, Vector<E> vector);

    /**
     * Gathers the set lanes at the start.
     *
     * @param vectorMask the {@code VectorMask<E>}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> compress(VectorMask<E> vectorMask);

    /**
     * Spreads the lanes from the start into the places the mask marks.
     *
     * @param vectorMask the {@code VectorMask<E>}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> expand(VectorMask<E> vectorMask);

    /**
     * Takes from another vector the lanes this one indicates.
     *
     * @param vector the {@code Vector<E>}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> selectFrom(Vector<E> vector);

    /**
     * Takes from another vector the lanes this one indicates.
     *
     * @param vector the {@code Vector<E>}
     * @param vector2 the {@code Vector<E>}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> selectFrom(Vector<E> vector, Vector<E> vector2);

    /**
     * Takes from another vector the lanes this one indicates.
     *
     * @param vector the {@code Vector<E>}
     * @param vectorMask the {@code VectorMask<E>}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> selectFrom(Vector<E> vector, VectorMask<E> vectorMask);

    /**
     * A vector with the same value in every lane.
     *
     * @param l the {@code long}
     * @return the {@code Vector<E>}
     */
    public abstract Vector<E> broadcast(long l);

    /**
     * A mask with every lane at that value.
     *
     * @param flag the {@code boolean}
     * @return the {@code VectorMask<E>}
     */
    public abstract VectorMask<E> maskAll(boolean flag);

    /**
     * This vector seen as a shuffle.
     *
     * @return the {@code VectorShuffle<E>}
     */
    public abstract VectorShuffle<E> toShuffle();

    /**
     * The same bits read as another species.
     *
     * @param <F> the type, in its boxed form
     * @param vectorSpecies the {@code VectorSpecies<F>}
     * @param i the {@code int}
     * @return the {@code Vector<F>}
     */
    public abstract <F extends Object> Vector<F> reinterpretShape(VectorSpecies<F> vectorSpecies,
            int i);

    /**
     * The same bits read as {@code byte}.
     *
     * @return the {@code ByteVector}
     */
    public abstract ByteVector reinterpretAsBytes();

    /**
     * The same bits read as {@code short}.
     *
     * @return the {@code ShortVector}
     */
    public abstract ShortVector reinterpretAsShorts();

    /**
     * The same bits read as {@code int}.
     *
     * @return the {@code IntVector}
     */
    public abstract IntVector reinterpretAsInts();

    /**
     * The same bits read as {@code long}.
     *
     * @return the {@code LongVector}
     */
    public abstract LongVector reinterpretAsLongs();

    /**
     * The same bits read as {@code float}.
     *
     * @return the {@code FloatVector}
     */
    public abstract FloatVector reinterpretAsFloats();

    /**
     * The same bits read as {@code double}.
     *
     * @return the {@code DoubleVector}
     */
    public abstract DoubleVector reinterpretAsDoubles();

    /**
     * The same bits seen as the integral type of the same size.
     *
     * @return the {@code Vector<?>}
     */
    public abstract Vector<?> viewAsIntegralLanes();

    /**
     * The same bits seen as the floating-point type of the same size.
     *
     * @return the {@code Vector<?>}
     */
    public abstract Vector<?> viewAsFloatingLanes();

    /**
     * Converts the lanes with that conversion operator.
     *
     * @param <F> the type, in its boxed form
     * @param conversion the {@code VectorOperators.Conversion<E, F>}
     * @param i the {@code int}
     * @return the {@code Vector<F>}
     */
    public abstract <F extends Object> Vector<F> convert(
            VectorOperators.Conversion<E, F> conversion, int i);

    /**
     * Converts to another species, taking the part indicated.
     *
     * @param <F> the type, in its boxed form
     * @param conversion the {@code VectorOperators.Conversion<E, F>}
     * @param vectorSpecies the {@code VectorSpecies<F>}
     * @param i the {@code int}
     * @return the {@code Vector<F>}
     */
    public abstract <F extends Object> Vector<F> convertShape(
            VectorOperators.Conversion<E, F> conversion, VectorSpecies<F> vectorSpecies, int i);

    /**
     * Like {@code convertShape}, but always preserving the value.
     *
     * @param <F> the type, in its boxed form
     * @param vectorSpecies the {@code VectorSpecies<F>}
     * @param i the {@code int}
     * @return the {@code Vector<F>}
     */
    public abstract <F extends Object> Vector<F> castShape(VectorSpecies<F> vectorSpecies, int i);

    /**
     * Checks that the lane type is that one and returns the same, already typed.
     *
     * @param <F> the type, in its boxed form
     * @param classArg the {@code Class<F>}
     * @return the {@code Vector<F>}
     */
    public abstract <F extends Object> Vector<F> check(Class<F> classArg);

    /**
     * Checks that the lane type is that one and returns the same, already typed.
     *
     * @param <F> the type, in its boxed form
     * @param vectorSpecies the {@code VectorSpecies<F>}
     * @return the {@code Vector<F>}
     */
    public abstract <F extends Object> Vector<F> check(VectorSpecies<F> vectorSpecies);

    /**
     * Writes the vector into that memory segment.
     *
     * @param memorySegment the {@code java.lang.foreign.MemorySegment}
     * @param l the {@code long}
     * @param byteOrder the {@code java.nio.ByteOrder}
     */
    public abstract void intoMemorySegment(java.lang.foreign.MemorySegment memorySegment, long l,
            java.nio.ByteOrder byteOrder);

    /**
     * Writes the vector into that memory segment.
     *
     * @param memorySegment the {@code java.lang.foreign.MemorySegment}
     * @param l the {@code long}
     * @param byteOrder the {@code java.nio.ByteOrder}
     * @param vectorMask the {@code VectorMask<E>}
     */
    public abstract void intoMemorySegment(java.lang.foreign.MemorySegment memorySegment, long l,
            java.nio.ByteOrder byteOrder, VectorMask<E> vectorMask);

    /**
     * The lanes in a new array.
     *
     * @return the {@code Object}
     */
    public abstract Object toArray();

    /**
     * The lanes in a new {@code int} array.
     *
     * @return the {@code int[]}
     */
    public abstract int[] toIntArray();

    /**
     * The lanes in a new {@code long} array.
     *
     * @return the {@code long[]}
     */
    public abstract long[] toLongArray();

    /**
     * The lanes in a new {@code double} array.
     *
     * @return the {@code double[]}
     */
    public abstract double[] toDoubleArray();

    /**
     * A readable representation.
     *
     * @return the text
     */
    public abstract String toString();

    /**
     * Whether the other one is equal to this one.
     *
     * @param obj the {@code Object}
     * @return true or false, as the case may be
     */
    public abstract boolean equals(Object obj);

    /**
     * The hash code.
     *
     * @return the number
     */
    public abstract int hashCode();
}
