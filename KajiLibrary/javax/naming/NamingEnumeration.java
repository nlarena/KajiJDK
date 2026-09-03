package javax.naming;

import java.util.Enumeration;

/**
 * Una `Enumeration` que puede fallar **mientras** se recorre, y que hay que cerrar.
 *
 * <p>Listar un contexto no es recorrer una coleccion en memoria: es traer resultados de un
 * servidor de a pedazos. Eso rompe las dos suposiciones de `Enumeration`: que avanzar no falla y
 * que soltar la referencia alcanza. De ahi los tres metodos.
 *
 * <p>`next()` y `hasMore()` son `nextElement()` y `hasMoreElements()` **con `throws`**. Los
 * heredados siguen existiendo porque la interfaz extiende `Enumeration` y hay codigo viejo que la
 * usa asi; cuando esos fallan tienen que envolver la `NamingException` en una no chequeada, que
 * es exactamente el problema que los metodos nuevos vienen a evitar. Si podes elegir, usa
 * `hasMore`/`next`.
 *
 * <p>`close()` libera lo que haya del lado del servidor. Recorrer hasta el final tambien cierra;
 * `close()` esta para el que corta antes, que es el caso comun cuando se busca una sola entrada.
 */
public interface NamingEnumeration<T> extends Enumeration<T> {

    T next() throws NamingException;

    boolean hasMore() throws NamingException;

    void close() throws NamingException;
}
