package jdk.incubator.vector;

import java.lang.foreign.MemorySegment;
import java.nio.ByteOrder;

/**
 * A vector of {@code byte} lanes.
 *
 * <p>It is one of the six classes where the API becomes concrete. {@link Vector} talks about lanes
 * without saying what they are, and that is why its methods take and return {@code Object} or the
 * boxed type; here the lanes really are {@code byte}, so one can load from a {@code byte[]}, read a
 * lane as a {@code byte} and operate without boxing anything.
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
public abstract class ByteVector extends AbstractVector<Byte> {

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
    ByteVector(Object payload) {
        super(payload);
    }

    /** The {@code byte} species of 64 bits. */
    public static final VectorSpecies<Byte> SPECIES_64 =
            SpeciesImpl.create(byte.class, VectorShape.S_64_BIT);

    /** The {@code byte} species of 128 bits. */
    public static final VectorSpecies<Byte> SPECIES_128 =
            SpeciesImpl.create(byte.class, VectorShape.S_128_BIT);

    /** The {@code byte} species of 256 bits. */
    public static final VectorSpecies<Byte> SPECIES_256 =
            SpeciesImpl.create(byte.class, VectorShape.S_256_BIT);

    /** The {@code byte} species of 512 bits. */
    public static final VectorSpecies<Byte> SPECIES_512 =
            SpeciesImpl.create(byte.class, VectorShape.S_512_BIT);

    /** The {@code byte} species of this machine\'s largest shape. */
    public static final VectorSpecies<Byte> SPECIES_MAX =
            SpeciesImpl.create(byte.class, VectorShape.S_Max_BIT);

    /** The {@code byte} species of this machine\'s preferred shape. */
    public static final VectorSpecies<Byte> SPECIES_PREFERRED =
            SpeciesImpl.create(byte.class, VectorShape.preferredShape());

    /**
     * A vector with every lane at zero.
     *
     * @param vectorSpecies the {@code VectorSpecies<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static ByteVector zero(VectorSpecies<Byte> vectorSpecies) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector with the same value in every lane.
     *
     * @param b the {@code byte}
     * @return the {@code ByteVector}
     */
    public abstract ByteVector broadcast(byte b);

    /**
     * A vector with the same value in every lane.
     *
     * @param vectorSpecies the {@code VectorSpecies<Byte>}
     * @param b the {@code byte}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static ByteVector broadcast(VectorSpecies<Byte> vectorSpecies, byte b) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector with the same value in every lane.
     *
     * @param l the {@code long}
     * @return the {@code ByteVector}
     */
    public abstract ByteVector broadcast(long l);

    /**
     * A vector with the same value in every lane.
     *
     * @param vectorSpecies the {@code VectorSpecies<Byte>}
     * @param l the {@code long}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static ByteVector broadcast(VectorSpecies<Byte> vectorSpecies, long l) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param unary the {@code VectorOperators.Unary}
     * @return the {@code ByteVector}
     */
    public abstract ByteVector lanewise(VectorOperators.Unary unary);

    /**
     * Applies that operator to each lane.
     *
     * @param unary the {@code VectorOperators.Unary}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code ByteVector}
     */
    public abstract ByteVector lanewise(VectorOperators.Unary unary, VectorMask<Byte> vectorMask);

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param vector the {@code Vector<Byte>}
     * @return the {@code ByteVector}
     */
    public abstract ByteVector lanewise(VectorOperators.Binary binary, Vector<Byte> vector);

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param vector the {@code Vector<Byte>}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code ByteVector}
     */
    public abstract ByteVector lanewise(VectorOperators.Binary binary, Vector<Byte> vector,
            VectorMask<Byte> vectorMask);

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param b the {@code byte}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector lanewise(VectorOperators.Binary binary, byte b) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param b the {@code byte}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector lanewise(VectorOperators.Binary binary, byte b,
            VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param l the {@code long}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector lanewise(VectorOperators.Binary binary, long l) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param binary the {@code VectorOperators.Binary}
     * @param l the {@code long}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector lanewise(VectorOperators.Binary binary, long l,
            VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param vector the {@code Vector<Byte>}
     * @param vector2 the {@code Vector<Byte>}
     * @return the {@code ByteVector}
     */
    public abstract ByteVector lanewise(VectorOperators.Ternary ternary, Vector<Byte> vector,
            Vector<Byte> vector2);

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param vector the {@code Vector<Byte>}
     * @param vector2 the {@code Vector<Byte>}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code ByteVector}
     */
    public abstract ByteVector lanewise(VectorOperators.Ternary ternary, Vector<Byte> vector,
            Vector<Byte> vector2, VectorMask<Byte> vectorMask);

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param b the {@code byte}
     * @param b2 the {@code byte}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector lanewise(VectorOperators.Ternary ternary, byte b, byte b2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param b the {@code byte}
     * @param b2 the {@code byte}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector lanewise(VectorOperators.Ternary ternary, byte b, byte b2,
            VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param vector the {@code Vector<Byte>}
     * @param b the {@code byte}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector lanewise(VectorOperators.Ternary ternary, Vector<Byte> vector, byte b) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param vector the {@code Vector<Byte>}
     * @param b the {@code byte}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector lanewise(VectorOperators.Ternary ternary, Vector<Byte> vector, byte b,
            VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param b the {@code byte}
     * @param vector the {@code Vector<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector lanewise(VectorOperators.Ternary ternary, byte b, Vector<Byte> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Applies that operator to each lane.
     *
     * @param ternary the {@code VectorOperators.Ternary}
     * @param b the {@code byte}
     * @param vector the {@code Vector<Byte>}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector lanewise(VectorOperators.Ternary ternary, byte b, Vector<Byte> vector,
            VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Adds lane by lane.
     *
     * @param vector the {@code Vector<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector add(Vector<Byte> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Adds lane by lane.
     *
     * @param b the {@code byte}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector add(byte b) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Adds lane by lane.
     *
     * @param vector the {@code Vector<Byte>}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector add(Vector<Byte> vector, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Adds lane by lane.
     *
     * @param b the {@code byte}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector add(byte b, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Subtracts lane by lane.
     *
     * @param vector the {@code Vector<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector sub(Vector<Byte> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Subtracts lane by lane.
     *
     * @param b the {@code byte}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector sub(byte b) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Subtracts lane by lane.
     *
     * @param vector the {@code Vector<Byte>}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector sub(Vector<Byte> vector, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Subtracts lane by lane.
     *
     * @param b the {@code byte}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector sub(byte b, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Multiplies lane by lane.
     *
     * @param vector the {@code Vector<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector mul(Vector<Byte> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Multiplies lane by lane.
     *
     * @param b the {@code byte}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector mul(byte b) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Multiplies lane by lane.
     *
     * @param vector the {@code Vector<Byte>}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector mul(Vector<Byte> vector, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Multiplies lane by lane.
     *
     * @param b the {@code byte}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector mul(byte b, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Divides lane by lane.
     *
     * @param vector the {@code Vector<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector div(Vector<Byte> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Divides lane by lane.
     *
     * @param b the {@code byte}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector div(byte b) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Divides lane by lane.
     *
     * @param vector the {@code Vector<Byte>}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector div(Vector<Byte> vector, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Divides lane by lane.
     *
     * @param b the {@code byte}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector div(byte b, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The smaller of each pair of lanes.
     *
     * @param vector the {@code Vector<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector min(Vector<Byte> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The smaller of each pair of lanes.
     *
     * @param b the {@code byte}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector min(byte b) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The larger of each pair of lanes.
     *
     * @param vector the {@code Vector<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector max(Vector<Byte> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The larger of each pair of lanes.
     *
     * @param b the {@code byte}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector max(byte b) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The bitwise conjunction.
     *
     * @param vector the {@code Vector<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector and(Vector<Byte> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The bitwise conjunction.
     *
     * @param b the {@code byte}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector and(byte b) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The bitwise disjunction.
     *
     * @param vector the {@code Vector<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector or(Vector<Byte> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The bitwise disjunction.
     *
     * @param b the {@code byte}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector or(byte b) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The negation of each lane.
     *
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector neg() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The absolute value of each lane.
     *
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector abs() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Inverts each lane.
     *
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector not() {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The mask of the equal lanes.
     *
     * @param vector the {@code Vector<Byte>}
     * @return the {@code VectorMask<Byte>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Byte> eq(Vector<Byte> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The mask of the equal lanes.
     *
     * @param b the {@code byte}
     * @return the {@code VectorMask<Byte>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Byte> eq(byte b) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The mask of the lanes that are less.
     *
     * @param vector the {@code Vector<Byte>}
     * @return the {@code VectorMask<Byte>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Byte> lt(Vector<Byte> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The mask of the lanes that are less.
     *
     * @param b the {@code byte}
     * @return the {@code VectorMask<Byte>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Byte> lt(byte b) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * The mask of the lanes that pass that test.
     *
     * @param test the {@code VectorOperators.Test}
     * @return the {@code VectorMask<Byte>}
     */
    public abstract VectorMask<Byte> test(VectorOperators.Test test);

    /**
     * The mask of the lanes that pass that test.
     *
     * @param test the {@code VectorOperators.Test}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code VectorMask<Byte>}
     */
    public abstract VectorMask<Byte> test(VectorOperators.Test test, VectorMask<Byte> vectorMask);

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param vector the {@code Vector<Byte>}
     * @return the {@code VectorMask<Byte>}
     */
    public abstract VectorMask<Byte> compare(VectorOperators.Comparison comparison,
            Vector<Byte> vector);

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param b the {@code byte}
     * @return the {@code VectorMask<Byte>}
     */
    public abstract VectorMask<Byte> compare(VectorOperators.Comparison comparison, byte b);

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param b the {@code byte}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code VectorMask<Byte>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Byte> compare(VectorOperators.Comparison comparison, byte b,
            VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param l the {@code long}
     * @return the {@code VectorMask<Byte>}
     */
    public abstract VectorMask<Byte> compare(VectorOperators.Comparison comparison, long l);

    /**
     * Compares lane by lane and returns the mask of the result.
     *
     * @param comparison the {@code VectorOperators.Comparison}
     * @param l the {@code long}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code VectorMask<Byte>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final VectorMask<Byte> compare(VectorOperators.Comparison comparison, long l,
            VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Blends two vectors, taking from one or the other according to the mask.
     *
     * @param vector the {@code Vector<Byte>}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code ByteVector}
     */
    public abstract ByteVector blend(Vector<Byte> vector, VectorMask<Byte> vectorMask);

    /**
     * Adds to each lane its own index multiplied by that step.
     *
     * @param i the {@code int}
     * @return the {@code ByteVector}
     */
    public abstract ByteVector addIndex(int i);

    /**
     * Blends two vectors, taking from one or the other according to the mask.
     *
     * @param b the {@code byte}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector blend(byte b, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Blends two vectors, taking from one or the other according to the mask.
     *
     * @param l the {@code long}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector blend(long l, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector starting at that lane.
     *
     * @param i the {@code int}
     * @param vector the {@code Vector<Byte>}
     * @return the {@code ByteVector}
     */
    public abstract ByteVector slice(int i, Vector<Byte> vector);

    /**
     * A vector starting at that lane.
     *
     * @param i the {@code int}
     * @param vector the {@code Vector<Byte>}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector slice(int i, Vector<Byte> vector, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector starting at that lane.
     *
     * @param i the {@code int}
     * @return the {@code ByteVector}
     */
    public abstract ByteVector slice(int i);

    /**
     * The inverse of {@code slice}: puts the lanes back in their place.
     *
     * @param i the {@code int}
     * @param vector the {@code Vector<Byte>}
     * @param i2 the {@code int}
     * @return the {@code ByteVector}
     */
    public abstract ByteVector unslice(int i, Vector<Byte> vector, int i2);

    /**
     * The inverse of {@code slice}: puts the lanes back in their place.
     *
     * @param i the {@code int}
     * @param vector the {@code Vector<Byte>}
     * @param i2 the {@code int}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code ByteVector}
     */
    public abstract ByteVector unslice(int i, Vector<Byte> vector, int i2,
            VectorMask<Byte> vectorMask);

    /**
     * The inverse of {@code slice}: puts the lanes back in their place.
     *
     * @param i the {@code int}
     * @return the {@code ByteVector}
     */
    public abstract ByteVector unslice(int i);

    /**
     * Rearranges the lanes according to that shuffle.
     *
     * @param vectorShuffle the {@code VectorShuffle<Byte>}
     * @return the {@code ByteVector}
     */
    public abstract ByteVector rearrange(VectorShuffle<Byte> vectorShuffle);

    /**
     * Rearranges the lanes according to that shuffle.
     *
     * @param vectorShuffle the {@code VectorShuffle<Byte>}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code ByteVector}
     */
    public abstract ByteVector rearrange(VectorShuffle<Byte> vectorShuffle,
            VectorMask<Byte> vectorMask);

    /**
     * Rearranges the lanes according to that shuffle.
     *
     * @param vectorShuffle the {@code VectorShuffle<Byte>}
     * @param vector the {@code Vector<Byte>}
     * @return the {@code ByteVector}
     */
    public abstract ByteVector rearrange(VectorShuffle<Byte> vectorShuffle, Vector<Byte> vector);

    /**
     * Gathers the set lanes at the start.
     *
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code ByteVector}
     */
    public abstract ByteVector compress(VectorMask<Byte> vectorMask);

    /**
     * Spreads the lanes from the start into the places the mask marks.
     *
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code ByteVector}
     */
    public abstract ByteVector expand(VectorMask<Byte> vectorMask);

    /**
     * Takes from another vector the lanes this one indicates.
     *
     * @param vector the {@code Vector<Byte>}
     * @return the {@code ByteVector}
     */
    public abstract ByteVector selectFrom(Vector<Byte> vector);

    /**
     * Takes from another vector the lanes this one indicates.
     *
     * @param vector the {@code Vector<Byte>}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code ByteVector}
     */
    public abstract ByteVector selectFrom(Vector<Byte> vector, VectorMask<Byte> vectorMask);

    /**
     * Takes from another vector the lanes this one indicates.
     *
     * @param vector the {@code Vector<Byte>}
     * @param vector2 the {@code Vector<Byte>}
     * @return the {@code ByteVector}
     */
    public abstract ByteVector selectFrom(Vector<Byte> vector, Vector<Byte> vector2);

    /**
     * Chooses bit by bit between two vectors according to a third.
     *
     * @param vector the {@code Vector<Byte>}
     * @param vector2 the {@code Vector<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector bitwiseBlend(Vector<Byte> vector, Vector<Byte> vector2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Chooses bit by bit between two vectors according to a third.
     *
     * @param b the {@code byte}
     * @param b2 the {@code byte}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector bitwiseBlend(byte b, byte b2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Chooses bit by bit between two vectors according to a third.
     *
     * @param b the {@code byte}
     * @param vector the {@code Vector<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector bitwiseBlend(byte b, Vector<Byte> vector) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Chooses bit by bit between two vectors according to a third.
     *
     * @param vector the {@code Vector<Byte>}
     * @param b the {@code byte}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector bitwiseBlend(Vector<Byte> vector, byte b) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Combines all the lanes into a single value with that operator.
     *
     * @param associative the {@code VectorOperators.Associative}
     * @return the {@code byte}
     */
    public abstract byte reduceLanes(VectorOperators.Associative associative);

    /**
     * Combines all the lanes into a single value with that operator.
     *
     * @param associative the {@code VectorOperators.Associative}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code byte}
     */
    public abstract byte reduceLanes(VectorOperators.Associative associative,
            VectorMask<Byte> vectorMask);

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
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the number
     */
    public abstract long reduceLanesToLong(VectorOperators.Associative associative,
            VectorMask<Byte> vectorMask);

    /**
     * The value of that lane.
     *
     * @param i the {@code int}
     * @return the {@code byte}
     */
    public abstract byte lane(int i);

    /**
     * The same vector with that lane changed.
     *
     * @param i the {@code int}
     * @param b the {@code byte}
     * @return the {@code ByteVector}
     */
    public abstract ByteVector withLane(int i, byte b);

    /**
     * The lanes in a new array.
     *
     * @return the {@code byte[]}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final byte[] toArray() {
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
     * @param vectorSpecies the {@code VectorSpecies<Byte>}
     * @param bs the {@code byte[]}
     * @param i the {@code int}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static ByteVector fromArray(VectorSpecies<Byte> vectorSpecies, byte[] bs, int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that array.
     *
     * @param vectorSpecies the {@code VectorSpecies<Byte>}
     * @param bs the {@code byte[]}
     * @param i the {@code int}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static ByteVector fromArray(VectorSpecies<Byte> vectorSpecies, byte[] bs, int i,
            VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that array.
     *
     * @param vectorSpecies the {@code VectorSpecies<Byte>}
     * @param bs the {@code byte[]}
     * @param i the {@code int}
     * @param is the {@code int[]}
     * @param i2 the {@code int}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static ByteVector fromArray(VectorSpecies<Byte> vectorSpecies, byte[] bs, int i,
            int[] is, int i2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that array.
     *
     * @param vectorSpecies the {@code VectorSpecies<Byte>}
     * @param bs the {@code byte[]}
     * @param i the {@code int}
     * @param is the {@code int[]}
     * @param i2 the {@code int}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static ByteVector fromArray(VectorSpecies<Byte> vectorSpecies, byte[] bs, int i,
            int[] is, int i2, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A mask read from that array of flags.
     *
     * @param vectorSpecies the {@code VectorSpecies<Byte>}
     * @param flags the {@code boolean[]}
     * @param i the {@code int}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static ByteVector fromBooleanArray(VectorSpecies<Byte> vectorSpecies, boolean[] flags,
            int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A mask read from that array of flags.
     *
     * @param vectorSpecies the {@code VectorSpecies<Byte>}
     * @param flags the {@code boolean[]}
     * @param i the {@code int}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static ByteVector fromBooleanArray(VectorSpecies<Byte> vectorSpecies, boolean[] flags,
            int i, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A mask read from that array of flags.
     *
     * @param vectorSpecies the {@code VectorSpecies<Byte>}
     * @param flags the {@code boolean[]}
     * @param i the {@code int}
     * @param is the {@code int[]}
     * @param i2 the {@code int}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static ByteVector fromBooleanArray(VectorSpecies<Byte> vectorSpecies, boolean[] flags,
            int i, int[] is, int i2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A mask read from that array of flags.
     *
     * @param vectorSpecies the {@code VectorSpecies<Byte>}
     * @param flags the {@code boolean[]}
     * @param i the {@code int}
     * @param is the {@code int[]}
     * @param i2 the {@code int}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static ByteVector fromBooleanArray(VectorSpecies<Byte> vectorSpecies, boolean[] flags,
            int i, int[] is, int i2, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that memory segment.
     *
     * @param vectorSpecies the {@code VectorSpecies<Byte>}
     * @param memorySegment the {@code java.lang.foreign.MemorySegment}
     * @param l the {@code long}
     * @param byteOrder the {@code java.nio.ByteOrder}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static ByteVector fromMemorySegment(VectorSpecies<Byte> vectorSpecies,
            java.lang.foreign.MemorySegment memorySegment, long l, java.nio.ByteOrder byteOrder) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * A vector read from that memory segment.
     *
     * @param vectorSpecies the {@code VectorSpecies<Byte>}
     * @param memorySegment the {@code java.lang.foreign.MemorySegment}
     * @param l the {@code long}
     * @param byteOrder the {@code java.nio.ByteOrder}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public static ByteVector fromMemorySegment(VectorSpecies<Byte> vectorSpecies,
            java.lang.foreign.MemorySegment memorySegment, long l, java.nio.ByteOrder byteOrder,
            VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the vector into that array.
     *
     * @param bs the {@code byte[]}
     * @param i the {@code int}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoArray(byte[] bs, int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the vector into that array.
     *
     * @param bs the {@code byte[]}
     * @param i the {@code int}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoArray(byte[] bs, int i, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the vector into that array.
     *
     * @param bs the {@code byte[]}
     * @param i the {@code int}
     * @param is the {@code int[]}
     * @param i2 the {@code int}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoArray(byte[] bs, int i, int[] is, int i2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the vector into that array.
     *
     * @param bs the {@code byte[]}
     * @param i the {@code int}
     * @param is the {@code int[]}
     * @param i2 the {@code int}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoArray(byte[] bs, int i, int[] is, int i2, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the mask into that array of flags.
     *
     * @param flags the {@code boolean[]}
     * @param i the {@code int}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoBooleanArray(boolean[] flags, int i) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the mask into that array of flags.
     *
     * @param flags the {@code boolean[]}
     * @param i the {@code int}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoBooleanArray(boolean[] flags, int i, VectorMask<Byte> vectorMask) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the mask into that array of flags.
     *
     * @param flags the {@code boolean[]}
     * @param i the {@code int}
     * @param is the {@code int[]}
     * @param i2 the {@code int}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoBooleanArray(boolean[] flags, int i, int[] is, int i2) {
        throw new UnsupportedOperationException(Msg.NOT_THERE);
    }

    /**
     * Writes the mask into that array of flags.
     *
     * @param flags the {@code boolean[]}
     * @param i the {@code int}
     * @param is the {@code int[]}
     * @param i2 the {@code int}
     * @param vectorMask the {@code VectorMask<Byte>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoBooleanArray(boolean[] flags, int i, int[] is, int i2,
            VectorMask<Byte> vectorMask) {
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
     * @param vectorMask the {@code VectorMask<Byte>}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final void intoMemorySegment(java.lang.foreign.MemorySegment memorySegment, long l,
            java.nio.ByteOrder byteOrder, VectorMask<Byte> vectorMask) {
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
     * @return the {@code ByteVector}
     * @throws UnsupportedOperationException always, in this library; see the class note
     */
    public final ByteVector viewAsIntegralLanes() {
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
