package jdk.jshell;

import java.io.IOError;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;

/**
 * La consola que ve el codigo escrito en `jshell`.
 *
 * <p>Cuando un fragmento llama a {@link java.lang.System#console()}, lo que le llega no es la
 * consola del proceso: es esta, que puede vivir del otro lado de una conexion --el codigo del
 * usuario corre en **otra VM**-- o dentro de la ventana de un IDE.
 *
 * <p>Por eso {@link #charset()} esta: el que teclea y el que ejecuta pueden estar en maquinas con
 * codificaciones distintas, y el codigo del usuario tiene derecho a saber en cual esta leyendo.
 *
 * @since 22
 */
public interface JShellConsole {

    /** Por donde el codigo del usuario escribe. */
    PrintWriter writer();

    /** De donde el codigo del usuario lee. */
    Reader reader();

    /**
     * Lee una linea, mostrando antes ese texto.
     *
     * @param prompt lo que se muestra antes de leer, o `null` para nada
     * @return la linea sin el salto, o `null` si se acabo la entrada
     * @throws IOError si falla la lectura
     */
    String readLine(String prompt) throws IOError;

    /**
     * Lee una linea sin mostrar lo que se teclea.
     *
     * <p>Devuelve `char[]` y no `String` por lo de siempre con las contrasenias: un arreglo se
     * puede borrar en el acto, y una cadena se queda en el monton hasta que el recolector quiera.
     *
     * @param prompt lo que se muestra antes de leer, o `null` para nada
     * @return los caracteres, o `null` si se acabo la entrada
     * @throws IOError si falla la lectura
     */
    char[] readPassword(String prompt) throws IOError;

    /** Vacia lo que haya pendiente de escribir. */
    void flush();

    /** La codificacion con la que esta consola lee y escribe. */
    Charset charset();
}
