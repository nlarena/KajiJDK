package jdk.internal.util.random;

import java.util.random.RandomGenerator;

/**
 * The bit-mixing functions every modern generator in {@code java.util.random} seeds itself with.
 *
 * <p>Mixing is needed because a generator's quality is a property of its STATE TRANSITION, not of
 * its starting point. Seed a shift-register generator with {@code 1} and its first outputs carry
 * almost no entropy — the state is nearly all zeros and it takes many steps to fill up. So the seed
 * is first run through a bijective avalanche function that spreads one changed input bit over all
 * 64 output bits, and the result becomes the state.
 *
 * <p>Every mixer here has the same shape: xor the value with a shifted copy of itself, multiply by
 * an odd constant, repeat. The xor-shift spreads bits downward, the multiply spreads them upward,
 * and alternating the two is what makes every input bit reach every output bit. Built only from
 * xor, shift and odd multiplication, each one is a BIJECTION — no two seeds can collide.
 *
 * <p>The constants and shift distances are not tunable. They are the published, search-tuned
 * values, and they were extracted from the JDK's own bytecode rather than transcribed.
 *
 * @implNote An internal package, not part of the public API. It exists here because the generators
 *           in {@code jdk.internal.random} need it, exactly as in the JDK.
 */
public final class RandomSupport {

    // ---- los mensajes de error, compartidos ------------------------------------------------------
    //
    // Son `public` porque el JDK los expone: cada generador los usa al validar sus argumentos, y que
    // esten aca es lo que hace que el mensaje sea **el mismo** venga de donde venga.

    /** El de un limite superior que no es positivo. */
    public static final String BAD_BOUND = "bound must be positive";

    /** El de un rango vacio o invertido. */
    public static final String BAD_RANGE = "bound must be greater than origin";

    /** El de un tamanio de flujo negativo. */
    public static final String BAD_SIZE = "size must be non-negative";

    /** El de una distancia de salto que no es finita y positiva. */
    public static final String BAD_DISTANCE =
            "jump distance must be finite, positive, and an exact integer";

    /** El de un limite en coma flotante que no es finito y positivo. */
    public static final String BAD_FLOATING_BOUND = "bound must be finite and positive";

    // ---- las dos constantes irracionales ---------------------------------------------------------
    //
    // Son los primeros bits de la parte fraccionaria de dos irracionales --la razon aurea y la razon
    // de plata-- redondeados a impar. Que sean irracionales es el punto: como incremento de un
    // contador, un numero cuya expansion binaria no tiene periodo hace que los estados sucesivos no
    // caigan en un patron; y que sean impares los vuelve invertibles modulo dos a la n, con lo cual
    // ningun par de contadores distintos colisiona.

    /** La razon aurea en 32 bits. */
    public static final int GOLDEN_RATIO_32 = 0x9e3779b9;

    /** La razon aurea en 64 bits. */
    public static final long GOLDEN_RATIO_64 = 0x9e3779b97f4a7c15L;

    /** La razon de plata en 32 bits. */
    public static final int SILVER_RATIO_32 = 0x6A09E667;

    /** La razon de plata en 64 bits. */
    public static final long SILVER_RATIO_64 = 0x6A09E667F3BCC909L;

    // `protected` y no `private`: es lo que declara el JDK. Una clase de utilidades estaticas no se
    // instancia, pero dejarlo protegido permite que una subclase exista, que es la diferencia entre
    // "no tiene sentido" y "esta prohibido".
    protected RandomSupport() {
    }

    // ---- validacion de argumentos ----------------------------------------------------------------

    /** @throws IllegalArgumentException si el limite no es positivo */
    public static void checkBound(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException(BAD_BOUND);
        }
    }

    /** @throws IllegalArgumentException si el limite no es positivo */
    public static void checkBound(long bound) {
        if (bound <= 0L) {
            throw new IllegalArgumentException(BAD_BOUND);
        }
    }

    /** @throws IllegalArgumentException si el limite no es finito y positivo */
    public static void checkBound(float bound) {
        if (!(0.0f < bound && bound < Float.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException(BAD_FLOATING_BOUND);
        }
    }

    /** @throws IllegalArgumentException si el limite no es finito y positivo */
    public static void checkBound(double bound) {
        if (!(0.0d < bound && bound < Double.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException(BAD_FLOATING_BOUND);
        }
    }

    /** @throws IllegalArgumentException si el rango esta vacio o invertido */
    public static void checkRange(int origin, int bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
    }

    /** @throws IllegalArgumentException si el rango esta vacio o invertido */
    public static void checkRange(long origin, long bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
    }

    // Las dos de coma flotante piden ademas que los dos extremos sean **finitos**. La forma negada
    // no es un adorno: atrapa tambien al NaN, que no es ni mayor ni menor que nada y con la
    // comparacion directa se colaria.

    /** @throws IllegalArgumentException si el rango esta vacio, invertido, o no es finito */
    public static void checkRange(float origin, float bound) {
        if (!(Float.NEGATIVE_INFINITY < origin && origin < bound
                && bound < Float.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
    }

    /** @throws IllegalArgumentException si el rango esta vacio, invertido, o no es finito */
    public static void checkRange(double origin, double bound) {
        if (!(Double.NEGATIVE_INFINITY < origin && origin < bound
                && bound < Double.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
    }

    /** @throws IllegalArgumentException si el tamanio del flujo es negativo */
    public static void checkStreamSize(long streamSize) {
        if (streamSize < 0L) {
            throw new IllegalArgumentException(BAD_SIZE);
        }
    }

    // ---- valores acotados ------------------------------------------------------------------------
    //
    // Los cuatro enteros comparten la misma idea, que es lo unico interesante de este bloque: tomar
    // el resto de un valor uniforme **sesga** el resultado cuando el rango no divide al espacio, asi
    // que hay que **rechazar** los candidatos sobre-representados y volver a tirar. Un rango que es
    // potencia de dos no tiene ese problema y se resuelve con una mascara.
    //
    // El bucle de rechazo tiene una forma incomoda a proposito --el trabajo esta en la condicion-- y
    // es la del JDK: como el primer candidato ya esta disponible, hace falta salir desde el medio.

    /** Un int uniforme en el rango de cero a bound, sin incluirlo. */
    public static int boundedNextInt(RandomGenerator rng, int bound) {
        final int m = bound - 1;
        int r = rng.nextInt();
        if ((bound & m) == 0) {
            r &= m;
        } else {
            for (int u = r >>> 1; u + m - (r = u % bound) < 0; u = rng.nextInt() >>> 1) {
                continue;
            }
        }
        return r;
    }

    /** Un int uniforme en el rango de origin a bound, sin incluirlo. */
    public static int boundedNextInt(RandomGenerator rng, int origin, int bound) {
        int r = rng.nextInt();
        if (origin < bound) {
            final int n = bound - origin;
            final int m = n - 1;
            if ((n & m) == 0) {
                r = (r & m) + origin;
            } else if (n > 0) {
                for (int u = r >>> 1; u + m - (r = u % n) < 0; u = rng.nextInt() >>> 1) {
                    continue;
                }
                r = r + origin;
            } else {
                // El ancho del rango no entra en un int: no hay aritmetica que sirva, se tira hasta
                // acertar.
                while (r < origin || r >= bound) {
                    r = rng.nextInt();
                }
            }
        }
        return r;
    }

    /** Un long uniforme en el rango de cero a bound, sin incluirlo. */
    public static long boundedNextLong(RandomGenerator rng, long bound) {
        final long m = bound - 1L;
        long r = rng.nextLong();
        if ((bound & m) == 0L) {
            r &= m;
        } else {
            for (long u = r >>> 1; u + m - (r = u % bound) < 0L; u = rng.nextLong() >>> 1) {
                continue;
            }
        }
        return r;
    }

    /** Un long uniforme en el rango de origin a bound, sin incluirlo. */
    public static long boundedNextLong(RandomGenerator rng, long origin, long bound) {
        long r = rng.nextLong();
        if (origin < bound) {
            final long n = bound - origin;
            final long m = n - 1L;
            if ((n & m) == 0L) {
                r = (r & m) + origin;
            } else if (n > 0L) {
                for (long u = r >>> 1; u + m - (r = u % n) < 0L; u = rng.nextLong() >>> 1) {
                    continue;
                }
                r = r + origin;
            } else {
                while (r < origin || r >= bound) {
                    r = rng.nextLong();
                }
            }
        }
        return r;
    }

    // Las de coma flotante escalan y despues **corrigen**: multiplicar puede redondear justo hasta el
    // limite, y el limite es exclusivo. Sin la correccion, el limite sale de vez en cuando.

    /** Un double uniforme en el rango de cero a bound, sin incluirlo. */
    public static double boundedNextDouble(RandomGenerator rng, double bound) {
        double r = rng.nextDouble();
        r = r * bound;
        if (r >= bound) {
            r = Math.nextDown(bound);
        }
        return r;
    }

    /** Un double uniforme en el rango de origin a bound, sin incluirlo. */
    public static double boundedNextDouble(RandomGenerator rng, double origin, double bound) {
        double r = rng.nextDouble();
        if (origin < bound) {
            if (bound - origin < Double.POSITIVE_INFINITY) {
                r = r * (bound - origin) + origin;
            } else {
                // El ancho no entra en un double: se escala a la mitad y se duplica al final.
                double mitadOrigen = 0.5d * origin;
                r = (r * (0.5d * bound - mitadOrigen) + mitadOrigen) * 2.0d;
            }
            if (r >= bound) {
                r = Math.nextDown(bound);
            }
        }
        return r;
    }

    /** Un float uniforme en el rango de cero a bound, sin incluirlo. */
    public static float boundedNextFloat(RandomGenerator rng, float bound) {
        float r = rng.nextFloat();
        r = r * bound;
        if (r >= bound) {
            r = Math.nextDown(bound);
        }
        return r;
    }

    /** Un float uniforme en el rango de origin a bound, sin incluirlo. */
    public static float boundedNextFloat(RandomGenerator rng, float origin, float bound) {
        float r = rng.nextFloat();
        if (origin < bound) {
            if (bound - origin < Float.POSITIVE_INFINITY) {
                r = r * (bound - origin) + origin;
            } else {
                float mitadOrigen = 0.5f * origin;
                r = (r * (0.5f * bound - mitadOrigen) + mitadOrigen) * 2.0f;
            }
            if (r >= bound) {
                r = Math.nextDown(bound);
            }
        }
        return r;
    }

    // ---- semillas --------------------------------------------------------------------------------

    /**
     * Una semilla inicial, distinta en cada llamada.
     *
     * <p>Mezcla el reloj de pared con el de alta resolucion **por separado** y despues los combina:
     * los dos solos son predecibles --el primero avanza de a milisegundos, el segundo arranca en un
     * origen arbitrario-- y lo que aporta cada uno es distinto.
     */
    public static long initialSeed() {
        return mixStafford13(System.currentTimeMillis()) ^ mixStafford13(System.nanoTime());
    }

    /**
     * Convierte una semilla de bytes de cualquier largo en n valores long, garantizando que los
     * ultimos z no sean todos cero.
     *
     * <p>Los tres pasos responden a tres problemas distintos, y conviene no confundirlos: empaquetar
     * los bytes que hay; **rellenar** con un generador si no alcanzan (una semilla corta dejaria el
     * resto en cero, que es un estado pobre); y garantizar que la cola no sea toda cero, porque para
     * un generador xor-shift el cero es un **punto fijo** -- se queda ahi para siempre.
     *
     * <p>El and con el complemento de uno de la ultima parte no es decorativo: cubre el caso z igual
     * a uno, donde hay que asegurar que el primer valor generado no sea cero.
     */
    public static long[] convertSeedBytesToLongs(byte[] seed, int n, int z) {
        final long[] result = new long[n];
        final int m = Math.min(seed.length, n << 3);
        int j = 0;
        while (j < m) {
            result[j >> 3] = (result[j >> 3] << 8) | (long) (seed[j] & 0xFF);
            j = j + 1;
        }
        long v = result[0];
        j = (m + 7) >> 3;
        while (j < n) {
            v = v + SILVER_RATIO_64;
            result[j] = mixMurmur64(v);
            j = j + 1;
        }
        boolean algunoNoCero = false;
        j = n - z;
        while (j < n) {
            if (result[j] != 0L) {
                algunoNoCero = true;
            }
            j = j + 1;
        }
        if (!algunoNoCero) {
            long w = result[0] & ~1L;
            j = n - z;
            while (j < n) {
                w = w + SILVER_RATIO_64;
                result[j] = mixMurmur64(w);
                j = j + 1;
            }
        }
        return result;
    }

    /** El gemelo de 32 bits de {@link #convertSeedBytesToLongs}. */
    public static int[] convertSeedBytesToInts(byte[] seed, int n, int z) {
        final int[] result = new int[n];
        final int m = Math.min(seed.length, n << 2);
        int j = 0;
        while (j < m) {
            result[j >> 2] = (result[j >> 2] << 8) | (seed[j] & 0xFF);
            j = j + 1;
        }
        int v = result[0];
        j = (m + 3) >> 2;
        while (j < n) {
            v = v + SILVER_RATIO_32;
            result[j] = mixMurmur32(v);
            j = j + 1;
        }
        boolean algunoNoCero = false;
        j = n - z;
        while (j < n) {
            if (result[j] != 0) {
                algunoNoCero = true;
            }
            j = j + 1;
        }
        if (!algunoNoCero) {
            int w = result[0] & ~1;
            j = n - z;
            while (j < n) {
                w = w + SILVER_RATIO_32;
                result[j] = mixMurmur32(w);
                j = j + 1;
            }
        }
        return result;
    }

    // ---- las dos distribuciones no uniformes -----------------------------------------------------
    //
    // **Delegan en el generador**, y eso es una diferencia con el JDK que conviene decir de frente:
    // el JDK las calcula con el ziggurat modificado de McFarland, que son dos tablas generadas de
    // varios cientos de entradas. Esta biblioteca usa el metodo polar para la normal y la
    // transformada inversa para la exponencial -- **la distribucion es la correcta, la secuencia no
    // es la misma**.
    //
    // Es la misma diferencia que ya tienen `RandomGenerator.nextGaussian` y `nextExponential`, y
    // esta escrita alli tambien. Delegar es lo que la mantiene en **un solo lugar**: si algun dia
    // entra el ziggurat, entra una vez.

    /** Un valor de una normal estandar. Ver la nota de arriba sobre la secuencia. */
    public static double computeNextGaussian(RandomGenerator rng) {
        return rng.nextGaussian();
    }

    /** Un valor de una exponencial de media 1. Ver la nota de arriba. */
    public static double computeNextExponential(RandomGenerator rng) {
        return rng.nextExponential();
    }

    /**
     * Igual que {@link #computeNextExponential}, con un tope **blando** en maxValue.
     *
     * <p>El tope existe en el JDK para acotar el peor caso del ziggurat: garantiza que el minimo
     * entre el valor y el tope tenga la distribucion correcta con una cantidad de llamadas lineal en
     * el tope, y para lograrlo **puede devolver un valor mayor** que el tope. Sin el ziggurat no hay
     * peor caso que acotar, asi que el tope no cambia nada -- y devolver mas que el tope sigue
     * estando permitido, que es justo lo que "blando" quiere decir.
     */
    public static double computeNextExponentialSoftCapped(RandomGenerator rng, double maxValue) {
        return rng.nextExponential();
    }

    /**
     * Stafford's variant 13 of the MurmurHash3 finalizer.
     *
     * <p>This is the mixer used to expand a seed into generator state throughout
     * {@code java.util.random}.
     *
     * @param z the value to mix
     * @return the mixed value
     */
    public static long mixStafford13(long z) {
        z = (z ^ (z >>> 30)) * -4658895280553007687L;
        z = (z ^ (z >>> 27)) * -7723592293110705685L;
        return z ^ (z >>> 31);
    }

    /**
     * Doug Lea's 64-bit mixer: the same constant twice, with 32-bit shifts.
     *
     * <p>Unlike the others this one is on the HOT PATH rather than only in seeding — the LXM
     * generators run every output through it.
     *
     * @param z the value to mix
     * @return the mixed value
     */
    public static long mixLea64(long z) {
        z = (z ^ (z >>> 32)) * -2685821657736338717L;
        z = (z ^ (z >>> 32)) * -2685821657736338717L;
        return z ^ (z >>> 32);
    }

    /**
     * The original MurmurHash3 64-bit finalizer.
     *
     * @param z the value to mix
     * @return the mixed value
     */
    public static long mixMurmur64(long z) {
        z = (z ^ (z >>> 33)) * -49064778989728563L;
        z = (z ^ (z >>> 33)) * -4265267296055464877L;
        return z ^ (z >>> 33);
    }

    /**
     * The 32-bit counterpart of {@link #mixLea64}, for generators whose state is int-sized.
     *
     * @param z the value to mix
     * @return the mixed value
     * @implSpec Same shape as the 64-bit version, with the shifts and constant retuned for half
     *           the width.
     */
    public static int mixLea32(int z) {
        z = (z ^ (z >>> 16)) * -747796405;
        z = (z ^ (z >>> 16)) * -747796405;
        return z ^ (z >>> 16);
    }

    /**
     * The 32-bit counterpart of {@link #mixMurmur64}.
     *
     * @param z the value to mix
     * @return the mixed value
     */
    public static int mixMurmur32(int z) {
        z = (z ^ (z >>> 16)) * -2048144789;
        z = (z ^ (z >>> 13)) * -1028477387;
        return z ^ (z >>> 16);
    }
}
