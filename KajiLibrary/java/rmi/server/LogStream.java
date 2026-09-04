package java.rmi.server;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * El registro de RMI, de antes de que existiera una API de logging.
 *
 * @deprecated no hay reemplazo dentro de RMI: quien quiera registrar usa
 *     {@code java.lang.System.Logger} o la biblioteca que prefiera. Vive porque
 *     {@link RemoteServer#getLog} devuelve un {@link PrintStream} y esta era su implementacion.
 */
@Deprecated(since = "1.1")
public class LogStream extends PrintStream {

    /** Sin registro. */
    public static final int SILENT = 0;

    /** Solo lo esencial. */
    public static final int BRIEF = 10;

    /** Todo. */
    public static final int VERBOSE = 20;

    private static final Map<String, LogStream> CONOCIDOS = new HashMap<String, LogStream>();
    private static PrintStream porDefecto = System.err;

    private OutputStream salida;

    private LogStream(OutputStream out) {
        super(out);
        this.salida = out;
    }

    /**
     * El registro con ese nombre, creandolo si no estaba.
     *
     * <p>Uno por nombre y compartido, que es lo que permite que dos partes del programa escriban en
     * el mismo sin pasarselo.
     */
    public static LogStream log(String name) {
        synchronized (CONOCIDOS) {
            LogStream l = CONOCIDOS.get(name);
            if (l == null) {
                l = new LogStream(porDefecto);
                CONOCIDOS.put(name, l);
            }
            return l;
        }
    }

    /** Adonde van los registros nuevos. */
    public static synchronized PrintStream getDefaultStream() {
        return porDefecto;
    }

    /** Cambia adonde van los registros nuevos; los ya creados no se mueven. */
    public static synchronized void setDefaultStream(PrintStream newDefault) {
        porDefecto = newDefault;
    }

    /** Adonde escribe este registro. */
    public synchronized OutputStream getOutputStream() {
        return this.salida;
    }

    /** Cambia adonde escribe este registro. */
    public synchronized void setOutputStream(OutputStream out) {
        this.salida = out;
    }

    /** Escribe un byte. */
    public void write(int b) {
        super.write(b);
    }

    /** Escribe un tramo. */
    public void write(byte[] b, int off, int len) {
        super.write(b, off, len);
    }

    public String toString() {
        return "LogStream";
    }

    /**
     * Traduce {@code "SILENT"}, {@code "BRIEF"} o {@code "VERBOSE"} a su numero.
     *
     * @return el nivel, o {@code -1} si el nombre no es ninguno de los tres
     */
    public static int parseLevel(String s) {
        if (s == null) {
            return -1;
        }
        String t = s.trim().toUpperCase(java.util.Locale.ENGLISH);
        if (t.equals("SILENT")) {
            return SILENT;
        }
        if (t.equals("BRIEF")) {
            return BRIEF;
        }
        if (t.equals("VERBOSE")) {
            return VERBOSE;
        }
        return -1;
    }
}
