package jdk.internal.random;

import java.util.random.RandomGenerator;
import java.util.random.RandomGenerator.SplittableGenerator;
import jdk.internal.util.random.RandomSupport;

/**
 * The whole LXM construction in 32 bits.
 *
 * <p>This is the smallest member of the family and the only one whose native output is an INT: the
 * LCG, the xor-shift half and the mixer all work on 32-bit words, so {@code nextInt()} is the primitive
 * and {@code nextLong()} is built by concatenating two draws.
 *
 * <p>That inversion is worth noticing, because it is the opposite of every other generator here. The
 * RandomGenerator interface declares {@code nextLong()} as the abstract method and derives nextInt() from
 * it; this class has to derive in the other direction. The JDK does the same, which is a reminder
 * that "the primitive" is a property of the ENGINE, not of the interface — the interface just has
 * to pick one, and everything else adapts.
 *
 * <p>It exists for 32-bit hosts and for state-constrained uses: 96 bits of state instead of 192.
 *
 * @implNote An INTERNAL class; the JDK's is splittable through a nested interface (finding #101), so {@code split}
 *           is omitted.
 */
public final class L32X64MixRandom implements RandomGenerator.SplittableGenerator {

    // The 32-bit LCG multiplier, 0xadb4a92d.
    private static int multiplier() {
        return -1380669139;
    }

    // 2^32 / golden ratio, 0x9e3779b9 — the 32-bit sibling of the constant the others use.
    private static int goldenRatio32() {
        return -1640531527;
    }

    private int s;
    private final int a;
    private int x0;
    private int x1;

    /**
     * Creates a generator with the given subgenerator state.
     *
     * @param a the LCG addend; forced odd, which is what gives the LCG its full period
     * @param s the initial LCG state
     * @param x0 the first xor-shift state word
     * @param x1 the second xor-shift state word
     * @implSpec An all-zero xor-shift state is a fixed point of its transition, so it is
     *           replaced by a known-good state. The LCG half needs no such guard: it
     *           advances whatever its value.
     */
    public L32X64MixRandom(int a, int s, int x0, int x1) {
        this.a = a | 1;
        this.s = s;
        this.x0 = x0;
        this.x1 = x1;
        if ((x0 | x1) == 0) {
            this.x0 = L32X64MixRandom.goldenRatio32();
            this.x1 = 1;
        }
    }

    // Note that the addend comes from the HIGH half of the seed and the XBG words from the low
    // half, so the 64-bit seed is genuinely spread across the 96-bit state.
    /**
     * Creates a generator seeded from a single {@code long}.
     *
     * @param seed the seed
     * @implSpec The seed is expanded through the mixing functions of
     *           {@link jdk.internal.util.random.RandomSupport}: a raw seed would leave a
     *           nearly-empty state that takes many steps to fill. The LCG addend and the
     *           xor-shift words come from DIFFERENT mixers, so no seed can make the two
     *           subgenerators agree.
     */
    public L32X64MixRandom(long seed) {
        this(RandomSupport.mixMurmur32((int) ((seed ^ Bits.silverRatio64()) >>> 32)),
                1,
                RandomSupport.mixLea32((int) (seed ^ Bits.silverRatio64())),
                RandomSupport.mixLea32((int) (seed ^ Bits.silverRatio64())
                        + L32X64MixRandom.goldenRatio32()));
    }

    // The engine's real primitive.
    /**
     * {@inheritDoc}
     *
     * @implSpec This engine's real primitive: its whole state is 32-bit, so the int is
     *           produced directly rather than derived from a long.
     */
    public int nextInt() {
        int result = RandomSupport.mixLea32(this.s + this.x0);

        this.s = L32X64MixRandom.multiplier() * this.s + this.a;

        // xoroshiro64, the 32-bit analogue of the xoroshiro128 the L64 members use.
        int q0 = this.x0;
        int q1 = this.x1;
        q1 = q1 ^ q0;
        q0 = Bits.rotateLeft32(q0, 26);
        q0 = q0 ^ q1 ^ (q1 << 9);
        q1 = Bits.rotateLeft32(q1, 13);
        this.x0 = q0;
        this.x1 = q1;
        return result;
    }

    // Two draws concatenated. The JDK xors the second into the low half rather than or-ing it,
    // which is equivalent here because the halves do not overlap.
    /**
     * {@inheritDoc}
     *
     * @implSpec The output is computed from the CURRENT state of both subgenerators, and
     *           only then are the two advanced independently — the LCG by its multiply-add,
     *           the xor-shift by its own transition. Mixing two generators whose failure
     *           modes differ is what makes the combination stronger than either half.
     */
    public long nextLong() {
        return (((long) this.nextInt()) << 32) ^ (long) this.nextInt();
    }

    // ---- las tres entradas que faltaban ----------------------------------------------------------

    // La semilla de los generadores sin argumentos. Es un contador compartido que avanza de a
    // GOLDEN_RATIO_64: dos generadores creados uno detras del otro no arrancan en estados vecinos,
    // que es lo unico que se le pide.
    private static final java.util.concurrent.atomic.AtomicLong SEMILLERO =
            new java.util.concurrent.atomic.AtomicLong(RandomSupport.initialSeed());

    /** Un generador con una semilla elegida sola, distinta en cada llamada. */
    public L32X64MixRandom() {
        this(SEMILLERO.getAndAdd(RandomSupport.GOLDEN_RATIO_64));
    }

    /**
     * Un generador sembrado desde bytes.
     *
     * <p>Los bytes se reparten en las palabras del estado y, si no alcanzan, el resto se rellena con
     * un generador auxiliar -- una semilla corta dejaria el estado casi en cero, que para un
     * xor-shift es un punto fijo. Lo hace {@link RandomSupport#convertSeedBytesToLongs}, con los
     * mismos 4 y 2 que usa el JDK para este algoritmo, asi que la misma semilla da el mismo
     * generador.
     */
    public L32X64MixRandom(byte[] seed) {
        int[] data = RandomSupport.convertSeedBytesToInts(seed, 4, 2);
        int a = data[0];
        int s = data[1];
        int x0 = data[2];
        int x1 = data[3];
        this.a = a | 1;
        this.s = s;
        this.x0 = x0;
        this.x1 = x1;
    }

    // ---- particion ---------------------------------------------------------------------------------
    //
    // Partir no es "sembrar otro al azar": dos semillas cercanas pueden dar secuencias que se pisan.
    // La garantia sale de que el **addend** del LCG --la `a`-- del hijo se toma de la salmuera
    // (`brine`) y no del azar, con lo cual cada hijo recorre una orbita distinta del mismo espacio.
    //
    // El corrimiento `brine << 1` deja el bit bajo libre, que es donde el constructor fuerza el
    // impar. Sin eso, la mitad de las salmueras darian el mismo addend.

    /** Un generador independiente de este, con la entropia de este. */
    public SplittableGenerator split() {
        return this.split(this);
    }

    /** Un generador independiente de este, con la entropia de `source`. */
    public SplittableGenerator split(SplittableGenerator source) {
        return this.split(source, source.nextLong());
    }

    /** El de arriba con la salmuera explicita: es el que hace el trabajo. */
    public SplittableGenerator split(SplittableGenerator source, long brine) {
        return new L32X64MixRandom((int) (brine << 1), (int) source.nextLong(),
                (int) source.nextLong(), (int) source.nextLong());
    }

    /** `streamSize` generadores independientes, con la entropia de este. */
    public java.util.stream.Stream<SplittableGenerator> splits(long streamSize) {
        return this.splits(streamSize, this);
    }

    /** `streamSize` generadores independientes, con la entropia de `source`. */
    public java.util.stream.Stream<SplittableGenerator> splits(long streamSize,
            SplittableGenerator source) {
        return Splits.de(this, streamSize, source);
    }
}
