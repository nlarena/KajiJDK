package javax.naming.spi;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.spi.InitialContextFactory -- where the initial context comes from.
 *
 * <p>The entry point of a whole JNDI provider. The application puts the name of the class that
 * implements it in the {@code java.naming.factory.initial} property, and everything else --the
 * {@code InitialContext}, its lookups, its subcontexts-- comes from what this method returns.
 *
 * <p>An implementation must have a public no-argument constructor: the platform loads it by name
 * and instantiates it by reflection. (In this library, {@code NamingManager.getInitialContext}
 * does that, but {@code javax.naming.InitialContext} never calls it; see its class header.)
 */
public interface InitialContextFactory {

    /**
     * The initial context for that environment.
     *
     * @throws NamingException if the environment is not enough to create it
     */
    Context getInitialContext(Hashtable<?, ?> environment) throws NamingException;
}
