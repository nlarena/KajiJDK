package java.nio.file;

// Las cuatro clases de evento que define la spec para el servicio de vigilancia.
//
// Se comparan por **identidad**, no por nombre: son constantes unicas, y un `WatchEvent.Kind` ajeno
// que se llame `"ENTRY_CREATE"` no es este. Por eso la implementacion privada no define `equals`.
//
// KajiJDK no tiene servicio de vigilancia --`FileSystem.newWatchService()` levanta
// `UnsupportedOperationException`-- asi que estas constantes nunca llegan en un evento. Existen
// porque el codigo que registra un directorio las nombra y tiene que compilar.
public final class StandardWatchEventKinds {

    // Solo constantes: no hay nada que instanciar.
    private StandardWatchEventKinds() {
    }

    // La unica implementacion de `Kind`. Privada a proposito: nadie deberia poder fabricar una
    // clase de evento que se haga pasar por estas.
    private static class ClaseEstandar<T> implements WatchEvent.Kind<T> {

        private final String nombre;
        private final Class<T> tipo;

        ClaseEstandar(String nombre, Class<T> tipo) {
            this.nombre = nombre;
            this.tipo = tipo;
        }

        public String name() {
            return this.nombre;
        }

        public Class<T> type() {
            return this.tipo;
        }

        public String toString() {
            return this.nombre;
        }
    }

    /**
     * Se perdieron eventos.
     *
     * <p>Su contexto es `Object` y no `Path` porque no hay ninguna ruta que informar: lo que dice
     * es que la cola se desbordo y hay cambios que no se van a ver.
     */
    public static final WatchEvent.Kind<Object> OVERFLOW =
            new ClaseEstandar<Object>("OVERFLOW", Object.class);

    /** Se creo una entrada en el directorio vigilado. */
    public static final WatchEvent.Kind<Path> ENTRY_CREATE =
            new ClaseEstandar<Path>("ENTRY_CREATE", Path.class);

    /** Se borro una entrada. */
    public static final WatchEvent.Kind<Path> ENTRY_DELETE =
            new ClaseEstandar<Path>("ENTRY_DELETE", Path.class);

    /** Se modifico una entrada. */
    public static final WatchEvent.Kind<Path> ENTRY_MODIFY =
            new ClaseEstandar<Path>("ENTRY_MODIFY", Path.class);
}
