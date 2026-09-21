package jdk.incubator.vector;

import java.lang.foreign.MemorySegment;
import java.nio.ByteOrder;

/**
 * A vector of {@code float} lanes.
 *
 * <p>It is one of the six classes where the API becomes concrete. {@link Vector} talks about lanes
 * without saying what they are, and that is why its methods take and return {@code Object} or the
 * boxed type; here the lanes really are {@code float}, so one can load from a {@code float[]}, read
 * a lane as a {@code float} and operate without boxing anything.
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
public abstract class FloatVector extends AbstractVector<Float> {

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
    FloatVector(Object payload) {
        super(payload);
    }

    /** The {@code float} species of 64 bits. */
    public static final VectorSpecies<Float> SPECIES_64 =
            SpeciesImpl.create(float.class, VectorShape.S_64_BIT);

    /** The {@code float} species of 128 bits. */
    public static final VectorSpecies<Float> SPECIES_128 =
            SpeciesImpl.create(float.class, VectorShape.S_128_BIT);

    /** The {@code float} species of 256 bits. */
    public static final VectorSpecies<Float> SPECIES_256 =
            SpeciesImpl.create(float.class, VectorShape.S_256_BIT);

    /** The {@code float} species of 512 bits. */
    public static final VectorSpecies<Float> SPECIES_512 =
            SpeciesImpl.create(float.class, VectorShape.S_512_BIT);

    /** The {@code float} species of this machine\'s largest shape. */
    public static final VectorSpecies<Float> SPECIES_MAX =
            SpeciesImpl.create(float.class, VectorShape.S_Max_BIT);

    /** The {@code float} species of this machine\'s preferred shape. */
    public static final VectorSpecies<Float> SPECIES_PREFERRED =
            SpeciesImpl.create(float.class, VectorShape.preferredShape());

    /**
     * A vector with every lane at zero.
     *
     * @param vectorSpecies the {@code VectorSpecies<Float>}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static FloatVector zero(VectorSpecies<Float> vectorSpecies) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector with the same value in every lane.
     *
     * @param f the {@code float}
     * @return the {@code FloatVector}
     */
    public abstract FloatVector broadcast(float f);

    /**
     * A vector with the same value in every lane.
     *
     * @param vectorSpecies the {@code VectorSpecies<Float>}
     * @param f the {@code float}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static FloatVector broadcast(VectorSpecies<Float> vectorSpecies, float f) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector with the same value in every lane.
     *
     * @param l the {@code long}
     * @return the {@code FloatVector}
     */
    public abstract FloatVector broadcast(long l);

    /**
     * A vector with the same value in every lane.
     *
     * @param vectorSpecies the {@code VectorSpecies<Float>}
     * @param l the {@code long}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static FloatVector broadcast(VectorSpecies<Float> vectorSpecies, long l) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param unary the {@code VectorOperators.Unary}
     * @return the {@code FloatVector}
     */
    public abstract FloatVector lanewise(VectorOperators.Unary unary);

    /**
     * Applies that operator to each lane.
     *
     * @param unary the {@code VectorOperators.Unary}
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code FloatVector}
     */
    public abstract FloatVector lanewise(VectorOperators.Unary unary, VectorMask<Float> vectorMask);

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param vector the {@code Vector<Float>}
     * @return the {@code FloatVector}
     */
    public abstract FloatVector lanewise(VectorOperators.Binary binary, Vector<Float> vector);

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param vector the {@code Vector<Float>}
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code FloatVector}
     */
    public abstract FloatVector lanewise(VectorOperators.Binary binary, Vector<Float> vector,
            VectorMask<Float> vectorMask);

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param f the {@code float}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector lanewise(VectorOperators.Binary binary, float f) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param f the {@code float}
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector lanewise(VectorOperators.Binary binary, float f,
            VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param l the {@code long}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector lanewise(VectorOperators.Binary binary, long l) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param l the {@code long}
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector lanewise(VectorOperators.Binary binary, long l,
            VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param vector the {@code Vector<Float>}
     * @param vector2 the {@code Vector<Float>}
     * @return the {@code FloatVector}
     */
    public abstract FloatVector lanewise(VectorOperators.Ternary ternary, Vector<Float> vector,
            Vector<Float> vector2);

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param vector the {@code Vector<Float>}
     * @param vector2 the {@code Vector<Float>}
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code FloatVector}
     */
    public abstract FloatVector lanewise(VectorOperators.Ternary ternary, Vector<Float> vector,
            Vector<Float> vector2, VectorMask<Float> vectorMask);

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param f the {@code float}
     * @param f2 the {@code float}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector lanewise(VectorOperators.Ternary ternary, float f, float f2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param f the {@code float}
     * @param f2 the {@code float}
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector lanewise(VectorOperators.Ternary ternary, float f, float f2,
            VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param vector the {@code Vector<Float>}
     * @param f the {@code float}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector lanewise(VectorOperators.Ternary ternary, Vector<Float> vector, float f
            ) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param vector the {@code Vector<Float>}
     * @param f the {@code float}
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector lanewise(VectorOperators.Ternary ternary, Vector<Float> vector,
            float f, VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param f the {@code float}
     * @param vector the {@code Vector<Float>}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector lanewise(VectorOperators.Ternary ternary, float f, Vector<Float> vector
            ) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param f the {@code float}
     * @param vector the {@code Vector<Float>}
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector lanewise(VectorOperators.Ternary ternary, float f,
            Vector<Float> vector, VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Adds lane by lane.
     *
     * @param vector the {@code Vector<Float>}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector add(Vector<Float> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Adds lane by lane.
     *
     * @param f the {@code float}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector add(float f) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Adds lane by lane.
     *
     * @param vector the {@code Vector<Float>}
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector add(Vector<Float> vector, VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Adds lane by lane.
     *
     * @param f the {@code float}
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector add(float f, VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Subtracts lane by lane.
     *
     * @param vector the {@code Vector<Float>}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector sub(Vector<Float> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Subtracts lane by lane.
     *
     * @param f the {@code float}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector sub(float f) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Subtracts lane by lane.
     *
     * @param vector the {@code Vector<Float>}
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector sub(Vector<Float> vector, VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Subtracts lane by lane.
     *
     * @param f the {@code float}
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector sub(float f, VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Multiplies lane by lane.
     *
     * @param vector the {@code Vector<Float>}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector mul(Vector<Float> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Multiplies lane by lane.
     *
     * @param f the {@code float}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector mul(float f) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Multiplies lane by lane.
     *
     * @param vector the {@code Vector<Float>}
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector mul(Vector<Float> vector, VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Multiplies lane by lane.
     *
     * @param f the {@code float}
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector mul(float f, VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Divides lane by lane.
     *
     * @param vector the {@code Vector<Float>}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector div(Vector<Float> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Divides lane by lane.
     *
     * @param f the {@code float}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector div(float f) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Divides lane by lane.
     *
     * @param vector the {@code Vector<Float>}
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector div(Vector<Float> vector, VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Divides lane by lane.
     *
     * @param f the {@code float}
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector div(float f, VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The smaller of each pair of lanes.
     *
     * @param vector the {@code Vector<Float>}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector min(Vector<Float> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The smaller of each pair of lanes.
     *
     * @param f the {@code float}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector min(float f) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The larger of each pair of lanes.
     *
     * @param vector the {@code Vector<Float>}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector max(Vector<Float> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The larger of each pair of lanes.
     *
     * @param f the {@code float}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector max(float f) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Each lane raised to the corresponding lane of the other.
     *
     * @param vector the {@code Vector<Float>}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector pow(Vector<Float> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Each lane raised to the corresponding lane of the other.
     *
     * @param f the {@code float}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector pow(float f) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The negation of each lane.
     *
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector neg() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The absolute value of each lane.
     *
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector abs() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The square root of each lane.
     *
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector sqrt() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The mask of the equal lanes.
     *
     * @param vector the {@code Vector<Float>}
     * @return the {@code VectorMask<Float>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Float> eq(Vector<Float> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The mask of the equal lanes.
     *
     * @param f the {@code float}
     * @return the {@code VectorMask<Float>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Float> eq(float f) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The mask of the lanes that are less.
     *
     * @param vector the {@code Vector<Float>}
     * @return the {@code VectorMask<Float>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Float> lt(Vector<Float> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The mask of the lanes that are less.
     *
     * @param f the {@code float}
     * @return the {@code VectorMask<Float>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Float> lt(float f) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The mask of the lanes that pass that test.
     *
     * @param test the {@code VectorOperators.Test}
     * @return the {@code VectorMask<Float>}
     */
    public abstract VectorMask<Float> test(VectorOperators.Test test);

    /**
     * The mask of the lanes that pass that test.
     *
     * @param test the {@code VectorOperators.Test}
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code VectorMask<Float>}
     */
    public abstract VectorMask<Float> test(VectorOperators.Test test, VectorMask<Float> vectorMask);

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param vector the {@code Vector<Float>}
     * @return the {@code VectorMask<Float>}
     */
    public abstract VectorMask<Float> compare(VectorOperators.Comparison comparison,
            Vector<Float> vector);

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param f the {@code float}
     * @return the {@code VectorMask<Float>}
     */
    public abstract VectorMask<Float> compare(VectorOperators.Comparison comparison, float f);

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param f the {@code float}
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code VectorMask<Float>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Float> compare(VectorOperators.Comparison comparison, float f,
            VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param l the {@code long}
     * @return the {@code VectorMask<Float>}
     */
    public abstract VectorMask<Float> compare(VectorOperators.Comparison comparison, long l);

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param l the {@code long}
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code VectorMask<Float>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Float> compare(VectorOperators.Comparison comparison, long l,
            VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Blends two vectors, taking from one or the other according to the mask.
     *
     * @param vector the {@code Vector<Float>}
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code FloatVector}
     */
    public abstract FloatVector blend(Vector<Float> vector, VectorMask<Float> vectorMask);

    /**
     * Adds to each lane its own index multiplied by that step.
     *
     * @param i the {@code int}
     * @return the {@code FloatVector}
     */
    public abstract FloatVector addIndex(int i);

    /**
     * Blends two vectors, taking from one or the other according to the mask.
     *
     * @param f the {@code float}
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector blend(float f, VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Blends two vectors, taking from one or the other according to the mask.
     *
     * @param l the {@code long}
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector blend(long l, VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector starting at that lane.
     *
     * @param i the {@code int}
     * @param vector the {@code Vector<Float>}
     * @return the {@code FloatVector}
     */
    public abstract FloatVector slice(int i, Vector<Float> vector);

    /**
     * A vector starting at that lane.
     *
     * @param i the {@code int}
     * @param vector the {@code Vector<Float>}
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector slice(int i, Vector<Float> vector, VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector starting at that lane.
     *
     * @param i the {@code int}
     * @return the {@code FloatVector}
     */
    public abstract FloatVector slice(int i);

    /**
     * The inverse of {@code slice}: puts the lanes back in their place.
     *
     * @param i the {@code int}
     * @param vector the {@code Vector<Float>}
     * @param i2 the {@code int}
     * @return the {@code FloatVector}
     */
    public abstract FloatVector unslice(int i, Vector<Float> vector, int i2);

    /**
     * The inverse of {@code slice}: puts the lanes back in their place.
     *
     * @param i the {@code int}
     * @param vector the {@code Vector<Float>}
     * @param i2 the {@code int}
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code FloatVector}
     */
    public abstract FloatVector unslice(int i, Vector<Float> vector, int i2,
            VectorMask<Float> vectorMask);

    /**
     * The inverse of {@code slice}: puts the lanes back in their place.
     *
     * @param i the {@code int}
     * @return the {@code FloatVector}
     */
    public abstract FloatVector unslice(int i);

    /**
     * Rearranges the lanes according to that shuffle.
     *
     * @param vectorShuffle the {@code VectorShuffle<Float>}
     * @return the {@code FloatVector}
     */
    public abstract FloatVector rearrange(VectorShuffle<Float> vectorShuffle);

    /**
     * Rearranges the lanes according to that shuffle.
     *
     * @param vectorShuffle the {@code VectorShuffle<Float>}
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code FloatVector}
     */
    public abstract FloatVector rearrange(VectorShuffle<Float> vectorShuffle,
            VectorMask<Float> vectorMask);

    /**
     * Rearranges the lanes according to that shuffle.
     *
     * @param vectorShuffle the {@code VectorShuffle<Float>}
     * @param vector the {@code Vector<Float>}
     * @return the {@code FloatVector}
     */
    public abstract FloatVector rearrange(VectorShuffle<Float> vectorShuffle, Vector<Float> vector);

    /**
     * Gathers the set lanes at the start.
     *
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code FloatVector}
     */
    public abstract FloatVector compress(VectorMask<Float> vectorMask);

    /**
     * Spreads the lanes from the start into the places the mask marks.
     *
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code FloatVector}
     */
    public abstract FloatVector expand(VectorMask<Float> vectorMask);

    /**
     * Takes from another vector the lanes this one indicates.
     *
     * @param vector the {@code Vector<Float>}
     * @return the {@code FloatVector}
     */
    public abstract FloatVector selectFrom(Vector<Float> vector);

    /**
     * Takes from another vector the lanes this one indicates.
     *
     * @param vector the {@code Vector<Float>}
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code FloatVector}
     */
    public abstract FloatVector selectFrom(Vector<Float> vector, VectorMask<Float> vectorMask);

    /**
     * Takes from another vector the lanes this one indicates.
     *
     * @param vector the {@code Vector<Float>}
     * @param vector2 the {@code Vector<Float>}
     * @return the {@code FloatVector}
     */
    public abstract FloatVector selectFrom(Vector<Float> vector, Vector<Float> vector2);

    /**
     * Multiplies and adds with a single rounding at the end, not two; see {@code Math.fma}.
     *
     * @param vector the {@code Vector<Float>}
     * @param vector2 the {@code Vector<Float>}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector fma(Vector<Float> vector, Vector<Float> vector2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Multiplies and adds with a single rounding at the end, not two; see {@code Math.fma}.
     *
     * @param f the {@code float}
     * @param f2 the {@code float}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final FloatVector fma(float f, float f2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Combines all the lanes into a single value with that operator.
     *
     * @param associative the {@code VectorOperators.Associative}
     * @return the {@code float}
     */
    public abstract float reduceLanes(VectorOperators.Associative associative);

    /**
     * Combines all the lanes into a single value with that operator.
     *
     * @param associative the {@code VectorOperators.Associative}
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code float}
     */
    public abstract float reduceLanes(VectorOperators.Associative associative,
            VectorMask<Float> vectorMask);

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
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the number
     */
    public abstract long reduceLanesToLong(VectorOperators.Associative associative,
            VectorMask<Float> vectorMask);

    /**
     * The value of that lane.
     *
     * @param i the {@code int}
     * @return the {@code float}
     */
    public abstract float lane(int i);

    /**
     * The same vector with that lane changed.
     *
     * @param i the {@code int}
     * @param f the {@code float}
     * @return the {@code FloatVector}
     */
    public abstract FloatVector withLane(int i, float f);

    /**
     * The lanes in a new array.
     *
     * @return the {@code float[]}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final float[] toArray() {
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
     * @param vectorSpecies the {@code VectorSpecies<Float>}
     * @param fs the {@code float[]}
     * @param i the {@code int}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static FloatVector fromArray(VectorSpecies<Float> vectorSpecies, float[] fs, int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that array.
     *
     * @param vectorSpecies the {@code VectorSpecies<Float>}
     * @param fs the {@code float[]}
     * @param i the {@code int}
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static FloatVector fromArray(VectorSpecies<Float> vectorSpecies, float[] fs, int i,
            VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that array.
     *
     * @param vectorSpecies the {@code VectorSpecies<Float>}
     * @param fs the {@code float[]}
     * @param i the {@code int}
     * @param is the {@code int[]}
     * @param i2 the {@code int}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static FloatVector fromArray(VectorSpecies<Float> vectorSpecies, float[] fs, int i,
            int[] is, int i2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that array.
     *
     * @param vectorSpecies the {@code VectorSpecies<Float>}
     * @param fs the {@code float[]}
     * @param i the {@code int}
     * @param is the {@code int[]}
     * @param i2 the {@code int}
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static FloatVector fromArray(VectorSpecies<Float> vectorSpecies, float[] fs, int i,
            int[] is, int i2, VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that memory segment.
     *
     * @param vectorSpecies the {@code VectorSpecies<Float>}
     * @param memorySegment the {@code java.lang.foreign.MemorySegment}
     * @param l the {@code long}
     * @param byteOrder the {@code java.nio.ByteOrder}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static FloatVector fromMemorySegment(VectorSpecies<Float> vectorSpecies,
            java.lang.foreign.MemorySegment memorySegment, long l, java.nio.ByteOrder byteOrder) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that memory segment.
     *
     * @param vectorSpecies the {@code VectorSpecies<Float>}
     * @param memorySegment the {@code java.lang.foreign.MemorySegment}
     * @param l the {@code long}
     * @param byteOrder the {@code java.nio.ByteOrder}
     * @param vectorMask the {@code VectorMask<Float>}
     * @return the {@code FloatVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static FloatVector fromMemorySegment(VectorSpecies<Float> vectorSpecies,
            java.lang.foreign.MemorySegment memorySegment, long l, java.nio.ByteOrder byteOrder,
            VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the vector into that array.
     *
     * @param fs the {@code float[]}
     * @param i the {@code int}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoArray(float[] fs, int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the vector into that array.
     *
     * @param fs the {@code float[]}
     * @param i the {@code int}
     * @param vectorMask the {@code VectorMask<Float>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoArray(float[] fs, int i, VectorMask<Float> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the vector into that array.
     *
     * @param fs the {@code float[]}
     * @param i the {@code int}
     * @param is the {@code int[]}
     * @param i2 the {@code int}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoArray(float[] fs, int i, int[] is, int i2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the vector into that array.
     *
     * @param fs the {@code float[]}
     * @param i the {@code int}
     * @param is the {@code int[]}
     * @param i2 the {@code int}
     * @param vectorMask the {@code VectorMask<Float>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoArray(float[] fs, int i, int[] is, int i2, VectorMask<Float> vectorMask) {
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
     * @param vectorMask the {@code VectorMask<Float>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoMemorySegment(java.lang.foreign.MemorySegment memorySegment, long l,
            java.nio.ByteOrder byteOrder, VectorMask<Float> vectorMask) {
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
