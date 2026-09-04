package java.lang.runtime;

/**
 * Las veintiuna preguntas de la forma "¿esta conversión pierde algo?".
 *
 * <h2>Para qué existe</h2>
 *
 * <p>Desde que un {@code switch} puede tener patrones de tipo primitivo, el compilador necesita
 * emitir una prueba: {@code case byte b} sobre un {@code int} tiene que aceptar el valor sólo si el
 * {@code int} <em>cabe</em> en un {@code byte}. Esa prueba no se puede escribir una sola vez para
 * todos los pares —cada uno pierde algo distinto— así que el JDK la resuelve con un método por par,
 * y esto es esa tabla. La llama código generado, no gente.
 *
 * <h2>La idea, que es una sola</h2>
 *
 * <p>Toda conversión exacta cumple lo mismo: <strong>convertir y volver da el valor original</strong>.
 * De ahí que casi todos los cuerpos sean {@code n == (T)(U)n}. Convertir a {@code U} pierde lo que
 * tenga que perder; volver a {@code T} no puede recuperarlo; si igual coinciden, no se perdió nada.
 *
 * <h2>Las dos excepciones, que son donde está el contenido</h2>
 *
 * <p><strong>El cero negativo.</strong> IEEE-754 tiene dos ceros, {@code +0.0} y {@code -0.0}, y son
 * distintos: difieren en el bit de signo. Los enteros tienen uno solo. Así que {@code -0.0f} pasa la
 * prueba de ida y vuelta —{@code (float)(int)(-0.0f)} es {@code 0.0f}, y {@code 0.0f == -0.0f} da
 * {@code true}— y sin embargo la conversión <em>sí</em> perdió algo: el signo. Por eso cada prueba
 * de punto flotante a entero lleva pegado un {@code && !esCeroNegativo(n)}.
 *
 * <p>Que {@code 0.0f == -0.0f} sea {@code true} es lo que hace que la comparación no alcance, y es
 * también por qué {@link #esCeroNegativo(float)} mira los <em>bits</em>: es la única forma de
 * distinguir dos valores que el operador {@code ==} declara iguales.
 *
 * <p><strong>NaN hacia {@code float}.</strong> {@link #isDoubleToFloatExact} es el único que acepta
 * un valor que falla la ida y vuelta, y a propósito: {@code NaN != NaN}, así que la comparación da
 * {@code false} aunque un {@code double} NaN se convierta a {@code float} sin perder nada que Java
 * sepa observar —el <em>payload</em> del NaN es justamente lo que la especificación deja sin
 * definir—. De ahí el {@code || n != n}, que es la forma canónica de preguntar "¿es NaN?".
 *
 * @since 21
 */
public final class ExactConversionsSupport {

    // Nadie instancia una tabla de funciones.
    private ExactConversionsSupport() {
    }

    /**
     * Si {@code n} es {@code -0.0f}.
     *
     * <p>Por los bits y no por comparación, porque {@code -0.0f == 0.0f} es {@code true} y entonces
     * ningún {@code ==} puede separarlos. {@link Float#floatToRawIntBits} de {@code -0.0f} es
     * exactamente {@link Integer#MIN_VALUE}: el bit de signo prendido y todo lo demás en cero.
     *
     * <p>Es {@code Raw} y no {@code floatToIntBits} porque acá no hay que colapsar NaNs — un NaN
     * nunca va a dar {@code MIN_VALUE}, y colapsarlo sería trabajo de más en la prueba más caliente
     * que tiene esta clase.
     */
    private static boolean esCeroNegativo(float n) {
        return Float.floatToRawIntBits(n) == Integer.MIN_VALUE;
    }

    /** Lo mismo para {@code double}: {@code -0.0} tiene los bits de {@link Long#MIN_VALUE}. */
    private static boolean esCeroNegativo(double n) {
        return Double.doubleToRawLongBits(n) == Long.MIN_VALUE;
    }

    /** Si {@code n} cabe en un {@code byte}, o sea si está en {@code [-128, 127]}. */
    public static boolean isIntToByteExact(int n) {
        return n == (int) (byte) n;
    }

    /** Si {@code n} cabe en un {@code short}. */
    public static boolean isIntToShortExact(int n) {
        return n == (int) (short) n;
    }

    /**
     * Si {@code n} cabe en un {@code char}.
     *
     * <p>{@code char} no tiene signo, así que ningún negativo pasa por chico que sea: {@code -1} da
     * {@code 65535} al convertir y no vuelve.
     */
    public static boolean isIntToCharExact(int n) {
        return n == (int) (char) n;
    }

    /**
     * Si {@code n} cabe en un {@code float}.
     *
     * <p>No es una pregunta de rango sino de <em>precisión</em>: un {@code float} tiene 24 bits de
     * mantisa y un {@code int} tiene 32, así que los enteros grandes se redondean. {@code 16777217}
     * es el primero que falla.
     */
    public static boolean isIntToFloatExact(int n) {
        return n == (int) (float) n;
    }

    /** Si {@code n} cabe en un {@code byte}. */
    public static boolean isLongToByteExact(long n) {
        return n == (long) (byte) n;
    }

    /** Si {@code n} cabe en un {@code short}. */
    public static boolean isLongToShortExact(long n) {
        return n == (long) (short) n;
    }

    /** Si {@code n} cabe en un {@code char}. */
    public static boolean isLongToCharExact(long n) {
        return n == (long) (char) n;
    }

    /** Si {@code n} cabe en un {@code int}. */
    public static boolean isLongToIntExact(long n) {
        return n == (long) (int) n;
    }

    /** Si {@code n} cabe en un {@code float}, con los 24 bits de mantisa que eso implica. */
    public static boolean isLongToFloatExact(long n) {
        return n == (long) (float) n;
    }

    /**
     * Si {@code n} cabe en un {@code double}.
     *
     * <p>53 bits de mantisa contra 64 de {@code long}: la mayoría entra, los muy grandes no.
     */
    public static boolean isLongToDoubleExact(long n) {
        return n == (long) (double) n;
    }

    /** Si {@code n} es un entero de {@code byte} y no es {@code -0.0f}. */
    public static boolean isFloatToByteExact(float n) {
        return n == (float) (byte) n && !esCeroNegativo(n);
    }

    /** Si {@code n} es un entero de {@code short} y no es {@code -0.0f}. */
    public static boolean isFloatToShortExact(float n) {
        return n == (float) (short) n && !esCeroNegativo(n);
    }

    /** Si {@code n} es un entero de {@code char} y no es {@code -0.0f}. */
    public static boolean isFloatToCharExact(float n) {
        return n == (float) (char) n && !esCeroNegativo(n);
    }

    /** Si {@code n} es un entero de {@code int} y no es {@code -0.0f}. */
    public static boolean isFloatToIntExact(float n) {
        return n == (float) (int) n && !esCeroNegativo(n);
    }

    /** Si {@code n} es un entero de {@code long} y no es {@code -0.0f}. */
    public static boolean isFloatToLongExact(float n) {
        return n == (float) (long) n && !esCeroNegativo(n);
    }

    /** Si {@code n} es un entero de {@code byte} y no es {@code -0.0}. */
    public static boolean isDoubleToByteExact(double n) {
        return n == (double) (byte) n && !esCeroNegativo(n);
    }

    /** Si {@code n} es un entero de {@code short} y no es {@code -0.0}. */
    public static boolean isDoubleToShortExact(double n) {
        return n == (double) (short) n && !esCeroNegativo(n);
    }

    /** Si {@code n} es un entero de {@code char} y no es {@code -0.0}. */
    public static boolean isDoubleToCharExact(double n) {
        return n == (double) (char) n && !esCeroNegativo(n);
    }

    /** Si {@code n} es un entero de {@code int} y no es {@code -0.0}. */
    public static boolean isDoubleToIntExact(double n) {
        return n == (double) (int) n && !esCeroNegativo(n);
    }

    /** Si {@code n} es un entero de {@code long} y no es {@code -0.0}. */
    public static boolean isDoubleToLongExact(double n) {
        return n == (double) (long) n && !esCeroNegativo(n);
    }

    /**
     * Si {@code n} cabe en un {@code float}.
     *
     * <p>El único de los veintiuno que acepta algo que falla la ida y vuelta — ver el NaN en la
     * descripción de la clase. Y el único de los de punto flotante que <strong>no</strong> excluye
     * el cero negativo, porque acá el destino también tiene dos ceros: {@code -0.0} se convierte a
     * {@code -0.0f} sin perder nada.
     */
    public static boolean isDoubleToFloatExact(double n) {
        return n == (double) (float) n || n != n;
    }
}
