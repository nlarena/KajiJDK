package jdk.incubator.vector;

import java.lang.foreign.MemorySegment;
import java.nio.ByteOrder;

/**
 * A vector of {@code int} lanes.
 *
 * <p>It is one of the six classes where the API becomes concrete. {@link Vector} talks about lanes
 * without saying what they are, and that is why its methods take and return {@code Object} or the
 * boxed type; here the lanes really are {@code int}, so one can load from a {@code int[]}, read a
 * lane as a {@code int} and operate without boxing anything.
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
public abstract class IntVector extends AbstractVector<Integer> {

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
    IntVector(Object payload) {
        super(payload);
    }

    /** The {@code int} species of 64 bits. */
    public static final VectorSpecies<Integer> SPECIES_64 =
            SpeciesImpl.create(int.class, VectorShape.S_64_BIT);

    /** The {@code int} species of 128 bits. */
    public static final VectorSpecies<Integer> SPECIES_128 =
            SpeciesImpl.create(int.class, VectorShape.S_128_BIT);

    /** The {@code int} species of 256 bits. */
    public static final VectorSpecies<Integer> SPECIES_256 =
            SpeciesImpl.create(int.class, VectorShape.S_256_BIT);

    /** The {@code int} species of 512 bits. */
    public static final VectorSpecies<Integer> SPECIES_512 =
            SpeciesImpl.create(int.class, VectorShape.S_512_BIT);

    /** The {@code int} species of this machine\'s largest shape. */
    public static final VectorSpecies<Integer> SPECIES_MAX =
            SpeciesImpl.create(int.class, VectorShape.S_Max_BIT);

    /** The {@code int} species of this machine\'s preferred shape. */
    public static final VectorSpecies<Integer> SPECIES_PREFERRED =
            SpeciesImpl.create(int.class, VectorShape.preferredShape());

    /**
     * A vector with every lane at zero.
     *
     * @param vectorSpecies the {@code VectorSpecies<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static IntVector zero(VectorSpecies<Integer> vectorSpecies) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector with the same value in every lane.
     *
     * @param i the {@code int}
     * @return the {@code IntVector}
     */
    public abstract IntVector broadcast(int i);

    /**
     * A vector with the same value in every lane.
     *
     * @param vectorSpecies the {@code VectorSpecies<Integer>}
     * @param i the {@code int}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static IntVector broadcast(VectorSpecies<Integer> vectorSpecies, int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector with the same value in every lane.
     *
     * @param l the {@code long}
     * @return the {@code IntVector}
     */
    public abstract IntVector broadcast(long l);

    /**
     * A vector with the same value in every lane.
     *
     * @param vectorSpecies the {@code VectorSpecies<Integer>}
     * @param l the {@code long}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static IntVector broadcast(VectorSpecies<Integer> vectorSpecies, long l) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param unary the {@code VectorOperators.Unary}
     * @return the {@code IntVector}
     */
    public abstract IntVector lanewise(VectorOperators.Unary unary);

    /**
     * Applies that operator to each lane.
     *
     * @param unary the {@code VectorOperators.Unary}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code IntVector}
     */
    public abstract IntVector lanewise(VectorOperators.Unary unary, VectorMask<Integer> vectorMask);

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param vector the {@code Vector<Integer>}
     * @return the {@code IntVector}
     */
    public abstract IntVector lanewise(VectorOperators.Binary binary, Vector<Integer> vector);

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param vector the {@code Vector<Integer>}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code IntVector}
     */
    public abstract IntVector lanewise(VectorOperators.Binary binary, Vector<Integer> vector,
            VectorMask<Integer> vectorMask);

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param i the {@code int}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector lanewise(VectorOperators.Binary binary, int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param i the {@code int}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector lanewise(VectorOperators.Binary binary, int i,
            VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param l the {@code long}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector lanewise(VectorOperators.Binary binary, long l) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param l the {@code long}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector lanewise(VectorOperators.Binary binary, long l,
            VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param vector the {@code Vector<Integer>}
     * @param vector2 the {@code Vector<Integer>}
     * @return the {@code IntVector}
     */
    public abstract IntVector lanewise(VectorOperators.Ternary ternary, Vector<Integer> vector,
            Vector<Integer> vector2);

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param vector the {@code Vector<Integer>}
     * @param vector2 the {@code Vector<Integer>}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code IntVector}
     */
    public abstract IntVector lanewise(VectorOperators.Ternary ternary, Vector<Integer> vector,
            Vector<Integer> vector2, VectorMask<Integer> vectorMask);

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param i the {@code int}
     * @param i2 the {@code int}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector lanewise(VectorOperators.Ternary ternary, int i, int i2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param i the {@code int}
     * @param i2 the {@code int}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector lanewise(VectorOperators.Ternary ternary, int i, int i2,
            VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param vector the {@code Vector<Integer>}
     * @param i the {@code int}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector lanewise(VectorOperators.Ternary ternary, Vector<Integer> vector, int i
            ) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param vector the {@code Vector<Integer>}
     * @param i the {@code int}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector lanewise(VectorOperators.Ternary ternary, Vector<Integer> vector, int i,
            VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param i the {@code int}
     * @param vector the {@code Vector<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector lanewise(VectorOperators.Ternary ternary, int i, Vector<Integer> vector
            ) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param i the {@code int}
     * @param vector the {@code Vector<Integer>}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector lanewise(VectorOperators.Ternary ternary, int i, Vector<Integer> vector,
            VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Adds lane by lane.
     *
     * @param vector the {@code Vector<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector add(Vector<Integer> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Adds lane by lane.
     *
     * @param i the {@code int}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector add(int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Adds lane by lane.
     *
     * @param vector the {@code Vector<Integer>}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector add(Vector<Integer> vector, VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Adds lane by lane.
     *
     * @param i the {@code int}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector add(int i, VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Subtracts lane by lane.
     *
     * @param vector the {@code Vector<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector sub(Vector<Integer> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Subtracts lane by lane.
     *
     * @param i the {@code int}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector sub(int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Subtracts lane by lane.
     *
     * @param vector the {@code Vector<Integer>}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector sub(Vector<Integer> vector, VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Subtracts lane by lane.
     *
     * @param i the {@code int}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector sub(int i, VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Multiplies lane by lane.
     *
     * @param vector the {@code Vector<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector mul(Vector<Integer> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Multiplies lane by lane.
     *
     * @param i the {@code int}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector mul(int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Multiplies lane by lane.
     *
     * @param vector the {@code Vector<Integer>}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector mul(Vector<Integer> vector, VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Multiplies lane by lane.
     *
     * @param i the {@code int}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector mul(int i, VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Divides lane by lane.
     *
     * @param vector the {@code Vector<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector div(Vector<Integer> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Divides lane by lane.
     *
     * @param i the {@code int}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector div(int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Divides lane by lane.
     *
     * @param vector the {@code Vector<Integer>}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector div(Vector<Integer> vector, VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Divides lane by lane.
     *
     * @param i the {@code int}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector div(int i, VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The smaller of each pair of lanes.
     *
     * @param vector the {@code Vector<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector min(Vector<Integer> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The smaller of each pair of lanes.
     *
     * @param i the {@code int}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector min(int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The larger of each pair of lanes.
     *
     * @param vector the {@code Vector<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector max(Vector<Integer> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The larger of each pair of lanes.
     *
     * @param i the {@code int}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector max(int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The bitwise conjunction.
     *
     * @param vector the {@code Vector<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector and(Vector<Integer> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The bitwise conjunction.
     *
     * @param i the {@code int}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector and(int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The bitwise disjunction.
     *
     * @param vector the {@code Vector<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector or(Vector<Integer> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The bitwise disjunction.
     *
     * @param i the {@code int}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector or(int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The negation of each lane.
     *
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector neg() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The absolute value of each lane.
     *
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector abs() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Inverts each lane.
     *
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector not() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The mask of the equal lanes.
     *
     * @param vector the {@code Vector<Integer>}
     * @return the {@code VectorMask<Integer>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Integer> eq(Vector<Integer> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The mask of the equal lanes.
     *
     * @param i the {@code int}
     * @return the {@code VectorMask<Integer>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Integer> eq(int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The mask of the lanes that are less.
     *
     * @param vector the {@code Vector<Integer>}
     * @return the {@code VectorMask<Integer>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Integer> lt(Vector<Integer> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The mask of the lanes that are less.
     *
     * @param i the {@code int}
     * @return the {@code VectorMask<Integer>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Integer> lt(int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The mask of the lanes that pass that test.
     *
     * @param test the {@code VectorOperators.Test}
     * @return the {@code VectorMask<Integer>}
     */
    public abstract VectorMask<Integer> test(VectorOperators.Test test);

    /**
     * The mask of the lanes that pass that test.
     *
     * @param test the {@code VectorOperators.Test}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code VectorMask<Integer>}
     */
    public abstract VectorMask<Integer> test(VectorOperators.Test test,
            VectorMask<Integer> vectorMask);

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param vector the {@code Vector<Integer>}
     * @return the {@code VectorMask<Integer>}
     */
    public abstract VectorMask<Integer> compare(VectorOperators.Comparison comparison,
            Vector<Integer> vector);

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param i the {@code int}
     * @return the {@code VectorMask<Integer>}
     */
    public abstract VectorMask<Integer> compare(VectorOperators.Comparison comparison, int i);

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param i the {@code int}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code VectorMask<Integer>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Integer> compare(VectorOperators.Comparison comparison, int i,
            VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param l the {@code long}
     * @return the {@code VectorMask<Integer>}
     */
    public abstract VectorMask<Integer> compare(VectorOperators.Comparison comparison, long l);

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param l the {@code long}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code VectorMask<Integer>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Integer> compare(VectorOperators.Comparison comparison, long l,
            VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Blends two vectors, taking from one or the other according to the mask.
     *
     * @param vector the {@code Vector<Integer>}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code IntVector}
     */
    public abstract IntVector blend(Vector<Integer> vector, VectorMask<Integer> vectorMask);

    /**
     * Adds to each lane its own index multiplied by that step.
     *
     * @param i the {@code int}
     * @return the {@code IntVector}
     */
    public abstract IntVector addIndex(int i);

    /**
     * Blends two vectors, taking from one or the other according to the mask.
     *
     * @param i the {@code int}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector blend(int i, VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Blends two vectors, taking from one or the other according to the mask.
     *
     * @param l the {@code long}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector blend(long l, VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector starting at that lane.
     *
     * @param i the {@code int}
     * @param vector the {@code Vector<Integer>}
     * @return the {@code IntVector}
     */
    public abstract IntVector slice(int i, Vector<Integer> vector);

    /**
     * A vector starting at that lane.
     *
     * @param i the {@code int}
     * @param vector the {@code Vector<Integer>}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector slice(int i, Vector<Integer> vector, VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector starting at that lane.
     *
     * @param i the {@code int}
     * @return the {@code IntVector}
     */
    public abstract IntVector slice(int i);

    /**
     * The inverse of {@code slice}: puts the lanes back in their place.
     *
     * @param i the {@code int}
     * @param vector the {@code Vector<Integer>}
     * @param i2 the {@code int}
     * @return the {@code IntVector}
     */
    public abstract IntVector unslice(int i, Vector<Integer> vector, int i2);

    /**
     * The inverse of {@code slice}: puts the lanes back in their place.
     *
     * @param i the {@code int}
     * @param vector the {@code Vector<Integer>}
     * @param i2 the {@code int}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code IntVector}
     */
    public abstract IntVector unslice(int i, Vector<Integer> vector, int i2,
            VectorMask<Integer> vectorMask);

    /**
     * The inverse of {@code slice}: puts the lanes back in their place.
     *
     * @param i the {@code int}
     * @return the {@code IntVector}
     */
    public abstract IntVector unslice(int i);

    /**
     * Rearranges the lanes according to that shuffle.
     *
     * @param vectorShuffle the {@code VectorShuffle<Integer>}
     * @return the {@code IntVector}
     */
    public abstract IntVector rearrange(VectorShuffle<Integer> vectorShuffle);

    /**
     * Rearranges the lanes according to that shuffle.
     *
     * @param vectorShuffle the {@code VectorShuffle<Integer>}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code IntVector}
     */
    public abstract IntVector rearrange(VectorShuffle<Integer> vectorShuffle,
            VectorMask<Integer> vectorMask);

    /**
     * Rearranges the lanes according to that shuffle.
     *
     * @param vectorShuffle the {@code VectorShuffle<Integer>}
     * @param vector the {@code Vector<Integer>}
     * @return the {@code IntVector}
     */
    public abstract IntVector rearrange(VectorShuffle<Integer> vectorShuffle, Vector<Integer> vector
            );

    /**
     * Gathers the set lanes at the start.
     *
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code IntVector}
     */
    public abstract IntVector compress(VectorMask<Integer> vectorMask);

    /**
     * Spreads the lanes from the start into the places the mask marks.
     *
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code IntVector}
     */
    public abstract IntVector expand(VectorMask<Integer> vectorMask);

    /**
     * Takes from another vector the lanes this one indicates.
     *
     * @param vector the {@code Vector<Integer>}
     * @return the {@code IntVector}
     */
    public abstract IntVector selectFrom(Vector<Integer> vector);

    /**
     * Takes from another vector the lanes this one indicates.
     *
     * @param vector the {@code Vector<Integer>}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code IntVector}
     */
    public abstract IntVector selectFrom(Vector<Integer> vector, VectorMask<Integer> vectorMask);

    /**
     * Takes from another vector the lanes this one indicates.
     *
     * @param vector the {@code Vector<Integer>}
     * @param vector2 the {@code Vector<Integer>}
     * @return the {@code IntVector}
     */
    public abstract IntVector selectFrom(Vector<Integer> vector, Vector<Integer> vector2);

    /**
     * Chooses bit by bit between two vectors according to a third.
     *
     * @param vector the {@code Vector<Integer>}
     * @param vector2 the {@code Vector<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector bitwiseBlend(Vector<Integer> vector, Vector<Integer> vector2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Chooses bit by bit between two vectors according to a third.
     *
     * @param i the {@code int}
     * @param i2 the {@code int}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector bitwiseBlend(int i, int i2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Chooses bit by bit between two vectors according to a third.
     *
     * @param i the {@code int}
     * @param vector the {@code Vector<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector bitwiseBlend(int i, Vector<Integer> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Chooses bit by bit between two vectors according to a third.
     *
     * @param vector the {@code Vector<Integer>}
     * @param i the {@code int}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector bitwiseBlend(Vector<Integer> vector, int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Combines all the lanes into a single value with that operator.
     *
     * @param associative the {@code VectorOperators.Associative}
     * @return the number
     */
    public abstract int reduceLanes(VectorOperators.Associative associative);

    /**
     * Combines all the lanes into a single value with that operator.
     *
     * @param associative the {@code VectorOperators.Associative}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the number
     */
    public abstract int reduceLanes(VectorOperators.Associative associative,
            VectorMask<Integer> vectorMask);

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
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the number
     */
    public abstract long reduceLanesToLong(VectorOperators.Associative associative,
            VectorMask<Integer> vectorMask);

    /**
     * The value of that lane.
     *
     * @param i the {@code int}
     * @return the number
     */
    public abstract int lane(int i);

    /**
     * The same vector with that lane changed.
     *
     * @param i the {@code int}
     * @param i2 the {@code int}
     * @return the {@code IntVector}
     */
    public abstract IntVector withLane(int i, int i2);

    /**
     * The lanes in a new array.
     *
     * @return the {@code int[]}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final int[] toArray() {
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
     * @param vectorSpecies the {@code VectorSpecies<Integer>}
     * @param is the {@code int[]}
     * @param i the {@code int}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static IntVector fromArray(VectorSpecies<Integer> vectorSpecies, int[] is, int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that array.
     *
     * @param vectorSpecies the {@code VectorSpecies<Integer>}
     * @param is the {@code int[]}
     * @param i the {@code int}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static IntVector fromArray(VectorSpecies<Integer> vectorSpecies, int[] is, int i,
            VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that array.
     *
     * @param vectorSpecies the {@code VectorSpecies<Integer>}
     * @param is the {@code int[]}
     * @param i the {@code int}
     * @param is2 the {@code int[]}
     * @param i2 the {@code int}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static IntVector fromArray(VectorSpecies<Integer> vectorSpecies, int[] is, int i,
            int[] is2, int i2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that array.
     *
     * @param vectorSpecies the {@code VectorSpecies<Integer>}
     * @param is the {@code int[]}
     * @param i the {@code int}
     * @param is2 the {@code int[]}
     * @param i2 the {@code int}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static IntVector fromArray(VectorSpecies<Integer> vectorSpecies, int[] is, int i,
            int[] is2, int i2, VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that memory segment.
     *
     * @param vectorSpecies the {@code VectorSpecies<Integer>}
     * @param memorySegment the {@code java.lang.foreign.MemorySegment}
     * @param l the {@code long}
     * @param byteOrder the {@code java.nio.ByteOrder}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static IntVector fromMemorySegment(VectorSpecies<Integer> vectorSpecies,
            java.lang.foreign.MemorySegment memorySegment, long l, java.nio.ByteOrder byteOrder) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that memory segment.
     *
     * @param vectorSpecies the {@code VectorSpecies<Integer>}
     * @param memorySegment the {@code java.lang.foreign.MemorySegment}
     * @param l the {@code long}
     * @param byteOrder the {@code java.nio.ByteOrder}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static IntVector fromMemorySegment(VectorSpecies<Integer> vectorSpecies,
            java.lang.foreign.MemorySegment memorySegment, long l, java.nio.ByteOrder byteOrder,
            VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the vector into that array.
     *
     * @param is the {@code int[]}
     * @param i the {@code int}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoArray(int[] is, int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the vector into that array.
     *
     * @param is the {@code int[]}
     * @param i the {@code int}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoArray(int[] is, int i, VectorMask<Integer> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the vector into that array.
     *
     * @param is the {@code int[]}
     * @param i the {@code int}
     * @param is2 the {@code int[]}
     * @param i2 the {@code int}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoArray(int[] is, int i, int[] is2, int i2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the vector into that array.
     *
     * @param is the {@code int[]}
     * @param i the {@code int}
     * @param is2 the {@code int[]}
     * @param i2 the {@code int}
     * @param vectorMask the {@code VectorMask<Integer>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoArray(int[] is, int i, int[] is2, int i2, VectorMask<Integer> vectorMask
            ) {
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
     * @param vectorMask the {@code VectorMask<Integer>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoMemorySegment(java.lang.foreign.MemorySegment memorySegment, long l,
            java.nio.ByteOrder byteOrder, VectorMask<Integer> vectorMask) {
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
     * @return the {@code IntVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final IntVector viewAsIntegralLanes() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The same bits seen as the floating-point type of the same size.
     *
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector viewAsFloatingLanes() {
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
