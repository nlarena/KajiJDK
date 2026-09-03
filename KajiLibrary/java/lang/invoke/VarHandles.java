package java.lang.invoke;

/**
 * KajiLibrary's java.lang.invoke.VarHandles -- la puerta por la que `java.lang.foreign` fabrica sus
 * {@link VarHandle}.
 *
 * <p>Existe por una razon de visibilidad y no de diseno: el constructor de {@link VarHandle} es
 * package-private, asi que su subclase tiene que vivir en `java.lang.invoke`, y `java.lang.foreign`
 * --que es quien la fabrica-- no puede nombrarla.
 *
 * <h2>Una divergencia, dicha de frente</h2>
 *
 * <p>El JDK tiene una clase con **este mismo nombre y en este mismo paquete**, y la tiene
 * package-private: `final class java.lang.invoke.VarHandles`. Puede darse ese lujo porque resuelve la
 * costura con **modulos** -- `jdk.internal.foreign` la alcanza por un `opens` calificado--. Sin
 * modulos, la unica forma de que un paquete llegue a otro es que el miembro sea publico.
 *
 * <p>Asi que esto es API que el JDK real no tiene, y conviene saberlo: codigo escrito contra
 * `VarHandles.deSegmento` compila aca y **no** compila contra un JDK de verdad. No es un miembro que
 * miente --hace exactamente lo que dice-- pero es un miembro de mas, que es la otra forma de no
 * coincidir. Se elige esto sobre las alternativas porque las dos que hay son peores: abrir el
 * constructor de `VarHandle` cambiaria una clase que **si** es API, y fabricar el objeto desde un
 * `native` esconderia en la VM una decision que merece leerse en el fuente.
 */
public final class VarHandles {

    private VarHandles() {
    }

    /**
     * Un `VarHandle` sobre un segmento: el layout del valor, el desplazamiento fijo del camino, y
     * cuanto mide cada paso abierto.
     */
    public static VarHandle deSegmento(java.lang.foreign.MemoryLayout distribucion,
            long desplazamientoFijo, long[] pasos) {
        return new VarHandleDeSegmento(distribucion, desplazamientoFijo, pasos);
    }

    /** `scaleHandle()`: `(long base, long indice) -> long`. */
    public static MethodHandle escala(java.lang.foreign.MemoryLayout raiz) {
        return new MethodHandleDeLayout(
                MethodType.methodType(Long.TYPE, new Class<?>[] {Long.TYPE, Long.TYPE}),
                MethodHandleDeLayout.ESCALA, raiz, raiz, 0L, new long[0]);
    }

    /** `byteOffsetHandle(camino)`: `(long base, long… indices) -> long`. */
    public static MethodHandle offsetDeCamino(java.lang.foreign.MemoryLayout raiz,
            java.lang.foreign.MemoryLayout destino, long desplazamientoFijo, long[] pasos) {
        return new MethodHandleDeLayout(tipoLong(pasos.length), MethodHandleDeLayout.OFFSET,
                raiz, destino, desplazamientoFijo, pasos);
    }

    /** `sliceHandle(camino)`: `(MemorySegment, long base, long… indices) -> MemorySegment`. */
    public static MethodHandle rebanadaDeCamino(java.lang.foreign.MemoryLayout raiz,
            java.lang.foreign.MemoryLayout destino, long desplazamientoFijo, long[] pasos) {
        return new MethodHandleDeLayout(tipoRebanada(pasos.length), MethodHandleDeLayout.REBANADA,
                raiz, destino, desplazamientoFijo, pasos);
    }

    // `(long, long…n) -> long`
    private static MethodType tipoLong(int n) {
        Class<?>[] ps = new Class<?>[1 + n];
        int i = 0;
        while (i < ps.length) {
            ps[i] = Long.TYPE;
            i = i + 1;
        }
        return MethodType.methodType(Long.TYPE, ps);
    }

    // `(MemorySegment, long, long…n) -> MemorySegment`
    private static MethodType tipoRebanada(int n) {
        Class<?>[] ps = new Class<?>[2 + n];
        ps[0] = java.lang.foreign.MemorySegment.class;
        int i = 1;
        while (i < ps.length) {
            ps[i] = Long.TYPE;
            i = i + 1;
        }
        return MethodType.methodType(java.lang.foreign.MemorySegment.class, ps);
    }
}
