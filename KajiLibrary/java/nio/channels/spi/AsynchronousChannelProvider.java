package java.nio.channels.spi;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * KajiLibrary's java.nio.channels.spi.AsynchronousChannelProvider — la fabrica de los canales
 * asincronicos.
 *
 * <p>Es a los `Asynchronous*Channel` lo que {@link SelectorProvider} es a los selectables, con una
 * diferencia: aca el proveedor tambien fabrica el **grupo**, que es donde viven los hilos que
 * ejecutan los `CompletionHandler`. Por eso las dos formas de armar un grupo son metodos suyos y no
 * constructores del grupo.
 *
 * <h2>{@link #provider()}, que es el mismo mecanismo que el de {@link SelectorProvider}</h2>
 *
 * <p>Busca en tres escalones y se queda con el primero: la propiedad de sistema
 * {@code java.nio.channels.spi.AsynchronousChannelProvider}, despues los proveedores declarados en
 * {@code META-INF/services} via {@link ServiceLoader}, y por ultimo la implementacion por omision de
 * la plataforma. <strong>El tercero no existe aca</strong> --esta VM no tiene nativos de red, asi
 * que no hay grupo del sistema que fabricar-- y en ese caso se lanza
 * {@link ServiceConfigurationError}, que es el error que el JDK usa cuando la busqueda no se puede
 * resolver. Los dos primeros escalones estan enteros y son lo que hace instalable un proveedor
 * propio; el segundo hoy no encuentra nada porque nuestro `ServiceLoader` no puede enumerar
 * recursos, y eso esta dicho en su propio encabezado.
 *
 * <p>El exito se cachea --`provider()` devuelve siempre el mismo objeto, como manda el contrato-- y
 * el fallo no, para que poner la propiedad despues siga sirviendo.
 */
public abstract class AsynchronousChannelProvider {

    /** La clave, que es a la vez el nombre del servicio y el de la propiedad de sistema. */
    private static final String CLAVE = "java.nio.channels.spi.AsynchronousChannelProvider";

    // Cerrojo propio y no la clase; ver la nota de SelectorProvider.
    private static final Object CERROJO = new Object();

    private static AsynchronousChannelProvider encontrado;

    protected AsynchronousChannelProvider() {
    }

    /**
     * El proveedor del sistema, buscado en los tres escalones del encabezado.
     *
     * <p>La primera llamada busca; las siguientes devuelven el mismo objeto.
     *
     * @return el proveedor del sistema
     * @throws ServiceConfigurationError si ninguno de los escalones da uno, o si el que nombra la
     *         propiedad no se puede cargar, instanciar, o no es un `AsynchronousChannelProvider`
     */
    public static AsynchronousChannelProvider provider() {
        synchronized (CERROJO) {
            if (encontrado != null) {
                return encontrado;
            }
            AsynchronousChannelProvider p = deLaPropiedad();
            if (p == null) {
                p = deServiceLoader();
            }
            if (p == null) {
                throw new ServiceConfigurationError(
                        CLAVE + ": no hay proveedor del sistema en esta VM (no tiene nativos de red);"
                                + " instale uno con la propiedad de sistema " + CLAVE);
            }
            encontrado = p;
            return p;
        }
    }

    /**
     * El proveedor que nombre la propiedad de sistema, o null si no esta puesta.
     *
     * <p>`getConstructor()` y no `getDeclaredConstructor()`: el constructor tiene que ser publico,
     * como exige el JDK. Ver la nota del metodo homologo de {@link SelectorProvider}.
     */
    private static AsynchronousChannelProvider deLaPropiedad() {
        String nombre;
        try {
            nombre = System.getProperty(CLAVE);
        } catch (SecurityException ignorada) {
            return null;
        }
        if (nombre == null || nombre.length() == 0) {
            return null;
        }
        Class<?> clase;
        try {
            clase = Class.forName(nombre, false, ClassLoader.getSystemClassLoader());
        } catch (ClassNotFoundException e) {
            throw new ServiceConfigurationError(CLAVE + ": provider " + nombre + " not found", e);
        }
        Object objeto;
        try {
            objeto = clase.getConstructor().newInstance();
        } catch (Exception e) {
            throw new ServiceConfigurationError(
                    CLAVE + ": provider " + nombre + " could not be instantiated", e);
        }
        if (!(objeto instanceof AsynchronousChannelProvider)) {
            throw new ServiceConfigurationError(CLAVE + ": provider " + nombre + " not a subtype");
        }
        return (AsynchronousChannelProvider) objeto;
    }

    /** El primer proveedor declarado en el classpath, o null si no hay ninguno. */
    private static AsynchronousChannelProvider deServiceLoader() {
        try {
            ServiceLoader<AsynchronousChannelProvider> sl =
                    ServiceLoader.load(AsynchronousChannelProvider.class);
            Iterator<AsynchronousChannelProvider> it = sl.iterator();
            if (it.hasNext()) {
                return it.next();
            }
        } catch (Throwable ignorada) {
            // Un proveedor roto no puede impedir que se pruebe el escalon siguiente.
        }
        return null;
    }

    /**
     * Un grupo con una cantidad fija de hilos.
     *
     * @param nThreads cuantos hilos; fijo significa que un `CompletionHandler` que se bloquea deja
     *        sin atender a los demas, que es el modo de fallar clasico de esta configuracion
     */
    public abstract AsynchronousChannelGroup openAsynchronousChannelGroup(int nThreads,
            ThreadFactory threadFactory) throws IOException;

    /**
     * Un grupo sobre un pool que crece.
     *
     * @param initialSize una pista sobre cuantos hilos hay ya esperando, no un limite
     */
    public abstract AsynchronousChannelGroup openAsynchronousChannelGroup(ExecutorService executor,
            int initialSize) throws IOException;

    /** Un canal de escucha asincronico en `group`. */
    public abstract AsynchronousServerSocketChannel openAsynchronousServerSocketChannel(
            AsynchronousChannelGroup group) throws IOException;

    /** Un canal de socket asincronico en `group`. */
    public abstract AsynchronousSocketChannel openAsynchronousSocketChannel(
            AsynchronousChannelGroup group) throws IOException;
}
