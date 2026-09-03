package java.lang.foreign;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * KajiLibrary's java.lang.foreign.SegmentAllocator -- algo que sabe entregar memoria.
 *
 * <p>Una sola operacion abstracta --{@link #allocate(long, long)}-- y veintitres formas comodas
 * encima. Esa proporcion es el diseno: quien escriba un asignador propio implementa **uno** de los
 * veinticuatro y hereda el resto andando.
 *
 * <p>Los `allocateFrom` son los que mas se usan y vale entender que hacen: reservan y **escriben** de
 * una. `allocateFrom(JAVA_INT, 1, 2, 3)` da un segmento de doce bytes con esos tres enteros adentro,
 * que es el patron de "armar un arreglo para pasarselo a una funcion".
 */
public interface SegmentAllocator {

    /**
     * Reserva `byteSize` bytes con ese alineamiento.
     *
     * @throws IllegalArgumentException si el tamanio es negativo o el alineamiento no es una potencia
     *     de dos positiva
     */
    MemorySegment allocate(long byteSize, long byteAlignment);

    /** Reserva `byteSize` bytes, sin exigir alineamiento. */
    default MemorySegment allocate(long byteSize) {
        return this.allocate(byteSize, 1L);
    }

    /** Reserva lo que ese layout ocupa, con su alineamiento. */
    default MemorySegment allocate(MemoryLayout layout) {
        if (layout == null) {
            throw new NullPointerException("layout");
        }
        return this.allocate(layout.byteSize(), layout.byteAlignment());
    }

    /** Reserva `count` copias de ese layout. */
    default MemorySegment allocate(MemoryLayout elementLayout, long count) {
        if (elementLayout == null) {
            throw new NullPointerException("elementLayout");
        }
        if (count < 0L) {
            throw new IllegalArgumentException("cantidad negativa: " + count);
        }
        return this.allocate(elementLayout.byteSize() * count, elementLayout.byteAlignment());
    }

    /** Reserva y escribe esa cadena, en UTF-8 y con el cero final. */
    default MemorySegment allocateFrom(String str) {
        return this.allocateFrom(str, StandardCharsets.UTF_8);
    }

    /**
     * Lo mismo con otro charset.
     *
     * <p>El byte del cero final entra en la cuenta, y por eso el segmento es un byte mas largo que la
     * codificacion: sin el, quien lea la cadena del otro lado seguiria leyendo hasta el proximo cero
     * que hubiera por ahi.
     */
    default MemorySegment allocateFrom(String str, Charset charset) {
        if (str == null) {
            throw new NullPointerException("str");
        }
        byte[] crudo = str.getBytes(charset);
        MemorySegment s = this.allocate((long) crudo.length + 1L, 1L);
        s.setString(0L, str, charset);
        return s;
    }

    /** Reserva y escribe ese valor. */
    default MemorySegment allocateFrom(ValueLayout.OfByte layout, byte value) {
        MemorySegment s = this.allocate(layout);
        s.set(layout, 0L, value);
        return s;
    }

    /** Reserva y escribe ese valor. */
    default MemorySegment allocateFrom(ValueLayout.OfChar layout, char value) {
        MemorySegment s = this.allocate(layout);
        s.set(layout, 0L, value);
        return s;
    }

    /** Reserva y escribe ese valor. */
    default MemorySegment allocateFrom(ValueLayout.OfShort layout, short value) {
        MemorySegment s = this.allocate(layout);
        s.set(layout, 0L, value);
        return s;
    }

    /** Reserva y escribe ese valor. */
    default MemorySegment allocateFrom(ValueLayout.OfInt layout, int value) {
        MemorySegment s = this.allocate(layout);
        s.set(layout, 0L, value);
        return s;
    }

    /** Reserva y escribe ese valor. */
    default MemorySegment allocateFrom(ValueLayout.OfLong layout, long value) {
        MemorySegment s = this.allocate(layout);
        s.set(layout, 0L, value);
        return s;
    }

    /** Reserva y escribe ese valor. */
    default MemorySegment allocateFrom(ValueLayout.OfFloat layout, float value) {
        MemorySegment s = this.allocate(layout);
        s.set(layout, 0L, value);
        return s;
    }

    /** Reserva y escribe ese valor. */
    default MemorySegment allocateFrom(ValueLayout.OfDouble layout, double value) {
        MemorySegment s = this.allocate(layout);
        s.set(layout, 0L, value);
        return s;
    }

    /** Reserva y escribe la direccion de ese segmento. */
    default MemorySegment allocateFrom(AddressLayout layout, MemorySegment value) {
        MemorySegment s = this.allocate(layout);
        s.set(layout, 0L, value);
        return s;
    }

    /** Reserva y escribe esos valores. */
    default MemorySegment allocateFrom(ValueLayout.OfByte elementLayout, byte... elements) {
        MemorySegment s = this.allocate(elementLayout, (long) elements.length);
        int i = 0;
        while (i < elements.length) {
            s.setAtIndex(elementLayout, (long) i, elements[i]);
            i = i + 1;
        }
        return s;
    }

    /** Reserva y escribe esos valores. */
    default MemorySegment allocateFrom(ValueLayout.OfChar elementLayout, char... elements) {
        MemorySegment s = this.allocate(elementLayout, (long) elements.length);
        int i = 0;
        while (i < elements.length) {
            s.setAtIndex(elementLayout, (long) i, elements[i]);
            i = i + 1;
        }
        return s;
    }

    /** Reserva y escribe esos valores. */
    default MemorySegment allocateFrom(ValueLayout.OfShort elementLayout, short... elements) {
        MemorySegment s = this.allocate(elementLayout, (long) elements.length);
        int i = 0;
        while (i < elements.length) {
            s.setAtIndex(elementLayout, (long) i, elements[i]);
            i = i + 1;
        }
        return s;
    }

    /** Reserva y escribe esos valores. */
    default MemorySegment allocateFrom(ValueLayout.OfInt elementLayout, int... elements) {
        MemorySegment s = this.allocate(elementLayout, (long) elements.length);
        int i = 0;
        while (i < elements.length) {
            s.setAtIndex(elementLayout, (long) i, elements[i]);
            i = i + 1;
        }
        return s;
    }

    /** Reserva y escribe esos valores. */
    default MemorySegment allocateFrom(ValueLayout.OfLong elementLayout, long... elements) {
        MemorySegment s = this.allocate(elementLayout, (long) elements.length);
        int i = 0;
        while (i < elements.length) {
            s.setAtIndex(elementLayout, (long) i, elements[i]);
            i = i + 1;
        }
        return s;
    }

    /** Reserva y escribe esos valores. */
    default MemorySegment allocateFrom(ValueLayout.OfFloat elementLayout, float... elements) {
        MemorySegment s = this.allocate(elementLayout, (long) elements.length);
        int i = 0;
        while (i < elements.length) {
            s.setAtIndex(elementLayout, (long) i, elements[i]);
            i = i + 1;
        }
        return s;
    }

    /** Reserva y escribe esos valores. */
    default MemorySegment allocateFrom(ValueLayout.OfDouble elementLayout, double... elements) {
        MemorySegment s = this.allocate(elementLayout, (long) elements.length);
        int i = 0;
        while (i < elements.length) {
            s.setAtIndex(elementLayout, (long) i, elements[i]);
            i = i + 1;
        }
        return s;
    }

    /** Reserva y copia `elementCount` elementos desde otro segmento. */
    default MemorySegment allocateFrom(ValueLayout elementLayout, MemorySegment source,
            ValueLayout sourceElementLayout, long sourceOffset, long elementCount) {
        MemorySegment s = this.allocate(elementLayout, elementCount);
        MemorySegment.copy(source, sourceElementLayout, sourceOffset, s, elementLayout, 0L,
                elementCount);
        return s;
    }

    /**
     * Un asignador que va **cortando** ese segmento, de adelante para atras.
     *
     * <p>Sirve para repartir un bloque reservado una sola vez entre varias reservas chicas. Se acaba
     * cuando se acaba el segmento, y ahi tira: no crece.
     */
    static SegmentAllocator slicingAllocator(MemorySegment segment) {
        if (segment == null) {
            throw new NullPointerException("segment");
        }
        return new AsignadorCortante(segment);
    }

    /**
     * Un asignador que devuelve **siempre el mismo prefijo** de ese segmento.
     *
     * <p>Es para reusar un unico buffer en un bucle sin reservar cada vez. Y por eso mismo hay que
     * tener cuidado: dos reservas seguidas devuelven la misma memoria, asi que la segunda pisa a la
     * primera. El JDK lo documenta igual; no es un descuido sino el punto.
     */
    static SegmentAllocator prefixAllocator(MemorySegment segment) {
        if (segment == null) {
            throw new NullPointerException("segment");
        }
        return new AsignadorPrefijo(segment);
    }
}

// Va cortando el segmento de adelante para atras, respetando el alineamiento que se le pida.
final class AsignadorCortante implements SegmentAllocator {

    private final MemorySegment bloque;
    private long usado;

    AsignadorCortante(MemorySegment bloque) {
        this.bloque = bloque;
        this.usado = 0L;
    }

    public MemorySegment allocate(long byteSize, long byteAlignment) {
        if (byteSize < 0L) {
            throw new IllegalArgumentException("tamanio negativo: " + byteSize);
        }
        Layouts.exigirAlineamiento(byteAlignment);
        long arranque = this.usado;
        long resto = arranque % byteAlignment;
        if (resto != 0L) {
            arranque = arranque + (byteAlignment - resto);
        }
        if (arranque + byteSize > this.bloque.byteSize()) {
            throw new IndexOutOfBoundsException(
                    "no queda lugar: se pidieron " + byteSize + " bytes y quedan "
                            + (this.bloque.byteSize() - arranque));
        }
        MemorySegment s = this.bloque.asSlice(arranque, byteSize);
        this.usado = arranque + byteSize;
        return s;
    }
}

// Devuelve siempre el mismo prefijo. Cada reserva pisa a la anterior, y eso es lo que se le pide.
final class AsignadorPrefijo implements SegmentAllocator {

    private final MemorySegment bloque;

    AsignadorPrefijo(MemorySegment bloque) {
        this.bloque = bloque;
    }

    public MemorySegment allocate(long byteSize, long byteAlignment) {
        if (byteSize < 0L || byteSize > this.bloque.byteSize()) {
            throw new IndexOutOfBoundsException(
                    "el prefijo pedido no entra: " + byteSize + " de " + this.bloque.byteSize());
        }
        Layouts.exigirAlineamiento(byteAlignment);
        return this.bloque.asSlice(0L, byteSize);
    }
}
