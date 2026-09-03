package java.lang.invoke;

import java.lang.foreign.AddressLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * KajiLibrary's java.lang.invoke.VarHandleDeSegmento -- el {@link VarHandle} que fabrica
 * {@link java.lang.foreign.MemoryLayout#varHandle}.
 *
 * <h2>Por que existe esta clase y no unos metodos en `VarHandle`</h2>
 *
 * <p>Los 31 accesores de `VarHandle` son `public final native`: no se pueden sobrescribir y su cuerpo
 * lo pone la VM al ligar. Eso no es un capricho del JDK -- son **polimorficos de firma** (JLS
 * §15.12.3), o sea que una sola declaracion `(Object...)` atiende sitios de llamada con descriptores
 * distintos, y ningun cuerpo Java puede tener esa forma.
 *
 * <p>Asi que la VM tiene que interceptarlos, y lo que intercepta necesita algo concreto a lo que
 * llamar. Eso son los metodos `leerXxx`/`escribirXxx` de aca abajo: uno por **carrier**, con un
 * descriptor fijo que la VM puede nombrar sin inventar nada. El mapeo es directo -- `get` con retorno
 * `I` va a `leerInt`, `set` con ultimo parametro `J` va a `escribirLong`--, y los indices de los pasos
 * abiertos del camino viajan en un `long[]` que la VM arma con los argumentos que sobran. Un arreglo
 * y no una aridad por cada cantidad de indices: son 18 metodos en vez de setenta y pico, y la VM ya
 * sabe construir arreglos.
 *
 * <h2>Que guarda</h2>
 *
 * <p>Un `VarHandle` de layout es una **direccion diferida**: el layout del valor, cuanto hay que
 * correrse desde el principio del layout raiz, y cuanto mide cada paso abierto. La direccion final es
 *
 * <pre>  desplazamiento del llamador + desplazamiento fijo del camino + sum(indices[i] * pasos[i])</pre>
 *
 * y el acceso lo hace {@link MemorySegment#get}, que ya chequea limites, alineacion y solo-lectura.
 * Nada de eso se reimplementa aca.
 *
 * <h2>Los modos de memoria</h2>
 *
 * <p>Se atienden los modos **plain**, `Opaque`, `Acquire`, `Release` y `Volatile` de lectura y
 * escritura, y los cinco hacen lo mismo. En un interprete de un solo hilo por acceso eso es correcto
 * y no una simplificacion: no hay reordenamiento que impedir ni cache que sincronizar, asi que la
 * garantia mas fuerte se cumple sin hacer nada. Los modos de **lectura-modificacion-escritura**
 * --`compareAndSet`, `getAndAdd` y compania-- **no** se atienden: esos si necesitan atomicidad de
 * verdad, y fingirla seria exactamente el tipo de mentira que este proyecto no admite. Un sitio que
 * los use cae en el `native` sin puente y recibe un `UnsatisfiedLinkError`.
 */
final class VarHandleDeSegmento extends VarHandle {

    // El layout del **valor**, ya bajado por el camino. Es un `ValueLayout` o un `AddressLayout`; el
    // molde en cada helper es lo que elige la sobrecarga de `MemorySegment.get`.
    private final java.lang.foreign.MemoryLayout distribucion;
    // Lo que el camino corre desde el principio del layout raiz. Fijo: sale de `byteOffset`.
    private final long desplazamientoFijo;
    // Cuanto mide cada paso **abierto** del camino, en bytes. Vacio si el camino era cerrado.
    private final long[] pasos;

    VarHandleDeSegmento(java.lang.foreign.MemoryLayout distribucion, long desplazamientoFijo,
            long[] pasos) {
        this.distribucion = distribucion;
        this.desplazamientoFijo = desplazamientoFijo;
        this.pasos = pasos == null ? new long[0] : pasos;
    }

    /**
     * La direccion final.
     *
     * <p>Exige que vengan **exactamente** tantos indices como pasos abiertos tiene el camino. Un
     * indice de menos daria un acceso a otro elemento, en silencio; uno de mas es un llamador que
     * cree estar accediendo a algo que este handle no designa.
     */
    private long total(long desplazamiento, long[] indices) {
        int n = indices == null ? 0 : indices.length;
        if (n != this.pasos.length) {
            throw new IllegalArgumentException(
                    "este VarHandle toma " + this.pasos.length + " indices, no " + n);
        }
        long t = desplazamiento + this.desplazamientoFijo;
        int i = 0;
        while (i < n) {
            t = t + indices[i] * this.pasos[i];
            i = i + 1;
        }
        return t;
    }

    // ---- los helpers que la VM nombra ----------------------------------------------------------------
    //
    // Package-private y no `public`: son el punto de entrada de la VM, no API. Que no sean privados es
    // deliberado -- un `private` obligaria a la VM a saltearse el control de acceso, y prefiero que el
    // mecanismo sea visible desde `java.lang.invoke` a que sea invisible y magico.

    /**
     * Una lectura cuyo valor **se descarta**: `vh.get(seg, off);` como sentencia.
     *
     * <p>Existe porque el retorno de una llamada polimorfica de firma lo fija el contexto (JLS
     * §15.12.3), y en una sentencia ese contexto es `void`. El JDK hace la lectura igual, y hay que
     * hacerla: los chequeos de limites, de alineacion y de cantidad de indices son **efectos** de
     * acceder, y saltearlos convertiria un acceso invalido en una linea que no hace nada.
     *
     * <p>El molde va por la clase del layout porque el carrier no se sabe desde el sitio de llamada:
     * ahi el descriptor dice `V`, que no distingue un `int` de un `double`.
     */
    void leerYDescartar(Object segmento, long desplazamiento, long[] indices) {
        MemorySegment m = (MemorySegment) segmento;
        long t = this.total(desplazamiento, indices);
        if (this.distribucion instanceof ValueLayout.OfInt) {
            m.get((ValueLayout.OfInt) this.distribucion, t);
        } else if (this.distribucion instanceof ValueLayout.OfLong) {
            m.get((ValueLayout.OfLong) this.distribucion, t);
        } else if (this.distribucion instanceof ValueLayout.OfDouble) {
            m.get((ValueLayout.OfDouble) this.distribucion, t);
        } else if (this.distribucion instanceof ValueLayout.OfFloat) {
            m.get((ValueLayout.OfFloat) this.distribucion, t);
        } else if (this.distribucion instanceof ValueLayout.OfBoolean) {
            m.get((ValueLayout.OfBoolean) this.distribucion, t);
        } else if (this.distribucion instanceof ValueLayout.OfByte) {
            m.get((ValueLayout.OfByte) this.distribucion, t);
        } else if (this.distribucion instanceof ValueLayout.OfChar) {
            m.get((ValueLayout.OfChar) this.distribucion, t);
        } else if (this.distribucion instanceof ValueLayout.OfShort) {
            m.get((ValueLayout.OfShort) this.distribucion, t);
        } else {
            m.get((AddressLayout) this.distribucion, t);
        }
    }

    boolean leerBoolean(Object segmento, long desplazamiento, long[] indices) {
        return ((MemorySegment) segmento).get(
                (ValueLayout.OfBoolean) this.distribucion, this.total(desplazamiento, indices));
    }

    void escribirBoolean(Object segmento, long desplazamiento, long[] indices, boolean valor) {
        ((MemorySegment) segmento).set(
                (ValueLayout.OfBoolean) this.distribucion, this.total(desplazamiento, indices), valor);
    }

    byte leerByte(Object segmento, long desplazamiento, long[] indices) {
        return ((MemorySegment) segmento).get(
                (ValueLayout.OfByte) this.distribucion, this.total(desplazamiento, indices));
    }

    void escribirByte(Object segmento, long desplazamiento, long[] indices, byte valor) {
        ((MemorySegment) segmento).set(
                (ValueLayout.OfByte) this.distribucion, this.total(desplazamiento, indices), valor);
    }

    char leerChar(Object segmento, long desplazamiento, long[] indices) {
        return ((MemorySegment) segmento).get(
                (ValueLayout.OfChar) this.distribucion, this.total(desplazamiento, indices));
    }

    void escribirChar(Object segmento, long desplazamiento, long[] indices, char valor) {
        ((MemorySegment) segmento).set(
                (ValueLayout.OfChar) this.distribucion, this.total(desplazamiento, indices), valor);
    }

    short leerShort(Object segmento, long desplazamiento, long[] indices) {
        return ((MemorySegment) segmento).get(
                (ValueLayout.OfShort) this.distribucion, this.total(desplazamiento, indices));
    }

    void escribirShort(Object segmento, long desplazamiento, long[] indices, short valor) {
        ((MemorySegment) segmento).set(
                (ValueLayout.OfShort) this.distribucion, this.total(desplazamiento, indices), valor);
    }

    int leerInt(Object segmento, long desplazamiento, long[] indices) {
        return ((MemorySegment) segmento).get(
                (ValueLayout.OfInt) this.distribucion, this.total(desplazamiento, indices));
    }

    void escribirInt(Object segmento, long desplazamiento, long[] indices, int valor) {
        ((MemorySegment) segmento).set(
                (ValueLayout.OfInt) this.distribucion, this.total(desplazamiento, indices), valor);
    }

    long leerLong(Object segmento, long desplazamiento, long[] indices) {
        return ((MemorySegment) segmento).get(
                (ValueLayout.OfLong) this.distribucion, this.total(desplazamiento, indices));
    }

    void escribirLong(Object segmento, long desplazamiento, long[] indices, long valor) {
        ((MemorySegment) segmento).set(
                (ValueLayout.OfLong) this.distribucion, this.total(desplazamiento, indices), valor);
    }

    float leerFloat(Object segmento, long desplazamiento, long[] indices) {
        return ((MemorySegment) segmento).get(
                (ValueLayout.OfFloat) this.distribucion, this.total(desplazamiento, indices));
    }

    void escribirFloat(Object segmento, long desplazamiento, long[] indices, float valor) {
        ((MemorySegment) segmento).set(
                (ValueLayout.OfFloat) this.distribucion, this.total(desplazamiento, indices), valor);
    }

    double leerDouble(Object segmento, long desplazamiento, long[] indices) {
        return ((MemorySegment) segmento).get(
                (ValueLayout.OfDouble) this.distribucion, this.total(desplazamiento, indices));
    }

    void escribirDouble(Object segmento, long desplazamiento, long[] indices, double valor) {
        ((MemorySegment) segmento).set(
                (ValueLayout.OfDouble) this.distribucion, this.total(desplazamiento, indices), valor);
    }

    MemorySegment leerRef(Object segmento, long desplazamiento, long[] indices) {
        return ((MemorySegment) segmento).get(
                (AddressLayout) this.distribucion, this.total(desplazamiento, indices));
    }

    void escribirRef(Object segmento, long desplazamiento, long[] indices, MemorySegment valor) {
        ((MemorySegment) segmento).set(
                (AddressLayout) this.distribucion, this.total(desplazamiento, indices), valor);
    }
}
