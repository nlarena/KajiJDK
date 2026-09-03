package java.nio.file;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;

// El visitante que `Files.walkFileTree` va llamando mientras baja por el arbol.
//
// **Los cuatro metodos, y por que son cuatro.** Un directorio se visita dos veces --antes de entrar
// y despues de salir-- porque hay tareas que solo se pueden hacer en cada momento: crear el destino
// va en `preVisitDirectory`, borrar el origen va en `postVisitDirectory`. Y `visitFileFailed` existe
// para que un archivo ilegible no corte el recorrido entero salvo que el visitante lo decida.
//
// **KajiJDK no lo llama nunca**: `Files.walkFileTree` no existe, porque recorrer requiere listar
// directorios y no hay nativo que lo haga. La interfaz esta para que el codigo que la implementa
// compile y para el dia que aparezca el nativo.
//
// @param <T> el tipo de las rutas, normalmente `Path`
public interface FileVisitor<T> {

    /**
     * Antes de entrar a un directorio.
     *
     * @param attrs los atributos del directorio
     * @return `CONTINUE` para entrar, `SKIP_SUBTREE` para saltearlo
     */
    FileVisitResult preVisitDirectory(T dir, BasicFileAttributes attrs) throws IOException;

    /** Por cada archivo del directorio. */
    FileVisitResult visitFile(T file, BasicFileAttributes attrs) throws IOException;

    /**
     * Cuando un archivo no se pudo visitar.
     *
     * <p>Recibe la excepcion en vez de que se propague sola: relanzarla es una opcion, seguir es la
     * otra, y quien decide es el visitante.
     */
    FileVisitResult visitFileFailed(T file, IOException exc) throws IOException;

    /**
     * Al salir de un directorio.
     *
     * @param exc `null` si se recorrio entero, o la falla que lo corto
     */
    FileVisitResult postVisitDirectory(T dir, IOException exc) throws IOException;
}
