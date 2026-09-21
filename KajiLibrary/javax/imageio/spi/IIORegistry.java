package javax.imageio.spi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * KajiLibrary's javax.imageio.spi.IIORegistry -- the registry {@code ImageIO} uses.
 *
 * <p>A {@link ServiceRegistry} with the five {@code javax.imageio} categories already declared, and
 * with the system providers already loaded.
 *
 * <p>It is the one {@code ImageIO} consults for everything. Registering a reader here is what makes
 * it visible to {@code ImageIO.read}.
 *
 * <h2>One instance per thread group</h2>
 *
 * <p>{@link #getDefaultInstance} does not return a single global registry. In the JDK there is one
 * per {@code ThreadGroup}, so that two applications sharing a virtual machine --what applets used
 * to be-- do not see each other's plug-ins.
 *
 * <p>This library has no such isolation and always returns the same one, which is what fits when
 * there is a single group: the observable behaviour is identical unless someone creates groups on
 * purpose to keep apart.
 *
 * <h2>{@link #registerApplicationClasspathSpis}</h2>
 *
 * <p>It walks the class path again looking for providers declared as services. It exists for the
 * cases where they appear after startup -- a new class loader, a plug-in added at run time.
 */
public final class IIORegistry extends ServiceRegistry {

    /** The five {@code javax.imageio} categories. (An earlier note said six.) */
    private static final Class<?>[] CATEGORIES = {
        ImageReaderSpi.class,
        ImageWriterSpi.class,
        ImageTranscoderSpi.class,
        ImageInputStreamSpi.class,
        ImageOutputStreamSpi.class,
    };

    /** The registry; see the class note. */
    private static IIORegistry theRegistry = null;

    /** Reached through {@link #getDefaultInstance}. */
    private IIORegistry() {
        super(categoryIterator());
        registerStandardSpis();
        registerApplicationClasspathSpis();
    }

    /**
     * Registers the four stream providers that come by default.
     *
     * <p>They are the ones that make {@code ImageIO.createImageInputStream} work over a {@code
     * File} or an {@code InputStream} without anybody installing anything. They are not format
     * plug-ins: they decode no image, they only wrap.
     */
    private void registerStandardSpis() {
        registerServiceProvider(new FileImageInputStreamSpi());
        registerServiceProvider(new InputStreamImageInputStreamSpi());
        registerServiceProvider(new FileImageOutputStreamSpi());
        registerServiceProvider(new OutputStreamImageOutputStreamSpi());
    }

    /** The registry {@code ImageIO} uses. See the class note. */
    public static IIORegistry getDefaultInstance() {
        synchronized (IIORegistry.class) {
            if (theRegistry == null) {
                theRegistry = new IIORegistry();
            }
            return theRegistry;
        }
    }

    /**
     * Looks again for providers declared as services. See the class note.
     *
     * <p>A provider that fails to load is skipped: a broken one cannot prevent the rest from being
     * registered.
     */
    public void registerApplicationClasspathSpis() {
        ClassLoader loader = null;
        try {
            loader = Thread.currentThread().getContextClassLoader();
        } catch (Throwable e) {
            // Without a context class loader, whichever ServiceLoader picks is used.
        }
        int i = 0;
        while (i < CATEGORIES.length) {
            registerFound(CATEGORIES[i], loader);
            i = i + 1;
        }
    }

    /** Registers the ones of that category, skipping those that do not load. */
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
                    // That provider does not load; carry on with the rest.
                }
            }
        } catch (Throwable e) {
            // The service loader could not even be opened for this category.
        }
    }

    /** The categories, as an iterator. */
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
