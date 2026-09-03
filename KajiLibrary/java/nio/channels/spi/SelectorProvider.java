package java.nio.channels.spi;

import java.io.IOException;
import java.net.ProtocolFamily;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * KajiLibrary's java.nio.channels.spi.SelectorProvider — la fabrica de canales selectables.
 *
 * <p>Todo canal de red y todo selector nacen de un proveedor. La indireccion existe para que se
 * pueda cambiar la implementacion entera de `java.nio.channels` --epoll, kqueue, IOCP, o una de
 * juguete para pruebas-- sin tocar una linea del codigo que la usa.
 *
 * <h2>{@link #provider()} y los tres escalones del contrato</h2>
 *
 * <p>El estatico busca el proveedor del sistema y se queda con el primero que encuentre:
 *
 * <ol>
 *   <li>la propiedad de sistema {@code java.nio.channels.spi.SelectorProvider}, tomada como el
 *       nombre completo de una clase con constructor sin argumentos;
 *   <li>el primer proveedor declarado via {@link ServiceLoader}, o sea
 *       {@code META-INF/services/java.nio.channels.spi.SelectorProvider} en el classpath;
 *   <li>la implementacion por omision de la plataforma.
 * </ol>
 *
 * <p><strong>El tercer escalon no existe en esta biblioteca y no puede existir</strong>: esta VM no
 * tiene nativos de red --se busco en `src/` y lo unico que abre sockets es el depurador JDWP, que no
 * es alcanzable desde Java--, asi que no hay ningun `openSelector()` del sistema que devolver.
 * Cuando ninguno de los tres da nada, {@link #provider()} lanza {@link ServiceConfigurationError},
 * que es **exactamente** el error que el JDK usa cuando la busqueda del proveedor no se puede
 * resolver, y por eso no es un stub sino el camino previsto por el contrato. Que el JDK real casi
 * nunca lo tome es un accidente de que trae `sun.nio.ch` adentro, no otra regla.
 *
 * <p>Los dos primeros escalones **si** estan enteros, y son la razon de que este metodo valga la
 * pena en vez de faltar: son el mecanismo por el que se instala un proveedor propio --sobre memoria,
 * sobre un socket falso, sobre lo que sea-- y son la unica forma de llegar a el por el idioma
 * estandar. Sin `provider()` el paquete `spi` no puede hacer lo unico para lo que existe. El
 * escalon 2 hoy no encuentra nada, y no por un atajo de aca: el {@link ServiceLoader} de esta
 * biblioteca no puede enumerar {@code META-INF/services} porque nuestro `ClassLoader` no tiene
 * recursos. La maquinaria esta enchufada donde va, asi que el dia que los recursos existan ese
 * escalon empieza a encontrar proveedores sin tocar una linea.
 *
 * <p>El resultado se **cachea**, como manda el contrato --"the first invocation locates the
 * provider"--: `provider()` devuelve siempre el mismo objeto. El fallo, en cambio, no se cachea: una
 * busqueda que no encontro nada no es una decision, es la ausencia de configuracion, y quien ponga
 * la propiedad despues tiene que poder llegar a su proveedor.
 *
 * <h2>Las tres sobrecargas con `ProtocolFamily`</h2>
 *
 * <p>{@link #openDatagramChannel(ProtocolFamily)} es abstracta, o sea una declaracion y nada mas.
 * Las otras dos son concretas y tiran {@link UnsupportedOperationException}, que **es el cuerpo que
 * el JDK les da**: un proveedor que no soporte la familia pedida hereda ese comportamiento tal cual,
 * y el que si la soporte redefine. No hay nada omitido aca.
 */
public abstract class SelectorProvider {

    /** La clave, que es a la vez el nombre del servicio y el de la propiedad de sistema. */
    private static final String CLAVE = "java.nio.channels.spi.SelectorProvider";

    // Cerrojo propio y no la clase: sincronizar sobre `SelectorProvider.class` deja que cualquiera
    // que tenga el literal trabe la busqueda del proveedor desde afuera.
    private static final Object CERROJO = new Object();

    // El proveedor ya encontrado. Solo se escribe con exito: ver la nota del encabezado.
    private static SelectorProvider encontrado;

    protected SelectorProvider() {
    }

    /**
     * El proveedor del sistema, buscado en los tres escalones del encabezado.
     *
     * <p>La primera llamada busca; las siguientes devuelven el mismo objeto.
     *
     * @return el proveedor del sistema
     * @throws ServiceConfigurationError si ninguno de los escalones da uno, o si el que nombra la
     *         propiedad no se puede cargar, instanciar, o no es un `SelectorProvider`
     */
    public static SelectorProvider provider() {
        synchronized (CERROJO) {
            if (encontrado != null) {
                return encontrado;
            }
            SelectorProvider p = deLaPropiedad();
            if (p == null) {
                p = deServiceLoader();
            }
            if (p == null) {
                // El escalon 3, que aca no existe. El mensaje nombra la propiedad porque es lo
                // unico que quien lea esto puede hacer al respecto.
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
     * <p>Los tres fallos se distinguen porque se arreglan distinto: **not found** es una clase que
     * no esta en el classpath, **could not be instantiated** es una que esta pero no tiene
     * constructor **publico** sin argumentos, y **not a subtype** es una que se instancio pero no
     * sirve.
     *
     * <p>Se usa `getConstructor()` y no `getDeclaredConstructor()` a proposito: el JDK exige que el
     * constructor sea publico, y aceptar uno package-private haria que un proveedor que anda aca
     * no ande alla, que es la peor clase de divergencia --la que se descubre en la otra VM--.
     *
     * <p>Una divergencia que **si** queda, medida: cuando la clase existe y se instancia pero no es
     * un `SelectorProvider`, el JDK deja escapar la {@link ClassCastException} de su cast y, como
     * la busqueda le vive dentro de un `Holder` estatico, el llamador la recibe envuelta en
     * `ExceptionInInitializerError` (y las llamadas siguientes en `NoClassDefFoundError`). Eso es
     * consecuencia del `Holder`, no del contrato. Aca la busqueda vive en el metodo, asi que ese
     * envoltorio no se puede reproducir ni conviene: se reporta `ServiceConfigurationError` con el
     * nombre de la clase adentro, que es el tipo que este mismo metodo ya usa para los otros dos
     * fallos de configuracion y dice cual es el problema en vez de esconderlo dos niveles abajo.
     */
    private static SelectorProvider deLaPropiedad() {
        String nombre;
        try {
            nombre = System.getProperty(CLAVE);
        } catch (SecurityException ignorada) {
            // Sin permiso para leerla es lo mismo que no estar puesta: se sigue al escalon 2.
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
        if (!(objeto instanceof SelectorProvider)) {
            throw new ServiceConfigurationError(
                    CLAVE + ": provider " + nombre + " not a subtype");
        }
        return (SelectorProvider) objeto;
    }

    /**
     * El primer proveedor declarado en el classpath, o null si no hay ninguno.
     *
     * <p>Hoy siempre da null; el porque esta en el encabezado y no es una decision de este metodo.
     */
    private static SelectorProvider deServiceLoader() {
        try {
            ServiceLoader<SelectorProvider> sl = ServiceLoader.load(SelectorProvider.class);
            Iterator<SelectorProvider> it = sl.iterator();
            if (it.hasNext()) {
                return it.next();
            }
        } catch (Throwable ignorada) {
            // Un proveedor roto no puede impedir que se pruebe el escalon siguiente.
        }
        return null;
    }

    // ---- el contrato de la fabrica ---------------------------------------------------------------

    /** Un canal de datagramas nuevo. */
    public abstract DatagramChannel openDatagramChannel() throws IOException;

    /**
     * Un canal de datagramas nuevo de la familia `family`.
     *
     * <p>Abstracta, como en el JDK: no hay omision razonable --un proveedor que sepa de familias
     * tiene que decir cuales-- y por eso la decision se le empuja a quien implementa.
     *
     * @param family la familia de protocolos, p. ej. {@link java.net.StandardProtocolFamily#INET}
     */
    public abstract DatagramChannel openDatagramChannel(ProtocolFamily family) throws IOException;

    /** Un pipe nuevo, con sus dos puntas. */
    public abstract Pipe openPipe() throws IOException;

    /** Un selector nuevo. */
    public abstract AbstractSelector openSelector() throws IOException;

    /** Un canal de escucha nuevo, todavia sin atar. */
    public abstract ServerSocketChannel openServerSocketChannel() throws IOException;

    /**
     * Un canal de escucha nuevo de la familia `family`.
     *
     * <p>Concreta y no abstracta a proposito, igual que el JDK: se agrego a la API mucho despues que
     * el resto, y hacerla abstracta habria roto todo proveedor ya escrito. La omision tira, que es
     * la respuesta correcta para un proveedor que no conoce familias.
     *
     * @throws UnsupportedOperationException siempre, salvo que el proveedor la redefina
     */
    public ServerSocketChannel openServerSocketChannel(ProtocolFamily family) throws IOException {
        throw new UnsupportedOperationException("Protocol family not supported");
    }

    /** Un canal de socket nuevo, todavia sin conectar. */
    public abstract SocketChannel openSocketChannel() throws IOException;

    /**
     * Un canal de socket nuevo de la familia `family`.
     *
     * <p>Vale la misma nota que {@link #openServerSocketChannel(ProtocolFamily)}.
     *
     * @throws UnsupportedOperationException siempre, salvo que el proveedor la redefina
     */
    public SocketChannel openSocketChannel(ProtocolFamily family) throws IOException {
        throw new UnsupportedOperationException("Protocol family not supported");
    }

    /**
     * El canal heredado del proceso que lanzo a este, si lo hay.
     *
     * <p>Devuelve `null`, que es la respuesta correcta y no un hueco: es lo que el JDK devuelve
     * cuando no se heredo ninguno, y esta VM nunca hereda ninguno porque no se la arranca desde un
     * `inetd`. Un `null` aca significa exactamente lo que el contrato dice que significa.
     */
    public Channel inheritedChannel() throws IOException {
        return null;
    }
}
