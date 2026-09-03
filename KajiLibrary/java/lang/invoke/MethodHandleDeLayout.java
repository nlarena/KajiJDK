package java.lang.invoke;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;

/**
 * KajiLibrary's java.lang.invoke.MethodHandleDeLayout -- los {@link MethodHandle} que fabrica
 * {@link java.lang.foreign.MemoryLayout}.
 *
 * <p>Son tres, y los tres calculan lo mismo con distinta cara: **una direccion**.
 * {@link java.lang.foreign.MemoryLayout#scaleHandle} escala un indice por el tamano del layout;
 * `byteOffsetHandle` baja un camino y suma; `sliceHandle` hace lo mismo y ademas recorta el segmento.
 *
 * <p>Existen como objeto y no como llamada suelta por lo mismo que un `VarHandle`: el calculo queda
 * **guardado**, se puede pasar y componer sin repetir el camino en cada lugar.
 *
 * <h2>Por que una subclase con helpers tipados</h2>
 *
 * <p>`MethodHandle.invoke`/`invokeExact` son polimorficos de firma: el descriptor del sitio es el
 * real, y la VM los intercepta. Lo que intercepta necesita algo concreto a lo que llamar, y eso son
 * {@link #aplicarLong} y {@link #aplicarSegmento} -- uno por **forma de retorno**, que es lo unico
 * que el sitio de llamada dice. Cual de los tres calculos hacer lo decide {@link #modo}, que es un
 * dato del objeto y no del sitio.
 *
 * <p>El intrinseco de `MethodHandle` que ya tenia la VM no servia: lee de la instancia los campos de
 * un handle **directo** (`owner`/`name`/`descriptor`/`kind`), y estos no apuntan a ningun metodo --
 * llevan adentro un layout y un camino.
 */
final class MethodHandleDeLayout extends MethodHandle {

    /** `scaleHandle()`: `(base, indice) -> base + indice * byteSize()`. */
    static final int ESCALA = 0;
    /** `byteOffsetHandle(camino)`: `(base, indices…) -> base + offset + sum(i*paso)`. */
    static final int OFFSET = 1;
    /** `sliceHandle(camino)`: lo de arriba, y ademas recorta. */
    static final int REBANADA = 2;

    private final int modo;
    private final MemoryLayout raiz;
    // El layout **al que llega el camino**: lo que mide la rebanada. Igual a `raiz` para ESCALA.
    private final MemoryLayout destino;
    private final long desplazamientoFijo;
    private final long[] pasos;

    MethodHandleDeLayout(MethodType type, int modo, MemoryLayout raiz, MemoryLayout destino,
            long desplazamientoFijo, long[] pasos) {
        super(type);
        this.modo = modo;
        this.raiz = raiz;
        this.destino = destino;
        this.desplazamientoFijo = desplazamientoFijo;
        this.pasos = pasos == null ? new long[0] : pasos;
    }

    /**
     * La direccion, que es lo que devuelven {@link #ESCALA} y {@link #OFFSET}.
     *
     * <p>Exige tantos indices como pasos abiertos tiene el camino, por el mismo motivo que en un
     * `VarHandle`: uno de menos daria la direccion de otro elemento, en silencio.
     */
    long aplicarLong(long base, long[] indices) {
        int n = indices == null ? 0 : indices.length;
        if (this.modo == ESCALA) {
            if (n != 1) {
                throw new IllegalArgumentException("scaleHandle toma un indice, no " + n);
            }
            return this.raiz.scale(base, indices[0]);
        }
        return this.total(base, indices);
    }

    /** El segmento recortado, que es lo que devuelve {@link #REBANADA}. */
    MemorySegment aplicarSegmento(Object segmento, long base, long[] indices) {
        long t = this.total(base, indices);
        return ((MemorySegment) segmento).asSlice(t, this.destino.byteSize());
    }

    private long total(long base, long[] indices) {
        int n = indices == null ? 0 : indices.length;
        if (n != this.pasos.length) {
            throw new IllegalArgumentException(
                    "este handle toma " + this.pasos.length + " indices, no " + n);
        }
        long t = base + this.desplazamientoFijo;
        int i = 0;
        while (i < n) {
            t = t + indices[i] * this.pasos[i];
            i = i + 1;
        }
        return t;
    }
}
