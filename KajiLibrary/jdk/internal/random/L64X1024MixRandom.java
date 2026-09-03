package jdk.internal.random;

import java.util.random.RandomGenerator;
import java.util.random.RandomGenerator.SplittableGenerator;
import jdk.internal.util.random.RandomSupport;

/**
 * The largest XBG in the LXM family: 1024 bits
 * of xor-shift state, held as SIXTEEN longs with a rotating index.
 *
 * <p>The structural change is the interesting part. The 128- and 256-bit versions keep their state in
 * named fields and touch every word on every step. At 1024 bits that would be wasteful, so
 * xoroshiro1024 keeps a RING: each step reads two words — the one at the cursor and the one just
 * behind it — updates only those two, and advances the cursor. A full sweep of the ring takes 16
 * steps, and that is what carries the state's memory forward.
 *
 * <p>{@code p} is the cursor, masked with 15 rather than reduced modulo 16 because the ring size is a power
 * of two — the same trick a circular buffer uses.
 *
 * <p>This is the highest-equidistribution member of the family. An INTERNAL class; the JDK's version
 * is splittable through a nested interface (finding #101), so {@code split} is omitted here.
 */
public final class L64X1024MixRandom implements RandomGenerator.SplittableGenerator {

    private static long multiplier() {
        return -3372029247567499371L;
    }

    private long s;
    private final long a;
    private final long[] x;
    // Inicializado **en la declaracion** y no en cada constructor, que es como lo hace el JDK:
    // es el indice del anillo, y un constructor que se lo olvide arranca desplazado una palabra y
    // emite otra secuencia. Paso exactamente eso con el de byte[].
    private int p = 15;

    // Package-private: the JDK spells this constructor out as eighteen (or twenty) separate
    // longs rather than an array, and a differing PUBLIC signature is a gate mismatch. The
    // array form is the useful one internally, so it simply stops being public.
    /**
     * Creates a generator with the given subgenerator state.
     *
     * @param a the LCG addend; forced odd, which is what gives the LCG its full period
     * @param s the initial LCG state
     * @param x the sixteen-word xor-shift ring
     * @implSpec An all-zero xor-shift state is a fixed point of its transition, so it is
     *           replaced by a known-good state. The LCG half needs no such guard: it
     *           advances whatever its value.
     */
    L64X1024MixRandom(long a, long s, long[] x) {
        this.a = a | 1L;
        this.s = s;
        this.x = x;
        this.p = 15;
    }

    // The seed is expanded into sixteen words, each a further step of the golden-ratio counter.
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
    /**
     * El constructor de **estado explicito**: cada palabra del generador se da a mano.
     *
     * <p>Es el que usa la particion, y por eso existe: los otros dos derivan todo el estado de una
     * semilla, y partir necesita justamente lo contrario -- fijar el addend del LCG y sortear el
     * resto.
     *
     * @implSpec El addend se fuerza impar (es lo que hace que el LCG recorra el periodo completo), y
     *           un estado xor-shift **todo cero** se reemplaza, porque el cero es un punto fijo de
     *           su transicion: el generador se quedaria ahi para siempre.
     */
    public L64X1024MixRandom(long a, long s,
            long x0, long x1, long x2, long x3,
            long x4, long x5, long x6, long x7,
            long x8, long x9, long x10, long x11,
            long x12, long x13, long x14, long x15) {
        this.a = a | 1L;
        this.s = s;
        this.x = new long[16];
        this.x[0] = x0;
        this.x[1] = x1;
        this.x[2] = x2;
        this.x[3] = x3;
        this.x[4] = x4;
        this.x[5] = x5;
        this.x[6] = x6;
        this.x[7] = x7;
        this.x[8] = x8;
        this.x[9] = x9;
        this.x[10] = x10;
        this.x[11] = x11;
        this.x[12] = x12;
        this.x[13] = x13;
        this.x[14] = x14;
        this.x[15] = x15;
        if ((x0 | x1 | x2 | x3 | x4 | x5 | x6 | x7 | x8 | x9 | x10 | x11 | x12 | x13 | x14 | x15) == 0L) {
            // Los dieciseis en cero: se rellenan con el mezclador, que garantiza
            // que al menos quince de los dieciseis salgan distintos de cero.
            long v = this.sembradorInicial();
            int j = 0;
            while (j < 16) {
                v = v + RandomSupport.GOLDEN_RATIO_64;
                this.x[j] = RandomSupport.mixStafford13(v);
                j = j + 1;
            }
        }
        this.p = 15;
    }

    // De donde sale el relleno cuando el estado xor-shift viene todo en cero. Es la
    // palabra baja del LCG, que es lo unico distinto de cero que se tiene a mano.
    private long sembradorInicial() {
        return this.s;
    }

    public L64X1024MixRandom(long seed) {
        long v = seed ^ Bits.silverRatio64();
        this.a = RandomSupport.mixMurmur64(v) | 1L;
        this.s = 1L;
        this.x = new long[16];
        int i = 0;
        while (i < 16) {
            this.x[i] = RandomSupport.mixStafford13(v);
            v = v + Bits.goldenRatio64();
            i = i + 1;
        }
        this.p = 15;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec The output is computed from the CURRENT state of both subgenerators, and
     *           only then are the two advanced independently — the LCG by its multiply-add,
     *           the xor-shift by its own transition. Mixing two generators whose failure
     *           modes differ is what makes the combination stronger than either half.
     */
    public long nextLong() {
        int q = this.p;
        // Advance the cursor first: the word it now points at is this step's "s0", and the one it
        // pointed at before is "s15" — the far end of the ring.
        this.p = (q + 1) & 15;
        long s0 = this.x[this.p];
        long s15 = this.x[q];

        long result = RandomSupport.mixLea64(this.s + s0);

        this.s = L64X1024MixRandom.multiplier() * this.s + this.a;

        s15 = s15 ^ s0;
        this.x[q] = Bits.rotateLeft(s0, 25) ^ s15 ^ (s15 << 27);
        this.x[this.p] = Bits.rotateLeft(s15, 36);
        return result;
    }

    // ---- las tres entradas que faltaban ----------------------------------------------------------

    // La semilla de los generadores sin argumentos. Es un contador compartido que avanza de a
    // GOLDEN_RATIO_64: dos generadores creados uno detras del otro no arrancan en estados vecinos,
    // que es lo unico que se le pide.
    private static final java.util.concurrent.atomic.AtomicLong SEMILLERO =
            new java.util.concurrent.atomic.AtomicLong(RandomSupport.initialSeed());

    /** Un generador con una semilla elegida sola, distinta en cada llamada. */
    public L64X1024MixRandom() {
        this(SEMILLERO.getAndAdd(RandomSupport.GOLDEN_RATIO_64));
    }

    /**
     * Un generador sembrado desde bytes.
     *
     * <p>Los bytes se reparten en las palabras del estado y, si no alcanzan, el resto se rellena con
     * un generador auxiliar -- una semilla corta dejaria el estado casi en cero, que para un
     * xor-shift es un punto fijo. Lo hace {@link RandomSupport#convertSeedBytesToLongs}, con los
     * mismos 18 y 16 que usa el JDK para este algoritmo, asi que la misma semilla da el mismo
     * generador.
     */
    public L64X1024MixRandom(byte[] seed) {
        long[] data = RandomSupport.convertSeedBytesToLongs(seed, 18, 16);
        this.a = data[0] | 1L;
        this.s = data[1];
        this.x = new long[16];
        int j = 0;
        while (j < 16) {
            this.x[j] = data[2 + j];
            j = j + 1;
        }
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
        long[] w = new long[16];
        int j = 0;
        while (j < 16) {
            w[j] = source.nextLong();
            j = j + 1;
        }
        return new L64X1024MixRandom(brine << 1, source.nextLong(),
                w[0], w[1], w[2], w[3], w[4], w[5], w[6], w[7],
                w[8], w[9], w[10], w[11], w[12], w[13], w[14], w[15]);
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
