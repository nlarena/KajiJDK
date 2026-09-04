package jdk.jshell.tool;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Locale;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Arma y corre la herramienta `jshell` desde adentro de un programa.
 *
 * <p>Es la unica forma soportada de embeber la consola: `jshell` como comando es una envoltura
 * fina sobre esto. Sirve para un IDE que quiera una consola integrada, o para un tutorial que
 * arranque una sesion con clases suyas ya cargadas.
 *
 * <h2>Por que hay tantos flujos</h2>
 *
 * <p>`jshell` mezcla **tres** conversaciones que en un programa comun serian una: lo que la
 * herramienta le dice al usuario, lo que el codigo del usuario imprime, y los diagnosticos. Por
 * eso {@link #out(PrintStream, PrintStream, PrintStream)} toma tres y no uno: un IDE quiere pintar
 * cada cosa distinto, y con un solo flujo no puede separarlas.
 *
 * <p>{@link #in(InputStream, InputStream)} toma dos por la misma razon al reves: lo que el usuario
 * teclea en la consola y lo que el **codigo del usuario** lee de `System.in` no son la misma
 * entrada.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>{@link #builder()} lanza {@link UnsupportedOperationException}. No es una pieza que falte:
 * `jshell` es un compilador incremental mas una VM remota mas un protocolo entre las dos, y nada de
 * eso esta en esta biblioteca.
 *
 * <p>La interfaz esta entera igual --con los dos {@code default} implementados-- porque es lo que
 * un programa compila contra ella. Devolver un armador que aceptara toda la configuracion y
 * fallara recien en {@code run()} seria peor: el programa creeria que la consola esta y se
 * enteraria de que no en el momento en que ya no puede hacer nada al respecto.
 *
 * @since 9
 */
public interface JavaShellToolBuilder {

    /**
     * Un armador nuevo.
     *
     * <p><b>No implementado en esta biblioteca.</b> Ver la nota de la interfaz.
     *
     * @throws UnsupportedOperationException siempre, en esta biblioteca
     */
    static JavaShellToolBuilder builder() {
        throw new UnsupportedOperationException(
                "no jshell tool in this library: it needs an incremental compiler, a remote "
                + "execution VM and the protocol between them, none of which are present");
    }

    /**
     * Los dos flujos de entrada: el de los comandos y el que ve el codigo del usuario.
     *
     * @param cmdIn de donde salen los comandos y los fragmentos que se teclean
     * @param userIn lo que el codigo del usuario lee de `System.in`, o `null` para que sea el
     *     mismo que `cmdIn`
     */
    JavaShellToolBuilder in(InputStream cmdIn, InputStream userIn);

    /**
     * Un solo flujo de salida para las tres conversaciones.
     *
     * @param output donde va todo lo que sale
     */
    JavaShellToolBuilder out(PrintStream output);

    /**
     * Los tres flujos de salida por separado. Ver la nota de la interfaz.
     *
     * @param cmdOut lo que la herramienta le dice al usuario
     * @param console el eco de la consola
     * @param userOut lo que el codigo del usuario imprime
     */
    JavaShellToolBuilder out(PrintStream cmdOut, PrintStream console, PrintStream userOut);

    /**
     * Un solo flujo de error.
     *
     * @param error donde van los diagnosticos y los errores del codigo del usuario
     */
    JavaShellToolBuilder err(PrintStream error);

    /**
     * Los dos flujos de error por separado.
     *
     * @param cmdErr los diagnosticos de la herramienta
     * @param userErr lo que el codigo del usuario escribe en `System.err`
     */
    JavaShellToolBuilder err(PrintStream cmdErr, PrintStream userErr);

    /**
     * Donde se guardan el historial y las opciones entre sesiones.
     *
     * @param prefs el nodo de preferencias
     */
    JavaShellToolBuilder persistence(Preferences prefs);

    /**
     * Lo mismo, en un mapa en memoria.
     *
     * <p>Sirve para una sesion que no tiene que dejar rastro, y para las pruebas.
     */
    JavaShellToolBuilder persistence(Map<String, String> prefsMap);

    /**
     * Las variables de entorno que ve el codigo del usuario.
     *
     * @param env el entorno, o `null` para el del proceso
     */
    JavaShellToolBuilder env(Map<String, String> env);

    /** El idioma de los mensajes de la herramienta. */
    JavaShellToolBuilder locale(Locale locale);

    /**
     * Si el indicador y el eco tienen que salir por el flujo de salida.
     *
     * <p>Con `true` la sesion se puede transcribir entera leyendo un solo flujo, que es lo que
     * necesita una prueba automatica. Por omision es `false`, porque en una terminal el eco lo hace
     * la terminal y saldria doble.
     */
    JavaShellToolBuilder promptCapture(boolean capture);

    /**
     * Si hay que tratar la entrada como una terminal interactiva.
     *
     * <p>Por omision no cambia nada: la herramienta lo detecta sola. Este metodo es para forzarlo
     * cuando la deteccion no puede acertar --una entrada redirigida que igual quiere edicion de
     * linea.
     */
    default JavaShellToolBuilder interactiveTerminal(boolean interactiveTerminal) {
        return this;
    }

    /**
     * El tamanio de la ventana, para las herramientas que no tienen una terminal de la que
     * averiguarlo.
     *
     * <p>Por omision no cambia nada, como {@link #interactiveTerminal}.
     */
    default JavaShellToolBuilder windowSize(int columns, int rows) {
        return this;
    }

    /**
     * Corre la herramienta con esos argumentos de linea de comandos.
     *
     * <p>Bloquea hasta que la sesion termina.
     *
     * @throws Exception lo que sea que falle al correrla
     */
    void run(String... arguments) throws Exception;

    /**
     * Corre la herramienta y devuelve su codigo de salida.
     *
     * <p>Por omision es {@link #run} y un cero: {@code run} avisa de los fallos lanzando, asi que
     * si volvio es que salio bien. Una implementacion que sepa distinguir grados de fallo lo
     * redefine.
     *
     * @throws Exception lo que sea que falle al correrla
     */
    default int start(String... arguments) throws Exception {
        run(arguments);
        return 0;
    }
}
