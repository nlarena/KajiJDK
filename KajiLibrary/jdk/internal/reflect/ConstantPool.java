package jdk.internal.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Member;

/**
 * KajiLibrary's jdk.internal.reflect.ConstantPool — acceso reflexivo al pool de constantes de una
 * clase ya cargada.
 *
 * <p>Existe para una sola cosa: las anotaciones. El atributo `RuntimeVisibleAnnotations` no guarda
 * texto sino **índices al pool de constantes** de la clase que las declara, así que quien quiera
 * parsear esos bytes crudos necesita, además de los bytes, el pool contra el cual resolverlos. Esta
 * clase es ese segundo argumento: es el tipo que el JDK nombra en
 * {@code VMSupport.encodeAnnotations(byte[], Class, ConstantPool, boolean, Class[])}.
 *
 * <p><strong>Ese método sigue sin estar en nuestro {@link jdk.internal.vm.VMSupport}</strong>, y no
 * se enlaza acá justamente por eso —un `@link` a un miembro que no existe promete algo que no está—.
 * Tener `ConstantPool` era necesario para poder siquiera escribir esa firma, pero no alcanza: el
 * cuerpo del JDK necesita además `sun.reflect.annotation.AnnotationParser`, que no está en esta
 * biblioteca, y un pool **con datos**, que por lo que sigue no puede existir. Los motivos completos
 * están en el encabezado de `VMSupport`.
 *
 * <h2>Es una superficie, y la razón es del otro lado de la frontera</h2>
 *
 * <p>En el JDK **los veinte métodos son un `native` de una línea**: cada uno le pasa al VM el campo
 * privado `constantPoolOop`, que es un puntero al pool interno de HotSpot y que **lo escribe el VM**
 * al fabricar el objeto. No hay constructor que lo llene: un `new ConstantPool()` en el JDK también
 * sale con el campo en `null` y todos sus métodos fallan. El único camino legítimo es que el VM te dé
 * uno, por `JavaLangAccess.getConstantPool(Class)`.
 *
 * <p>Esta VM no expone su pool de constantes a Java — no hay `JavaLangAccess.getConstantPool` ni nada
 * equivalente. Así que **no hay manera de que exista una instancia con datos**, y los métodos tiran
 * {@link UnsupportedOperationException} diciéndolo. Es la misma decisión que
 * {@link jdk.internal.vm.ContinuationSupport#ensureSupported()} y que {@code java.lang.StackWalker}:
 * el miembro está, con su firma y su tipo de retorno correctos, y corta apenas se lo usa en vez de
 * devolver un cero o un `null` que el que llama tomaría por un dato.
 *
 * <p><strong>Acá ningún método es `native`, y en el JDK los internos sí lo son.</strong> Es la misma
 * justificación que ya está escrita en {@code VMSupport.getVMTemporaryDirectory()} y en
 * {@code Continuation.pin()}: en esta VM un método `native` sin implementación registrada **no tira
 * una excepción, voltea el proceso**. Un `native` fiel al modificador mataría al programa que lo
 * llame; un método Java que tira deja al que llama con un error que puede atrapar y leer. Los
 * `native` del JDK son todos privados, así que la superficie pública no cambia.
 *
 * <p>El día que la VM entregue su pool, lo que cambia son los cuerpos y el campo — la firma de cada
 * método ya es la definitiva.
 */
public class ConstantPool {

    // El nombre lo conoce el VM en HotSpot; acá nadie lo escribe, y queda como la marca de por que
    // los metodos no pueden contestar. Se declara igual porque es lo que hace que la clase tenga una
    // sola razon de fallar en vez de veinte.
    private Object constantPoolOop;

    public ConstantPool() {
    }

    /** La cantidad de entradas, o sea el índice válido más grande. */
    public int getSize() {
        throw ConstantPool.sinPool("getSize");
    }

    /** La clase de la entrada `index`, cargándola si hace falta. */
    public Class<?> getClassAt(int index) {
        throw ConstantPool.sinPool("getClassAt");
    }

    /** Igual, pero `null` si esa clase todavía no está cargada. */
    public Class<?> getClassAtIfLoaded(int index) {
        throw ConstantPool.sinPool("getClassAtIfLoaded");
    }

    /** El índice de la referencia de clase de un método o un campo. */
    public int getClassRefIndexAt(int index) {
        throw ConstantPool.sinPool("getClassRefIndexAt");
    }

    /**
     * El método de la entrada `index`.
     *
     * <p>Devuelve `Member` y no `Method` porque también puede ser un constructor, y los
     * inicializadores estáticos vuelven como `Method`. Es del JDK, no una generalización nuestra.
     */
    public Member getMethodAt(int index) {
        throw ConstantPool.sinPool("getMethodAt");
    }

    /** Igual, pero `null` si la clase que lo declara no está cargada. */
    public Member getMethodAtIfLoaded(int index) {
        throw ConstantPool.sinPool("getMethodAtIfLoaded");
    }

    /** El campo de la entrada `index`. */
    public Field getFieldAt(int index) {
        throw ConstantPool.sinPool("getFieldAt");
    }

    /** Igual, pero `null` si la clase que lo declara no está cargada. */
    public Field getFieldAtIfLoaded(int index) {
        throw ConstantPool.sinPool("getFieldAtIfLoaded");
    }

    /** Nombre de clase, nombre de miembro y descriptor, en ese orden y sin cargar nada. */
    public String[] getMemberRefInfoAt(int index) {
        throw ConstantPool.sinPool("getMemberRefInfoAt");
    }

    /** El índice de la entrada `NameAndType` de un método, un campo o un `invokedynamic`. */
    public int getNameAndTypeRefIndexAt(int index) {
        throw ConstantPool.sinPool("getNameAndTypeRefIndexAt");
    }

    /** El nombre y el descriptor de una entrada `NameAndType`, en ese orden. */
    public String[] getNameAndTypeRefInfoAt(int index) {
        throw ConstantPool.sinPool("getNameAndTypeRefInfoAt");
    }

    /** La constante `int` de la entrada `index`. */
    public int getIntAt(int index) {
        throw ConstantPool.sinPool("getIntAt");
    }

    /** La constante `long`. */
    public long getLongAt(int index) {
        throw ConstantPool.sinPool("getLongAt");
    }

    /** La constante `float`. */
    public float getFloatAt(int index) {
        throw ConstantPool.sinPool("getFloatAt");
    }

    /** La constante `double`. */
    public double getDoubleAt(int index) {
        throw ConstantPool.sinPool("getDoubleAt");
    }

    /** La constante `String` — la entrada `CONSTANT_String`, ya resuelta. */
    public String getStringAt(int index) {
        throw ConstantPool.sinPool("getStringAt");
    }

    /** El texto crudo de una entrada `CONSTANT_Utf8`. */
    public String getUTF8At(int index) {
        throw ConstantPool.sinPool("getUTF8At");
    }

    /** Qué clase de entrada es la `index`. */
    public Tag getTagAt(int index) {
        throw ConstantPool.sinPool("getTagAt");
    }

    // Un solo lugar donde se dice por que, para que los veinte metodos den el mismo motivo y no
    // veinte variantes del mismo texto.
    private static UnsupportedOperationException sinPool(String metodo) {
        return new UnsupportedOperationException(
                "ConstantPool." + metodo + ": esta VM no expone el pool de constantes a Java");
    }

    /**
     * Qué clase de entrada es una del pool.
     *
     * <p>Los códigos son los de la especificación del formato `.class` (tabla 4.4-A), y por eso la
     * enumeración los lleva adentro en vez de depender del orden de las constantes. {@link #INVALID}
     * con código 0 es del JDK: cubre las entradas que el VM marca como no usables, que en la
     * especificación no tienen código propio.
     */
    public static enum Tag {
        /** `CONSTANT_Utf8`. */
        UTF8(1),
        /** `CONSTANT_Integer`. */
        INTEGER(3),
        /** `CONSTANT_Float`. */
        FLOAT(4),
        /** `CONSTANT_Long`. */
        LONG(5),
        /** `CONSTANT_Double`. */
        DOUBLE(6),
        /** `CONSTANT_Class`. */
        CLASS(7),
        /** `CONSTANT_String`. */
        STRING(8),
        /** `CONSTANT_Fieldref`. */
        FIELDREF(9),
        /** `CONSTANT_Methodref`. */
        METHODREF(10),
        /** `CONSTANT_InterfaceMethodref`. */
        INTERFACEMETHODREF(11),
        /** `CONSTANT_NameAndType`. */
        NAMEANDTYPE(12),
        /** `CONSTANT_MethodHandle`. */
        METHODHANDLE(15),
        /** `CONSTANT_MethodType`. */
        METHODTYPE(16),
        /** `CONSTANT_InvokeDynamic`. */
        INVOKEDYNAMIC(18),
        /** Ninguna de las anteriores. */
        INVALID(0);

        private final int codigo;

        private Tag(int codigo) {
            this.codigo = codigo;
        }

        // Del byte de la especificacion a la constante. Privado, igual que en el JDK: el codigo
        // numerico es un detalle del formato y no parte del contrato de la enumeracion.
        private static Tag deCodigo(byte v) {
            for (Tag t : Tag.values()) {
                if (t.codigo == v) {
                    return t;
                }
            }
            throw new IllegalArgumentException("codigo de tag desconocido " + v);
        }
    }
}
