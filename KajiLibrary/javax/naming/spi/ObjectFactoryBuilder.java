package javax.naming.spi;

import java.util.Hashtable;
import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.spi.ObjectFactoryBuilder -- decides which factory serves each piece of
 * data.
 *
 * <p>One more level of indirection over {@link ObjectFactory}, and it serves to take control from
 * the platform: once a builder is installed, the default lookup --which reads the class name from
 * the {@code Reference} itself and loads it-- <b>is no longer used</b>.
 *
 * <p>That is what makes it interesting and what makes it dangerous. Loading a class named by the
 * data stored in the directory is running code chosen by whoever wrote in the directory;
 * installing a builder of your own is the way to cut that at the root.
 *
 * <p>It is installed <b>once per process</b> with {@link NamingManager#setObjectFactoryBuilder},
 * and the second attempt fails.
 */
public interface ObjectFactoryBuilder {

    /**
     * The factory that serves that data.
     *
     * @throws NamingException if none can be created
     */
    ObjectFactory createObjectFactory(Object obj, Hashtable<?, ?> environment)
        throws NamingException;
}
