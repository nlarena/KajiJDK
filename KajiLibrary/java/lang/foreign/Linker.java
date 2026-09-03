package java.lang.foreign;

import java.lang.invoke.MethodHandle;
import java.util.Map;

/**
 * KajiLibrary's java.lang.foreign.Linker -- el puente entre Java y una funcion nativa.
 *
 * <p><strong>No hay enlazador en esta biblioteca, y {@link #nativeLinker()} lo dice.</strong> La
 * interfaz esta entera porque es parte de la forma del paquete y porque el codigo que la nombra
 * tiene que poder compilar; lo que no hay es una implementacion, y no puede haberla: enlazar con una
 * funcion nativa pide generar codigo de llamada para la convencion de la plataforma, cargar
 * bibliotecas dinamicas, y mover argumentos entre la pila de Java y la del sistema. Eso es
 * maquinaria de la VM, no biblioteca.
 *
 * <p>Que `nativeLinker()` tire **no es una mentira sino la rama que el contrato define**: su javadoc
 * dice que lanza `UnsupportedOperationException` si la plataforma nativa subyacente no esta
 * soportada, y esa es exactamente la situacion. Un enlazador que devolviera algo daria un
 * `MethodHandle` sobre el que ninguna invocacion puede funcionar, que es peor.
 *
 * <p>Lo que **si** sirve de este paquete es todo lo que describe memoria: los layouts,
 * {@link FunctionDescriptor}, y los segmentos sobre arreglos de Java. Ver {@link MemorySegment}.
 */
public interface Linker {

    /**
     * El enlazador de la plataforma.
     *
     * @throws UnsupportedOperationException siempre, en esta biblioteca. Ver la nota de la interfaz.
     */
    static Linker nativeLinker() {
        throw new UnsupportedOperationException(
                "KajiJDK no tiene enlazador nativo: enlazar pide generar codigo de llamada para la"
                        + " convencion de la plataforma, que es maquinaria de la VM");
    }

    /** Un handle para llamar a la funcion que este en esa direccion. */
    MethodHandle downcallHandle(MemorySegment address, FunctionDescriptor function,
            Linker.Option... options);

    /** Un handle sin direccion fija: la direccion se pasa como primer argumento. */
    MethodHandle downcallHandle(FunctionDescriptor function, Linker.Option... options);

    /** Un segmento que, llamado desde codigo nativo, ejecuta ese metodo de Java. */
    MemorySegment upcallStub(MethodHandle target, FunctionDescriptor function, Arena arena,
            Linker.Option... options);

    /** La busqueda de simbolos por defecto de la plataforma. */
    SymbolLookup defaultLookup();

    /** Los layouts canonicos de los tipos de C en esta plataforma (int, long, size_t...). */
    Map<String, MemoryLayout> canonicalLayouts();

    /**
     * Una opcion de enlace.
     *
     * <p>Se declara vacia a proposito: sus fabricas del JDK solo tienen sentido con un enlazador
     * detras, y sin el serian constructores de objetos que nadie consume.
     */
    interface Option {
    }
}
