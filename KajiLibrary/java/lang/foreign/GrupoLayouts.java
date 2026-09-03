package java.lang.foreign;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

// Las implementaciones de los layouts compuestos: relleno, secuencia, struct y union. De paquete,
// como las de valor: se llega a ellas por las fabricas de `MemoryLayout`.

// Lugar ocupado que no lleva nada. Su alineamiento arranca en 1 porque una restriccion sobre donde
// puede empezar la nada no restringe nada.
final class Relleno implements PaddingLayout {

    private final long tamanio;
    private final long alineamiento;
    private final String nombre;

    Relleno(long tamanio, long alineamiento, String nombre) {
        this.tamanio = tamanio;
        this.alineamiento = alineamiento;
        this.nombre = nombre;
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

    public PaddingLayout withName(String name) {
        return new Relleno(this.tamanio, this.alineamiento, Layouts.exigirNombre(name));
    }

    public PaddingLayout withoutName() {
        return new Relleno(this.tamanio, this.alineamiento, null);
    }

    public PaddingLayout withByteAlignment(long byteAlignment) {
        return new Relleno(this.tamanio, Layouts.exigirAlineamiento(byteAlignment), this.nombre);
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

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Relleno)) {
            return false;
        }
        Relleno otro = (Relleno) obj;
        boolean mismoNombre = this.nombre == null ? otro.nombre == null
                : this.nombre.equals(otro.nombre);
        return this.tamanio == otro.tamanio && this.alineamiento == otro.alineamiento
                && mismoNombre;
    }

    public int hashCode() {
        int h = (int) this.tamanio * 31 + (int) this.alineamiento;
        return h * 31 + (this.nombre == null ? 0 : this.nombre.hashCode());
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('x');
        sb.append(this.tamanio);
        Layouts.completarNombre(sb, this.nombre);
        return sb.toString();
    }
}

// N copias de un layout, una detras de la otra. El alineamiento **es el del elemento**: si cada
// elemento cae alineado, la secuencia entera tambien.
final class Secuencia implements SequenceLayout {

    private final long cantidad;
    private final MemoryLayout elemento;
    private final long alineamiento;
    private final String nombre;

    Secuencia(long cantidad, MemoryLayout elemento, long alineamiento, String nombre) {
        this.cantidad = cantidad;
        this.elemento = elemento;
        this.alineamiento = alineamiento;
        this.nombre = nombre;
    }

    public long byteSize() {
        return this.cantidad * this.elemento.byteSize();
    }

    public long byteAlignment() {
        return this.alineamiento;
    }

    public Optional<String> name() {
        return Optional.ofNullable(this.nombre);
    }

    public MemoryLayout elementLayout() {
        return this.elemento;
    }

    public long elementCount() {
        return this.cantidad;
    }

    public SequenceLayout withElementCount(long elementCount) {
        return Layouts.sequence(elementCount, this.elemento);
    }

    // Aplanar: una secuencia de secuencias se vuelve una sola, con el producto de las cantidades.
    // Es correcto porque en memoria ya estan asi -- anidar no agrega separacion, solo estructura.
    public SequenceLayout flatten() {
        long total = this.cantidad;
        MemoryLayout hoja = this.elemento;
        while (hoja instanceof SequenceLayout) {
            SequenceLayout s = (SequenceLayout) hoja;
            total = total * s.elementCount();
            hoja = s.elementLayout();
        }
        return Layouts.sequence(total, hoja);
    }

    /**
     * Reparte los mismos elementos en varias dimensiones. Una puede ser `-1` y se deduce.
     */
    public SequenceLayout reshape(long... elementCounts) {
        if (elementCounts == null || elementCounts.length == 0) {
            throw new IllegalArgumentException("hacen falta dimensiones");
        }
        int deducida = -1;
        long producto = 1L;
        int i = 0;
        while (i < elementCounts.length) {
            long n = elementCounts[i];
            if (n == -1L) {
                if (deducida >= 0) {
                    throw new IllegalArgumentException("solo una dimension puede ser -1");
                }
                deducida = i;
            } else if (n <= 0L) {
                throw new IllegalArgumentException("dimension no positiva: " + n);
            } else {
                producto = producto * n;
            }
            i = i + 1;
        }
        long[] dims = elementCounts.clone();
        if (deducida >= 0) {
            if (producto == 0L || this.cantidad % producto != 0L) {
                throw new IllegalArgumentException("la dimension deducida no da entera");
            }
            dims[deducida] = this.cantidad / producto;
            producto = this.cantidad;
        }
        if (producto != this.cantidad) {
            throw new IllegalArgumentException(
                    "el producto de las dimensiones es " + producto + " y hay " + this.cantidad
                            + " elementos");
        }
        // Se arma de adentro hacia afuera: la ultima dimension es la mas pegada al elemento.
        MemoryLayout actual = this.elemento;
        int j = dims.length - 1;
        while (j >= 0) {
            actual = Layouts.sequence(dims[j], actual);
            j = j - 1;
        }
        return (SequenceLayout) actual;
    }

    public SequenceLayout withName(String name) {
        return new Secuencia(this.cantidad, this.elemento, this.alineamiento,
                Layouts.exigirNombre(name));
    }

    public SequenceLayout withoutName() {
        return new Secuencia(this.cantidad, this.elemento, this.alineamiento, null);
    }

    public SequenceLayout withByteAlignment(long byteAlignment) {
        return new Secuencia(this.cantidad, this.elemento,
                Layouts.exigirAlineamiento(byteAlignment), this.nombre);
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

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Secuencia)) {
            return false;
        }
        Secuencia otra = (Secuencia) obj;
        boolean mismoNombre = this.nombre == null ? otra.nombre == null
                : this.nombre.equals(otra.nombre);
        return this.cantidad == otra.cantidad && this.elemento.equals(otra.elemento)
                && this.alineamiento == otra.alineamiento && mismoNombre;
    }

    public int hashCode() {
        int h = (int) this.cantidad * 31 + this.elemento.hashCode();
        h = h * 31 + (int) this.alineamiento;
        return h * 31 + (this.nombre == null ? 0 : this.nombre.hashCode());
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        sb.append(this.cantidad);
        sb.append(':');
        sb.append(this.elemento.toString());
        sb.append(']');
        Layouts.completarNombre(sb, this.nombre);
        return sb.toString();
    }
}

// La base de struct y union: la lista de miembros y todo lo que no depende de si se apilan o se
// superponen. Lo unico que los diferencia --el offset de cada miembro-- vive en las subclases.
abstract class Grupo implements GroupLayout {

    private final List<MemoryLayout> miembros;
    private final long alineamiento;
    private final String nombre;

    Grupo(List<MemoryLayout> miembros, long alineamiento, String nombre) {
        this.miembros = miembros;
        this.alineamiento = alineamiento;
        this.nombre = nombre;
    }

    public List<MemoryLayout> memberLayouts() {
        return Collections.unmodifiableList(this.miembros);
    }

    List<MemoryLayout> miembrosCrudos() {
        return this.miembros;
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

    public long byteOffset(MemoryLayout.PathElement... elements) {
        return Layouts.offsetPorCamino(this, elements);
    }

    public MemoryLayout select(MemoryLayout.PathElement... elements) {
        return Layouts.seleccionarPorCamino(this, elements);
    }

    public long scale(long offset, long index) {
        return Layouts.escalar(this, offset, index);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        Grupo otro = (Grupo) obj;
        boolean mismoNombre = this.nombre == null ? otro.nombre == null
                : this.nombre.equals(otro.nombre);
        return this.miembros.equals(otro.miembros) && this.alineamiento == otro.alineamiento
                && mismoNombre;
    }

    public int hashCode() {
        int h = this.miembros.hashCode() * 31 + (int) this.alineamiento;
        return h * 31 + (this.nombre == null ? 0 : this.nombre.hashCode());
    }

    // `[m1m2]` para un struct, `[m1|m2]` para una union: el separador dice cual es cual.
    String imprimir(String separador) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        int i = 0;
        while (i < this.miembros.size()) {
            if (i > 0) {
                sb.append(separador);
            }
            sb.append(this.miembros.get(i).toString());
            i = i + 1;
        }
        sb.append(']');
        Layouts.completarNombre(sb, this.nombre);
        return sb.toString();
    }
}

final class Estructura extends Grupo implements StructLayout {

    Estructura(List<MemoryLayout> miembros, long alineamiento, String nombre) {
        super(miembros, alineamiento, nombre);
    }

    // La suma de los miembros. No hay relleno implicito: si hiciera falta, la construccion ya fallo.
    public long byteSize() {
        long total = 0L;
        List<MemoryLayout> ms = this.miembrosCrudos();
        int i = 0;
        while (i < ms.size()) {
            total = total + ms.get(i).byteSize();
            i = i + 1;
        }
        return total;
    }

    public StructLayout withName(String name) {
        return new Estructura(this.miembrosCrudos(), this.byteAlignment(),
                Layouts.exigirNombre(name));
    }

    public StructLayout withoutName() {
        return new Estructura(this.miembrosCrudos(), this.byteAlignment(), null);
    }

    public StructLayout withByteAlignment(long byteAlignment) {
        return new Estructura(this.miembrosCrudos(), Layouts.exigirAlineamiento(byteAlignment),
                this.nombreCrudo());
    }

    public String toString() {
        return this.imprimir("");
    }
}

final class Union extends Grupo implements UnionLayout {

    Union(List<MemoryLayout> miembros, long alineamiento, String nombre) {
        super(miembros, alineamiento, nombre);
    }

    // El mas grande. Todos empiezan en cero, asi que el tamanio es el del que mas ocupa.
    public long byteSize() {
        long maximo = 0L;
        List<MemoryLayout> ms = this.miembrosCrudos();
        int i = 0;
        while (i < ms.size()) {
            long n = ms.get(i).byteSize();
            if (n > maximo) {
                maximo = n;
            }
            i = i + 1;
        }
        return maximo;
    }

    public UnionLayout withName(String name) {
        return new Union(this.miembrosCrudos(), this.byteAlignment(), Layouts.exigirNombre(name));
    }

    public UnionLayout withoutName() {
        return new Union(this.miembrosCrudos(), this.byteAlignment(), null);
    }

    public UnionLayout withByteAlignment(long byteAlignment) {
        return new Union(this.miembrosCrudos(), Layouts.exigirAlineamiento(byteAlignment),
                this.nombreCrudo());
    }

    public String toString() {
        return this.imprimir("|");
    }
}
