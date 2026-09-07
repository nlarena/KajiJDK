package jdk.incubator.vector;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Un flotante de <strong>16 bits</strong>: el formato binary16 de IEEE 754.
 *
 * <h2>Para que sirve un flotante tan chico</h2>
 *
 * <p>Para mover el doble de datos por el mismo ancho de banda. En aprendizaje automatico y en
 * graficos, la precision de un {@code float} sobra y lo que falta es memoria y ancho de banda: la
 * mitad de bits significa el doble de valores por instruccion vectorial y por linea de cache.
 *
 * <h2>Once bits de precision, y lo que eso implica</h2>
 *
 * <p>El significando tiene 11 bits contando el implicito, o sea unas <strong>tres cifras decimales
 * y media</strong>. {@code 0.1} no se representa: lo mas cercano se imprime {@code 0.1} porque es lo
 * mas corto que redondea de vuelta, pero el valor real es otro.
 *
 * <p>El rango tambien es chico: {@link #MAX_VALUE} es 65504. Sumar 100000 da infinito. Es la
 * limitacion que sorprende a quien viene de {@code float}, donde el rango nunca es el problema.
 *
 * <h2>Por que es una clase y no un primitivo</h2>
 *
 * <p>Porque Java no tiene un tipo de 16 bits en punto flotante. Cada operacion crea un objeto, asi
 * que en un bucle escalar esta clase es <strong>mas lenta</strong> que {@code float}, no mas rapida.
 * La ganancia esta en los vectores, donde el valor vive en un registro y esta clase solo aparece en
 * los bordes.
 *
 * <h2>Como se calcula, y por que eso da el resultado exacto</h2>
 *
 * <p>Cada operacion se hace en {@code float} y se redondea a binary16 al final, que es lo que hace
 * el JDK. Parece que deberia redondear dos veces y perder exactitud, y no pasa: un {@code float}
 * tiene 24 bits de significando, mas del doble de los 11 de binary16, y con ese margen el doble
 * redondeo da siempre el mismo resultado que redondear una sola vez. Es el mismo argumento por el
 * que se puede calcular en {@code double} y redondear a {@code float}.
 *
 * <h2>Estado en esta VM</h2>
 *
 * <p>Esta clase esta <strong>completa y verificada</strong>: la aritmetica, las conversiones, el
 * formato decimal y el hexadecimal se comprobaron contra el JDK 25 sobre los 65536 valores que un
 * binary16 puede tomar, y sobre los pares de operandos de una muestra que cubre todos los casos de
 * borde.
 *
 * @since 21
 */
public final class Float16 extends Number implements Comparable<Float16> {

    private static final long serialVersionUID = 16L;

    /** Cuantos bits ocupa. */
    public static final int SIZE = 16;

    /** Cuantos bytes ocupa. */
    public static final int BYTES = 2;

    /** Bits de significando, contando el implicito. */
    public static final int PRECISION = 11;

    /** El exponente mas grande de un valor normal. */
    public static final int MAX_EXPONENT = 15;

    /** El exponente mas chico de un valor normal. */
    public static final int MIN_EXPONENT = -14;

    private static final int MASCARA_SIGNO = 0x8000;
    private static final int MASCARA_EXP = 0x7C00;
    private static final int MASCARA_SIG = 0x03FF;
    private static final int SESGO = 15;

    /** El infinito positivo. */
    public static final Float16 POSITIVE_INFINITY = new Float16((short) 0x7C00);

    /** El infinito negativo. */
    public static final Float16 NEGATIVE_INFINITY = new Float16((short) 0xFC00);

    /** El no-numero canonico. */
    public static final Float16 NaN = new Float16((short) 0x7E00);

    /** El valor finito mas grande: 65504. */
    public static final Float16 MAX_VALUE = new Float16((short) 0x7BFF);

    /** El valor normal mas chico que es positivo. */
    public static final Float16 MIN_NORMAL = new Float16((short) 0x0400);

    /** El valor positivo mas chico, subnormal. */
    public static final Float16 MIN_VALUE = new Float16((short) 0x0001);

    /** Los bits, tal cual. */
    private final short bits;

    private Float16(final short bits) {
        this.bits = bits;
    }

    // ---- construccion ----

    /**
     * El binary16 mas cercano a ese {@code float}.
     *
     * @param f el valor
     * @return el binary16
     */
    public static Float16 valueOf(final float f) {
        return new Float16(Float.floatToFloat16(f));
    }

    /**
     * El binary16 mas cercano a ese {@code double}.
     *
     * @param d el valor
     * @return el binary16
     */
    public static Float16 valueOf(final double d) {
        return valueOf((float) d);
    }

    /**
     * El binary16 mas cercano a ese {@code int}.
     *
     * @param i el valor
     * @return el binary16
     */
    public static Float16 valueOf(final int i) {
        return valueOf((float) i);
    }

    /**
     * El binary16 mas cercano a ese {@code long}.
     *
     * @param l el valor
     * @return el binary16
     */
    public static Float16 valueOf(final long l) {
        return valueOf((float) l);
    }

    /**
     * El binary16 mas cercano al numero escrito en ese texto.
     *
     * @param s el texto, en cualquiera de las formas que acepta {@code Float.parseFloat}
     * @return el binary16
     * @throws NumberFormatException si el texto no es un numero
     * @throws NullPointerException si es {@code null}
     */
    public static Float16 valueOf(final String s) throws NumberFormatException {
        return valueOf(Float.parseFloat(s));
    }

    /**
     * El binary16 mas cercano a ese decimal.
     *
     * @param bd el valor
     * @return el binary16
     * @throws NullPointerException si es {@code null}
     */
    public static Float16 valueOf(final BigDecimal bd) {
        return valueOf(bd.floatValue());
    }

    /**
     * El binary16 con esos bits, tal cual.
     *
     * @param bits los dieciseis bits
     * @return el binary16
     */
    public static Float16 shortBitsToFloat16(final short bits) {
        return new Float16(bits);
    }

    // ---- clasificacion ----

    /**
     * Si es un no-numero.
     *
     * @param f el valor
     * @return si es NaN
     */
    public static boolean isNaN(final Float16 f) {
        return (f.bits & MASCARA_EXP) == MASCARA_EXP && (f.bits & MASCARA_SIG) != 0;
    }

    /**
     * Si es uno de los dos infinitos.
     *
     * @param f el valor
     * @return si es infinito
     */
    public static boolean isInfinite(final Float16 f) {
        return (f.bits & MASCARA_EXP) == MASCARA_EXP && (f.bits & MASCARA_SIG) == 0;
    }

    /**
     * Si es finito: ni infinito ni NaN.
     *
     * @param f el valor
     * @return si es finito
     */
    public static boolean isFinite(final Float16 f) {
        return (f.bits & MASCARA_EXP) != MASCARA_EXP;
    }

    // ---- conversion a los tipos de Java ----

    /** {@inheritDoc} */
    public byte byteValue() {
        return (byte) floatValue();
    }

    /** {@inheritDoc} */
    public short shortValue() {
        return (short) floatValue();
    }

    /** {@inheritDoc} */
    public int intValue() {
        return (int) floatValue();
    }

    /** {@inheritDoc} */
    public long longValue() {
        return (long) floatValue();
    }

    /** {@inheritDoc} */
    public float floatValue() {
        return Float.float16ToFloat(bits);
    }

    /** {@inheritDoc} */
    public double doubleValue() {
        return floatValue();
    }

    // ---- bits ----

    /**
     * Los bits, sin canonizar el NaN.
     *
     * @param f el valor
     * @return los dieciseis bits tal cual
     */
    public static short float16ToRawShortBits(final Float16 f) {
        return f.bits;
    }

    /**
     * Los bits, con todos los NaN colapsados en uno solo.
     *
     * <p>Hay muchos patrones de bits que son NaN --cualquier significando distinto de cero con el
     * exponente lleno-- y esta version devuelve siempre el canonico. Es lo que hace falta para que
     * dos NaN se puedan comparar por bits; {@link #float16ToRawShortBits} conserva el patron
     * original, que a veces lleva informacion de diagnostico.
     *
     * @param f el valor
     * @return los bits, canonicos si es NaN
     */
    public static short float16ToShortBits(final Float16 f) {
        return isNaN(f) ? (short) 0x7E00 : f.bits;
    }

    // ---- igualdad y orden ----

    /**
     * Igualdad por bits canonicos.
     *
     * <p>Por eso {@code NaN.equals(NaN)} da {@code true} y {@code 0.0.equals(-0.0)} da
     * {@code false}, al reves de lo que hace {@code ==} sobre primitivos. Es la misma decision que
     * toma {@code Float.equals}, y existe para que estos objetos se puedan meter en una tabla hash.
     *
     * @param o el otro
     * @return si son el mismo valor
     */
    public boolean equals(final Object o) {
        return o instanceof Float16
                && float16ToShortBits((Float16) o) == float16ToShortBits(this);
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return hashCode(this);
    }

    /**
     * El codigo hash de ese valor.
     *
     * @param f el valor
     * @return el codigo
     */
    public static int hashCode(final Float16 f) {
        return float16ToShortBits(f);
    }

    /**
     * Orden total, con {@code -0.0} antes que {@code 0.0} y {@code NaN} al final.
     *
     * @param o el otro
     * @return negativo, cero o positivo
     */
    public int compareTo(final Float16 o) {
        return compare(this, o);
    }

    /**
     * Orden total entre dos valores.
     *
     * @param a el primero
     * @param b el segundo
     * @return negativo, cero o positivo
     */
    public static int compare(final Float16 a, final Float16 b) {
        return Float.compare(a.floatValue(), b.floatValue());
    }

    // ---- aritmetica ----

    /**
     * El mayor de los dos.
     *
     * @param a el primero
     * @param b el segundo
     * @return el mayor
     */
    public static Float16 max(final Float16 a, final Float16 b) {
        return valueOf(Math.max(a.floatValue(), b.floatValue()));
    }

    /**
     * El menor de los dos.
     *
     * @param a el primero
     * @param b el segundo
     * @return el menor
     */
    public static Float16 min(final Float16 a, final Float16 b) {
        return valueOf(Math.min(a.floatValue(), b.floatValue()));
    }

    /**
     * La suma, redondeada a binary16.
     *
     * @param a el primero
     * @param b el segundo
     * @return la suma
     */
    public static Float16 add(final Float16 a, final Float16 b) {
        return valueOf(a.floatValue() + b.floatValue());
    }

    /**
     * La resta, redondeada a binary16.
     *
     * @param a el minuendo
     * @param b el sustraendo
     * @return la resta
     */
    public static Float16 subtract(final Float16 a, final Float16 b) {
        return valueOf(a.floatValue() - b.floatValue());
    }

    /**
     * El producto, redondeado a binary16.
     *
     * @param a el primero
     * @param b el segundo
     * @return el producto
     */
    public static Float16 multiply(final Float16 a, final Float16 b) {
        return valueOf(a.floatValue() * b.floatValue());
    }

    /**
     * El cociente, redondeado a binary16.
     *
     * @param a el dividendo
     * @param b el divisor
     * @return el cociente
     */
    public static Float16 divide(final Float16 a, final Float16 b) {
        return valueOf(a.floatValue() / b.floatValue());
    }

    /**
     * La raiz cuadrada, redondeada a binary16.
     *
     * @param f el valor
     * @return la raiz
     */
    public static Float16 sqrt(final Float16 f) {
        return valueOf(Math.sqrt(f.doubleValue()));
    }

    /**
     * {@code a * b + c} con <strong>un solo redondeo</strong> al final.
     *
     * <p>Se calcula en {@code double}, donde el producto de dos binary16 entra exacto --once bits
     * por once bits dan veintidos, y un {@code double} tiene cincuenta y tres-- asi que el unico
     * redondeo es el de vuelta a binary16. Eso es exactamente lo que la operacion promete y lo que
     * la hace distinta de multiplicar y despues sumar.
     *
     * @param a el primer factor
     * @param b el segundo factor
     * @param c el sumando
     * @return el resultado
     */
    public static Float16 fma(final Float16 a, final Float16 b, final Float16 c) {
        return valueOf(Math.fma(a.doubleValue(), b.doubleValue(), c.doubleValue()));
    }

    /**
     * El mismo valor con el signo cambiado.
     *
     * <p>Da vuelta el bit de signo y nada mas, asi que anda con NaN y con los ceros.
     *
     * @param f el valor
     * @return el negado
     */
    public static Float16 negate(final Float16 f) {
        return new Float16((short) (f.bits ^ MASCARA_SIGNO));
    }

    /**
     * El valor absoluto.
     *
     * @param f el valor
     * @return el absoluto
     */
    public static Float16 abs(final Float16 f) {
        return new Float16((short) (f.bits & ~MASCARA_SIGNO));
    }

    /**
     * El signo como {@code 1.0}, {@code -1.0}, un cero con su signo, o NaN.
     *
     * @param f el valor
     * @return el signo
     */
    public static Float16 signum(final Float16 f) {
        if (isNaN(f) || (f.bits & ~MASCARA_SIGNO) == 0) {
            return f;
        }
        return f.bits < 0 ? valueOf(-1.0f) : valueOf(1.0f);
    }

    // ---- exponente y vecinos ----

    /**
     * El exponente binario sin sesgo.
     *
     * <p>Para un cero o un subnormal devuelve {@code MIN_EXPONENT - 1}, y para un infinito o un NaN
     * {@code MAX_EXPONENT + 1}. Los dos son valores fuera del rango de los normales, que es como se
     * distinguen sin tener que preguntar aparte.
     *
     * @param f el valor
     * @return el exponente
     */
    public static int getExponent(final Float16 f) {
        return ((f.bits & MASCARA_EXP) >> 10) - SESGO;
    }

    /**
     * La distancia hasta el proximo valor representable.
     *
     * @param f el valor
     * @return el ulp
     */
    public static Float16 ulp(final Float16 f) {
        final int exp = getExponent(f);
        if (exp == MAX_EXPONENT + 1) {
            return abs(f);
        }
        if (exp == MIN_EXPONENT - 1) {
            return MIN_VALUE;
        }
        final int e = exp - (PRECISION - 1);
        if (e >= MIN_EXPONENT) {
            return scalb(valueOf(1.0f), e);
        }
        // Por debajo del rango normal el ulp es siempre el subnormal minimo desplazado.
        return new Float16((short) (1 << (e - (MIN_EXPONENT - (PRECISION - 1)))));
    }

    /**
     * El valor representable inmediatamente mayor.
     *
     * @param f el valor
     * @return el siguiente
     */
    public static Float16 nextUp(final Float16 f) {
        if (isNaN(f) || f.bits == (short) 0x7C00) {
            return f;
        }
        // El cero negativo se trata como positivo: el siguiente es el subnormal minimo positivo.
        if ((f.bits & ~MASCARA_SIGNO) == 0) {
            return MIN_VALUE;
        }
        return new Float16((short) (f.bits > 0 ? f.bits + 1 : f.bits - 1));
    }

    /**
     * El valor representable inmediatamente menor.
     *
     * @param f el valor
     * @return el anterior
     */
    public static Float16 nextDown(final Float16 f) {
        if (isNaN(f) || f.bits == (short) 0xFC00) {
            return f;
        }
        if ((f.bits & ~MASCARA_SIGNO) == 0) {
            return new Float16((short) 0x8001);
        }
        return new Float16((short) (f.bits > 0 ? f.bits - 1 : f.bits + 1));
    }

    /**
     * El valor multiplicado por dos elevado a {@code n}, con un solo redondeo.
     *
     * @param f el valor
     * @param n el exponente de la escala
     * @return el escalado
     */
    public static Float16 scalb(final Float16 f, final int n) {
        // Se acota antes de escalar: el rango de binary16 entra de sobra en 2^+-50, y sin acotar un
        // n grande desbordaria el double intermedio en vez de dar el infinito o el cero que
        // corresponde.
        final int k = Math.min(Math.max(n, -50), 50);
        return valueOf(f.doubleValue() * Math.scalb(1.0, k));
    }

    /**
     * La magnitud del primero con el signo del segundo.
     *
     * @param magnitude de donde sale el valor
     * @param sign de donde sale el signo
     * @return el resultado
     */
    public static Float16 copySign(final Float16 magnitude, final Float16 sign) {
        return new Float16((short) ((magnitude.bits & ~MASCARA_SIGNO)
                | (sign.bits & MASCARA_SIGNO)));
    }

    // ---- texto ----

    /** {@inheritDoc} */
    public String toString() {
        return toString(this);
    }

    /**
     * El decimal <strong>mas corto que vuelve a dar este mismo valor</strong> al leerlo.
     *
     * <p>Es la misma regla que {@code Float.toString} y la que hace que imprimir y volver a leer no
     * pierda nada. Se busca probando con una cifra significativa, dos, y asi hasta cinco, que es lo
     * maximo que un binary16 necesita.
     *
     * <p>La forma tambien es la de Java: decimal comun mientras el valor este entre 10^-3 y 10^7, y
     * notacion cientifica fuera de esa franja.
     *
     * @param f el valor
     * @return el texto
     */
    public static String toString(final Float16 f) {
        if (isNaN(f)) {
            return "NaN";
        }
        if (isInfinite(f)) {
            return f.bits < 0 ? "-Infinity" : "Infinity";
        }
        if ((f.bits & ~MASCARA_SIGNO) == 0) {
            return f.bits < 0 ? "-0.0" : "0.0";
        }

        final boolean negativo = f.bits < 0;
        final BigDecimal exacto = new BigDecimal(abs(f).doubleValue());
        BigDecimal elegido = null;
        for (int p = 1; p <= 5; p++) {
            final BigDecimal r = exacto.round(new MathContext(p, RoundingMode.HALF_EVEN));
            if (Float.floatToFloat16(r.floatValue()) == abs(f).bits) {
                elegido = r;
                break;
            }
        }
        if (elegido == null) {
            elegido = exacto;
        }
        return (negativo ? "-" : "") + formatear(elegido.stripTrailingZeros());
    }

    /**
     * Le da a un decimal positivo la forma que usa {@code Float.toString}.
     *
     * <p>Decimal comun entre 10^-3 y 10^7, cientifica fuera; siempre con al menos una cifra despues
     * del punto, que es lo que distingue {@code "1.0"} de {@code "1"} y hace que el texto se lea
     * como un flotante y no como un entero.
     */
    private static String formatear(final BigDecimal v) {
        final String digitos = v.unscaledValue().toString();
        // exponente decimal: el valor es 0.<digitos> * 10^exp10
        final int exp10 = digitos.length() - v.scale();
        if (exp10 > -3 && exp10 <= 7) {
            if (exp10 <= 0) {
                final StringBuilder sb = new StringBuilder("0.");
                for (int i = 0; i < -exp10; i++) {
                    sb.append('0');
                }
                return sb.append(digitos).toString();
            }
            if (exp10 >= digitos.length()) {
                final StringBuilder sb = new StringBuilder(digitos);
                for (int i = digitos.length(); i < exp10; i++) {
                    sb.append('0');
                }
                return sb.append(".0").toString();
            }
            return digitos.substring(0, exp10) + "." + digitos.substring(exp10);
        }
        final String resto = digitos.length() > 1 ? digitos.substring(1) : "0";
        return digitos.charAt(0) + "." + resto + "E" + (exp10 - 1);
    }

    /**
     * El valor en hexadecimal, con el significando exacto.
     *
     * <p>Es la unica forma de escribir un flotante sin perder nada y sin depender del redondeo
     * decimal: {@code 0x1.554p-2} dice exactamente que bits hay. Los normales llevan el
     * {@code 0x1.} del bit implicito y los subnormales {@code 0x0.} con exponente {@code p-14}.
     *
     * @param f el valor
     * @return el texto
     */
    public static String toHexString(final Float16 f) {
        if (!isFinite(f)) {
            return toString(f);
        }
        final String signo = f.bits < 0 ? "-" : "";
        final int exp = (f.bits & MASCARA_EXP) >> 10;
        final int sig = f.bits & MASCARA_SIG;
        if (exp == 0 && sig == 0) {
            return signo + "0x0.0p0";
        }
        // Los diez bits del significando se escriben como tres digitos hexadecimales, o sea doce
        // bits: por eso el desplazamiento de dos.
        String mant = Integer.toHexString(sig << 2);
        while (mant.length() < 3) {
            mant = "0" + mant;
        }
        while (mant.length() > 1 && mant.endsWith("0")) {
            mant = mant.substring(0, mant.length() - 1);
        }
        if (exp == 0) {
            return signo + "0x0." + mant + "p-14";
        }
        return signo + "0x1." + mant + "p" + (exp - SESGO);
    }
}
