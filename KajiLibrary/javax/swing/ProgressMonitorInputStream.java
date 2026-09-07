package javax.swing;

import java.awt.Component;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

/**
 * Un flujo de entrada que muestra el progreso de la lectura.
 *
 * <h2>Se envuelve, no se configura</h2>
 *
 * <p>Se le pasa el flujo de verdad y se lee de este. Cada lectura avanza el
 * {@link ProgressMonitor}, y el monitor decide solo si vale la pena mostrar un cartel -- ver su
 * nota, que explica las dos demoras.
 *
 * <p>El maximo sale de {@code available()}, que para un archivo es su tamano. Para algo que no lo
 * sabe -- una conexion de red -- da cero y la barra queda quieta: no hay de donde sacar cuanto
 * falta, y esta clase no lo inventa.
 *
 * <h2>Cancelar corta la lectura</h2>
 *
 * <p>Y lo hace como corresponde: lanzando {@link InterruptedIOException}, que es una
 * {@link IOException} y por lo tanto la atrapa cualquiera que ya estuviera manejando errores de
 * lectura. Es la unica forma de que una cancelacion no se pierda en un {@code catch} que solo mira
 * problemas de disco.
 */
public class ProgressMonitorInputStream extends FilterInputStream {

    private final ProgressMonitor monitor;
    private int nread = 0;
    private int size = 0;

    Component parentComponent;
    Object message;

    /**
     * Envuelve ese flujo.
     *
     * <p>Lee {@code available()} para saber el total; si el flujo no lo sabe, queda en cero. Ver la
     * nota de la clase.
     */
    public ProgressMonitorInputStream(Component parentComponent, Object message,
            InputStream in) {
        super(in);
        this.parentComponent = parentComponent;
        this.message = message;
        try {
            size = in.available();
        } catch (IOException ioe) {
            // Un flujo que no sabe cuanto tiene no es un error: la barra queda quieta.
            size = 0;
        }
        monitor = new ProgressMonitor(parentComponent, message, null, 0, size);
    }

    /** El monitor, por si hay que cambiarle una demora o leer si lo cancelaron. */
    public ProgressMonitor getProgressMonitor() {
        return monitor;
    }

    /**
     * @throws InterruptedIOException si el usuario cancelo
     * @throws IOException si falla la lectura
     */
    public int read() throws IOException {
        int c = in.read();
        if (c >= 0) {
            nread = nread + 1;
            monitor.setProgress(nread);
        }
        controlarCancelacion();
        return c;
    }

    /**
     * @throws InterruptedIOException si el usuario cancelo
     * @throws IOException si falla la lectura
     */
    public int read(byte[] b) throws IOException {
        int nr = in.read(b);
        if (nr > 0) {
            nread = nread + nr;
            monitor.setProgress(nread);
        }
        controlarCancelacion();
        return nr;
    }

    /**
     * @throws InterruptedIOException si el usuario cancelo
     * @throws IOException si falla la lectura
     */
    public int read(byte[] b, int off, int len) throws IOException {
        int nr = in.read(b, off, len);
        if (nr > 0) {
            nread = nread + nr;
            monitor.setProgress(nread);
        }
        controlarCancelacion();
        return nr;
    }

    /**
     * Saltea bytes; tambien cuentan como avance.
     *
     * @throws IOException si falla
     */
    public long skip(long n) throws IOException {
        long nr = in.skip(n);
        if (nr > 0) {
            nread = nread + (int) nr;
            monitor.setProgress(nread);
        }
        return nr;
    }

    /**
     * Cierra el flujo y el cartel.
     *
     * @throws IOException si falla el cierre
     */
    public void close() throws IOException {
        in.close();
        monitor.close();
    }

    /**
     * Vuelve al principio; el progreso vuelve con el.
     *
     * @throws IOException si el flujo no soporta volver
     */
    public synchronized void reset() throws IOException {
        in.reset();
        nread = size - in.available();
        monitor.setProgress(nread);
    }

    /**
     * @throws InterruptedIOException si el usuario cancelo
     */
    private void controlarCancelacion() throws InterruptedIOException {
        if (monitor.isCanceled()) {
            InterruptedIOException exc = new InterruptedIOException("progress");
            exc.bytesTransferred = nread;
            monitor.close();
            throw exc;
        }
    }
}
