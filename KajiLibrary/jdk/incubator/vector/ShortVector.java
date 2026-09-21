package jdk.incubator.vector;

import java.lang.foreign.MemorySegment;
import java.nio.ByteOrder;

/**
 * A vector of {@code short} lanes.
 *
 * <p>It is one of the six classes where the API becomes concrete. {@link Vector} talks about lanes
 * without saying what they are, and that is why its methods take and return {@code Object} or the
 * boxed type; here the lanes really are {@code short}, so one can load from a {@code short[]}, read
 * a lane as a {@code short} and operate without boxing anything.
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
public abstract class ShortVector extends AbstractVector<Short> {

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
    ShortVector(Object payload) {
        super(payload);
    }

    /** The {@code short} species of 64 bits. */
    public static final VectorSpecies<Short> SPECIES_64 =
            SpeciesImpl.create(short.class, VectorShape.S_64_BIT);

    /** The {@code short} species of 128 bits. */
    public static final VectorSpecies<Short> SPECIES_128 =
            SpeciesImpl.create(short.class, VectorShape.S_128_BIT);

    /** The {@code short} species of 256 bits. */
    public static final VectorSpecies<Short> SPECIES_256 =
            SpeciesImpl.create(short.class, VectorShape.S_256_BIT);

    /** The {@code short} species of 512 bits. */
    public static final VectorSpecies<Short> SPECIES_512 =
            SpeciesImpl.create(short.class, VectorShape.S_512_BIT);

    /** The {@code short} species of this machine\'s largest shape. */
    public static final VectorSpecies<Short> SPECIES_MAX =
            SpeciesImpl.create(short.class, VectorShape.S_Max_BIT);

    /** The {@code short} species of this machine\'s preferred shape. */
    public static final VectorSpecies<Short> SPECIES_PREFERRED =
            SpeciesImpl.create(short.class, VectorShape.preferredShape());

    /**
     * A vector with every lane at zero.
     *
     * @param vectorSpecies the {@code VectorSpecies<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static ShortVector zero(VectorSpecies<Short> vectorSpecies) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector with the same value in every lane.
     *
     * @param s the {@code short}
     * @return the {@code ShortVector}
     */
    public abstract ShortVector broadcast(short s);

    /**
     * A vector with the same value in every lane.
     *
     * @param vectorSpecies the {@code VectorSpecies<Short>}
     * @param s the {@code short}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static ShortVector broadcast(VectorSpecies<Short> vectorSpecies, short s) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector with the same value in every lane.
     *
     * @param l the {@code long}
     * @return the {@code ShortVector}
     */
    public abstract ShortVector broadcast(long l);

    /**
     * A vector with the same value in every lane.
     *
     * @param vectorSpecies the {@code VectorSpecies<Short>}
     * @param l the {@code long}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static ShortVector broadcast(VectorSpecies<Short> vectorSpecies, long l) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param unary the {@code VectorOperators.Unary}
     * @return the {@code ShortVector}
     */
    public abstract ShortVector lanewise(VectorOperators.Unary unary);

    /**
     * Applies that operator to each lane.
     *
     * @param unary the {@code VectorOperators.Unary}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code ShortVector}
     */
    public abstract ShortVector lanewise(VectorOperators.Unary unary, VectorMask<Short> vectorMask);

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param vector the {@code Vector<Short>}
     * @return the {@code ShortVector}
     */
    public abstract ShortVector lanewise(VectorOperators.Binary binary, Vector<Short> vector);

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param vector the {@code Vector<Short>}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code ShortVector}
     */
    public abstract ShortVector lanewise(VectorOperators.Binary binary, Vector<Short> vector,
            VectorMask<Short> vectorMask);

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param s the {@code short}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector lanewise(VectorOperators.Binary binary, short s) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param s the {@code short}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector lanewise(VectorOperators.Binary binary, short s,
            VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param l the {@code long}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector lanewise(VectorOperators.Binary binary, long l) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param l the {@code long}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector lanewise(VectorOperators.Binary binary, long l,
            VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param vector the {@code Vector<Short>}
     * @param vector2 the {@code Vector<Short>}
     * @return the {@code ShortVector}
     */
    public abstract ShortVector lanewise(VectorOperators.Ternary ternary, Vector<Short> vector,
            Vector<Short> vector2);

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param vector the {@code Vector<Short>}
     * @param vector2 the {@code Vector<Short>}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code ShortVector}
     */
    public abstract ShortVector lanewise(VectorOperators.Ternary ternary, Vector<Short> vector,
            Vector<Short> vector2, VectorMask<Short> vectorMask);

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param s the {@code short}
     * @param s2 the {@code short}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector lanewise(VectorOperators.Ternary ternary, short s, short s2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param s the {@code short}
     * @param s2 the {@code short}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector lanewise(VectorOperators.Ternary ternary, short s, short s2,
            VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param vector the {@code Vector<Short>}
     * @param s the {@code short}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector lanewise(VectorOperators.Ternary ternary, Vector<Short> vector, short s
            ) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param vector the {@code Vector<Short>}
     * @param s the {@code short}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector lanewise(VectorOperators.Ternary ternary, Vector<Short> vector,
            short s, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param s the {@code short}
     * @param vector the {@code Vector<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector lanewise(VectorOperators.Ternary ternary, short s, Vector<Short> vector
            ) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param s the {@code short}
     * @param vector the {@code Vector<Short>}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector lanewise(VectorOperators.Ternary ternary, short s,
            Vector<Short> vector, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Adds lane by lane.
     *
     * @param vector the {@code Vector<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector add(Vector<Short> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Adds lane by lane.
     *
     * @param s the {@code short}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector add(short s) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Adds lane by lane.
     *
     * @param vector the {@code Vector<Short>}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector add(Vector<Short> vector, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Adds lane by lane.
     *
     * @param s the {@code short}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector add(short s, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Subtracts lane by lane.
     *
     * @param vector the {@code Vector<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector sub(Vector<Short> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Subtracts lane by lane.
     *
     * @param s the {@code short}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector sub(short s) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Subtracts lane by lane.
     *
     * @param vector the {@code Vector<Short>}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector sub(Vector<Short> vector, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Subtracts lane by lane.
     *
     * @param s the {@code short}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector sub(short s, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Multiplies lane by lane.
     *
     * @param vector the {@code Vector<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector mul(Vector<Short> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Multiplies lane by lane.
     *
     * @param s the {@code short}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector mul(short s) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Multiplies lane by lane.
     *
     * @param vector the {@code Vector<Short>}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector mul(Vector<Short> vector, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Multiplies lane by lane.
     *
     * @param s the {@code short}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector mul(short s, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Divides lane by lane.
     *
     * @param vector the {@code Vector<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector div(Vector<Short> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Divides lane by lane.
     *
     * @param s the {@code short}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector div(short s) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Divides lane by lane.
     *
     * @param vector the {@code Vector<Short>}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector div(Vector<Short> vector, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Divides lane by lane.
     *
     * @param s the {@code short}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector div(short s, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The smaller of each pair of lanes.
     *
     * @param vector the {@code Vector<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector min(Vector<Short> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The smaller of each pair of lanes.
     *
     * @param s the {@code short}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector min(short s) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The larger of each pair of lanes.
     *
     * @param vector the {@code Vector<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector max(Vector<Short> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The larger of each pair of lanes.
     *
     * @param s the {@code short}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector max(short s) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The bitwise conjunction.
     *
     * @param vector the {@code Vector<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector and(Vector<Short> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The bitwise conjunction.
     *
     * @param s the {@code short}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector and(short s) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The bitwise disjunction.
     *
     * @param vector the {@code Vector<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector or(Vector<Short> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The bitwise disjunction.
     *
     * @param s the {@code short}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector or(short s) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The negation of each lane.
     *
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector neg() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The absolute value of each lane.
     *
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector abs() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Inverts each lane.
     *
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector not() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The mask of the equal lanes.
     *
     * @param vector the {@code Vector<Short>}
     * @return the {@code VectorMask<Short>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Short> eq(Vector<Short> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The mask of the equal lanes.
     *
     * @param s the {@code short}
     * @return the {@code VectorMask<Short>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Short> eq(short s) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The mask of the lanes that are less.
     *
     * @param vector the {@code Vector<Short>}
     * @return the {@code VectorMask<Short>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Short> lt(Vector<Short> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The mask of the lanes that are less.
     *
     * @param s the {@code short}
     * @return the {@code VectorMask<Short>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Short> lt(short s) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The mask of the lanes that pass that test.
     *
     * @param test the {@code VectorOperators.Test}
     * @return the {@code VectorMask<Short>}
     */
    public abstract VectorMask<Short> test(VectorOperators.Test test);

    /**
     * The mask of the lanes that pass that test.
     *
     * @param test the {@code VectorOperators.Test}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code VectorMask<Short>}
     */
    public abstract VectorMask<Short> test(VectorOperators.Test test, VectorMask<Short> vectorMask);

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param vector the {@code Vector<Short>}
     * @return the {@code VectorMask<Short>}
     */
    public abstract VectorMask<Short> compare(VectorOperators.Comparison comparison,
            Vector<Short> vector);

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param s the {@code short}
     * @return the {@code VectorMask<Short>}
     */
    public abstract VectorMask<Short> compare(VectorOperators.Comparison comparison, short s);

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param s the {@code short}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code VectorMask<Short>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Short> compare(VectorOperators.Comparison comparison, short s,
            VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param l the {@code long}
     * @return the {@code VectorMask<Short>}
     */
    public abstract VectorMask<Short> compare(VectorOperators.Comparison comparison, long l);

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param l the {@code long}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code VectorMask<Short>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Short> compare(VectorOperators.Comparison comparison, long l,
            VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Blends two vectors, taking from one or the other according to the mask.
     *
     * @param vector the {@code Vector<Short>}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code ShortVector}
     */
    public abstract ShortVector blend(Vector<Short> vector, VectorMask<Short> vectorMask);

    /**
     * Adds to each lane its own index multiplied by that step.
     *
     * @param i the {@code int}
     * @return the {@code ShortVector}
     */
    public abstract ShortVector addIndex(int i);

    /**
     * Blends two vectors, taking from one or the other according to the mask.
     *
     * @param s the {@code short}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector blend(short s, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Blends two vectors, taking from one or the other according to the mask.
     *
     * @param l the {@code long}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector blend(long l, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector starting at that lane.
     *
     * @param i the {@code int}
     * @param vector the {@code Vector<Short>}
     * @return the {@code ShortVector}
     */
    public abstract ShortVector slice(int i, Vector<Short> vector);

    /**
     * A vector starting at that lane.
     *
     * @param i the {@code int}
     * @param vector the {@code Vector<Short>}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector slice(int i, Vector<Short> vector, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector starting at that lane.
     *
     * @param i the {@code int}
     * @return the {@code ShortVector}
     */
    public abstract ShortVector slice(int i);

    /**
     * The inverse of {@code slice}: puts the lanes back in their place.
     *
     * @param i the {@code int}
     * @param vector the {@code Vector<Short>}
     * @param i2 the {@code int}
     * @return the {@code ShortVector}
     */
    public abstract ShortVector unslice(int i, Vector<Short> vector, int i2);

    /**
     * The inverse of {@code slice}: puts the lanes back in their place.
     *
     * @param i the {@code int}
     * @param vector the {@code Vector<Short>}
     * @param i2 the {@code int}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code ShortVector}
     */
    public abstract ShortVector unslice(int i, Vector<Short> vector, int i2,
            VectorMask<Short> vectorMask);

    /**
     * The inverse of {@code slice}: puts the lanes back in their place.
     *
     * @param i the {@code int}
     * @return the {@code ShortVector}
     */
    public abstract ShortVector unslice(int i);

    /**
     * Rearranges the lanes according to that shuffle.
     *
     * @param vectorShuffle the {@code VectorShuffle<Short>}
     * @return the {@code ShortVector}
     */
    public abstract ShortVector rearrange(VectorShuffle<Short> vectorShuffle);

    /**
     * Rearranges the lanes according to that shuffle.
     *
     * @param vectorShuffle the {@code VectorShuffle<Short>}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code ShortVector}
     */
    public abstract ShortVector rearrange(VectorShuffle<Short> vectorShuffle,
            VectorMask<Short> vectorMask);

    /**
     * Rearranges the lanes according to that shuffle.
     *
     * @param vectorShuffle the {@code VectorShuffle<Short>}
     * @param vector the {@code Vector<Short>}
     * @return the {@code ShortVector}
     */
    public abstract ShortVector rearrange(VectorShuffle<Short> vectorShuffle, Vector<Short> vector);

    /**
     * Gathers the set lanes at the start.
     *
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code ShortVector}
     */
    public abstract ShortVector compress(VectorMask<Short> vectorMask);

    /**
     * Spreads the lanes from the start into the places the mask marks.
     *
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code ShortVector}
     */
    public abstract ShortVector expand(VectorMask<Short> vectorMask);

    /**
     * Takes from another vector the lanes this one indicates.
     *
     * @param vector the {@code Vector<Short>}
     * @return the {@code ShortVector}
     */
    public abstract ShortVector selectFrom(Vector<Short> vector);

    /**
     * Takes from another vector the lanes this one indicates.
     *
     * @param vector the {@code Vector<Short>}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code ShortVector}
     */
    public abstract ShortVector selectFrom(Vector<Short> vector, VectorMask<Short> vectorMask);

    /**
     * Takes from another vector the lanes this one indicates.
     *
     * @param vector the {@code Vector<Short>}
     * @param vector2 the {@code Vector<Short>}
     * @return the {@code ShortVector}
     */
    public abstract ShortVector selectFrom(Vector<Short> vector, Vector<Short> vector2);

    /**
     * Chooses bit by bit between two vectors according to a third.
     *
     * @param vector the {@code Vector<Short>}
     * @param vector2 the {@code Vector<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector bitwiseBlend(Vector<Short> vector, Vector<Short> vector2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Chooses bit by bit between two vectors according to a third.
     *
     * @param s the {@code short}
     * @param s2 the {@code short}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector bitwiseBlend(short s, short s2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Chooses bit by bit between two vectors according to a third.
     *
     * @param s the {@code short}
     * @param vector the {@code Vector<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector bitwiseBlend(short s, Vector<Short> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Chooses bit by bit between two vectors according to a third.
     *
     * @param vector the {@code Vector<Short>}
     * @param s the {@code short}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector bitwiseBlend(Vector<Short> vector, short s) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Combines all the lanes into a single value with that operator.
     *
     * @param associative the {@code VectorOperators.Associative}
     * @return the {@code short}
     */
    public abstract short reduceLanes(VectorOperators.Associative associative);

    /**
     * Combines all the lanes into a single value with that operator.
     *
     * @param associative the {@code VectorOperators.Associative}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code short}
     */
    public abstract short reduceLanes(VectorOperators.Associative associative,
            VectorMask<Short> vectorMask);

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
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the number
     */
    public abstract long reduceLanesToLong(VectorOperators.Associative associative,
            VectorMask<Short> vectorMask);

    /**
     * The value of that lane.
     *
     * @param i the {@code int}
     * @return the {@code short}
     */
    public abstract short lane(int i);

    /**
     * The same vector with that lane changed.
     *
     * @param i the {@code int}
     * @param s the {@code short}
     * @return the {@code ShortVector}
     */
    public abstract ShortVector withLane(int i, short s);

    /**
     * The lanes in a new array.
     *
     * @return the {@code short[]}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final short[] toArray() {
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
     * @param vectorSpecies the {@code VectorSpecies<Short>}
     * @param ss the {@code short[]}
     * @param i the {@code int}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static ShortVector fromArray(VectorSpecies<Short> vectorSpecies, short[] ss, int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that array.
     *
     * @param vectorSpecies the {@code VectorSpecies<Short>}
     * @param ss the {@code short[]}
     * @param i the {@code int}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static ShortVector fromArray(VectorSpecies<Short> vectorSpecies, short[] ss, int i,
            VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that array.
     *
     * @param vectorSpecies the {@code VectorSpecies<Short>}
     * @param ss the {@code short[]}
     * @param i the {@code int}
     * @param is the {@code int[]}
     * @param i2 the {@code int}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static ShortVector fromArray(VectorSpecies<Short> vectorSpecies, short[] ss, int i,
            int[] is, int i2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that array.
     *
     * @param vectorSpecies the {@code VectorSpecies<Short>}
     * @param ss the {@code short[]}
     * @param i the {@code int}
     * @param is the {@code int[]}
     * @param i2 the {@code int}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static ShortVector fromArray(VectorSpecies<Short> vectorSpecies, short[] ss, int i,
            int[] is, int i2, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that array of characters.
     *
     * @param vectorSpecies the {@code VectorSpecies<Short>}
     * @param chars the {@code char[]}
     * @param i the {@code int}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static ShortVector fromCharArray(VectorSpecies<Short> vectorSpecies, char[] chars, int i
            ) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that array of characters.
     *
     * @param vectorSpecies the {@code VectorSpecies<Short>}
     * @param chars the {@code char[]}
     * @param i the {@code int}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static ShortVector fromCharArray(VectorSpecies<Short> vectorSpecies, char[] chars, int i,
            VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that array of characters.
     *
     * @param vectorSpecies the {@code VectorSpecies<Short>}
     * @param chars the {@code char[]}
     * @param i the {@code int}
     * @param is the {@code int[]}
     * @param i2 the {@code int}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static ShortVector fromCharArray(VectorSpecies<Short> vectorSpecies, char[] chars, int i,
            int[] is, int i2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that array of characters.
     *
     * @param vectorSpecies the {@code VectorSpecies<Short>}
     * @param chars the {@code char[]}
     * @param i the {@code int}
     * @param is the {@code int[]}
     * @param i2 the {@code int}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static ShortVector fromCharArray(VectorSpecies<Short> vectorSpecies, char[] chars, int i,
            int[] is, int i2, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that memory segment.
     *
     * @param vectorSpecies the {@code VectorSpecies<Short>}
     * @param memorySegment the {@code java.lang.foreign.MemorySegment}
     * @param l the {@code long}
     * @param byteOrder the {@code java.nio.ByteOrder}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static ShortVector fromMemorySegment(VectorSpecies<Short> vectorSpecies,
            java.lang.foreign.MemorySegment memorySegment, long l, java.nio.ByteOrder byteOrder) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that memory segment.
     *
     * @param vectorSpecies the {@code VectorSpecies<Short>}
     * @param memorySegment the {@code java.lang.foreign.MemorySegment}
     * @param l the {@code long}
     * @param byteOrder the {@code java.nio.ByteOrder}
     * @param vectorMask the {@code VectorMask<Short>}
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static ShortVector fromMemorySegment(VectorSpecies<Short> vectorSpecies,
            java.lang.foreign.MemorySegment memorySegment, long l, java.nio.ByteOrder byteOrder,
            VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the vector into that array.
     *
     * @param ss the {@code short[]}
     * @param i the {@code int}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoArray(short[] ss, int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the vector into that array.
     *
     * @param ss the {@code short[]}
     * @param i the {@code int}
     * @param vectorMask the {@code VectorMask<Short>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoArray(short[] ss, int i, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the vector into that array.
     *
     * @param ss the {@code short[]}
     * @param i the {@code int}
     * @param is the {@code int[]}
     * @param i2 the {@code int}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoArray(short[] ss, int i, int[] is, int i2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the vector into that array.
     *
     * @param ss the {@code short[]}
     * @param i the {@code int}
     * @param is the {@code int[]}
     * @param i2 the {@code int}
     * @param vectorMask the {@code VectorMask<Short>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoArray(short[] ss, int i, int[] is, int i2, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the vector into that array of characters.
     *
     * @param chars the {@code char[]}
     * @param i the {@code int}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoCharArray(char[] chars, int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the vector into that array of characters.
     *
     * @param chars the {@code char[]}
     * @param i the {@code int}
     * @param vectorMask the {@code VectorMask<Short>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoCharArray(char[] chars, int i, VectorMask<Short> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the vector into that array of characters.
     *
     * @param chars the {@code char[]}
     * @param i the {@code int}
     * @param is the {@code int[]}
     * @param i2 the {@code int}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoCharArray(char[] chars, int i, int[] is, int i2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the vector into that array of characters.
     *
     * @param chars the {@code char[]}
     * @param i the {@code int}
     * @param is the {@code int[]}
     * @param i2 the {@code int}
     * @param vectorMask the {@code VectorMask<Short>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoCharArray(char[] chars, int i, int[] is, int i2,
            VectorMask<Short> vectorMask) {
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
     * @param vectorMask the {@code VectorMask<Short>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoMemorySegment(java.lang.foreign.MemorySegment memorySegment, long l,
            java.nio.ByteOrder byteOrder, VectorMask<Short> vectorMask) {
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
     * @return the {@code ShortVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ShortVector viewAsIntegralLanes() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The same bits seen as the floating-point type of the same size.
     *
     * @return the {@code Vector<?>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final Vector<?> viewAsFloatingLanes() {
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
