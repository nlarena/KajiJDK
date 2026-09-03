package java.lang.foreign;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.stream.Stream;

// La implementacion de `MemorySegment` sobre un arreglo de Java. De paquete: se llega por
// `MemorySegment.ofArray` o por un `Arena`.
//
// **Todo el acceso pasa por dos metodos**, `leer(i)` y `escribir(i, v)`, que trabajan byte a byte
// sobre el arreglo de respaldo sea del tipo que sea. Lo demas --los 36 get/set-- se arma encima
// componiendo o descomponiendo bytes segun el orden del layout.
//
// Esa forma es a proposito y es lo que hace la clase revisable: la unica parte con filo --como se
// mapea el byte numero i a un elemento de un `int[]`-- esta en un solo lugar, y si esta bien esta
// bien para los ocho tipos. La alternativa, un camino por tipo de respaldo por tipo de valor, son
// cincuenta y seis caminos que hay que creer de a uno.
//
// El precio es velocidad, y aca no importa: esta biblioteca existe para ser correcta y legible.
final class SegmentoHeap implements MemorySegment {

    // El arreglo de respaldo, o `null` para los que no tienen (NULL y los de `ofAddress`).
    private final Object base;
    // Bytes por elemento del arreglo: 1, 2, 4 u 8. Es lo que traduce un offset en bytes a una
    // posicion del arreglo.
    private final int tamElem;
    // Donde empieza este segmento **dentro del respaldo**, en bytes.
    private final long inicio;
    private final long largo;
    private final boolean soloLectura;
    private final Ambito ambito;

    SegmentoHeap(Object base, int tamElem, long inicio, long largo, boolean soloLectura,
            Ambito ambito) {
        this.base = base;
        this.tamElem = tamElem;
        this.inicio = inicio;
        this.largo = largo;
        this.soloLectura = soloLectura;
        this.ambito = ambito;
    }

    // ---- fabricas -----------------------------------------------------------------------------

    static MemorySegment nulo() {
        return new SegmentoHeap(null, 1, 0L, 0L, true, Ambito.GLOBAL);
    }

    static MemorySegment enDireccion(long direccion) {
        // Largo cero, como en el JDK: una direccion suelta no dice cuanto se puede leer desde ahi, y
        // suponerlo seria inventar. Se agranda con `reinterpret`, que aca no se puede porque no hay
        // memoria detras -- ver su javadoc.
        return new SegmentoHeap(null, 1, direccion, 0L, false, Ambito.GLOBAL);
    }

    static MemorySegment deArreglo(Object arr, int tamElem) {
        if (arr == null) {
            throw new NullPointerException("arr");
        }
        long n = SegmentoHeap.largoDe(arr) * (long) tamElem;
        return new SegmentoHeap(arr, tamElem, 0L, n, false, Ambito.GLOBAL);
    }

    static MemorySegment deBuffer(java.nio.Buffer buffer) {
        if (buffer == null) {
            throw new NullPointerException("buffer");
        }
        if (buffer instanceof ByteBuffer) {
            ByteBuffer bb = (ByteBuffer) buffer;
            if (bb.hasArray()) {
                byte[] arr = bb.array();
                SegmentoHeap s = new SegmentoHeap(arr, 1, (long) (bb.arrayOffset() + bb.position()),
                        (long) bb.remaining(), bb.isReadOnly(), Ambito.GLOBAL);
                return s;
            }
        }
        // Un buffer sin arreglo accesible es uno directo, y esos viven fuera del heap. Se dice de
        // frente en vez de copiar: copiar daria un segmento que **no** es una vista del buffer, y
        // toda la utilidad de esto es que sea una vista.
        throw new UnsupportedOperationException(
                "solo se puede hacer un segmento sobre un buffer con arreglo accesible");
    }

    // ---- lo elemental -------------------------------------------------------------------------

    public long byteSize() {
        return this.largo;
    }

    public long address() {
        return this.inicio;
    }

    public Optional<Object> heapBase() {
        return Optional.ofNullable(this.base);
    }

    public boolean isNative() {
        return this.base == null;
    }

    public boolean isMapped() {
        return false;
    }

    public boolean isReadOnly() {
        return this.soloLectura;
    }

    public long maxByteAlignment() {
        // El del elemento del arreglo. Un `byte[]` da 1 y por eso `JAVA_INT` no entra: la JVM no
        // promete donde cae un arreglo de bytes.
        return (long) this.tamElem;
    }

    public MemorySegment.Scope scope() {
        return this.ambito;
    }

    public boolean isAccessibleBy(Thread thread) {
        return true;
    }

    public MemorySegment asReadOnly() {
        return new SegmentoHeap(this.base, this.tamElem, this.inicio, this.largo, true,
                this.ambito);
    }

    // ---- cortes -------------------------------------------------------------------------------

    public MemorySegment asSlice(long offset) {
        return this.asSlice(offset, this.largo - offset);
    }

    public MemorySegment asSlice(long offset, long newSize) {
        this.exigirRango(offset, newSize);
        return new SegmentoHeap(this.base, this.tamElem, this.inicio + offset, newSize,
                this.soloLectura, this.ambito);
    }

    public MemorySegment asSlice(long offset, long newSize, long byteAlignment) {
        Layouts.exigirAlineamiento(byteAlignment);
        if ((this.inicio + offset) % byteAlignment != 0L) {
            throw new IllegalArgumentException(
                    "el corte en " + offset + " no respeta el alineamiento " + byteAlignment);
        }
        return this.asSlice(offset, newSize);
    }

    public MemorySegment asSlice(long offset, MemoryLayout layout) {
        return this.asSlice(offset, layout.byteSize(), layout.byteAlignment());
    }

    public Optional<MemorySegment> asOverlappingSlice(MemorySegment other) {
        if (!(other instanceof SegmentoHeap)) {
            return Optional.empty();
        }
        SegmentoHeap o = (SegmentoHeap) other;
        // Dos arreglos distintos **nunca** se superponen, aunque sus offsets coincidan. Comparar por
        // identidad y no por contenido es lo unico correcto aca.
        if (this.base == null || this.base != o.base) {
            return Optional.empty();
        }
        long desde = Math.max(this.inicio, o.inicio);
        long hasta = Math.min(this.inicio + this.largo, o.inicio + o.largo);
        if (desde >= hasta) {
            return Optional.empty();
        }
        return Optional.of(new SegmentoHeap(this.base, this.tamElem, desde, hasta - desde,
                this.soloLectura, this.ambito));
    }

    public MemorySegment reinterpret(long newSize) {
        if (this.base == null) {
            throw new UnsupportedOperationException(
                    "no se puede agrandar un segmento sin memoria detras: cubriria bytes que nadie"
                            + " puede leer");
        }
        long disponible = SegmentoHeap.largoDe(this.base) * (long) this.tamElem;
        if (newSize < 0L || this.inicio + newSize > disponible) {
            throw new IllegalArgumentException(
                    "el tamanio " + newSize + " se sale del arreglo de respaldo");
        }
        return new SegmentoHeap(this.base, this.tamElem, this.inicio, newSize, this.soloLectura,
                this.ambito);
    }

    public MemorySegment reinterpret(Arena arena, java.util.function.Consumer<MemorySegment> cleanup) {
        return this.reinterpret(this.largo);
    }

    public MemorySegment reinterpret(long newSize, Arena arena,
            java.util.function.Consumer<MemorySegment> cleanup) {
        return this.reinterpret(newSize);
    }

    // ---- el acceso byte a byte, que es el corazon de la clase ---------------------------------
    //
    // Un `int[]` en memoria es una tira de bytes: el byte j del elemento k es el byte (k*4 + j) de la
    // tira. Leerlo es sacar ese byte del `int`, y en una maquina little-endian el byte 0 es el mas
    // bajo -- que es lo que hace el corrimiento de abajo.
    //
    // La VM de KajiJDK y las plataformas donde corre son little-endian, y esa suposicion esta aca y
    // en ningun otro lado: si algun dia hay una big-endian, este es el unico metodo que cambia.

    private byte leer(long i) {
        long abs = this.inicio + i;
        if (this.base instanceof byte[]) {
            return ((byte[]) this.base)[(int) abs];
        }
        int idx = (int) (abs / (long) this.tamElem);
        int desp = (int) (abs % (long) this.tamElem) * 8;
        long palabra = this.palabra(idx);
        return (byte) ((palabra >>> desp) & 0xFFL);
    }

    private void escribir(long i, byte v) {
        long abs = this.inicio + i;
        if (this.base instanceof byte[]) {
            ((byte[]) this.base)[(int) abs] = v;
            return;
        }
        int idx = (int) (abs / (long) this.tamElem);
        int desp = (int) (abs % (long) this.tamElem) * 8;
        long palabra = this.palabra(idx);
        long mascara = ~(0xFFL << desp);
        long nueva = (palabra & mascara) | (((long) v & 0xFFL) << desp);
        this.guardarPalabra(idx, nueva);
    }

    // Cuantos elementos tiene el arreglo de respaldo.
    //
    // Por switch de tipo y no por `java.lang.reflect.Array.getLength`: ese metodo esta declarado pero
    // es `native` y la VM no lo implementa -- corta con "no native implementation". Y de todos modos
    // el switch es lo que el resto de la clase ya hace, asi que salir a la reflexion era la rareza.
    static long largoDe(Object arr) {
        if (arr instanceof byte[]) {
            return (long) ((byte[]) arr).length;
        }
        if (arr instanceof char[]) {
            return (long) ((char[]) arr).length;
        }
        if (arr instanceof short[]) {
            return (long) ((short[]) arr).length;
        }
        if (arr instanceof int[]) {
            return (long) ((int[]) arr).length;
        }
        if (arr instanceof long[]) {
            return (long) ((long[]) arr).length;
        }
        if (arr instanceof float[]) {
            return (long) ((float[]) arr).length;
        }
        if (arr instanceof double[]) {
            return (long) ((double[]) arr).length;
        }
        throw new IllegalArgumentException("no es un arreglo de primitivos soportado");
    }

    // El elemento `idx` del respaldo, visto como los bits que ocupa.
    private long palabra(int idx) {
        if (this.base instanceof char[]) {
            return (long) ((char[]) this.base)[idx];
        }
        if (this.base instanceof short[]) {
            return (long) ((short[]) this.base)[idx] & 0xFFFFL;
        }
        if (this.base instanceof int[]) {
            return (long) ((int[]) this.base)[idx] & 0xFFFFFFFFL;
        }
        if (this.base instanceof long[]) {
            return ((long[]) this.base)[idx];
        }
        if (this.base instanceof float[]) {
            return (long) Float.floatToRawIntBits(((float[]) this.base)[idx]) & 0xFFFFFFFFL;
        }
        if (this.base instanceof double[]) {
            return Double.doubleToRawLongBits(((double[]) this.base)[idx]);
        }
        throw new UnsupportedOperationException("respaldo no soportado");
    }

    private void guardarPalabra(int idx, long v) {
        if (this.base instanceof char[]) {
            ((char[]) this.base)[idx] = (char) v;
            return;
        }
        if (this.base instanceof short[]) {
            ((short[]) this.base)[idx] = (short) v;
            return;
        }
        if (this.base instanceof int[]) {
            ((int[]) this.base)[idx] = (int) v;
            return;
        }
        if (this.base instanceof long[]) {
            ((long[]) this.base)[idx] = v;
            return;
        }
        if (this.base instanceof float[]) {
            ((float[]) this.base)[idx] = Float.intBitsToFloat((int) v);
            return;
        }
        if (this.base instanceof double[]) {
            ((double[]) this.base)[idx] = Double.longBitsToDouble(v);
            return;
        }
        throw new UnsupportedOperationException("respaldo no soportado");
    }

    // ---- componer y descomponer valores de varios bytes ---------------------------------------

    private long leerN(long offset, int n, boolean bigEndian) {
        long v = 0L;
        int i = 0;
        while (i < n) {
            int pos = bigEndian ? i : n - 1 - i;
            v = (v << 8) | ((long) this.leer(offset + (long) pos) & 0xFFL);
            i = i + 1;
        }
        return v;
    }

    private void escribirN(long offset, int n, boolean bigEndian, long v) {
        int i = 0;
        while (i < n) {
            int pos = bigEndian ? n - 1 - i : i;
            this.escribir(offset + (long) pos, (byte) ((v >>> (8 * i)) & 0xFFL));
            i = i + 1;
        }
    }

    // ---- comprobaciones -----------------------------------------------------------------------
    //
    // Las tres se hacen **antes** de tocar nada, y en este orden: primero si el segmento sigue vivo,
    // despues si el rango entra, y al final el alineamiento. El orden importa para el mensaje: sobre
    // un segmento cerrado, "fuera de rango" mandaria a buscar al lugar equivocado.

    private void exigirVivo() {
        if (!this.ambito.isAlive()) {
            throw new IllegalStateException("el ambito de este segmento ya se cerro");
        }
    }

    private void exigirRango(long offset, long n) {
        if (offset < 0L || n < 0L || offset + n > this.largo) {
            throw new IndexOutOfBoundsException(
                    "el rango [" + offset + ", " + (offset + n) + ") se sale de un segmento de "
                            + this.largo + " bytes");
        }
    }

    private void exigirLectura(ValueLayout layout, long offset) {
        this.exigirVivo();
        this.exigirRango(offset, layout.byteSize());
        long a = layout.byteAlignment();
        if (a > (long) this.tamElem) {
            throw new IllegalArgumentException(
                    "el layout " + layout + " exige alineamiento " + a + " y este segmento solo"
                            + " garantiza " + this.tamElem + "; use la variante UNALIGNED");
        }
        if ((this.inicio + offset) % a != 0L) {
            throw new IllegalArgumentException(
                    "el offset " + offset + " no respeta el alineamiento " + a + " de " + layout);
        }
    }

    private void exigirEscritura(ValueLayout layout, long offset) {
        if (this.soloLectura) {
            // `IllegalArgumentException` y no `UnsupportedOperationException`: es lo que tira el JDK,
            // y cambiarlo obligaria a atrapar dos cosas distintas segun la VM.
            throw new IllegalArgumentException("el segmento es de solo lectura");
        }
        this.exigirLectura(layout, offset);
    }

    private static boolean esBig(ValueLayout layout) {
        return layout.order() == ByteOrder.BIG_ENDIAN;
    }

    // ---- los 36 get/set, generados ------------------------------------------------------------
    //
    // Se generan porque son casi identicos y escribirlos a mano es donde se cuela un `Short`
    // donde iba un `Char`. Los cuatro pasos son siempre los mismos: comprobar, leer los bytes,
    // componer el valor, convertirlo al tipo Java.

    public boolean get(ValueLayout.OfBoolean layout, long offset) {
        this.exigirLectura(layout, offset);
        long crudo = this.leerN(offset, 1, SegmentoHeap.esBig(layout));
        return crudo != 0L;
    }

    public void set(ValueLayout.OfBoolean layout, long offset, boolean value) {
        this.exigirEscritura(layout, offset);
        this.escribirN(offset, 1, SegmentoHeap.esBig(layout), value ? 1L : 0L);
    }

    public boolean getAtIndex(ValueLayout.OfBoolean layout, long index) {
        return this.get(layout, layout.scale(0L, index));
    }

    public void setAtIndex(ValueLayout.OfBoolean layout, long index, boolean value) {
        this.set(layout, layout.scale(0L, index), value);
    }

    public byte get(ValueLayout.OfByte layout, long offset) {
        this.exigirLectura(layout, offset);
        long crudo = this.leerN(offset, 1, SegmentoHeap.esBig(layout));
        return (byte) crudo;
    }

    public void set(ValueLayout.OfByte layout, long offset, byte value) {
        this.exigirEscritura(layout, offset);
        this.escribirN(offset, 1, SegmentoHeap.esBig(layout), (long) value);
    }

    public byte getAtIndex(ValueLayout.OfByte layout, long index) {
        return this.get(layout, layout.scale(0L, index));
    }

    public void setAtIndex(ValueLayout.OfByte layout, long index, byte value) {
        this.set(layout, layout.scale(0L, index), value);
    }

    public char get(ValueLayout.OfChar layout, long offset) {
        this.exigirLectura(layout, offset);
        long crudo = this.leerN(offset, 2, SegmentoHeap.esBig(layout));
        return (char) crudo;
    }

    public void set(ValueLayout.OfChar layout, long offset, char value) {
        this.exigirEscritura(layout, offset);
        this.escribirN(offset, 2, SegmentoHeap.esBig(layout), (long) value);
    }

    public char getAtIndex(ValueLayout.OfChar layout, long index) {
        return this.get(layout, layout.scale(0L, index));
    }

    public void setAtIndex(ValueLayout.OfChar layout, long index, char value) {
        this.set(layout, layout.scale(0L, index), value);
    }

    public short get(ValueLayout.OfShort layout, long offset) {
        this.exigirLectura(layout, offset);
        long crudo = this.leerN(offset, 2, SegmentoHeap.esBig(layout));
        return (short) crudo;
    }

    public void set(ValueLayout.OfShort layout, long offset, short value) {
        this.exigirEscritura(layout, offset);
        this.escribirN(offset, 2, SegmentoHeap.esBig(layout), (long) value);
    }

    public short getAtIndex(ValueLayout.OfShort layout, long index) {
        return this.get(layout, layout.scale(0L, index));
    }

    public void setAtIndex(ValueLayout.OfShort layout, long index, short value) {
        this.set(layout, layout.scale(0L, index), value);
    }

    public int get(ValueLayout.OfInt layout, long offset) {
        this.exigirLectura(layout, offset);
        long crudo = this.leerN(offset, 4, SegmentoHeap.esBig(layout));
        return (int) crudo;
    }

    public void set(ValueLayout.OfInt layout, long offset, int value) {
        this.exigirEscritura(layout, offset);
        this.escribirN(offset, 4, SegmentoHeap.esBig(layout), (long) value);
    }

    public int getAtIndex(ValueLayout.OfInt layout, long index) {
        return this.get(layout, layout.scale(0L, index));
    }

    public void setAtIndex(ValueLayout.OfInt layout, long index, int value) {
        this.set(layout, layout.scale(0L, index), value);
    }

    public long get(ValueLayout.OfLong layout, long offset) {
        this.exigirLectura(layout, offset);
        long crudo = this.leerN(offset, 8, SegmentoHeap.esBig(layout));
        return crudo;
    }

    public void set(ValueLayout.OfLong layout, long offset, long value) {
        this.exigirEscritura(layout, offset);
        this.escribirN(offset, 8, SegmentoHeap.esBig(layout), value);
    }

    public long getAtIndex(ValueLayout.OfLong layout, long index) {
        return this.get(layout, layout.scale(0L, index));
    }

    public void setAtIndex(ValueLayout.OfLong layout, long index, long value) {
        this.set(layout, layout.scale(0L, index), value);
    }

    public float get(ValueLayout.OfFloat layout, long offset) {
        this.exigirLectura(layout, offset);
        long crudo = this.leerN(offset, 4, SegmentoHeap.esBig(layout));
        return Float.intBitsToFloat((int) crudo);
    }

    public void set(ValueLayout.OfFloat layout, long offset, float value) {
        this.exigirEscritura(layout, offset);
        this.escribirN(offset, 4, SegmentoHeap.esBig(layout), (long) Float.floatToRawIntBits(value));
    }

    public float getAtIndex(ValueLayout.OfFloat layout, long index) {
        return this.get(layout, layout.scale(0L, index));
    }

    public void setAtIndex(ValueLayout.OfFloat layout, long index, float value) {
        this.set(layout, layout.scale(0L, index), value);
    }

    public double get(ValueLayout.OfDouble layout, long offset) {
        this.exigirLectura(layout, offset);
        long crudo = this.leerN(offset, 8, SegmentoHeap.esBig(layout));
        return Double.longBitsToDouble(crudo);
    }

    public void set(ValueLayout.OfDouble layout, long offset, double value) {
        this.exigirEscritura(layout, offset);
        this.escribirN(offset, 8, SegmentoHeap.esBig(layout), Double.doubleToRawLongBits(value));
    }

    public double getAtIndex(ValueLayout.OfDouble layout, long index) {
        return this.get(layout, layout.scale(0L, index));
    }

    public void setAtIndex(ValueLayout.OfDouble layout, long index, double value) {
        this.set(layout, layout.scale(0L, index), value);
    }

    // La direccion se lee como un `long` y se devuelve como un segmento de largo cero: eso es lo que
    // una direccion suelta **es**, y agrandarla es decision de quien la lee.
    public MemorySegment get(AddressLayout layout, long offset) {
        this.exigirLectura(layout, offset);
        long crudo = this.leerN(offset, 8, SegmentoHeap.esBig(layout));
        return MemorySegment.ofAddress(crudo);
    }

    public void set(AddressLayout layout, long offset, MemorySegment value) {
        this.exigirEscritura(layout, offset);
        this.escribirN(offset, 8, SegmentoHeap.esBig(layout),
                value == null ? 0L : value.address());
    }

    public MemorySegment getAtIndex(AddressLayout layout, long index) {
        return this.get(layout, layout.scale(0L, index));
    }

    public void setAtIndex(AddressLayout layout, long index, MemorySegment value) {
        this.set(layout, layout.scale(0L, index), value);
    }

    public byte[] toArray(ValueLayout.OfByte elementLayout) {
        this.exigirVivo();
        if (this.largo % elementLayout.byteSize() != 0L) {
            throw new IllegalStateException(
                    "el segmento de " + this.largo + " bytes no es multiplo de " + elementLayout);
        }
        int n = (int) (this.largo / elementLayout.byteSize());
        byte[] out = new byte[n];
        int i = 0;
        while (i < n) {
            out[i] = this.get(elementLayout, elementLayout.scale(0L, (long) i));
            i = i + 1;
        }
        return out;
    }

    public char[] toArray(ValueLayout.OfChar elementLayout) {
        this.exigirVivo();
        if (this.largo % elementLayout.byteSize() != 0L) {
            throw new IllegalStateException(
                    "el segmento de " + this.largo + " bytes no es multiplo de " + elementLayout);
        }
        int n = (int) (this.largo / elementLayout.byteSize());
        char[] out = new char[n];
        int i = 0;
        while (i < n) {
            out[i] = this.get(elementLayout, elementLayout.scale(0L, (long) i));
            i = i + 1;
        }
        return out;
    }

    public short[] toArray(ValueLayout.OfShort elementLayout) {
        this.exigirVivo();
        if (this.largo % elementLayout.byteSize() != 0L) {
            throw new IllegalStateException(
                    "el segmento de " + this.largo + " bytes no es multiplo de " + elementLayout);
        }
        int n = (int) (this.largo / elementLayout.byteSize());
        short[] out = new short[n];
        int i = 0;
        while (i < n) {
            out[i] = this.get(elementLayout, elementLayout.scale(0L, (long) i));
            i = i + 1;
        }
        return out;
    }

    public int[] toArray(ValueLayout.OfInt elementLayout) {
        this.exigirVivo();
        if (this.largo % elementLayout.byteSize() != 0L) {
            throw new IllegalStateException(
                    "el segmento de " + this.largo + " bytes no es multiplo de " + elementLayout);
        }
        int n = (int) (this.largo / elementLayout.byteSize());
        int[] out = new int[n];
        int i = 0;
        while (i < n) {
            out[i] = this.get(elementLayout, elementLayout.scale(0L, (long) i));
            i = i + 1;
        }
        return out;
    }

    public long[] toArray(ValueLayout.OfLong elementLayout) {
        this.exigirVivo();
        if (this.largo % elementLayout.byteSize() != 0L) {
            throw new IllegalStateException(
                    "el segmento de " + this.largo + " bytes no es multiplo de " + elementLayout);
        }
        int n = (int) (this.largo / elementLayout.byteSize());
        long[] out = new long[n];
        int i = 0;
        while (i < n) {
            out[i] = this.get(elementLayout, elementLayout.scale(0L, (long) i));
            i = i + 1;
        }
        return out;
    }

    public float[] toArray(ValueLayout.OfFloat elementLayout) {
        this.exigirVivo();
        if (this.largo % elementLayout.byteSize() != 0L) {
            throw new IllegalStateException(
                    "el segmento de " + this.largo + " bytes no es multiplo de " + elementLayout);
        }
        int n = (int) (this.largo / elementLayout.byteSize());
        float[] out = new float[n];
        int i = 0;
        while (i < n) {
            out[i] = this.get(elementLayout, elementLayout.scale(0L, (long) i));
            i = i + 1;
        }
        return out;
    }

    public double[] toArray(ValueLayout.OfDouble elementLayout) {
        this.exigirVivo();
        if (this.largo % elementLayout.byteSize() != 0L) {
            throw new IllegalStateException(
                    "el segmento de " + this.largo + " bytes no es multiplo de " + elementLayout);
        }
        int n = (int) (this.largo / elementLayout.byteSize());
        double[] out = new double[n];
        int i = 0;
        while (i < n) {
            out[i] = this.get(elementLayout, elementLayout.scale(0L, (long) i));
            i = i + 1;
        }
        return out;
    }

    // ---- copiar, llenar, comparar --------------------------------------------------------------

    public MemorySegment copyFrom(MemorySegment src) {
        SegmentoHeap.copiar(src, 0L, this, 0L, src.byteSize());
        return this;
    }

    public MemorySegment fill(byte value) {
        this.exigirVivo();
        if (this.soloLectura) {
            throw new IllegalArgumentException("el segmento es de solo lectura");
        }
        long i = 0L;
        while (i < this.largo) {
            this.escribir(i, value);
            i = i + 1L;
        }
        return this;
    }

    public long mismatch(MemorySegment other) {
        return SegmentoHeap.diferenciaEntre(this, 0L, this.largo, other, 0L, other.byteSize());
    }

    static long diferenciaEntre(MemorySegment a, long desdeA, long hastaA, MemorySegment b,
            long desdeB, long hastaB) {
        long nA = hastaA - desdeA;
        long nB = hastaB - desdeB;
        long comun = Math.min(nA, nB);
        long i = 0L;
        while (i < comun) {
            if (SegmentoHeap.byteDe(a, desdeA + i) != SegmentoHeap.byteDe(b, desdeB + i)) {
                return i;
            }
            i = i + 1L;
        }
        // Si uno es prefijo del otro, "difieren" donde el corto se acaba. Y si son del mismo largo y
        // todo coincide, no difieren en ningun lado: -1.
        return nA == nB ? -1L : comun;
    }

    // ---- cadenas ------------------------------------------------------------------------------

    public String getString(long offset) {
        return this.getString(offset, StandardCharsets.UTF_8);
    }

    public String getString(long offset, Charset charset) {
        this.exigirVivo();
        // Se busca el cero **primero** y despues se decodifica: una cadena de C termina donde el cero,
        // no donde el segmento.
        long fin = offset;
        while (fin < this.largo && this.leer(fin) != 0) {
            fin = fin + 1L;
        }
        if (fin >= this.largo) {
            throw new IndexOutOfBoundsException(
                    "no hay terminador cero desde el offset " + offset);
        }
        byte[] crudo = new byte[(int) (fin - offset)];
        int i = 0;
        while (i < crudo.length) {
            crudo[i] = this.leer(offset + (long) i);
            i = i + 1;
        }
        return new String(crudo, charset);
    }

    public void setString(long offset, String str) {
        this.setString(offset, str, StandardCharsets.UTF_8);
    }

    public void setString(long offset, String str, Charset charset) {
        this.exigirVivo();
        if (this.soloLectura) {
            throw new IllegalArgumentException("el segmento es de solo lectura");
        }
        byte[] crudo = str.getBytes(charset);
        // El +1 es el terminador, y no es opcional: sin el, `getString` leeria hasta el proximo cero
        // que haya por ahi.
        this.exigirRango(offset, (long) crudo.length + 1L);
        int i = 0;
        while (i < crudo.length) {
            this.escribir(offset + (long) i, crudo[i]);
            i = i + 1;
        }
        this.escribir(offset + (long) crudo.length, (byte) 0);
    }

    // ---- vistas -------------------------------------------------------------------------------

    public ByteBuffer asByteBuffer() {
        this.exigirVivo();
        // Se copia, y hay que decirlo: en el JDK esto es una **vista** y las escrituras se ven de los
        // dos lados. Aca solo se puede envolver un `byte[]` sin copiar, y este segmento puede estar
        // sobre cualquier arreglo. Copiar y avisar es mejor que envolver a veces si y a veces no.
        byte[] copia = new byte[(int) this.largo];
        long i = 0L;
        while (i < this.largo) {
            copia[(int) i] = this.leer(i);
            i = i + 1L;
        }
        ByteBuffer bb = ByteBuffer.wrap(copia);
        return this.soloLectura ? bb.asReadOnlyBuffer() : bb;
    }

    public Stream<MemorySegment> elements(MemoryLayout elementLayout) {
        List<MemorySegment> partes = new ArrayList<MemorySegment>();
        long tam = elementLayout.byteSize();
        if (tam <= 0L) {
            throw new IllegalArgumentException("el elemento tiene que ocupar algo");
        }
        long i = 0L;
        while (i + tam <= this.largo) {
            partes.add(this.asSlice(i, tam));
            i = i + tam;
        }
        return partes.stream();
    }

    public Spliterator<MemorySegment> spliterator(MemoryLayout elementLayout) {
        return this.elements(elementLayout).spliterator();
    }

    // ---- los cuatro de archivos mapeados -------------------------------------------------------

    public boolean isLoaded() {
        return false;
    }

    public void load() {
    }

    public void unload() {
    }

    public void force() {
    }

    // ---- identidad ----------------------------------------------------------------------------
    //
    // Dos segmentos son iguales si son **la misma region**: mismo respaldo, mismo arranque, mismo
    // largo. No se compara el contenido, y eso es deliberado -- dos regiones distintas con los mismos
    // bytes no son la misma memoria, y confundirlas seria el peor error posible aca.

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SegmentoHeap)) {
            return false;
        }
        SegmentoHeap o = (SegmentoHeap) obj;
        return this.base == o.base && this.inicio == o.inicio && this.largo == o.largo;
    }

    public int hashCode() {
        int h = this.base == null ? 0 : System.identityHashCode(this.base);
        h = h * 31 + (int) this.inicio;
        return h * 31 + (int) this.largo;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MemorySegment{ kind: ");
        sb.append(this.base == null ? "native" : "heap");
        sb.append(", address: 0x");
        sb.append(Long.toHexString(this.inicio));
        sb.append(", byteSize: ");
        sb.append(this.largo);
        sb.append(" }");
        return sb.toString();
    }

    // ---- los estaticos que la interfaz delega --------------------------------------------------

    static byte byteDe(MemorySegment s, long i) {
        if (s instanceof SegmentoHeap) {
            return ((SegmentoHeap) s).leer(i);
        }
        return s.get(ValueLayout.JAVA_BYTE, i);
    }

    static void copiar(MemorySegment src, long srcOffset, MemorySegment dst, long dstOffset,
            long bytes) {
        if (dst.isReadOnly()) {
            throw new IllegalArgumentException("el destino es de solo lectura");
        }
        // De atras para adelante cuando se superponen y el destino esta despues: si no, se pisan los
        // bytes que todavia faltan leer. Es el mismo cuidado que `System.arraycopy`.
        boolean haciaAtras = src == dst && dstOffset > srcOffset;
        long i = haciaAtras ? bytes - 1L : 0L;
        while (haciaAtras ? i >= 0L : i < bytes) {
            byte b = SegmentoHeap.byteDe(src, srcOffset + i);
            SegmentoHeap.escribirEn(dst, dstOffset + i, b);
            i = haciaAtras ? i - 1L : i + 1L;
        }
    }

    private static void escribirEn(MemorySegment s, long i, byte v) {
        if (s instanceof SegmentoHeap) {
            ((SegmentoHeap) s).exigirVivo();
            ((SegmentoHeap) s).exigirRango(i, 1L);
            ((SegmentoHeap) s).escribir(i, v);
            return;
        }
        s.set(ValueLayout.JAVA_BYTE, i, v);
    }

    static void copiarElementos(MemorySegment src, ValueLayout srcLayout, long srcOffset,
            MemorySegment dst, ValueLayout dstLayout, long dstOffset, long elementCount) {
        if (srcLayout.carrier() != dstLayout.carrier()) {
            throw new IllegalArgumentException(
                    "los dos layouts tienen que transportar el mismo tipo");
        }
        // Elemento por elemento y no byte a byte: los dos layouts pueden tener **orden de bytes
        // distinto**, y copiar crudo dejaria el destino al reves.
        long i = 0L;
        while (i < elementCount) {
            long deOff = srcOffset + i * srcLayout.byteSize();
            long aOff = dstOffset + i * dstLayout.byteSize();
            long crudo = SegmentoHeap.leerCrudo(src, srcLayout, deOff);
            SegmentoHeap.escribirCrudo(dst, dstLayout, aOff, crudo);
            i = i + 1L;
        }
    }

    private static long leerCrudo(MemorySegment s, ValueLayout l, long off) {
        SegmentoHeap h = (SegmentoHeap) s;
        h.exigirLectura(l, off);
        return h.leerN(off, (int) l.byteSize(), SegmentoHeap.esBig(l));
    }

    private static void escribirCrudo(MemorySegment s, ValueLayout l, long off, long v) {
        SegmentoHeap h = (SegmentoHeap) s;
        h.exigirEscritura(l, off);
        h.escribirN(off, (int) l.byteSize(), SegmentoHeap.esBig(l), v);
    }

    static void copiarDesdeArreglo(Object srcArray, int srcIndex, MemorySegment dst,
            ValueLayout dstLayout, long dstOffset, int elementCount) {
        int i = 0;
        while (i < elementCount) {
            long crudo = SegmentoHeap.elementoCrudo(srcArray, srcIndex + i);
            SegmentoHeap.escribirCrudo(dst, dstLayout, dstOffset + (long) i * dstLayout.byteSize(),
                    crudo);
            i = i + 1;
        }
    }

    static void copiarAArreglo(MemorySegment src, ValueLayout srcLayout, long srcOffset,
            Object dstArray, int dstIndex, int elementCount) {
        int i = 0;
        while (i < elementCount) {
            long crudo = SegmentoHeap.leerCrudo(src, srcLayout,
                    srcOffset + (long) i * srcLayout.byteSize());
            SegmentoHeap.guardarElementoCrudo(dstArray, dstIndex + i, crudo);
            i = i + 1;
        }
    }

    // Los dos de abajo hacen lo que harian `Array.get`/`Array.set`, por switch de tipo y sin boxear.
    // Ver la nota de `largoDe`.
    private static long elementoCrudo(Object arr, int i) {
        if (arr instanceof byte[]) {
            return (long) ((byte[]) arr)[i];
        }
        if (arr instanceof char[]) {
            return (long) ((char[]) arr)[i];
        }
        if (arr instanceof short[]) {
            return (long) ((short[]) arr)[i];
        }
        if (arr instanceof int[]) {
            return (long) ((int[]) arr)[i];
        }
        if (arr instanceof long[]) {
            return ((long[]) arr)[i];
        }
        if (arr instanceof float[]) {
            return (long) Float.floatToRawIntBits(((float[]) arr)[i]);
        }
        if (arr instanceof double[]) {
            return Double.doubleToRawLongBits(((double[]) arr)[i]);
        }
        throw new IllegalArgumentException("no es un arreglo de primitivos soportado");
    }

    private static void guardarElementoCrudo(Object arr, int i, long crudo) {
        if (arr instanceof byte[]) {
            ((byte[]) arr)[i] = (byte) crudo;
            return;
        }
        if (arr instanceof char[]) {
            ((char[]) arr)[i] = (char) crudo;
            return;
        }
        if (arr instanceof short[]) {
            ((short[]) arr)[i] = (short) crudo;
            return;
        }
        if (arr instanceof int[]) {
            ((int[]) arr)[i] = (int) crudo;
            return;
        }
        if (arr instanceof long[]) {
            ((long[]) arr)[i] = crudo;
            return;
        }
        if (arr instanceof float[]) {
            ((float[]) arr)[i] = Float.intBitsToFloat((int) crudo);
            return;
        }
        if (arr instanceof double[]) {
            ((double[]) arr)[i] = Double.longBitsToDouble(crudo);
            return;
        }
        throw new IllegalArgumentException("no es un arreglo de primitivos soportado");
    }

}

// El ambito de un segmento: un interruptor que dice si todavia se puede usar.
//
// Es lo que convierte un error de memoria en una excepcion. Sin ambito, usar un segmento despues de
// que su arena se cerro leeria lo que hubiera quedado ahi; con ambito falla en el acto y con la linea
// exacta.
final class Ambito implements MemorySegment.Scope {

    // El de los segmentos que no se cierran nunca: los de `ofArray`, y el nulo. Un arreglo de Java lo
    // libera el recolector cuando nadie lo mira, asi que no hay nada que cerrar.
    static final Ambito GLOBAL = new Ambito();

    private boolean vivo = true;

    public boolean isAlive() {
        return this.vivo;
    }

    void cerrar() {
        this.vivo = false;
    }
}
