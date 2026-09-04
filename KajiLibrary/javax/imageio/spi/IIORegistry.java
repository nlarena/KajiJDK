package javax.imageio.spi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * KajiLibrary's javax.imageio.spi.IIORegistry -- el registro que usa {@code ImageIO}.
 *
 * <p>Un {@link ServiceRegistry} con las seis categorias de {@code javax.imageio} ya declaradas, y con
 * los proveedores del sistema ya cargados.
 *
 * <p>Es el que {@code ImageIO} consulta para todo. Registrar un lector aca es lo que lo hace visible
 * para {@code ImageIO.read}.
 *
 * <h2>Una instancia por grupo de hilos</h2>
 *
 * <p>{@link #getDefaultInstance} no devuelve un unico registro global. En el JDK hay uno por
 * {@code ThreadGroup}, para que dos aplicaciones que compartan maquina virtual --lo que antes eran los
 * applets-- no se vean los complementos.
 *
 * <p>Esta biblioteca no tiene ese aislamiento y devuelve siempre el mismo, que es lo que corresponde
 * cuando hay un solo grupo: el comportamiento observable es identico salvo que alguien cree grupos a
 * proposito para separarse.
 *
 * <h2>{@link #registerApplicationClasspathSpis}</h2>
 *
 * <p>Vuelve a recorrer la ruta de clases buscando proveedores declarados como servicio. Existe para
 * los casos en que aparecen despues de arrancar -- un cargador de clases nuevo, un complemento que se
 * agrega en caliente.
 */
public final class IIORegistry extends ServiceRegistry {

    /** Las seis categorias de {@code javax.imageio}. */
    private static final Class<?>[] CATEGORIES = {
        ImageReaderSpi.class,
        ImageWriterSpi.class,
        ImageTranscoderSpi.class,
        ImageInputStreamSpi.class,
        ImageOutputStreamSpi.class,
    };

    /** El registro; ver la nota de la clase. */
    private static IIORegistry theRegistry = null;

    /** Se llega por {@link #getDefaultInstance}. */
    private IIORegistry() {
        super(categoryIterator());
        registerStandardSpis();
        registerApplicationClasspathSpis();
    }

    /**
     * Registra los cuatro proveedores de flujo que vienen de fabrica.
     *
     * <p>Son los que hacen que {@code ImageIO.createImageInputStream} funcione sobre un {@code File} o
     * un {@code InputStream} sin que nadie instale nada. No son complementos de formato: no decodifican
     * ninguna imagen, solo envuelven.
     */
    private void registerStandardSpis() {
        registerServiceProvider(new FileImageInputStreamSpi());
        registerServiceProvider(new InputStreamImageInputStreamSpi());
        registerServiceProvider(new FileImageOutputStreamSpi());
        registerServiceProvider(new OutputStreamImageOutputStreamSpi());
    }

    /** El registro que usa {@code ImageIO}. Ver la nota de la clase. */
    public static IIORegistry getDefaultInstance() {
        synchronized (IIORegistry.class) {
            if (theRegistry == null) {
                theRegistry = new IIORegistry();
            }
            return theRegistry;
        }
    }

    /**
     * Vuelve a buscar proveedores declarados como servicio. Ver la nota de la clase.
     *
     * <p>Un proveedor que falle al cargarse se saltea: uno roto no puede impedir que se registren los
     * demas.
     */
    public void registerApplicationClasspathSpis() {
        ClassLoader loader = null;
        try {
            loader = Thread.currentThread().getContextClassLoader();
        } catch (Throwable e) {
            // Sin cargador de contexto se usa el que ServiceLoader elija.
        }
        int i = 0;
        while (i < CATEGORIES.length) {
            registerFound(CATEGORIES[i], loader);
            i = i + 1;
        }
    }

    /** Registra los de esa categoria, salteando los que no carguen. */
    private void registerFound(Class<?> category, ClassLoader loader) {
        try {
            Iterator<?> it;
            if (loader != null) {
                it = ServiceRegistry.lookupProviders(category, loader);
            } else {
                it = ServiceRegistry.lookupProviders(category);
            }
            while (it.hasNext()) {
                try {
                    registerServiceProvider(it.next());
                } catch (Throwable e) {
                    // Ese proveedor no carga; se sigue con los demas.
                }
            }
        } catch (Throwable e) {
            // Ni siquiera se pudo abrir el cargador de servicios para esta categoria.
        }
    }

    /** Las categorias, como iterador. */
    private static Iterator<Class<?>> categoryIterator() {
        List<Class<?>> list = new ArrayList<Class<?>>();
        int i = 0;
        while (i < CATEGORIES.length) {
            list.add(CATEGORIES[i]);
            i = i + 1;
        }
        return list.iterator();
    }
}
