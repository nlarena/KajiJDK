package java.util.spi;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * KajiLibrary's java.util.spi.ToolProvider -- una herramienta de linea de comandos, llamable desde
 * codigo.
 *
 * <p>Es lo que permite correr {@code javac} o {@code jar} sin lanzar un proceso: la herramienta se
 * busca por nombre y corre en la misma maquina virtual, con sus salidas redirigidas a donde uno
 * quiera. Para un build o una prueba eso cambia mucho -- no hay que parsear la salida de un proceso
 * ni pelearse con la codificacion de la consola.
 *
 * <h2>Por que la version de caracteres es la principal</h2>
 *
 * <p>Las dos sobrecargas de {@code run} hacen lo mismo, y la de {@code PrintStream} <b>envuelve</b> a
 * la de {@code PrintWriter}, no al reves. Tiene que ser en ese orden: un {@code PrintStream} escribe
 * bytes con una codificacion ya fijada, asi que si la herramienta escribiera ahi directamente, un
 * mensaje con acentos saldria mal y no habria forma de arreglarlo desde afuera. Con caracteres,
 * quien llama elige la codificacion al construir el writer.
 *
 * <p>El default vacia los dos writers en un {@code finally}: si la herramienta tiro, lo que alcanzo
 * a escribir es justamente lo que hace falta para saber por que.
 */
public interface ToolProvider {

    /** El nombre por el que se la encuentra: {@code "javac"}, {@code "jar"}. */
    String name();

    /** Una linea de descripcion, o vacio si no la tiene. */
    default Optional<String> description() {
        return Optional.empty();
    }

    /**
     * Corre la herramienta.
     *
     * @return el codigo de salida; 0 si anduvo
     */
    int run(PrintWriter out, PrintWriter err, String... args);

    /** Idem, con flujos de bytes. Ver la nota de la clase sobre por que esta es la envoltura. */
    default int run(PrintStream out, PrintStream err, String... args) {
        if (out == null || err == null) {
            throw new NullPointerException();
        }
        PrintWriter outWriter = new PrintWriter(out, true);
        PrintWriter errWriter = new PrintWriter(err, true);
        try {
            return run(outWriter, errWriter, args);
        } finally {
            outWriter.flush();
            errWriter.flush();
        }
    }

    /**
     * La primera herramienta con ese nombre, buscada entre las registradas como servicio.
     *
     * @return vacio si no hay ninguna
     */
    static Optional<ToolProvider> findFirst(String name) {
        if (name == null) {
            throw new NullPointerException();
        }
        ServiceLoader<ToolProvider> loader = ServiceLoader.load(ToolProvider.class);
        Iterator<ToolProvider> it = loader.iterator();
        while (it.hasNext()) {
            ToolProvider p = it.next();
            if (name.equals(p.name())) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }
}
