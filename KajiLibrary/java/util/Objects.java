package java.util;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.util.Arrays;
import java.util.Comparator;

// java.util.Objects — los ayudantes estaticos que toleran null: igualdad, hash y toString que no
// revientan con una referencia vacia, y las guardas `requireNonNull` que convierten un NPE
// silencioso y lejano en uno ruidoso y en el lugar. No se instancia.
//
// La otra mitad, la que cierra el contrato en esta tanda, son los seis `check*`: la verificacion de
// indices y rangos que hasta ahora cada clase escribia a mano. No parecen gran cosa hasta que se ve
// que la razon por la que existen es que **el desbordamiento las hace dificiles de escribir bien**:
// el `from + size <= length` obvio da un falso positivo cuando `from + size` desborda, y por eso la
// version de aca compara al reves. Estan en el JDK desde el 9 justamente porque cada quien las
// escribia distinto y algunas mal.
public final class Objects {

    private Objects() {}

    // Equal if both null, or a.equals(b). Null-safe (a.equals is only called when a != null).
    public static boolean equals(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if (a == null) {
            return false;
        }
        return a.equals(b);
    }

    // The object's hashCode, or 0 for null.
    public static int hashCode(Object o) {
        if (o == null) {
            return 0;
        }
        return o.hashCode();
    }

    // The object's toString, or "null".
    public static String toString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString();
    }

    // The object's toString, or `nullDefault` for null.
    public static String toString(Object o, String nullDefault) {
        if (o == null) {
            return nullDefault;
        }
        return o.toString();
    }

    // Return `obj` if non-null, else throw NullPointerException. The standard argument guard.
    public static <T> T requireNonNull(T obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        return obj;
    }

    public static <T> T requireNonNull(T obj, String message) {
        if (obj == null) {
            throw new NullPointerException(message);
        }
        return obj;
    }

    public static boolean isNull(Object obj) {
        return obj == null;
    }

    public static boolean nonNull(Object obj) {
        return obj != null;
    }

    // Los dos argumentos comparados por `c`, o 0 si son el MISMO objeto.
    //
    // El atajo por identidad es lo que permite pasar `null, null`: nunca llega al comparador. Con
    // dos referencias distintas, en cambio, la responsabilidad de tolerar null es de `c`.
    public static <T> int compare(T a, T b, Comparator<? super T> c) {
        if (a == b) {
            return 0;
        }
        return c.compare(a, b);
    }

    // Igualdad **honda**: si los dos son arreglos, se comparan elemento por elemento (y si esos
    // elementos son arreglos, se baja otro nivel). Para cualquier otra cosa es `equals`.
    //
    // Existe porque `equals` sobre un arreglo es identidad: `new int[]{1}.equals(new int[]{1})` da
    // false, y no hay forma de arreglarlo desde `Object`.
    public static boolean deepEquals(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a instanceof Object[] && b instanceof Object[]) {
            return Arrays.deepEquals((Object[]) a, (Object[]) b);
        }
        if (a instanceof byte[] && b instanceof byte[]) {
            return Arrays.equals((byte[]) a, (byte[]) b);
        }
        if (a instanceof short[] && b instanceof short[]) {
            return Arrays.equals((short[]) a, (short[]) b);
        }
        if (a instanceof int[] && b instanceof int[]) {
            return Arrays.equals((int[]) a, (int[]) b);
        }
        if (a instanceof long[] && b instanceof long[]) {
            return Arrays.equals((long[]) a, (long[]) b);
        }
        if (a instanceof char[] && b instanceof char[]) {
            return Arrays.equals((char[]) a, (char[]) b);
        }
        if (a instanceof float[] && b instanceof float[]) {
            return Arrays.equals((float[]) a, (float[]) b);
        }
        if (a instanceof double[] && b instanceof double[]) {
            return Arrays.equals((double[]) a, (double[]) b);
        }
        if (a instanceof boolean[] && b instanceof boolean[]) {
            return Arrays.equals((boolean[]) a, (boolean[]) b);
        }
        return a.equals(b);
    }

    // El hash de una **secuencia** de valores, con la formula que fija el contrato de List.
    //
    // Ojo con el caso de un solo argumento: `hash(x)` NO es `hashCode(x)`, porque el varargs arma
    // un arreglo de uno y le aplica igual el `31 * 1 + h`. Es una trampa conocida del JDK y se
    // replica tal cual, porque el numero es parte del contrato.
    public static int hash(Object... values) {
        return Arrays.hashCode(values);
    }

    // La representacion que da `Object.toString` por defecto, aunque la clase la haya
    // sobreescrito: clase y hash de **identidad**.
    //
    // Sirve para lo que su nombre dice y no para lo que parece: cuando el `toString` propio miente,
    // o cuesta caro, o entra en recursion, este dice quien es el objeto sin preguntarle.
    public static String toIdentityString(Object o) {
        requireNonNull(o);
        return o.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(o));
    }

    // `requireNonNull` con el mensaje **diferido**: solo se arma si hay que lanzar.
    //
    // Esa es toda la diferencia con la version de String, y es la razon de ser: una guarda que pasa
    // el 99.99% de las veces no tiene por que pagar la concatenacion del mensaje.
    public static <T> T requireNonNull(T obj, java.util.function.Supplier<String> messageSupplier) {
        if (obj == null) {
            String m = null;
            if (messageSupplier != null) {
                m = messageSupplier.get();
            }
            throw new NullPointerException(m);
        }
        return obj;
    }

    // El primero si no es null, si no el segundo -- que **si** tiene que ser no-null.
    public static <T> T requireNonNullElse(T obj, T defaultObj) {
        if (obj != null) {
            return obj;
        }
        return requireNonNull(defaultObj, "defaultObj");
    }

    // Igual, con el reemplazo diferido.
    public static <T> T requireNonNullElseGet(T obj,
            java.util.function.Supplier<? extends T> supplier) {
        if (obj != null) {
            return obj;
        }
        T alterno = requireNonNull(supplier, "supplier").get();
        return requireNonNull(alterno, "supplier.get()");
    }

    // ---- verificacion de indices y rangos -------------------------------------------------------
    //
    // Las seis devuelven lo que recibieron cuando la comprobacion pasa, para poder encadenarlas en
    // una expresion: `arr[checkIndex(i, arr.length)]`.

    // `index` valido en `[0, length)`.
    public static int checkIndex(int index, int length) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException(
                    "Index " + index + " out of bounds for length " + length);
        }
        return index;
    }

    public static long checkIndex(long index, long length) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException(
                    "Index " + index + " out of bounds for length " + length);
        }
        return index;
    }

    // El rango `[from, to)` dentro de `[0, length]`. Devuelve `from`.
    public static int checkFromToIndex(int fromIndex, int toIndex, int length) {
        if (fromIndex < 0 || fromIndex > toIndex || toIndex > length) {
            throw new IndexOutOfBoundsException("Range [" + fromIndex + ", " + toIndex
                    + ") out of bounds for length " + length);
        }
        return fromIndex;
    }

    public static long checkFromToIndex(long fromIndex, long toIndex, long length) {
        if (fromIndex < 0 || fromIndex > toIndex || toIndex > length) {
            throw new IndexOutOfBoundsException("Range [" + fromIndex + ", " + toIndex
                    + ") out of bounds for length " + length);
        }
        return fromIndex;
    }

    // El rango `[from, from + size)` dentro de `[0, length]`. Devuelve `from`.
    //
    // La comparacion va escrita como `size > length - from` y **no** como `from + size > length`,
    // que es la forma obvia: la obvia desborda con un `size` grande, la suma da negativa, y el
    // chequeo pasa justo en el caso que tenia que atajar.
    public static int checkFromIndexSize(int fromIndex, int size, int length) {
        if (fromIndex < 0 || size < 0 || length < 0 || size > length - fromIndex) {
            throw new IndexOutOfBoundsException("Range [" + fromIndex + ", " + fromIndex + " + "
                    + size + ") out of bounds for length " + length);
        }
        return fromIndex;
    }

    public static long checkFromIndexSize(long fromIndex, long size, long length) {
        if (fromIndex < 0 || size < 0 || length < 0 || size > length - fromIndex) {
            throw new IndexOutOfBoundsException("Range [" + fromIndex + ", " + fromIndex + " + "
                    + size + ") out of bounds for length " + length);
        }
        return fromIndex;
    }
}
