package jdk.incubator.vector;

import java.lang.foreign.MemorySegment;
import java.nio.ByteOrder;

/**
 * A vector of {@code long} lanes.
 *
 * <p>It is one of the six classes where the API becomes concrete. {@link Vector} talks about lanes
 * without saying what they are, and that is why its methods take and return {@code Object} or the
 * boxed type; here the lanes really are {@code long}, so one can load from a {@code long[]}, read a
 * lane as a {@code long} and operate without boxing anything.
 *
 * <h2>The {@code SPECIES_} constants</h2>
 *
 * <p>Each one is this class with a shape already chosen, and they are real objects: they answer how
 * many lanes they have, how much they take and where a loop that advances one vector at a time
 * ends. What they cannot do is make the vector.
 *
 * <p>{@link #SPECIES_MAX} and {@link #SPECIES_PREFERRED} depend on the machine. Here both give 64
 * bits, which is the API's minimum: the real maximum comes from the intrinsics and there is nobody
 * to ask.
 *
 * <h2>State on this VM</h2>
 *
 * <p>The signatures are all here and they are JDK 25's, so code that uses this API compiles. No
 * operation can run: creating or operating on a vector relies on VM intrinsics --each operation is
 * replaced by a vector instruction of the machine-- and this VM does not have them. Each concrete
 * method throws {@link UnsupportedOperationException} instead of returning a made-up vector, which
 * is the only honest thing to do -- a vector of zeros would compile all the same and give wrong
 * results without warning.
 *
 * @since 16
 */
public abstract class LongVector extends AbstractVector<Long> {

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
    LongVector(Object payload) {
        super(payload);
    }

    /** The {@code long} species of 64 bits. */
    public static final VectorSpecies<Long> SPECIES_64 =
            SpeciesImpl.create(long.class, VectorShape.S_64_BIT);

    /** The {@code long} species of 128 bits. */
    public static final VectorSpecies<Long> SPECIES_128 =
            SpeciesImpl.create(long.class, VectorShape.S_128_BIT);

    /** The {@code long} species of 256 bits. */
    public static final VectorSpecies<Long> SPECIES_256 =
            SpeciesImpl.create(long.class, VectorShape.S_256_BIT);

    /** The {@code long} species of 512 bits. */
    public static final VectorSpecies<Long> SPECIES_512 =
            SpeciesImpl.create(long.class, VectorShape.S_512_BIT);

    /** The {@code long} species of this machine\'s largest shape. */
    public static final VectorSpecies<Long> SPECIES_MAX =
            SpeciesImpl.create(long.class, VectorShape.S_Max_BIT);

    /** The {@code long} species of this machine\'s preferred shape. */
    public static final VectorSpecies<Long> SPECIES_PREFERRED =
            SpeciesImpl.create(long.class, VectorShape.preferredShape());

    /**
     * A vector with every lane at zero.
     *
     * @param vectorSpecies the {@code VectorSpecies<Long>}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static LongVector zero(VectorSpecies<Long> vectorSpecies) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector with the same value in every lane.
     *
     * @param l the {@code long}
     * @return the {@code LongVector}
     */
    public abstract LongVector broadcast(long l);

    /**
     * A vector with the same value in every lane.
     *
     * @param vectorSpecies the {@code VectorSpecies<Long>}
     * @param l the {@code long}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static LongVector broadcast(VectorSpecies<Long> vectorSpecies, long l) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param unary the {@code VectorOperators.Unary}
     * @return the {@code LongVector}
     */
    public abstract LongVector lanewise(VectorOperators.Unary unary);

    /**
     * Applies that operator to each lane.
     *
     * @param unary the {@code VectorOperators.Unary}
     * @param vectorMask the {@code VectorMask<Long>}
     * @return the {@code LongVector}
     */
    public abstract LongVector lanewise(VectorOperators.Unary unary, VectorMask<Long> vectorMask);

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param vector the {@code Vector<Long>}
     * @return the {@code LongVector}
     */
    public abstract LongVector lanewise(VectorOperators.Binary binary, Vector<Long> vector);

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param vector the {@code Vector<Long>}
     * @param vectorMask the {@code VectorMask<Long>}
     * @return the {@code LongVector}
     */
    public abstract LongVector lanewise(VectorOperators.Binary binary, Vector<Long> vector,
            VectorMask<Long> vectorMask);

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param l the {@code long}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector lanewise(VectorOperators.Binary binary, long l) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param l the {@code long}
     * @param vectorMask the {@code VectorMask<Long>}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector lanewise(VectorOperators.Binary binary, long l,
            VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param vector the {@code Vector<Long>}
     * @param vector2 the {@code Vector<Long>}
     * @return the {@code LongVector}
     */
    public abstract LongVector lanewise(VectorOperators.Ternary ternary, Vector<Long> vector,
            Vector<Long> vector2);

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param vector the {@code Vector<Long>}
     * @param vector2 the {@code Vector<Long>}
     * @param vectorMask the {@code VectorMask<Long>}
     * @return the {@code LongVector}
     */
    public abstract LongVector lanewise(VectorOperators.Ternary ternary, Vector<Long> vector,
            Vector<Long> vector2, VectorMask<Long> vectorMask);

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param l the {@code long}
     * @param l2 the {@code long}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector lanewise(VectorOperators.Ternary ternary, long l, long l2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param l the {@code long}
     * @param l2 the {@code long}
     * @param vectorMask the {@code VectorMask<Long>}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector lanewise(VectorOperators.Ternary ternary, long l, long l2,
            VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param vector the {@code Vector<Long>}
     * @param l the {@code long}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector lanewise(VectorOperators.Ternary ternary, Vector<Long> vector, long l) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param vector the {@code Vector<Long>}
     * @param l the {@code long}
     * @param vectorMask the {@code VectorMask<Long>}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector lanewise(VectorOperators.Ternary ternary, Vector<Long> vector, long l,
            VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param l the {@code long}
     * @param vector the {@code Vector<Long>}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector lanewise(VectorOperators.Ternary ternary, long l, Vector<Long> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param l the {@code long}
     * @param vector the {@code Vector<Long>}
     * @param vectorMask the {@code VectorMask<Long>}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector lanewise(VectorOperators.Ternary ternary, long l, Vector<Long> vector,
            VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Adds lane by lane.
     *
     * @param vector the {@code Vector<Long>}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector add(Vector<Long> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Adds lane by lane.
     *
     * @param l the {@code long}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector add(long l) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Adds lane by lane.
     *
     * @param vector the {@code Vector<Long>}
     * @param vectorMask the {@code VectorMask<Long>}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector add(Vector<Long> vector, VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Adds lane by lane.
     *
     * @param l the {@code long}
     * @param vectorMask the {@code VectorMask<Long>}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector add(long l, VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Subtracts lane by lane.
     *
     * @param vector the {@code Vector<Long>}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector sub(Vector<Long> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Subtracts lane by lane.
     *
     * @param l the {@code long}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector sub(long l) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Subtracts lane by lane.
     *
     * @param vector the {@code Vector<Long>}
     * @param vectorMask the {@code VectorMask<Long>}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector sub(Vector<Long> vector, VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Subtracts lane by lane.
     *
     * @param l the {@code long}
     * @param vectorMask the {@code VectorMask<Long>}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector sub(long l, VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Multiplies lane by lane.
     *
     * @param vector the {@code Vector<Long>}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector mul(Vector<Long> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Multiplies lane by lane.
     *
     * @param l the {@code long}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector mul(long l) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Multiplies lane by lane.
     *
     * @param vector the {@code Vector<Long>}
     * @param vectorMask the {@code VectorMask<Long>}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector mul(Vector<Long> vector, VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Multiplies lane by lane.
     *
     * @param l the {@code long}
     * @param vectorMask the {@code VectorMask<Long>}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector mul(long l, VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Divides lane by lane.
     *
     * @param vector the {@code Vector<Long>}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector div(Vector<Long> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Divides lane by lane.
     *
     * @param l the {@code long}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector div(long l) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Divides lane by lane.
     *
     * @param vector the {@code Vector<Long>}
     * @param vectorMask the {@code VectorMask<Long>}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector div(Vector<Long> vector, VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Divides lane by lane.
     *
     * @param l the {@code long}
     * @param vectorMask the {@code VectorMask<Long>}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector div(long l, VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The smaller of each pair of lanes.
     *
     * @param vector the {@code Vector<Long>}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector min(Vector<Long> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The smaller of each pair of lanes.
     *
     * @param l the {@code long}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector min(long l) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The larger of each pair of lanes.
     *
     * @param vector the {@code Vector<Long>}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector max(Vector<Long> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The larger of each pair of lanes.
     *
     * @param l the {@code long}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector max(long l) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The bitwise conjunction.
     *
     * @param vector the {@code Vector<Long>}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector and(Vector<Long> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The bitwise conjunction.
     *
     * @param l the {@code long}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector and(long l) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The bitwise disjunction.
     *
     * @param vector the {@code Vector<Long>}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector or(Vector<Long> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The bitwise disjunction.
     *
     * @param l the {@code long}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector or(long l) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The negation of each lane.
     *
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector neg() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The absolute value of each lane.
     *
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector abs() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Inverts each lane.
     *
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector not() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The mask of the equal lanes.
     *
     * @param vector the {@code Vector<Long>}
     * @return the {@code VectorMask<Long>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Long> eq(Vector<Long> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The mask of the equal lanes.
     *
     * @param l the {@code long}
     * @return the {@code VectorMask<Long>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Long> eq(long l) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The mask of the lanes that are less.
     *
     * @param vector the {@code Vector<Long>}
     * @return the {@code VectorMask<Long>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Long> lt(Vector<Long> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The mask of the lanes that are less.
     *
     * @param l the {@code long}
     * @return the {@code VectorMask<Long>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Long> lt(long l) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The mask of the lanes that pass that test.
     *
     * @param test the {@code VectorOperators.Test}
     * @return the {@code VectorMask<Long>}
     */
    public abstract VectorMask<Long> test(VectorOperators.Test test);

    /**
     * The mask of the lanes that pass that test.
     *
     * @param test the {@code VectorOperators.Test}
     * @param vectorMask the {@code VectorMask<Long>}
     * @return the {@code VectorMask<Long>}
     */
    public abstract VectorMask<Long> test(VectorOperators.Test test, VectorMask<Long> vectorMask);

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param vector the {@code Vector<Long>}
     * @return the {@code VectorMask<Long>}
     */
    public abstract VectorMask<Long> compare(VectorOperators.Comparison comparison,
            Vector<Long> vector);

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param l the {@code long}
     * @return the {@code VectorMask<Long>}
     */
    public abstract VectorMask<Long> compare(VectorOperators.Comparison comparison, long l);

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param l the {@code long}
     * @param vectorMask the {@code VectorMask<Long>}
     * @return the {@code VectorMask<Long>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Long> compare(VectorOperators.Comparison comparison, long l,
            VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Blends two vectors, taking from one or the other according to the mask.
     *
     * @param vector the {@code Vector<Long>}
     * @param vectorMask the {@code VectorMask<Long>}
     * @return the {@code LongVector}
     */
    public abstract LongVector blend(Vector<Long> vector, VectorMask<Long> vectorMask);

    /**
     * Adds to each lane its own index multiplied by that step.
     *
     * @param i the {@code int}
     * @return the {@code LongVector}
     */
    public abstract LongVector addIndex(int i);

    /**
     * Blends two vectors, taking from one or the other according to the mask.
     *
     * @param l the {@code long}
     * @param vectorMask the {@code VectorMask<Long>}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector blend(long l, VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector starting at that lane.
     *
     * @param i the {@code int}
     * @param vector the {@code Vector<Long>}
     * @return the {@code LongVector}
     */
    public abstract LongVector slice(int i, Vector<Long> vector);

    /**
     * A vector starting at that lane.
     *
     * @param i the {@code int}
     * @param vector the {@code Vector<Long>}
     * @param vectorMask the {@code VectorMask<Long>}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector slice(int i, Vector<Long> vector, VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector starting at that lane.
     *
     * @param i the {@code int}
     * @return the {@code LongVector}
     */
    public abstract LongVector slice(int i);

    /**
     * The inverse of {@code slice}: puts the lanes back in their place.
     *
     * @param i the {@code int}
     * @param vector the {@code Vector<Long>}
     * @param i2 the {@code int}
     * @return the {@code LongVector}
     */
    public abstract LongVector unslice(int i, Vector<Long> vector, int i2);

    /**
     * The inverse of {@code slice}: puts the lanes back in their place.
     *
     * @param i the {@code int}
     * @param vector the {@code Vector<Long>}
     * @param i2 the {@code int}
     * @param vectorMask the {@code VectorMask<Long>}
     * @return the {@code LongVector}
     */
    public abstract LongVector unslice(int i, Vector<Long> vector, int i2,
            VectorMask<Long> vectorMask);

    /**
     * The inverse of {@code slice}: puts the lanes back in their place.
     *
     * @param i the {@code int}
     * @return the {@code LongVector}
     */
    public abstract LongVector unslice(int i);

    /**
     * Rearranges the lanes according to that shuffle.
     *
     * @param vectorShuffle the {@code VectorShuffle<Long>}
     * @return the {@code LongVector}
     */
    public abstract LongVector rearrange(VectorShuffle<Long> vectorShuffle);

    /**
     * Rearranges the lanes according to that shuffle.
     *
     * @param vectorShuffle the {@code VectorShuffle<Long>}
     * @param vectorMask the {@code VectorMask<Long>}
     * @return the {@code LongVector}
     */
    public abstract LongVector rearrange(VectorShuffle<Long> vectorShuffle,
            VectorMask<Long> vectorMask);

    /**
     * Rearranges the lanes according to that shuffle.
     *
     * @param vectorShuffle the {@code VectorShuffle<Long>}
     * @param vector the {@code Vector<Long>}
     * @return the {@code LongVector}
     */
    public abstract LongVector rearrange(VectorShuffle<Long> vectorShuffle, Vector<Long> vector);

    /**
     * Gathers the set lanes at the start.
     *
     * @param vectorMask the {@code VectorMask<Long>}
     * @return the {@code LongVector}
     */
    public abstract LongVector compress(VectorMask<Long> vectorMask);

    /**
     * Spreads the lanes from the start into the places the mask marks.
     *
     * @param vectorMask the {@code VectorMask<Long>}
     * @return the {@code LongVector}
     */
    public abstract LongVector expand(VectorMask<Long> vectorMask);

    /**
     * Takes from another vector the lanes this one indicates.
     *
     * @param vector the {@code Vector<Long>}
     * @return the {@code LongVector}
     */
    public abstract LongVector selectFrom(Vector<Long> vector);

    /**
     * Takes from another vector the lanes this one indicates.
     *
     * @param vector the {@code Vector<Long>}
     * @param vectorMask the {@code VectorMask<Long>}
     * @return the {@code LongVector}
     */
    public abstract LongVector selectFrom(Vector<Long> vector, VectorMask<Long> vectorMask);

    /**
     * Takes from another vector the lanes this one indicates.
     *
     * @param vector the {@code Vector<Long>}
     * @param vector2 the {@code Vector<Long>}
     * @return the {@code LongVector}
     */
    public abstract LongVector selectFrom(Vector<Long> vector, Vector<Long> vector2);

    /**
     * Chooses bit by bit between two vectors according to a third.
     *
     * @param vector the {@code Vector<Long>}
     * @param vector2 the {@code Vector<Long>}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector bitwiseBlend(Vector<Long> vector, Vector<Long> vector2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Chooses bit by bit between two vectors according to a third.
     *
     * @param l the {@code long}
     * @param l2 the {@code long}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector bitwiseBlend(long l, long l2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Chooses bit by bit between two vectors according to a third.
     *
     * @param l the {@code long}
     * @param vector the {@code Vector<Long>}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector bitwiseBlend(long l, Vector<Long> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Chooses bit by bit between two vectors according to a third.
     *
     * @param vector the {@code Vector<Long>}
     * @param l the {@code long}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector bitwiseBlend(Vector<Long> vector, long l) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Combines all the lanes into a single value with that operator.
     *
     * @param associative the {@code VectorOperators.Associative}
     * @return the number
     */
    public abstract long reduceLanes(VectorOperators.Associative associative);

    /**
     * Combines all the lanes into a single value with that operator.
     *
     * @param associative the {@code VectorOperators.Associative}
     * @param vectorMask the {@code VectorMask<Long>}
     * @return the number
     */
    public abstract long reduceLanes(VectorOperators.Associative associative,
            VectorMask<Long> vectorMask);

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
     * @param vectorMask the {@code VectorMask<Long>}
     * @return the number
     */
    public abstract long reduceLanesToLong(VectorOperators.Associative associative,
            VectorMask<Long> vectorMask);

    /**
     * The value of that lane.
     *
     * @param i the {@code int}
     * @return the number
     */
    public abstract long lane(int i);

    /**
     * The same vector with that lane changed.
     *
     * @param i the {@code int}
     * @param l the {@code long}
     * @return the {@code LongVector}
     */
    public abstract LongVector withLane(int i, long l);

    /**
     * The lanes in a new array.
     *
     * @return the {@code long[]}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final long[] toArray() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The lanes in a new {@code int} array.
     *
     * @return the {@code int[]}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final int[] toIntArray() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The lanes in a new {@code long} array.
     *
     * @return the {@code long[]}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final long[] toLongArray() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The lanes in a new {@code double} array.
     *
     * @return the {@code double[]}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final double[] toDoubleArray() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that array.
     *
     * @param vectorSpecies the {@code VectorSpecies<Long>}
     * @param ls the {@code long[]}
     * @param i the {@code int}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static LongVector fromArray(VectorSpecies<Long> vectorSpecies, long[] ls, int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that array.
     *
     * @param vectorSpecies the {@code VectorSpecies<Long>}
     * @param ls the {@code long[]}
     * @param i the {@code int}
     * @param vectorMask the {@code VectorMask<Long>}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static LongVector fromArray(VectorSpecies<Long> vectorSpecies, long[] ls, int i,
            VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that array.
     *
     * @param vectorSpecies the {@code VectorSpecies<Long>}
     * @param ls the {@code long[]}
     * @param i the {@code int}
     * @param is the {@code int[]}
     * @param i2 the {@code int}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static LongVector fromArray(VectorSpecies<Long> vectorSpecies, long[] ls, int i,
            int[] is, int i2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that array.
     *
     * @param vectorSpecies the {@code VectorSpecies<Long>}
     * @param ls the {@code long[]}
     * @param i the {@code int}
     * @param is the {@code int[]}
     * @param i2 the {@code int}
     * @param vectorMask the {@code VectorMask<Long>}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static LongVector fromArray(VectorSpecies<Long> vectorSpecies, long[] ls, int i,
            int[] is, int i2, VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that memory segment.
     *
     * @param vectorSpecies the {@code VectorSpecies<Long>}
     * @param memorySegment the {@code java.lang.foreign.MemorySegment}
     * @param l the {@code long}
     * @param byteOrder the {@code java.nio.ByteOrder}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static LongVector fromMemorySegment(VectorSpecies<Long> vectorSpecies,
            java.lang.foreign.MemorySegment memorySegment, long l, java.nio.ByteOrder byteOrder) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that memory segment.
     *
     * @param vectorSpecies the {@code VectorSpecies<Long>}
     * @param memorySegment the {@code java.lang.foreign.MemorySegment}
     * @param l the {@code long}
     * @param byteOrder the {@code java.nio.ByteOrder}
     * @param vectorMask the {@code VectorMask<Long>}
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static LongVector fromMemorySegment(VectorSpecies<Long> vectorSpecies,
            java.lang.foreign.MemorySegment memorySegment, long l, java.nio.ByteOrder byteOrder,
            VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the vector into that array.
     *
     * @param ls the {@code long[]}
     * @param i the {@code int}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoArray(long[] ls, int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the vector into that array.
     *
     * @param ls the {@code long[]}
     * @param i the {@code int}
     * @param vectorMask the {@code VectorMask<Long>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoArray(long[] ls, int i, VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the vector into that array.
     *
     * @param ls the {@code long[]}
     * @param i the {@code int}
     * @param is the {@code int[]}
     * @param i2 the {@code int}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoArray(long[] ls, int i, int[] is, int i2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the vector into that array.
     *
     * @param ls the {@code long[]}
     * @param i the {@code int}
     * @param is the {@code int[]}
     * @param i2 the {@code int}
     * @param vectorMask the {@code VectorMask<Long>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoArray(long[] ls, int i, int[] is, int i2, VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the vector into that memory segment.
     *
     * @param memorySegment the {@code java.lang.foreign.MemorySegment}
     * @param l the {@code long}
     * @param byteOrder the {@code java.nio.ByteOrder}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoMemorySegment(java.lang.foreign.MemorySegment memorySegment, long l,
            java.nio.ByteOrder byteOrder) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the vector into that memory segment.
     *
     * @param memorySegment the {@code java.lang.foreign.MemorySegment}
     * @param l the {@code long}
     * @param byteOrder the {@code java.nio.ByteOrder}
     * @param vectorMask the {@code VectorMask<Long>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoMemorySegment(java.lang.foreign.MemorySegment memorySegment, long l,
            java.nio.ByteOrder byteOrder, VectorMask<Long> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The same bits read as {@code byte}.
     *
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector reinterpretAsBytes() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The same bits seen as the integral type of the same size.
     *
     * @return the {@code LongVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final LongVector viewAsIntegralLanes() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The same bits seen as the floating-point type of the same size.
     *
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector viewAsFloatingLanes() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A readable representation.
     *
     * @return the text
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final String toString() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Whether the other one is equal to this one.
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
}
