package java.lang.foreign;

import java.nio.ByteOrder;

/**
 * KajiLibrary's java.lang.foreign.ValueLayout -- el layout de un valor que se puede leer y escribir
 * de una: los ocho primitivos de Java, mas la direccion.
 *
 * <p>Es la hoja del arbol de layouts. Todo lo demas --secuencias, structs, uniones-- se compone de
 * estos, y son los unicos que un {@link MemorySegment} sabe leer directamente.
 *
 * <p>Lleva tres cosas que un layout compuesto no tiene: el **tipo Java** que transporta
 * ({@link #carrier()}), el **orden de bytes** ({@link #order()}), y --por lo tanto-- una lectura
 * bien definida. Los ocho subtipos anidados (`OfInt`, `OfLong`, ...) existen para que el compilador
 * pueda distinguirlos: `segmento.get(JAVA_INT, 0)` devuelve un `int` y no un `Object` porque la
 * sobrecarga se elige por el tipo del layout.
 *
 * <h2>Las constantes, y por que hay dos de cada una</h2>
 *
 * <p>`JAVA_INT` se alinea a 4; `JAVA_INT_UNALIGNED` a 1. La diferencia importa mas de lo que parece:
 * un segmento sobre un `byte[]` tiene alineamiento maximo **1**, asi que
 * `segmento.get(JAVA_INT, 0)` sobre un arreglo de bytes **falla**, y hay que usar la version sin
 * alinear. No es un capricho de la biblioteca: es lo que hace el JDK, y el motivo es que la JVM no
 * garantiza donde cae un `byte[]` en memoria.
 *
 * <p>`JAVA_BYTE` y `JAVA_BOOLEAN` no tienen gemelo sin alinear porque ocupan un byte: ya estan
 * alineados a 1.
 *
 * <h2>Lo que falta: `varHandle()`</h2>
 *
 * <p>Queda afuera `varHandle()`, el atajo de `JAVA_INT.varHandle()` para el `VarHandle` de
 * coordenadas `(MemorySegment, long)` que en el JDK lee y escribe un `int` a un offset. Es el mismo
 * bloqueo que deja afuera los cinco de {@link MemoryLayout}, y esta explicado en detalle en el
 * encabezado de esa interfaz: nuestra VM no intercepta `VarHandle`, un `native` sin implementacion
 * voltea el proceso en vez de tirar, y nuestro javac no compila un sitio de llamada polimorfico en
 * la firma. Devolver algo aca seria devolver un objeto que mata la VM al primer acceso.
 *
 * <p>El reemplazo directo es {@link MemorySegment#get(ValueLayout.OfInt, long)} y su familia, que
 * hacen exactamente la misma lectura tomando el layout como argumento en vez de horneado en un
 * handle.
 */
public interface ValueLayout extends MemoryLayout {

    /**
     * El atajo de `JAVA_INT.varHandle()`: el {@link java.lang.invoke.VarHandle} de **este** valor,
     * sin camino.
     *
     * <p>Sus coordenadas son el segmento y el desplazamiento, y nada mas -- no hay pasos que abrir.
     */
    default java.lang.invoke.VarHandle varHandle() {
        return java.lang.invoke.VarHandles.deSegmento(this, 0L, new long[0]);
    }

    /** El tipo Java que este layout transporta: `int.class`, `long.class`... */
    Class<?> carrier();

    /** El orden de bytes con el que se lee y escribe. */
    ByteOrder order();

    /** El mismo layout con otro orden de bytes. */
    ValueLayout withOrder(ByteOrder order);

    ValueLayout withName(String name);

    ValueLayout withoutName();

    ValueLayout withByteAlignment(long byteAlignment);

    // ---- los ocho subtipos ------------------------------------------------------------------------
    //
    // Cada uno estrecha los tres `with*` a si mismo. Eso no es adorno: es lo que permite escribir
    // `JAVA_INT.withName("x").withOrder(BIG_ENDIAN)` sin castear, y --mas importante-- lo que hace
    // que `get(JAVA_INT.withName("x"), 0)` siga eligiendo la sobrecarga que devuelve `int`.

    /** El layout de un `boolean`. */
    interface OfBoolean extends ValueLayout {
        OfBoolean withName(String name);

        OfBoolean withoutName();

        OfBoolean withByteAlignment(long byteAlignment);

        OfBoolean withOrder(ByteOrder order);
    }

    /** El layout de un `byte`. */
    interface OfByte extends ValueLayout {
        OfByte withName(String name);

        OfByte withoutName();

        OfByte withByteAlignment(long byteAlignment);

        OfByte withOrder(ByteOrder order);
    }

    /** El layout de un `char`. */
    interface OfChar extends ValueLayout {
        OfChar withName(String name);

        OfChar withoutName();

        OfChar withByteAlignment(long byteAlignment);

        OfChar withOrder(ByteOrder order);
    }

    /** El layout de un `short`. */
    interface OfShort extends ValueLayout {
        OfShort withName(String name);

        OfShort withoutName();

        OfShort withByteAlignment(long byteAlignment);

        OfShort withOrder(ByteOrder order);
    }

    /** El layout de un `int`. */
    interface OfInt extends ValueLayout {
        OfInt withName(String name);

        OfInt withoutName();

        OfInt withByteAlignment(long byteAlignment);

        OfInt withOrder(ByteOrder order);
    }

    /** El layout de un `long`. */
    interface OfLong extends ValueLayout {
        OfLong withName(String name);

        OfLong withoutName();

        OfLong withByteAlignment(long byteAlignment);

        OfLong withOrder(ByteOrder order);
    }

    /** El layout de un `float`. */
    interface OfFloat extends ValueLayout {
        OfFloat withName(String name);

        OfFloat withoutName();

        OfFloat withByteAlignment(long byteAlignment);

        OfFloat withOrder(ByteOrder order);
    }

    /** El layout de un `double`. */
    interface OfDouble extends ValueLayout {
        OfDouble withName(String name);

        OfDouble withoutName();

        OfDouble withByteAlignment(long byteAlignment);

        OfDouble withOrder(ByteOrder order);
    }

    // ---- las constantes ---------------------------------------------------------------------------

    /** `boolean`, un byte. */
    ValueLayout.OfBoolean JAVA_BOOLEAN = Layouts.booleano();

    /** `byte`, un byte. */
    ValueLayout.OfByte JAVA_BYTE = Layouts.deByte();

    /** `char`, dos bytes, alineado a 2. */
    ValueLayout.OfChar JAVA_CHAR = Layouts.deChar(2L);

    /** `char` sin restriccion de alineamiento. */
    ValueLayout.OfChar JAVA_CHAR_UNALIGNED = Layouts.deChar(1L);

    /** `short`, dos bytes, alineado a 2. */
    ValueLayout.OfShort JAVA_SHORT = Layouts.deShort(2L);

    /** `short` sin restriccion de alineamiento. */
    ValueLayout.OfShort JAVA_SHORT_UNALIGNED = Layouts.deShort(1L);

    /** `int`, cuatro bytes, alineado a 4. */
    ValueLayout.OfInt JAVA_INT = Layouts.deInt(4L);

    /** `int` sin restriccion de alineamiento -- el que hace falta sobre un `byte[]`. */
    ValueLayout.OfInt JAVA_INT_UNALIGNED = Layouts.deInt(1L);

    /** `long`, ocho bytes, alineado a 8. */
    ValueLayout.OfLong JAVA_LONG = Layouts.deLong(8L);

    /** `long` sin restriccion de alineamiento. */
    ValueLayout.OfLong JAVA_LONG_UNALIGNED = Layouts.deLong(1L);

    /** `float`, cuatro bytes, alineado a 4. */
    ValueLayout.OfFloat JAVA_FLOAT = Layouts.deFloat(4L);

    /** `float` sin restriccion de alineamiento. */
    ValueLayout.OfFloat JAVA_FLOAT_UNALIGNED = Layouts.deFloat(1L);

    /** `double`, ocho bytes, alineado a 8. */
    ValueLayout.OfDouble JAVA_DOUBLE = Layouts.deDouble(8L);

    /** `double` sin restriccion de alineamiento. */
    ValueLayout.OfDouble JAVA_DOUBLE_UNALIGNED = Layouts.deDouble(1L);

    /**
     * Una direccion, ocho bytes.
     *
     * <p>Ocho y no cuatro porque este es un modelo de 64 bits. El JDK la ajusta a la plataforma; aca
     * es fija, y esa es una diferencia que se nota si alguien describe una estructura nativa de 32
     * bits.
     */
    AddressLayout ADDRESS = Layouts.direccion(8L);

    /** Una direccion sin restriccion de alineamiento. */
    AddressLayout ADDRESS_UNALIGNED = Layouts.direccion(1L);
}
