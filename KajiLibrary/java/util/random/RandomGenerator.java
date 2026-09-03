package java.util.random;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * An object that generates a stream of pseudorandom values.
 *
 * <p>A generator has exactly one primitive operation — produce the next batch of random bits — and
 * every other value a caller asks for (a bounded {@code int}, a {@code double} in
 * <code>[0,1)</code>, a {@code boolean}, a fill of bytes) is derived from it by arithmetic that has
 * nothing to do with the underlying algorithm. So {@link #nextLong()} is the single abstract method
 * and the rest are {@code default}s: a new engine implements one method and inherits an API.
 *
 * <p>Two values obtained from the same generator seeded the same way are <em>reproducible</em>.
 * That is the contract, and it is why an implementation may not substitute a different algorithm
 * for one whose sequence is specified.
 *
 * @implNote This interface replaces the old arrangement in which the derived methods were baked
 *           into {@link java.util.Random}, a concrete class. Every new engine then had to either
 *           re-derive them — and risk getting the bounded case subtly non-uniform — or extend a
 *           class whose 48-bit LCG state it did not want.
 *
 * @implNote A KajiLibrary subset. The stream methods ({@code ints}/{@code longs}/{@code doubles})
 *           are omitted: the unbounded forms are infinite, which the eager
 *           {@code java.util.stream} cannot express. {@code nextGaussian}/{@code nextExponential}
 *           are omitted because the JDK computes them with the modified ziggurat method, a pair of
 *           generated lookup tables; a polar-method version would have the right distribution but
 *           a different sequence, so it could not be validated against the JDK. {@code of(String)}
 *           and {@code getDefault()} need a registry of implementations.
 */
public interface RandomGenerator {

    /**
     * Returns the next pseudorandomly generated {@code long} value.
     *
     * <p>This is the generator's single primitive: every other method of this interface is defined
     * in terms of it.
     *
     * @return the next pseudorandom {@code long}
     */
    long nextLong();

    /**
     * Returns the next pseudorandomly generated {@code int} value.
     *
     * @return the next pseudorandom {@code int}
     * @implSpec Returns the HIGH 32 bits of {@link #nextLong()}. The top bits are used rather than
     *           the bottom ones because in several classic generators the low bits have a much
     *           shorter period than the high ones, and taking the top is the habit that survives a
     *           change of engine.
     */
    default int nextInt() {
        return (int) (this.nextLong() >>> 32);
    }

    /**
     * Returns a pseudorandomly generated {@code int} value uniformly distributed between zero
     * (inclusive) and {@code bound} (exclusive).
     *
     * @param bound the upper bound (exclusive); must be positive
     * @return a pseudorandom {@code int} in <code>[0, bound)</code>
     * @throws IllegalArgumentException if {@code bound} is not positive
     * @implSpec Rejection sampling, not a modulus. {@code nextInt() % bound} is biased whenever
     *           {@code bound} does not divide 2<sup>32</sup>, because the leftover values at the
     *           top of the range map back onto the low results; rejecting that leftover band is
     *           what restores uniformity. A power-of-two bound divides the range exactly, so it is
     *           handled by masking instead.
     */
    default int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive");
        }
        int m = bound - 1;
        int r = this.nextInt();
        if ((bound & m) == 0) {
            return r & m;
        }
        int u = r >>> 1;
        r = u % bound;
        while (u + m - r < 0) {
            u = this.nextInt() >>> 1;
            r = u % bound;
        }
        return r;
    }

    /**
     * Returns a pseudorandomly generated {@code int} value uniformly distributed between
     * {@code origin} (inclusive) and {@code bound} (exclusive).
     *
     * @param origin the least value that can be returned
     * @param bound the upper bound (exclusive)
     * @return a pseudorandom {@code int} in <code>[origin, bound)</code>
     * @throws IllegalArgumentException if {@code origin} is greater than or equal to {@code bound}
     */
    default int nextInt(int origin, int bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException("bound must be greater than origin");
        }
        return origin + this.nextInt(bound - origin);
    }

    /**
     * Returns a pseudorandomly generated {@code long} value uniformly distributed between zero
     * (inclusive) and {@code bound} (exclusive).
     *
     * @param bound the upper bound (exclusive); must be positive
     * @return a pseudorandom {@code long} in <code>[0, bound)</code>
     * @throws IllegalArgumentException if {@code bound} is not positive
     * @implSpec The same rejection loop as {@link #nextInt(int)}, widened to 64 bits.
     */
    default long nextLong(long bound) {
        if (bound <= 0L) {
            throw new IllegalArgumentException("bound must be positive");
        }
        long m = bound - 1L;
        long r = this.nextLong();
        if ((bound & m) == 0L) {
            return r & m;
        }
        long u = r >>> 1;
        r = u % bound;
        while (u + m - r < 0L) {
            u = this.nextLong() >>> 1;
            r = u % bound;
        }
        return r;
    }

    /**
     * Returns a pseudorandomly generated {@code long} value uniformly distributed between
     * {@code origin} (inclusive) and {@code bound} (exclusive).
     *
     * @param origin the least value that can be returned
     * @param bound the upper bound (exclusive)
     * @return a pseudorandom {@code long} in <code>[origin, bound)</code>
     * @throws IllegalArgumentException if {@code origin} is greater than or equal to {@code bound}
     */
    default long nextLong(long origin, long bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException("bound must be greater than origin");
        }
        return origin + this.nextLong(bound - origin);
    }

    /**
     * Returns a pseudorandomly generated {@code boolean} value.
     *
     * @return a pseudorandom {@code boolean}
     * @implSpec The sign bit of {@link #nextInt()}, which is as random as any other and costs
     *           nothing to read.
     */
    default boolean nextBoolean() {
        return this.nextInt() < 0;
    }

    /**
     * Returns a pseudorandomly generated {@code double} value between zero (inclusive) and one
     * (exclusive).
     *
     * @return a pseudorandom {@code double} in <code>[0.0, 1.0)</code>
     * @implSpec Takes the top 53 bits of {@link #nextLong()} and scales by 2<sup>-53</sup>. 53 is
     *           exactly the precision of a {@code double}'s mantissa, so every representable value
     *           in the range gets the same probability: more bits would not add resolution, fewer
     *           would leave gaps.
     */
    default double nextDouble() {
        return (this.nextLong() >>> 11) * 1.1102230246251565E-16;
    }

    /**
     * Returns a pseudorandomly generated {@code double} value between zero (inclusive) and
     * {@code bound} (exclusive).
     *
     * @param bound the upper bound (exclusive); must be positive and finite
     * @return a pseudorandom {@code double} in <code>[0.0, bound)</code>
     * @throws IllegalArgumentException if {@code bound} is not positive
     */
    default double nextDouble(double bound) {
        if (!(bound > 0.0)) {
            throw new IllegalArgumentException("bound must be positive");
        }
        return this.nextDouble() * bound;
    }

    /**
     * Returns a pseudorandomly generated {@code double} value between {@code origin} (inclusive)
     * and {@code bound} (exclusive).
     *
     * @param origin the least value that can be returned
     * @param bound the upper bound (exclusive)
     * @return a pseudorandom {@code double} in <code>[origin, bound)</code>
     * @throws IllegalArgumentException if {@code origin} is not less than {@code bound}
     */
    default double nextDouble(double origin, double bound) {
        if (!(origin < bound)) {
            throw new IllegalArgumentException("bound must be greater than origin");
        }
        return origin + this.nextDouble() * (bound - origin);
    }

    /**
     * Returns a pseudorandomly generated {@code float} value between zero (inclusive) and one
     * (exclusive).
     *
     * @return a pseudorandom {@code float} in <code>[0.0f, 1.0f)</code>
     * @implSpec 24 bits, for the same reason {@link #nextDouble()} uses 53: a {@code float}'s
     *           mantissa.
     */
    default float nextFloat() {
        return (float) ((this.nextLong() >>> 40) * 5.9604644775390625E-8);
    }

    /**
     * Returns a pseudorandomly generated {@code float} value between zero (inclusive) and
     * {@code bound} (exclusive).
     *
     * @param bound the upper bound (exclusive); must be positive and finite
     * @return a pseudorandom {@code float} in <code>[0.0f, bound)</code>
     * @throws IllegalArgumentException if {@code bound} is not positive
     */
    default float nextFloat(float bound) {
        if (!(bound > 0.0f)) {
            throw new IllegalArgumentException("bound must be positive");
        }
        return this.nextFloat() * bound;
    }

    /**
     * Returns a pseudorandomly generated {@code float} value between {@code origin} (inclusive)
     * and {@code bound} (exclusive).
     *
     * @param origin the least value that can be returned
     * @param bound the upper bound (exclusive)
     * @return a pseudorandom {@code float} in <code>[origin, bound)</code>
     * @throws IllegalArgumentException if {@code origin} is not less than {@code bound}
     */
    default float nextFloat(float origin, float bound) {
        if (!(origin < bound)) {
            throw new IllegalArgumentException("bound must be greater than origin");
        }
        return origin + this.nextFloat() * (bound - origin);
    }

    /**
     * Fills the given array with pseudorandomly generated bytes.
     *
     * @param bytes the array to fill
     * @implSpec Consumes one {@link #nextLong()} per eight bytes rather than one per byte.
     */
    default void nextBytes(byte[] bytes) {
        int i = 0;
        int len = bytes.length;
        while (i < len) {
            long rnd = this.nextLong();
            int n = len - i;
            if (n > 8) {
                n = 8;
            }
            int k = 0;
            while (k < n) {
                bytes[i] = (byte) rnd;
                rnd = rnd >> 8;
                i = i + 1;
                k = k + 1;
            }
        }
    }

    // ---- los flujos de valores ------------------------------------------------------------------
    //
    // Doce fabricas que son la misma idea cuatro veces por tipo: con o sin cantidad, con o sin
    // rango. Estan aca --y no en cada generador-- porque no dependen de nada mas que de `nextInt`,
    // `nextLong` y `nextDouble`, que es justamente lo que cada implementacion aporta.
    //
    // **Divergencia deliberada, y es la unica**: las formas **sin cantidad** (`ints()`, `longs()`,
    // `doubles()`) se niegan en vez de devolver un flujo infinito.
    //
    // El JDK las define como "efectivamente ilimitadas", y eso pide un flujo **perezoso**: los
    // valores se generan a medida que alguien los pide, y `limit(n)` corta antes de generar el
    // resto. Los flujos de esta biblioteca estan respaldados por un arreglo y son **ansiosos** --
    // se materializan enteros al crearse --, asi que un flujo infinito no se puede representar.
    //
    // De las dos salidas posibles se elige la ruidosa. Devolver un prefijo largo y fingir que es
    // infinito andaria para `ints().limit(10)` y daria **menos** valores de los pedidos para
    // `ints().limit(un_millon)`, en silencio. Un metodo que se niega y dice con que reemplazarlo es
    // peor de usar y mejor de confiar.
    private static UnsupportedOperationException sinTamano(String cual) {
        return new UnsupportedOperationException(
                "los flujos de esta biblioteca son ansiosos: use " + cual + "(streamSize)");
    }

    /**
     * `streamSize` enteros pseudoaleatorios.
     *
     * @throws IllegalArgumentException si `streamSize` es negativo
     */
    default IntStream ints(long streamSize) {
        if (streamSize < 0) {
            throw new IllegalArgumentException("streamSize must be non-negative");
        }
        int[] a = new int[(int) streamSize];
        int i = 0;
        while (i < a.length) {
            a[i] = this.nextInt();
            i = i + 1;
        }
        return IntStream.of(a);
    }

    // `streamSize` enteros en `[origin, bound)`.
    default IntStream ints(long streamSize, int randomNumberOrigin, int randomNumberBound) {
        if (streamSize < 0) {
            throw new IllegalArgumentException("streamSize must be non-negative");
        }
        int[] a = new int[(int) streamSize];
        int i = 0;
        while (i < a.length) {
            a[i] = this.nextInt(randomNumberOrigin, randomNumberBound);
            i = i + 1;
        }
        return IntStream.of(a);
    }

    default IntStream ints() {
        throw sinTamano("ints");
    }

    default IntStream ints(int randomNumberOrigin, int randomNumberBound) {
        throw sinTamano("ints");
    }

    default LongStream longs(long streamSize) {
        if (streamSize < 0) {
            throw new IllegalArgumentException("streamSize must be non-negative");
        }
        long[] a = new long[(int) streamSize];
        int i = 0;
        while (i < a.length) {
            a[i] = this.nextLong();
            i = i + 1;
        }
        return LongStream.of(a);
    }

    default LongStream longs(long streamSize, long randomNumberOrigin, long randomNumberBound) {
        if (streamSize < 0) {
            throw new IllegalArgumentException("streamSize must be non-negative");
        }
        long[] a = new long[(int) streamSize];
        int i = 0;
        while (i < a.length) {
            a[i] = this.nextLong(randomNumberOrigin, randomNumberBound);
            i = i + 1;
        }
        return LongStream.of(a);
    }

    default LongStream longs() {
        throw sinTamano("longs");
    }

    // Ojo con esta: **no** es la de la cantidad. `longs(long, long)` son origen y limite; la de una
    // sola cantidad es `longs(long)`. La colision de firmas es del JDK y se replica tal cual.
    default LongStream longs(long randomNumberOrigin, long randomNumberBound) {
        throw sinTamano("longs");
    }

    default DoubleStream doubles(long streamSize) {
        if (streamSize < 0) {
            throw new IllegalArgumentException("streamSize must be non-negative");
        }
        double[] a = new double[(int) streamSize];
        int i = 0;
        while (i < a.length) {
            a[i] = this.nextDouble();
            i = i + 1;
        }
        return DoubleStream.of(a);
    }

    default DoubleStream doubles(long streamSize, double randomNumberOrigin,
            double randomNumberBound) {
        if (streamSize < 0) {
            throw new IllegalArgumentException("streamSize must be non-negative");
        }
        double[] a = new double[(int) streamSize];
        int i = 0;
        while (i < a.length) {
            a[i] = this.nextDouble(randomNumberOrigin, randomNumberBound);
            i = i + 1;
        }
        return DoubleStream.of(a);
    }

    default DoubleStream doubles() {
        throw sinTamano("doubles");
    }

    default DoubleStream doubles(double randomNumberOrigin, double randomNumberBound) {
        throw sinTamano("doubles");
    }

    /**
     * Un generador que sabe **partirse**: dar otro generador independiente del primero.
     *
     * <p>Existe por un problema muy concreto del paralelismo. Compartir un generador entre hilos
     * exige sincronizarlo, y eso lo vuelve el cuello de botella; darle a cada hilo su propia semilla
     * "al azar" no garantiza nada -- dos semillas cercanas pueden dar secuencias solapadas. Partir
     * resuelve las dos cosas: cada hilo se lleva un generador propio, sin candado, y con la garantia
     * de que las secuencias no se pisan.
     *
     * <p>Las formas con `source` toman la entropia de **otro** generador en vez de la propia, que es
     * lo que permite reproducir una particion entera desde una sola semilla.
     */
    interface SplittableGenerator extends RandomGenerator {

        SplittableGenerator split();

        SplittableGenerator split(SplittableGenerator source);

        Stream<SplittableGenerator> splits(long streamSize);

        Stream<SplittableGenerator> splits(long streamSize, SplittableGenerator source);

        // Las dos sin cantidad se niegan, por la misma razon que `ints()`/`longs()`/`doubles()`:
        // los flujos de esta biblioteca son ansiosos y no pueden ser infinitos.
        default Stream<SplittableGenerator> splits() {
            throw new UnsupportedOperationException(
                    "los flujos de esta biblioteca son ansiosos: use splits(streamSize)");
        }

        default Stream<SplittableGenerator> splits(SplittableGenerator source) {
            throw new UnsupportedOperationException(
                    "los flujos de esta biblioteca son ansiosos: use splits(streamSize, source)");
        }
    }

    /**
     * Un generador que sabe **saltar**: avanzar de golpe una distancia enorme de su secuencia.
     *
     * <p>Resuelve el mismo problema que partir, por el otro camino. Un generador con un periodo
     * gigantesco se puede repartir entre hilos dandole a cada uno un tramo **disjunto**: el hilo N
     * arranca en la posicion N por la distancia de salto. La garantia no es estadistica sino
     * aritmetica -- los tramos no se solapan porque la distancia es conocida.
     *
     * <p>La diferencia con partir: saltar necesita que el algoritmo tenga una forma cerrada de
     * avanzar (una matriz de transicion elevada a una potencia), y no todos la tienen. Los LXM se
     * parten; los xoshiro saltan.
     */
    interface JumpableGenerator extends RandomGenerator {

        /** Una copia de este generador, en el mismo estado. */
        JumpableGenerator copy();

        /** Avanza este generador una distancia de salto. */
        void jump();

        /** Cuantos valores avanza {@link #jump()}. */
        double jumpDistance();

        /**
         * Una copia en el estado actual, y **este** queda avanzado un salto.
         *
         * <p>El orden importa y es el que dice el nombre: se copia primero. Lo que se devuelve es el
         * tramo que empieza donde estaba, y el que llama se queda con el siguiente.
         */
        default RandomGenerator copyAndJump() {
            RandomGenerator copia = this.copy();
            this.jump();
            return copia;
        }

        /** `streamSize` generadores, cada uno un salto mas adelante que el anterior. */
        default Stream<RandomGenerator> jumps(long streamSize) {
            if (streamSize < 0L) {
                throw new IllegalArgumentException("size must be non-negative");
            }
            java.util.List<RandomGenerator> salida = new java.util.ArrayList<RandomGenerator>();
            long i = 0L;
            while (i < streamSize) {
                salida.add(this.copyAndJump());
                i = i + 1L;
            }
            return salida.stream();
        }

        /** Igual que {@link #jumps(long)}: los flujos de esta biblioteca son ansiosos. */
        default Stream<RandomGenerator> rngs(long streamSize) {
            return this.jumps(streamSize);
        }

        // Las dos sin cantidad se niegan, por lo mismo que `ints()`/`longs()`/`doubles()`.
        default Stream<RandomGenerator> jumps() {
            throw new UnsupportedOperationException(
                    "los flujos de esta biblioteca son ansiosos: use jumps(streamSize)");
        }

        default Stream<RandomGenerator> rngs() {
            throw new UnsupportedOperationException(
                    "los flujos de esta biblioteca son ansiosos: use rngs(streamSize)");
        }

        /**
         * Un generador saltable del algoritmo que se nombra.
         *
         * @throws IllegalArgumentException si ese algoritmo no existe o no sabe saltar
         */
        static JumpableGenerator of(String name) {
            RandomGenerator g = RandomGeneratorFactory.of(name).create();
            if (!(g instanceof JumpableGenerator)) {
                throw new IllegalArgumentException("el algoritmo " + name + " no sabe saltar");
            }
            return (JumpableGenerator) g;
        }
    }

    /**
     * Un generador que ademas sabe dar un **salto largo**.
     *
     * <p>Los dos niveles no son un capricho: el salto reparte tramos entre hilos, y el salto largo
     * reparte **conjuntos de tramos** entre maquinas. Con un solo tamanio hay que elegir entre
     * granularidad fina y alcance, y con dos no.
     */
    interface LeapableGenerator extends JumpableGenerator {

        /** Una copia de este generador, en el mismo estado. */
        LeapableGenerator copy();

        /** Avanza este generador una distancia de salto largo. */
        void leap();

        /** Cuantos valores avanza {@link #leap()}. */
        double leapDistance();

        /** Una copia en el estado actual, y **este** queda avanzado un salto largo. */
        default JumpableGenerator copyAndLeap() {
            JumpableGenerator copia = this.copy();
            this.leap();
            return copia;
        }

        /** `streamSize` generadores, cada uno un salto largo mas adelante que el anterior. */
        default Stream<JumpableGenerator> leaps(long streamSize) {
            if (streamSize < 0L) {
                throw new IllegalArgumentException("size must be non-negative");
            }
            java.util.List<JumpableGenerator> salida = new java.util.ArrayList<JumpableGenerator>();
            long i = 0L;
            while (i < streamSize) {
                salida.add(this.copyAndLeap());
                i = i + 1L;
            }
            return salida.stream();
        }

        default Stream<JumpableGenerator> leaps() {
            throw new UnsupportedOperationException(
                    "los flujos de esta biblioteca son ansiosos: use leaps(streamSize)");
        }

        /**
         * Un generador de salto largo del algoritmo que se nombra.
         *
         * @throws IllegalArgumentException si ese algoritmo no existe o no sabe saltar largo
         */
        static LeapableGenerator of(String name) {
            RandomGenerator g = RandomGeneratorFactory.of(name).create();
            if (!(g instanceof LeapableGenerator)) {
                throw new IllegalArgumentException("el algoritmo " + name + " no sabe saltar largo");
            }
            return (LeapableGenerator) g;
        }
    }

    // ---- los estaticos de fabrica ---------------------------------------------------------------

    /**
     * Un generador del algoritmo que se nombra.
     *
     * <p>Los nombres son los doce de `RandomGeneratorFactory.names()`. Uno que no este ahi es un
     * `IllegalArgumentException`, no un generador por defecto silencioso: pedir `"Xoshiro256"` y
     * recibir otra cosa seria el peor resultado posible, porque el codigo seguiria andando con
     * propiedades estadisticas que no son las que pidio.
     *
     * @throws IllegalArgumentException si no hay implementacion de ese algoritmo
     */
    static RandomGenerator of(String name) {
        return RandomGeneratorFactory.of(name).create();
    }

    /**
     * El generador por defecto.
     *
     * <p>Delega en `RandomGeneratorFactory.getDefault()`, y esa delegacion es el punto: si los dos
     * eligieran por su cuenta podrian dejar de coincidir, y "el algoritmo por defecto" pasaria a
     * depender de por cual de las dos puertas se entro.
     */
    static RandomGenerator getDefault() {
        return RandomGeneratorFactory.getDefault().create();
    }

    // ---- las distribuciones ----------------------------------------------------------------------

    /**
     * Si el algoritmo esta **desaconsejado**.
     *
     * <p>`false` acá, y lo sobreescribe el que lo este. Hoy no lo esta **ninguno** de los doce, y se
     * verifico contra el JDK 25 en vez de darlo por sentado: `java.util.Random` es el candidato
     * obvio --su LCG de 48 bits sobrevive solo por compatibilidad, porque su secuencia es parte del
     * contrato y mejorarla romperia a todo el que dependa de ella-- y sin embargo `java` real
     * tambien devuelve `false` para el. Coincide con lo que dice `RandomGeneratorFactory`.
     */
    default boolean isDeprecated() {
        return false;
    }

    /**
     * Un valor de una normal de media 0 y desvio 1.
     *
     * <p>Es el metodo polar de Marsaglia: se tiran puntos uniformes en el cuadrado
     * {@code [-1,1]x[-1,1]} hasta que uno caiga dentro del circulo unitario, y el factor
     * {@code sqrt(-2*log(s)/s)} lo convierte en una normal.
     *
     * <p><b>El valor no es parte del contrato, y la distincion importa.</b> `java.util.Random`
     * **sobreescribe** este metodo y ahi el valor **si** lo es --su javadoc nombra el algoritmo, asi
     * que dos `Random` con la misma semilla tienen que dar los mismos gaussianos--. Este default no
     * nombra ninguno: lo unico que promete es la distribucion. Por eso no se copio la version del
     * JDK (un ziggurat con tablas de 256 entradas): daria otros numeros y ninguno de los dos estaria
     * mal.
     *
     * <p>A diferencia del de `Random`, este **no guarda** el segundo valor del par: se descarta uno
     * de cada dos. Guardarlo pediria estado, y una interfaz no tiene donde ponerlo.
     */
    default double nextGaussian() {
        double v1 = 0.0d;
        double s = 0.0d;
        boolean sirve = false;
        while (!sirve) {
            v1 = 2 * this.nextDouble() - 1;
            double v2 = 2 * this.nextDouble() - 1;
            s = v1 * v1 + v2 * v2;
            // `s == 0` se descarta junto con los de afuera del circulo: no solo dividiria por cero,
            // sino que `log(0)` es -infinito.
            sirve = s < 1 && s != 0;
        }
        return v1 * StrictMath.sqrt(-2 * StrictMath.log(s) / s);
    }

    /**
     * Un valor de una normal con la media y el desvio dados.
     *
     * @throws IllegalArgumentException si `stddev` es negativo
     */
    default double nextGaussian(double mean, double stddev) {
        // `stddev < 0`, la forma directa, y **no** una negada que atrape tambien al `NaN`.
        //
        // La version negada (`!(stddev >= 0)`) parece mejor y esta mal: el contrato dice "si stddev
        // es negativo", y `NaN` no es negativo. Se verifico contra `java` real, que devuelve `NaN`
        // en vez de tirar. Un `-0.0` tampoco tira, y tambien coincide: `-0.0 < 0` es false.
        if (stddev < 0.0d) {
            throw new IllegalArgumentException("stddev must be non-negative");
        }
        return mean + stddev * this.nextGaussian();
    }

    /**
     * Un valor de una exponencial de media 1.
     *
     * <p>Por transformada inversa: si {@code u} es uniforme en {@code (0,1]}, entonces
     * {@code -log(u)} es exponencial de media 1. Se usa {@code 1 - nextDouble()} y no
     * {@code nextDouble()} a secas justamente para que el cero quede afuera --`nextDouble()` es
     * {@code [0,1)}, y `log(0)` daria infinito--.
     *
     * <p>Como el de arriba, el valor no es parte del contrato: solo la distribucion. El JDK usa un
     * ziggurat, que es mas rapido y da otros numeros.
     */
    default double nextExponential() {
        return -StrictMath.log(1.0d - this.nextDouble());
    }

    /**
     * Un flujo de doubles equidistribuidos en el rango dado.
     *
     * <p>**Se niega**, por lo mismo que `ints()`/`longs()`/`doubles()`: el JDK lo define sin limite
     * de cantidad, y los flujos de esta biblioteca son ansiosos. Ver la nota larga de `sinTamano`.
     *
     * <p>Y acá no hay siquiera un reemplazo con tamaño que ofrecer --`equiDoubles` no tiene una
     * sobrecarga con `streamSize`--, asi que el mensaje manda a `doubles(streamSize, origin, bound)`,
     * que es lo mas cerca que se puede estar.
     */
    default DoubleStream equiDoubles(double origin, double bound, boolean isOriginInclusive,
            boolean isBoundInclusive) {
        throw new UnsupportedOperationException(
                "los flujos de esta biblioteca son ansiosos y `equiDoubles` no tiene una sobrecarga"
                        + " con tamano: use doubles(streamSize, origin, bound)");
    }
}
