package javax.naming.spi;

import java.util.Hashtable;
import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.spi.InitialContextFactoryBuilder -- decides which provider is used.
 *
 * <p>The same as {@link ObjectFactoryBuilder} but one level up: once one is installed, the {@code
 * java.naming.factory.initial} property <b>is no longer looked at</b> by the platform and this
 * builder decides the provider of every initial context.
 *
 * <p>It is what an application container uses so that each application sees its own JNDI tree
 * without any being able to ask for another's. It is installed once per process, and that
 * uniqueness is precisely what makes it reliable as a boundary.
 */
public interface InitialContextFactoryBuilder {

    /**
     * The initial context factory for that environment.
     *
     * @throws NamingException if none can be created
     */
    InitialContextFactory createInitialContextFactory(Hashtable<?, ?> environment)
        throws NamingException;
}
