package jdk.incubator.vector;

import java.lang.foreign.MemorySegment;
import java.nio.ByteOrder;

/**
 * A vector of {@code double} lanes.
 *
 * <p>It is one of the six classes where the API becomes concrete. {@link Vector} talks about lanes
 * without saying what they are, and that is why its methods take and return {@code Object} or the
 * boxed type; here the lanes really are {@code double}, so one can load from a {@code double[]},
 * read a lane as a {@code double} and operate without boxing anything.
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
public abstract class DoubleVector extends AbstractVector<Double> {

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
    DoubleVector(Object payload) {
        super(payload);
    }

    /** The {@code double} species of 64 bits. */
    public static final VectorSpecies<Double> SPECIES_64 =
            SpeciesImpl.create(double.class, VectorShape.S_64_BIT);

    /** The {@code double} species of 128 bits. */
    public static final VectorSpecies<Double> SPECIES_128 =
            SpeciesImpl.create(double.class, VectorShape.S_128_BIT);

    /** The {@code double} species of 256 bits. */
    public static final VectorSpecies<Double> SPECIES_256 =
            SpeciesImpl.create(double.class, VectorShape.S_256_BIT);

    /** The {@code double} species of 512 bits. */
    public static final VectorSpecies<Double> SPECIES_512 =
            SpeciesImpl.create(double.class, VectorShape.S_512_BIT);

    /** The {@code double} species of this machine\'s largest shape. */
    public static final VectorSpecies<Double> SPECIES_MAX =
            SpeciesImpl.create(double.class, VectorShape.S_Max_BIT);

    /** The {@code double} species of this machine\'s preferred shape. */
    public static final VectorSpecies<Double> SPECIES_PREFERRED =
            SpeciesImpl.create(double.class, VectorShape.preferredShape());

    /**
     * A vector with every lane at zero.
     *
     * @param vectorSpecies the {@code VectorSpecies<Double>}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static DoubleVector zero(VectorSpecies<Double> vectorSpecies) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector with the same value in every lane.
     *
     * @param d the {@code double}
     * @return the {@code DoubleVector}
     */
    public abstract DoubleVector broadcast(double d);

    /**
     * A vector with the same value in every lane.
     *
     * @param vectorSpecies the {@code VectorSpecies<Double>}
     * @param d the {@code double}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static DoubleVector broadcast(VectorSpecies<Double> vectorSpecies, double d) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector with the same value in every lane.
     *
     * @param l the {@code long}
     * @return the {@code DoubleVector}
     */
    public abstract DoubleVector broadcast(long l);

    /**
     * A vector with the same value in every lane.
     *
     * @param vectorSpecies the {@code VectorSpecies<Double>}
     * @param l the {@code long}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static DoubleVector broadcast(VectorSpecies<Double> vectorSpecies, long l) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param unary the {@code VectorOperators.Unary}
     * @return the {@code DoubleVector}
     */
    public abstract DoubleVector lanewise(VectorOperators.Unary unary);

    /**
     * Applies that operator to each lane.
     *
     * @param unary the {@code VectorOperators.Unary}
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code DoubleVector}
     */
    public abstract DoubleVector lanewise(VectorOperators.Unary unary, VectorMask<Double> vectorMask
            );

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param vector the {@code Vector<Double>}
     * @return the {@code DoubleVector}
     */
    public abstract DoubleVector lanewise(VectorOperators.Binary binary, Vector<Double> vector);

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param vector the {@code Vector<Double>}
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code DoubleVector}
     */
    public abstract DoubleVector lanewise(VectorOperators.Binary binary, Vector<Double> vector,
            VectorMask<Double> vectorMask);

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param d the {@code double}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector lanewise(VectorOperators.Binary binary, double d) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param d the {@code double}
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector lanewise(VectorOperators.Binary binary, double d,
            VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param l the {@code long}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector lanewise(VectorOperators.Binary binary, long l) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param l the {@code long}
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector lanewise(VectorOperators.Binary binary, long l,
            VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param vector the {@code Vector<Double>}
     * @param vector2 the {@code Vector<Double>}
     * @return the {@code DoubleVector}
     */
    public abstract DoubleVector lanewise(VectorOperators.Ternary ternary, Vector<Double> vector,
            Vector<Double> vector2);

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param vector the {@code Vector<Double>}
     * @param vector2 the {@code Vector<Double>}
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code DoubleVector}
     */
    public abstract DoubleVector lanewise(VectorOperators.Ternary ternary, Vector<Double> vector,
            Vector<Double> vector2, VectorMask<Double> vectorMask);

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param d the {@code double}
     * @param d2 the {@code double}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector lanewise(VectorOperators.Ternary ternary, double d, double d2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param d the {@code double}
     * @param d2 the {@code double}
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector lanewise(VectorOperators.Ternary ternary, double d, double d2,
            VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param vector the {@code Vector<Double>}
     * @param d the {@code double}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector lanewise(VectorOperators.Ternary ternary, Vector<Double> vector,
            double d) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param vector the {@code Vector<Double>}
     * @param d the {@code double}
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector lanewise(VectorOperators.Ternary ternary, Vector<Double> vector,
            double d, VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param d the {@code double}
     * @param vector the {@code Vector<Double>}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector lanewise(VectorOperators.Ternary ternary, double d,
            Vector<Double> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param d the {@code double}
     * @param vector the {@code Vector<Double>}
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector lanewise(VectorOperators.Ternary ternary, double d,
            Vector<Double> vector, VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Adds lane by lane.
     *
     * @param vector the {@code Vector<Double>}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector add(Vector<Double> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Adds lane by lane.
     *
     * @param d the {@code double}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector add(double d) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Adds lane by lane.
     *
     * @param vector the {@code Vector<Double>}
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector add(Vector<Double> vector, VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Adds lane by lane.
     *
     * @param d the {@code double}
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector add(double d, VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Subtracts lane by lane.
     *
     * @param vector the {@code Vector<Double>}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector sub(Vector<Double> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Subtracts lane by lane.
     *
     * @param d the {@code double}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector sub(double d) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Subtracts lane by lane.
     *
     * @param vector the {@code Vector<Double>}
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector sub(Vector<Double> vector, VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Subtracts lane by lane.
     *
     * @param d the {@code double}
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector sub(double d, VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Multiplies lane by lane.
     *
     * @param vector the {@code Vector<Double>}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector mul(Vector<Double> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Multiplies lane by lane.
     *
     * @param d the {@code double}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector mul(double d) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Multiplies lane by lane.
     *
     * @param vector the {@code Vector<Double>}
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector mul(Vector<Double> vector, VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Multiplies lane by lane.
     *
     * @param d the {@code double}
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector mul(double d, VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Divides lane by lane.
     *
     * @param vector the {@code Vector<Double>}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector div(Vector<Double> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Divides lane by lane.
     *
     * @param d the {@code double}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector div(double d) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Divides lane by lane.
     *
     * @param vector the {@code Vector<Double>}
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector div(Vector<Double> vector, VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Divides lane by lane.
     *
     * @param d the {@code double}
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector div(double d, VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The smaller of each pair of lanes.
     *
     * @param vector the {@code Vector<Double>}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector min(Vector<Double> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The smaller of each pair of lanes.
     *
     * @param d the {@code double}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector min(double d) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The larger of each pair of lanes.
     *
     * @param vector the {@code Vector<Double>}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector max(Vector<Double> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The larger of each pair of lanes.
     *
     * @param d the {@code double}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector max(double d) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Each lane raised to the corresponding lane of the other.
     *
     * @param vector the {@code Vector<Double>}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector pow(Vector<Double> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Each lane raised to the corresponding lane of the other.
     *
     * @param d the {@code double}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector pow(double d) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The negation of each lane.
     *
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector neg() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The absolute value of each lane.
     *
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector abs() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The square root of each lane.
     *
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector sqrt() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The mask of the equal lanes.
     *
     * @param vector the {@code Vector<Double>}
     * @return the {@code VectorMask<Double>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Double> eq(Vector<Double> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The mask of the equal lanes.
     *
     * @param d the {@code double}
     * @return the {@code VectorMask<Double>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Double> eq(double d) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The mask of the lanes that are less.
     *
     * @param vector the {@code Vector<Double>}
     * @return the {@code VectorMask<Double>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Double> lt(Vector<Double> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The mask of the lanes that are less.
     *
     * @param d the {@code double}
     * @return the {@code VectorMask<Double>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Double> lt(double d) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The mask of the lanes that pass that test.
     *
     * @param test the {@code VectorOperators.Test}
     * @return the {@code VectorMask<Double>}
     */
    public abstract VectorMask<Double> test(VectorOperators.Test test);

    /**
     * The mask of the lanes that pass that test.
     *
     * @param test the {@code VectorOperators.Test}
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code VectorMask<Double>}
     */
    public abstract VectorMask<Double> test(VectorOperators.Test test, VectorMask<Double> vectorMask
            );

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param vector the {@code Vector<Double>}
     * @return the {@code VectorMask<Double>}
     */
    public abstract VectorMask<Double> compare(VectorOperators.Comparison comparison,
            Vector<Double> vector);

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param d the {@code double}
     * @return the {@code VectorMask<Double>}
     */
    public abstract VectorMask<Double> compare(VectorOperators.Comparison comparison, double d);

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param d the {@code double}
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code VectorMask<Double>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Double> compare(VectorOperators.Comparison comparison, double d,
            VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param l the {@code long}
     * @return the {@code VectorMask<Double>}
     */
    public abstract VectorMask<Double> compare(VectorOperators.Comparison comparison, long l);

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param l the {@code long}
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code VectorMask<Double>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Double> compare(VectorOperators.Comparison comparison, long l,
            VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Blends two vectors, taking from one or the other according to the mask.
     *
     * @param vector the {@code Vector<Double>}
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code DoubleVector}
     */
    public abstract DoubleVector blend(Vector<Double> vector, VectorMask<Double> vectorMask);

    /**
     * Adds to each lane its own index multiplied by that step.
     *
     * @param i the {@code int}
     * @return the {@code DoubleVector}
     */
    public abstract DoubleVector addIndex(int i);

    /**
     * Blends two vectors, taking from one or the other according to the mask.
     *
     * @param d the {@code double}
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector blend(double d, VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Blends two vectors, taking from one or the other according to the mask.
     *
     * @param l the {@code long}
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector blend(long l, VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector starting at that lane.
     *
     * @param i the {@code int}
     * @param vector the {@code Vector<Double>}
     * @return the {@code DoubleVector}
     */
    public abstract DoubleVector slice(int i, Vector<Double> vector);

    /**
     * A vector starting at that lane.
     *
     * @param i the {@code int}
     * @param vector the {@code Vector<Double>}
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector slice(int i, Vector<Double> vector, VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector starting at that lane.
     *
     * @param i the {@code int}
     * @return the {@code DoubleVector}
     */
    public abstract DoubleVector slice(int i);

    /**
     * The inverse of {@code slice}: puts the lanes back in their place.
     *
     * @param i the {@code int}
     * @param vector the {@code Vector<Double>}
     * @param i2 the {@code int}
     * @return the {@code DoubleVector}
     */
    public abstract DoubleVector unslice(int i, Vector<Double> vector, int i2);

    /**
     * The inverse of {@code slice}: puts the lanes back in their place.
     *
     * @param i the {@code int}
     * @param vector the {@code Vector<Double>}
     * @param i2 the {@code int}
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code DoubleVector}
     */
    public abstract DoubleVector unslice(int i, Vector<Double> vector, int i2,
            VectorMask<Double> vectorMask);

    /**
     * The inverse of {@code slice}: puts the lanes back in their place.
     *
     * @param i the {@code int}
     * @return the {@code DoubleVector}
     */
    public abstract DoubleVector unslice(int i);

    /**
     * Rearranges the lanes according to that shuffle.
     *
     * @param vectorShuffle the {@code VectorShuffle<Double>}
     * @return the {@code DoubleVector}
     */
    public abstract DoubleVector rearrange(VectorShuffle<Double> vectorShuffle);

    /**
     * Rearranges the lanes according to that shuffle.
     *
     * @param vectorShuffle the {@code VectorShuffle<Double>}
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code DoubleVector}
     */
    public abstract DoubleVector rearrange(VectorShuffle<Double> vectorShuffle,
            VectorMask<Double> vectorMask);

    /**
     * Rearranges the lanes according to that shuffle.
     *
     * @param vectorShuffle the {@code VectorShuffle<Double>}
     * @param vector the {@code Vector<Double>}
     * @return the {@code DoubleVector}
     */
    public abstract DoubleVector rearrange(VectorShuffle<Double> vectorShuffle,
            Vector<Double> vector);

    /**
     * Gathers the set lanes at the start.
     *
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code DoubleVector}
     */
    public abstract DoubleVector compress(VectorMask<Double> vectorMask);

    /**
     * Spreads the lanes from the start into the places the mask marks.
     *
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code DoubleVector}
     */
    public abstract DoubleVector expand(VectorMask<Double> vectorMask);

    /**
     * Takes from another vector the lanes this one indicates.
     *
     * @param vector the {@code Vector<Double>}
     * @return the {@code DoubleVector}
     */
    public abstract DoubleVector selectFrom(Vector<Double> vector);

    /**
     * Takes from another vector the lanes this one indicates.
     *
     * @param vector the {@code Vector<Double>}
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code DoubleVector}
     */
    public abstract DoubleVector selectFrom(Vector<Double> vector, VectorMask<Double> vectorMask);

    /**
     * Takes from another vector the lanes this one indicates.
     *
     * @param vector the {@code Vector<Double>}
     * @param vector2 the {@code Vector<Double>}
     * @return the {@code DoubleVector}
     */
    public abstract DoubleVector selectFrom(Vector<Double> vector, Vector<Double> vector2);

    /**
     * Multiplies and adds with a single rounding at the end, not two; see {@code Math.fma}.
     *
     * @param vector the {@code Vector<Double>}
     * @param vector2 the {@code Vector<Double>}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector fma(Vector<Double> vector, Vector<Double> vector2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Multiplies and adds with a single rounding at the end, not two; see {@code Math.fma}.
     *
     * @param d the {@code double}
     * @param d2 the {@code double}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final DoubleVector fma(double d, double d2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Combines all the lanes into a single value with that operator.
     *
     * @param associative the {@code VectorOperators.Associative}
     * @return the {@code double}
     */
    public abstract double reduceLanes(VectorOperators.Associative associative);

    /**
     * Combines all the lanes into a single value with that operator.
     *
     * @param associative the {@code VectorOperators.Associative}
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code double}
     */
    public abstract double reduceLanes(VectorOperators.Associative associative,
            VectorMask<Double> vectorMask);

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
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the number
     */
    public abstract long reduceLanesToLong(VectorOperators.Associative associative,
            VectorMask<Double> vectorMask);

    /**
     * The value of that lane.
     *
     * @param i the {@code int}
     * @return the {@code double}
     */
    public abstract double lane(int i);

    /**
     * The same vector with that lane changed.
     *
     * @param i the {@code int}
     * @param d the {@code double}
     * @return the {@code DoubleVector}
     */
    public abstract DoubleVector withLane(int i, double d);

    /**
     * The lanes in a new array.
     *
     * @return the {@code double[]}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final double[] toArray() {
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
     * @param vectorSpecies the {@code VectorSpecies<Double>}
     * @param ds the {@code double[]}
     * @param i the {@code int}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static DoubleVector fromArray(VectorSpecies<Double> vectorSpecies, double[] ds, int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that array.
     *
     * @param vectorSpecies the {@code VectorSpecies<Double>}
     * @param ds the {@code double[]}
     * @param i the {@code int}
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static DoubleVector fromArray(VectorSpecies<Double> vectorSpecies, double[] ds, int i,
            VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that array.
     *
     * @param vectorSpecies the {@code VectorSpecies<Double>}
     * @param ds the {@code double[]}
     * @param i the {@code int}
     * @param is the {@code int[]}
     * @param i2 the {@code int}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static DoubleVector fromArray(VectorSpecies<Double> vectorSpecies, double[] ds, int i,
            int[] is, int i2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that array.
     *
     * @param vectorSpecies the {@code VectorSpecies<Double>}
     * @param ds the {@code double[]}
     * @param i the {@code int}
     * @param is the {@code int[]}
     * @param i2 the {@code int}
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static DoubleVector fromArray(VectorSpecies<Double> vectorSpecies, double[] ds, int i,
            int[] is, int i2, VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that memory segment.
     *
     * @param vectorSpecies the {@code VectorSpecies<Double>}
     * @param memorySegment the {@code java.lang.foreign.MemorySegment}
     * @param l the {@code long}
     * @param byteOrder the {@code java.nio.ByteOrder}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static DoubleVector fromMemorySegment(VectorSpecies<Double> vectorSpecies,
            java.lang.foreign.MemorySegment memorySegment, long l, java.nio.ByteOrder byteOrder) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that memory segment.
     *
     * @param vectorSpecies the {@code VectorSpecies<Double>}
     * @param memorySegment the {@code java.lang.foreign.MemorySegment}
     * @param l the {@code long}
     * @param byteOrder the {@code java.nio.ByteOrder}
     * @param vectorMask the {@code VectorMask<Double>}
     * @return the {@code DoubleVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static DoubleVector fromMemorySegment(VectorSpecies<Double> vectorSpecies,
            java.lang.foreign.MemorySegment memorySegment, long l, java.nio.ByteOrder byteOrder,
            VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the vector into that array.
     *
     * @param ds the {@code double[]}
     * @param i the {@code int}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoArray(double[] ds, int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the vector into that array.
     *
     * @param ds the {@code double[]}
     * @param i the {@code int}
     * @param vectorMask the {@code VectorMask<Double>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoArray(double[] ds, int i, VectorMask<Double> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the vector into that array.
     *
     * @param ds the {@code double[]}
     * @param i the {@code int}
     * @param is the {@code int[]}
     * @param i2 the {@code int}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoArray(double[] ds, int i, int[] is, int i2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the vector into that array.
     *
     * @param ds the {@code double[]}
     * @param i the {@code int}
     * @param is the {@code int[]}
     * @param i2 the {@code int}
     * @param vectorMask the {@code VectorMask<Double>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoArray(double[] ds, int i, int[] is, int i2, VectorMask<Double> vectorMask
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
     * @param vectorMask the {@code VectorMask<Double>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoMemorySegment(java.lang.foreign.MemorySegment memorySegment, long l,
            java.nio.ByteOrder byteOrder, VectorMask<Double> vectorMask) {
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
