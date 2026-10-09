package java.net.spi;

import java.net.URLStreamHandlerFactory;

/**
 * KajiLibrary's java.net.spi.URLStreamHandlerProvider -- new URL protocols.
 *
 * <p>It declares nothing of its own: it inherits {@link URLStreamHandlerFactory}'s only method and
 * adds no more than being loadable as a service. That is the whole difference from the old factory,
 * and it is a real difference: the factory is installed by calling
 * {@code URL.setURLStreamHandlerFactory}, which can be called <b>once only per process</b>, so the
 * first library that used it left every other one out. As a service, each registers its own and the
 * platform asks them all in order.
 *
 * <p>A provider returns null for the protocols it is not interested in, and there the next one is
 * asked.
 */
public abstract class URLStreamHandlerProvider implements URLStreamHandlerFactory {

    /** For the subclasses. */
    protected URLStreamHandlerProvider() {
    }
}
