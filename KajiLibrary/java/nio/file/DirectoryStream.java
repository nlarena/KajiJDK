package java.nio.file;

import java.io.Closeable;
import java.util.Iterator;

// Un directorio abierto, recorrible una sola vez y que hay que cerrar.
//
// **Por que no es una `List` ni un `Stream`.** Un directorio puede tener millones de entradas; el
// contrato es que se leen de a poco y que el recurso del sistema se libera con `close()`, por eso
// extiende `Closeable` y por eso `iterator()` se puede llamar **una sola vez**.
//
// **KajiJDK no produce ninguno.** Listar un directorio necesita un nativo que no existe --las seis
// operaciones de `jdk.internal.io.Fs` son leer, escribir, `stat`, tamaño, borrar y crear
// directorio, y ninguna enumera-- asi que `Files.newDirectoryStream` no esta. La interfaz existe
// porque es el tipo con el que se escribe codigo que la reciba, y porque el dia que aparezca el
// nativo lo unico que falta es la implementacion.
//
// @param <T> el tipo de las entradas
public interface DirectoryStream<T> extends Closeable, Iterable<T> {

    /**
     * El iterador. **Uno solo por stream**: llamarlo de nuevo levanta `IllegalStateException`.
     *
     * <p>Sus metodos no declaran `IOException` --`Iterator` no lo permite-- asi que una falla de
     * I/O en el medio del recorrido llega envuelta en `DirectoryIteratorException`.
     */
    Iterator<T> iterator();

    /**
     * El filtro que decide que entradas entran en el stream.
     *
     * <p>Va aca adentro y no como interfaz suelta porque solo tiene sentido junto a
     * `DirectoryStream`; anidarla evita un nombre generico mas en el paquete.
     *
     * @param <T> el tipo de las entradas
     */
    interface Filter<T> {

        /** `true` si la entrada se acepta. */
        boolean accept(T entry) throws java.io.IOException;
    }
}
