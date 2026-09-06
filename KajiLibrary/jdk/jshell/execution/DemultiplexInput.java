package jdk.jshell.execution;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * El que reparte lo que llega por un canal compartido; ver {@link MultiplexingOutputStream}.
 *
 * <h2>Como funciona</h2>
 *
 * <p>Es un hilo que lee bloque tras bloque, mira el nombre de cada uno y escribe sus datos en el
 * flujo que le corresponde. Un bloque cuyo nombre no esta en el mapa se descarta: puede venir de una
 * corriente que la otra punta abrio y esta no conoce, y cortar la conexion por eso seria perder
 * tambien las corrientes que si se entienden.
 *
 * <h2>El cierre</h2>
 *
 * <p>Cuando el canal se termina se cierra todo lo que se le encargo cerrar. Es lo que hace que el
 * lado que estaba esperando en un {@code read} se entere de que no viene nada mas, en vez de quedarse
 * colgado para siempre.
 */
final class DemultiplexInput extends Thread {

    private final DataInputStream origen;
    private final Map<String, OutputStream> destinos;
    private final Iterable<? extends Closeable> alCerrar;

    DemultiplexInput(InputStream origen, Map<String, OutputStream> destinos,
            Iterable<? extends Closeable> alCerrar) {
        super("output reader");
        this.origen = new DataInputStream(origen);
        this.destinos = destinos;
        this.alCerrar = alCerrar;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            while (true) {
                final int largoNombre = origen.read();
                if (largoNombre == -1) {
                    break;
                }
                final byte[] nombre = new byte[largoNombre];
                origen.readFully(nombre);
                final int largoDatos = origen.read();
                if (largoDatos == -1) {
                    break;
                }
                final byte[] datos = new byte[largoDatos];
                origen.readFully(datos);
                final OutputStream destino =
                        destinos.get(new String(nombre, StandardCharsets.UTF_8));
                if (destino != null) {
                    destino.write(datos);
                }
            }
        } catch (IOException e) {
            // El canal se corto. Es la unica forma normal de que este bucle termine.
        } finally {
            for (final Closeable c : alCerrar) {
                try {
                    c.close();
                } catch (IOException e) {
                    // Ya se esta cerrando todo; que uno se resista no cambia nada.
                }
            }
        }
    }
}
