package java.nio.file;

import java.net.URI;

// Las dos fabricas de rutas que existian antes de `Path.of`.
//
// **Estan obsoletas en el JDK y aca tambien**, y conviene decir por que existen igual: desde Java 11
// `Path.of(...)` hace exactamente lo mismo, y tener la fabrica en la interfaz que devuelve evita
// tener que importar dos tipos. `Paths` queda para el codigo anterior, y por eso las dos delegan sin
// agregar nada -- que haya **una sola** implementacion es lo que garantiza que las dos formas den
// siempre lo mismo.
public final class Paths {

    // Solo fabricas: no hay nada que instanciar.
    private Paths() {
    }

    /** Igual que `Path.of(first, more)`. */
    public static Path get(String first, String... more) {
        return Path.of(first, more);
    }

    /** Igual que `Path.of(uri)`. */
    public static Path get(URI uri) {
        return Path.of(uri);
    }
}
