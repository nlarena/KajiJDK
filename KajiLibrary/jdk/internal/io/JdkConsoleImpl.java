package jdk.internal.io;

import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Locale;

/**
 * KajiLibrary's jdk.internal.io.JdkConsoleImpl — la consola por omisión.
 *
 * <p><strong>Esta VM no tiene terminal, y esta clase lo dice en vez de fingir.</strong> La salida va a
 * un sumidero y la entrada está en fin de archivo, exactamente como {@link java.io.Console} —que es
 * quien la usaría— ya lo documenta desde antes. `System.console()` devuelve `null`, así que en la
 * práctica nadie llega hasta acá.
 *
 * <p>Vale explicar por qué eso **no** la convierte en un miembro que miente, que es la línea que este
 * proyecto no cruza. Ninguno de estos métodos promete que haya alguien del otro lado:
 *
 * <ul>
 * <li>{@link #readLine()} devuelve `null` en fin de entrada, y eso es lo que el contrato dice que
 *     pasa cuando la entrada se agotó. Una entrada vacía **está** agotada desde el principio.</li>
 * <li>{@link #readPassword()} devuelve `null` por lo mismo.</li>
 * <li>Escribir en una consola sin terminal no tiene resultado observable, así que un sumidero es una
 *     implementación correcta y no una simulación.</li>
 * </ul>
 *
 * <p>Lo que sí sería mentir es que `System.console()` devolviera una de éstas: el programa creería
 * que hay un usuario mirando. Por eso devuelve `null`, y por eso esta clase es alcanzable sólo por
 * quien la construya a mano sabiendo lo que hace.
 *
 * <p>La única diferencia visible con el JDK está en `readPassword`: allá el eco se apaga tocando la
 * terminal, y acá no hay terminal que tocar. No hay nada que apagar y nada que se muestre.
 */
public final class JdkConsoleImpl implements JdkConsole {

    private final Charset entrada;
    private final Charset salida;
    private final PrintWriter escritor;
    private final Reader lector;

    /**
     * @param inCharset el juego de caracteres de la entrada
     * @param outCharset el de la salida
     */
    public JdkConsoleImpl(Charset inCharset, Charset outCharset) {
        this.entrada = inCharset;
        this.salida = outCharset;
        this.escritor = new PrintWriter(new Sumidero(), true);
        this.lector = new EnFinDeArchivo();
    }

    public PrintWriter writer() {
        return this.escritor;
    }

    public Reader reader() {
        return this.lector;
    }

    public JdkConsole println(Object obj) {
        this.escritor.println(obj);
        this.escritor.flush();
        return this;
    }

    public JdkConsole print(Object obj) {
        this.escritor.print(obj);
        this.escritor.flush();
        return this;
    }

    public JdkConsole format(Locale locale, String format, Object... args) {
        this.escritor.write(String.format(locale, format, args));
        this.escritor.flush();
        return this;
    }

    // El mensaje se escribe igual antes de leer: que la lectura no vaya a dar nada no cambia el
    // orden de las operaciones, y un llamador que mire la salida tiene que ver el pedido.
    public String readLine(Locale locale, String format, Object... args) {
        this.format(locale, format, args);
        return this.readLine();
    }

    public String readLine() {
        return null;
    }

    public char[] readPassword(Locale locale, String format, Object... args) {
        this.format(locale, format, args);
        return this.readPassword();
    }

    public char[] readPassword() {
        return null;
    }

    public void flush() {
        this.escritor.flush();
    }

    /**
     * El juego de caracteres de la **salida**.
     *
     * <p>El constructor recibe dos y este método devuelve uno: el del JDK devuelve el de salida, que
     * es el que gobierna lo que se escribe. El de entrada se guarda porque el constructor lo declara
     * y porque es parte del estado de la consola, no porque haga falta para responder esto.
     */
    public Charset charset() {
        return this.salida;
    }

    /** El juego de caracteres de la entrada, para quien lo necesite dentro del paquete. */
    Charset charsetDeEntrada() {
        return this.entrada;
    }

    // Sin terminal, escribir no tiene efecto observable. Se descarta en vez de acumular: guardar
    // texto que nadie va a leer seria una perdida silenciosa de memoria.
    private static final class Sumidero extends Writer {
        public void write(char[] buf, int off, int len) {
        }

        public void flush() {
        }

        public void close() {
        }
    }

    // Una entrada que ya se agoto. `read` devuelve -1, que es como se dice "no hay mas".
    private static final class EnFinDeArchivo extends Reader {
        public int read(char[] buf, int off, int len) {
            return -1;
        }

        public void close() {
        }
    }
}
