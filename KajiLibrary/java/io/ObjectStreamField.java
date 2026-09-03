package java.io;

// KajiLibrary's java.io.ObjectStreamField -- la descripcion de **un** campo serializable.
//
// Es un valor, no una operacion: nombre, tipo, y si va compartido. Vale por si solo -- quien declara
// un `serialPersistentFields` esta describiendo la forma de su clase, y esa descripcion es correcta
// se serialice o no despues -- y ademas es la moneda con la que los dos flujos se entienden: el que
// escribe saca de aca el orden y el codigo de tipo de cada campo, y el que lee arma uno por cada
// campo que viene del otro lado, con **la firma del flujo** y no con la de un tipo local.
//
// **El orden es parte del formato, no una comodidad.** `compareTo` pone los primitivos antes que
// las referencias, y dentro de cada grupo ordena por nombre. La separacion existe porque el flujo
// escribe primero todos los valores primitivos --de tamanio conocido, uno pegado al otro-- y despues
// las referencias, que llevan cada una su propia estructura. Mezclarlos obligaria a intercalar dos
// formas de decodificar en el mismo bloque.
public class ObjectStreamField implements Comparable<Object> {

    private final String name;
    private final Class<?> type;
    private final boolean unshared;

    // El descriptor JVM del tipo: `I`, `Ljava/lang/String;`, `[[D`. Se calcula una vez porque
    // `toString` y `getTypeString` lo piden y armarlo recorre el nombre del tipo.
    private final String signature;

    private int offset = 0;

    /** Un campo compartido (`unshared` en falso), que es lo normal. */
    public ObjectStreamField(String name, Class<?> type) {
        this(name, type, false);
    }

    /**
     * Un campo del tipo dado.
     *
     * <p>`unshared` en verdadero pide que el valor se escriba y se lea **sin** pasar por la tabla de
     * referencias compartidas del flujo. Sirve cuando el objeto tiene que ser exclusivo de este
     * campo: con la tabla, dos campos que apuntaban al mismo objeto lo siguen compartiendo despues
     * de deserializar, y una clase que dependa de tener el suyo propio se rompe en silencio.
     *
     * @throws NullPointerException si `name` o `type` son `null` -- un campo sin nombre o sin tipo
     *     no describe nada
     */
    public ObjectStreamField(String name, Class<?> type, boolean unshared) {
        if (name == null || type == null) {
            throw new NullPointerException();
        }
        this.name = name;
        this.type = type;
        this.unshared = unshared;
        this.signature = descriptor(type);
    }

    /**
     * El campo tal como vino de un flujo: nombre y **firma del flujo**, no de un tipo local.
     *
     * <p>La firma se toma cruda en vez de derivarla de una `Class` porque el flujo puede nombrar un
     * tipo que de este lado no existe -- y esa firma es justamente lo que hay que comparar contra el
     * campo local para decidir si son el mismo campo. Derivarla de un tipo local obligaria a
     * resolver la clase antes de poder comparar, que es al reves de como se lee.
     *
     * <p>`type` queda en {@code Object.class} para los campos de referencia, como en el JDK: el tipo
     * de verdad puede no estar cargado, y `Object` es lo unico cierto que se puede afirmar sin
     * cargarlo.
     */
    ObjectStreamField(String name, String firma) {
        this.name = name;
        this.signature = firma;
        this.unshared = false;
        this.type = tipoDeFirma(firma);
    }

    private static Class<?> tipoDeFirma(String firma) {
        char c = firma.charAt(0);
        if (c == 'I') {
            return Integer.TYPE;
        }
        if (c == 'J') {
            return Long.TYPE;
        }
        if (c == 'D') {
            return Double.TYPE;
        }
        if (c == 'F') {
            return Float.TYPE;
        }
        if (c == 'B') {
            return Byte.TYPE;
        }
        if (c == 'S') {
            return Short.TYPE;
        }
        if (c == 'C') {
            return Character.TYPE;
        }
        if (c == 'Z') {
            return Boolean.TYPE;
        }
        return Object.class;
    }

    public String getName() {
        return this.name;
    }

    public Class<?> getType() {
        return this.type;
    }

    /**
     * La letra del tipo: `B C D F I J S Z` para los primitivos, `[` para arreglos, `L` para el
     * resto.
     *
     * <p>Es la primera letra del descriptor, y por eso sale de ahi en vez de repetir la tabla: dos
     * copias de la misma correspondencia terminan discrepando.
     */
    public char getTypeCode() {
        return this.signature.charAt(0);
    }

    /**
     * El descriptor completo, o `null` si el campo es primitivo.
     *
     * <p>`null` y no la letra suelta, que es lo que uno esperaria: para un primitivo el codigo de
     * tipo ya dice todo lo que hay que saber, y devolver algo aca haria que el que llama tuviera dos
     * fuentes para el mismo dato. El contrato usa la ausencia para decir "primitivo".
     */
    public String getTypeString() {
        return this.isPrimitive() ? null : this.signature;
    }

    /** Donde cae este campo dentro del bloque de datos del flujo. */
    public int getOffset() {
        return this.offset;
    }

    /**
     * Fija el desplazamiento.
     *
     * <p>Es `protected` porque lo decide quien arma el bloque --el descriptor de la clase-- y no
     * quien describe el campo: un `serialPersistentFields` escrito a mano que se pusiera a mover
     * offsets desacomodaria el formato para todos los demas campos.
     */
    protected void setOffset(int offset) {
        this.offset = offset;
    }

    public boolean isPrimitive() {
        char c = this.signature.charAt(0);
        return c != 'L' && c != '[';
    }

    public boolean isUnshared() {
        return this.unshared;
    }

    /**
     * Primitivos antes que referencias; a igual categoria, por nombre.
     *
     * <p>Recibe `Object` y no `ObjectStreamField` porque asi lo declara el JDK --la clase es
     * `Comparable<Object>`-- y estrecharlo aca haria que un `Comparable` crudo dejara de compilar.
     */
    public int compareTo(Object obj) {
        ObjectStreamField otro = (ObjectStreamField) obj;
        boolean mio = this.isPrimitive();
        if (mio != otro.isPrimitive()) {
            return mio ? -1 : 1;
        }
        return this.name.compareTo(otro.name);
    }

    /** El descriptor y el nombre, que es como se lee un campo en un volcado de clase. */
    public String toString() {
        return this.signature + " " + this.name;
    }

    // El descriptor JVM de un tipo. Los arreglos se piden a `Class.getName()`, que ya devuelve la
    // forma con corchetes (`[[D`) y solo hay que cambiarle los puntos por barras.
    private static String descriptor(Class<?> t) {
        if (t == Integer.TYPE) {
            return "I";
        }
        if (t == Long.TYPE) {
            return "J";
        }
        if (t == Double.TYPE) {
            return "D";
        }
        if (t == Float.TYPE) {
            return "F";
        }
        if (t == Byte.TYPE) {
            return "B";
        }
        if (t == Short.TYPE) {
            return "S";
        }
        if (t == Character.TYPE) {
            return "C";
        }
        if (t == Boolean.TYPE) {
            return "Z";
        }
        if (t == Void.TYPE) {
            return "V";
        }
        String n = t.getName();
        if (n.charAt(0) == '[') {
            return n.replace('.', '/');
        }
        return "L" + n.replace('.', '/') + ";";
    }
}
