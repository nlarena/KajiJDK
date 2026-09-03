package java.lang.foreign;

import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * KajiLibrary's java.lang.foreign.SymbolLookup -- la busqueda de un simbolo nativo por nombre.
 *
 * <p>Es la mitad de arriba de una llamada nativa: primero se encuentra la direccion de la funcion,
 * despues {@link Linker} la convierte en algo invocable. Sin enlazador la segunda mitad no existe, y
 * las dos formas que **cargan** una biblioteca tampoco pueden: cargar un dll o un so es una
 * operacion del sistema operativo que esta VM no hace.
 *
 * <p>La interfaz esta entera igual, y los dos `default` --{@link #findOrThrow} y {@link #or}-- son
 * reales: se apoyan solo en {@link #find}, asi que una busqueda propia escrita por alguien mas los
 * hereda funcionando. Eso es lo que hace que valga la pena declararla en vez de omitirla.
 */
public interface SymbolLookup {

    /** La direccion del simbolo de ese nombre, o vacio si no esta. */
    Optional<MemorySegment> find(String name);

    /**
     * El de arriba, exigiendo que este.
     *
     * @throws NoSuchElementException si no esta
     */
    default MemorySegment findOrThrow(String name) {
        Optional<MemorySegment> hallado = this.find(name);
        if (!hallado.isPresent()) {
            throw new NoSuchElementException("simbolo no encontrado: " + name);
        }
        return hallado.get();
    }

    /**
     * Esta busqueda, y si falla la otra.
     *
     * <p>El orden importa y es el que se lee: **esta** gana. Es lo que permite poner una tabla propia
     * delante de la de la plataforma.
     */
    default SymbolLookup or(SymbolLookup other) {
        if (other == null) {
            throw new NullPointerException("other");
        }
        return new BusquedaEncadenada(this, other);
    }

    /**
     * Los simbolos que el cargador de clases haya publicado.
     *
     * <p>Devuelve una busqueda que **no encuentra nada**, y eso es la verdad y no un stub: esta VM no
     * carga bibliotecas nativas, asi que no hay ningun simbolo publicado. Es la misma respuesta que
     * da el JDK cuando no se cargo ninguna.
     */
    static SymbolLookup loaderLookup() {
        return new BusquedaVacia();
    }

    /**
     * Los simbolos de esa biblioteca.
     *
     * @throws UnsupportedOperationException siempre, en esta biblioteca: cargar un dll o un so es una
     *     operacion del sistema operativo que esta VM no hace. Devolver una busqueda vacia seria
     *     peor -- diria "la cargue y no tiene simbolos" en vez de "no la puedo cargar".
     */
    static SymbolLookup libraryLookup(String name, Arena arena) {
        throw new UnsupportedOperationException("KajiJDK no carga bibliotecas nativas: " + name);
    }

    /** Ver {@link #libraryLookup(String, Arena)}. */
    static SymbolLookup libraryLookup(java.nio.file.Path path, Arena arena) {
        throw new UnsupportedOperationException("KajiJDK no carga bibliotecas nativas: " + path);
    }
}

// Las dos implementaciones que los estaticos devuelven. Son clases con nombre y no anonimas porque
// las anonimas de este compilador arrastran capturas que aca no hacen falta.
final class BusquedaVacia implements SymbolLookup {

    public Optional<MemorySegment> find(String name) {
        return Optional.empty();
    }
}

final class BusquedaEncadenada implements SymbolLookup {

    private final SymbolLookup primera;
    private final SymbolLookup segunda;

    BusquedaEncadenada(SymbolLookup primera, SymbolLookup segunda) {
        this.primera = primera;
        this.segunda = segunda;
    }

    public Optional<MemorySegment> find(String name) {
        Optional<MemorySegment> hallado = this.primera.find(name);
        if (hallado.isPresent()) {
            return hallado;
        }
        return this.segunda.find(name);
    }
}
