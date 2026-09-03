package java.lang.foreign;

import java.nio.ByteOrder;
import java.util.Optional;

// Las implementaciones de `ValueLayout` y `AddressLayout`. Son de paquete: nadie las nombra desde
// afuera, se las obtiene por las constantes de `ValueLayout`.
//
// **Un layout es inmutable**, y eso gobierna toda la forma del archivo: cada `with*` construye uno
// nuevo en vez de mutar. No es un lujo -- `ValueLayout.JAVA_INT` es una constante compartida por
// todo el programa, y un `withName` que mutara le cambiaria el nombre a todo el mundo.
//
// La clase base junta lo que no depende del tipo transportado: tamanio, alineamiento, nombre, orden,
// y la comparacion. Los ocho subtipos existen **solo** para estrechar el retorno de los `with*`, que
// es lo que permite encadenarlos sin castear y --mas importante-- lo que hace que la sobrecarga de
// `MemorySegment.get` elija la que devuelve el primitivo correcto.
abstract class ValorBase implements ValueLayout {

    // La letra con que el JDK imprime cada tipo. Se copian porque el `toString` de un layout es
    // parte de lo que la gente lee cuando algo no cuadra, y que diga otra cosa que el JDK obligaria
    // a traducir mentalmente.
    static final char LETRA_BOOLEAN = 'z';
    static final char LETRA_BYTE = 'b';
    static final char LETRA_CHAR = 'c';
    static final char LETRA_SHORT = 's';
    static final char LETRA_INT = 'i';
    static final char LETRA_LONG = 'j';
    static final char LETRA_FLOAT = 'f';
    static final char LETRA_DOUBLE = 'd';
    static final char LETRA_ADDRESS = 'a';

    private final char letra;
    private final long tamanio;
    private final long alineamiento;
    private final String nombre;
    private final ByteOrder orden;

    ValorBase(char letra, long tamanio, long alineamiento, String nombre, ByteOrder orden) {
        this.letra = letra;
        this.tamanio = tamanio;
        this.alineamiento = alineamiento;
        this.nombre = nombre;
        this.orden = orden;
    }

    char letra() {
        return this.letra;
    }

    public long byteSize() {
        return this.tamanio;
    }

    public long byteAlignment() {
        return this.alineamiento;
    }

    public Optional<String> name() {
        return Optional.ofNullable(this.nombre);
    }

    String nombreCrudo() {
        return this.nombre;
    }

    public ByteOrder order() {
        return this.orden;
    }

    public long byteOffset(MemoryLayout.PathElement... elements) {
        return Layouts.offsetPorCamino(this, elements);
    }

    public MemoryLayout select(MemoryLayout.PathElement... elements) {
        return Layouts.seleccionarPorCamino(this, elements);
    }

    public long scale(long offset, long index) {
        return Layouts.escalar(this, offset, index);
    }

    // Dos layouts son iguales si describen **lo mismo**, y el nombre es parte de eso: un campo
    // llamado `x` y uno llamado `y` del mismo tipo no son intercambiables en un struct.
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ValorBase)) {
            return false;
        }
        ValorBase otro = (ValorBase) obj;
        if (this.getClass() != otro.getClass()) {
            return false;
        }
        boolean mismoNombre = this.nombre == null ? otro.nombre == null
                : this.nombre.equals(otro.nombre);
        return this.letra == otro.letra && this.tamanio == otro.tamanio
                && this.alineamiento == otro.alineamiento && this.orden == otro.orden
                && mismoNombre;
    }

    public int hashCode() {
        int h = this.letra;
        h = h * 31 + (int) this.tamanio;
        h = h * 31 + (int) this.alineamiento;
        h = h * 31 + (this.orden == ByteOrder.BIG_ENDIAN ? 1 : 0);
        h = h * 31 + (this.nombre == null ? 0 : this.nombre.hashCode());
        return h;
    }

    // El formato del JDK, y las tres partes tienen significado:
    //   `1%`   el alineamiento, **solo** cuando no es el natural del tipo;
    //   `I`    mayuscula si el orden es big-endian, minuscula si es little;
    //   `(x)`  el nombre, si tiene.
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.alineamiento != this.tamanio) {
            sb.append(this.alineamiento);
            sb.append('%');
        }
        char c = this.letra;
        if (this.orden == ByteOrder.BIG_ENDIAN) {
            c = Character.toUpperCase(c);
        }
        sb.append(c);
        sb.append(this.tamanio);
        this.completarNombre(sb);
        return sb.toString();
    }

    void completarNombre(StringBuilder sb) {
        if (this.nombre != null) {
            sb.append('(');
            sb.append(this.nombre);
            sb.append(')');
        }
    }
}

final class ValorBoolean extends ValorBase implements ValueLayout.OfBoolean {

    ValorBoolean(long alineamiento, String nombre, ByteOrder orden) {
        super(ValorBase.LETRA_BOOLEAN, 1L, alineamiento, nombre, orden);
    }

    public Class<?> carrier() {
        return Boolean.TYPE;
    }

    public ValueLayout.OfBoolean withName(String name) {
        return new ValorBoolean(this.byteAlignment(), Layouts.exigirNombre(name), this.order());
    }

    public ValueLayout.OfBoolean withoutName() {
        return new ValorBoolean(this.byteAlignment(), null, this.order());
    }

    public ValueLayout.OfBoolean withByteAlignment(long byteAlignment) {
        return new ValorBoolean(Layouts.exigirAlineamiento(byteAlignment), this.nombreCrudo(),
                this.order());
    }

    public ValueLayout.OfBoolean withOrder(ByteOrder order) {
        return new ValorBoolean(this.byteAlignment(), this.nombreCrudo(),
                Layouts.exigirOrden(order));
    }
}

final class ValorByte extends ValorBase implements ValueLayout.OfByte {

    ValorByte(long alineamiento, String nombre, ByteOrder orden) {
        super(ValorBase.LETRA_BYTE, 1L, alineamiento, nombre, orden);
    }

    public Class<?> carrier() {
        return Byte.TYPE;
    }

    public ValueLayout.OfByte withName(String name) {
        return new ValorByte(this.byteAlignment(), Layouts.exigirNombre(name), this.order());
    }

    public ValueLayout.OfByte withoutName() {
        return new ValorByte(this.byteAlignment(), null, this.order());
    }

    public ValueLayout.OfByte withByteAlignment(long byteAlignment) {
        return new ValorByte(Layouts.exigirAlineamiento(byteAlignment), this.nombreCrudo(),
                this.order());
    }

    public ValueLayout.OfByte withOrder(ByteOrder order) {
        return new ValorByte(this.byteAlignment(), this.nombreCrudo(), Layouts.exigirOrden(order));
    }
}

final class ValorChar extends ValorBase implements ValueLayout.OfChar {

    ValorChar(long alineamiento, String nombre, ByteOrder orden) {
        super(ValorBase.LETRA_CHAR, 2L, alineamiento, nombre, orden);
    }

    public Class<?> carrier() {
        return Character.TYPE;
    }

    public ValueLayout.OfChar withName(String name) {
        return new ValorChar(this.byteAlignment(), Layouts.exigirNombre(name), this.order());
    }

    public ValueLayout.OfChar withoutName() {
        return new ValorChar(this.byteAlignment(), null, this.order());
    }

    public ValueLayout.OfChar withByteAlignment(long byteAlignment) {
        return new ValorChar(Layouts.exigirAlineamiento(byteAlignment), this.nombreCrudo(),
                this.order());
    }

    public ValueLayout.OfChar withOrder(ByteOrder order) {
        return new ValorChar(this.byteAlignment(), this.nombreCrudo(), Layouts.exigirOrden(order));
    }
}

final class ValorShort extends ValorBase implements ValueLayout.OfShort {

    ValorShort(long alineamiento, String nombre, ByteOrder orden) {
        super(ValorBase.LETRA_SHORT, 2L, alineamiento, nombre, orden);
    }

    public Class<?> carrier() {
        return Short.TYPE;
    }

    public ValueLayout.OfShort withName(String name) {
        return new ValorShort(this.byteAlignment(), Layouts.exigirNombre(name), this.order());
    }

    public ValueLayout.OfShort withoutName() {
        return new ValorShort(this.byteAlignment(), null, this.order());
    }

    public ValueLayout.OfShort withByteAlignment(long byteAlignment) {
        return new ValorShort(Layouts.exigirAlineamiento(byteAlignment), this.nombreCrudo(),
                this.order());
    }

    public ValueLayout.OfShort withOrder(ByteOrder order) {
        return new ValorShort(this.byteAlignment(), this.nombreCrudo(), Layouts.exigirOrden(order));
    }
}

final class ValorInt extends ValorBase implements ValueLayout.OfInt {

    ValorInt(long alineamiento, String nombre, ByteOrder orden) {
        super(ValorBase.LETRA_INT, 4L, alineamiento, nombre, orden);
    }

    public Class<?> carrier() {
        return Integer.TYPE;
    }

    public ValueLayout.OfInt withName(String name) {
        return new ValorInt(this.byteAlignment(), Layouts.exigirNombre(name), this.order());
    }

    public ValueLayout.OfInt withoutName() {
        return new ValorInt(this.byteAlignment(), null, this.order());
    }

    public ValueLayout.OfInt withByteAlignment(long byteAlignment) {
        return new ValorInt(Layouts.exigirAlineamiento(byteAlignment), this.nombreCrudo(),
                this.order());
    }

    public ValueLayout.OfInt withOrder(ByteOrder order) {
        return new ValorInt(this.byteAlignment(), this.nombreCrudo(), Layouts.exigirOrden(order));
    }
}

final class ValorLong extends ValorBase implements ValueLayout.OfLong {

    ValorLong(long alineamiento, String nombre, ByteOrder orden) {
        super(ValorBase.LETRA_LONG, 8L, alineamiento, nombre, orden);
    }

    public Class<?> carrier() {
        return Long.TYPE;
    }

    public ValueLayout.OfLong withName(String name) {
        return new ValorLong(this.byteAlignment(), Layouts.exigirNombre(name), this.order());
    }

    public ValueLayout.OfLong withoutName() {
        return new ValorLong(this.byteAlignment(), null, this.order());
    }

    public ValueLayout.OfLong withByteAlignment(long byteAlignment) {
        return new ValorLong(Layouts.exigirAlineamiento(byteAlignment), this.nombreCrudo(),
                this.order());
    }

    public ValueLayout.OfLong withOrder(ByteOrder order) {
        return new ValorLong(this.byteAlignment(), this.nombreCrudo(), Layouts.exigirOrden(order));
    }
}

final class ValorFloat extends ValorBase implements ValueLayout.OfFloat {

    ValorFloat(long alineamiento, String nombre, ByteOrder orden) {
        super(ValorBase.LETRA_FLOAT, 4L, alineamiento, nombre, orden);
    }

    public Class<?> carrier() {
        return Float.TYPE;
    }

    public ValueLayout.OfFloat withName(String name) {
        return new ValorFloat(this.byteAlignment(), Layouts.exigirNombre(name), this.order());
    }

    public ValueLayout.OfFloat withoutName() {
        return new ValorFloat(this.byteAlignment(), null, this.order());
    }

    public ValueLayout.OfFloat withByteAlignment(long byteAlignment) {
        return new ValorFloat(Layouts.exigirAlineamiento(byteAlignment), this.nombreCrudo(),
                this.order());
    }

    public ValueLayout.OfFloat withOrder(ByteOrder order) {
        return new ValorFloat(this.byteAlignment(), this.nombreCrudo(), Layouts.exigirOrden(order));
    }
}

final class ValorDouble extends ValorBase implements ValueLayout.OfDouble {

    ValorDouble(long alineamiento, String nombre, ByteOrder orden) {
        super(ValorBase.LETRA_DOUBLE, 8L, alineamiento, nombre, orden);
    }

    public Class<?> carrier() {
        return Double.TYPE;
    }

    public ValueLayout.OfDouble withName(String name) {
        return new ValorDouble(this.byteAlignment(), Layouts.exigirNombre(name), this.order());
    }

    public ValueLayout.OfDouble withoutName() {
        return new ValorDouble(this.byteAlignment(), null, this.order());
    }

    public ValueLayout.OfDouble withByteAlignment(long byteAlignment) {
        return new ValorDouble(Layouts.exigirAlineamiento(byteAlignment), this.nombreCrudo(),
                this.order());
    }

    public ValueLayout.OfDouble withOrder(ByteOrder order) {
        return new ValorDouble(this.byteAlignment(), this.nombreCrudo(),
                Layouts.exigirOrden(order));
    }
}

// La direccion. Es un valor de ocho bytes **mas** el layout al que apunta, que es opcional: en C hay
// punteros a `void`, y forzar un destino obligaria a inventar uno.
final class Direccion extends ValorBase implements AddressLayout {

    private final MemoryLayout destino;

    Direccion(long alineamiento, String nombre, ByteOrder orden, MemoryLayout destino) {
        super(ValorBase.LETRA_ADDRESS, 8L, alineamiento, nombre, orden);
        this.destino = destino;
    }

    public Class<?> carrier() {
        return MemorySegment.class;
    }

    public Optional<MemoryLayout> targetLayout() {
        return Optional.ofNullable(this.destino);
    }

    public AddressLayout withTargetLayout(MemoryLayout layout) {
        if (layout == null) {
            throw new IllegalArgumentException("el destino no puede ser null");
        }
        return new Direccion(this.byteAlignment(), this.nombreCrudo(), this.order(), layout);
    }

    public AddressLayout withoutTargetLayout() {
        return new Direccion(this.byteAlignment(), this.nombreCrudo(), this.order(), null);
    }

    public AddressLayout withName(String name) {
        return new Direccion(this.byteAlignment(), Layouts.exigirNombre(name), this.order(),
                this.destino);
    }

    public AddressLayout withoutName() {
        return new Direccion(this.byteAlignment(), null, this.order(), this.destino);
    }

    public AddressLayout withByteAlignment(long byteAlignment) {
        return new Direccion(Layouts.exigirAlineamiento(byteAlignment), this.nombreCrudo(),
                this.order(), this.destino);
    }

    public AddressLayout withOrder(ByteOrder order) {
        return new Direccion(this.byteAlignment(), this.nombreCrudo(), Layouts.exigirOrden(order),
                this.destino);
    }

    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        Direccion otra = (Direccion) obj;
        return this.destino == null ? otra.destino == null : this.destino.equals(otra.destino);
    }

    public int hashCode() {
        return super.hashCode() * 31 + (this.destino == null ? 0 : this.destino.hashCode());
    }

    // `a8:i4` -- el destino va detras de dos puntos, que es como lo imprime el JDK.
    public String toString() {
        String base = super.toString();
        if (this.destino == null) {
            return base;
        }
        return base + ":" + this.destino.toString();
    }
}
