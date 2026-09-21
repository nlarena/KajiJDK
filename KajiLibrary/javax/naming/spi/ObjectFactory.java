package javax.naming.spi;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;

/**
 * KajiLibrary's javax.naming.spi.ObjectFactory -- turns what is stored into a useful object.
 *
 * <p>A directory does not store Java objects: it stores a {@code Reference} --the name of a class
 * and some data-- or something from the protocol underneath. This factory is what turns that into
 * the object the application expects to get from a {@code lookup}.
 *
 * <p>It is what allows storing something like a data source in LDAP: what is stored is the recipe
 * --the driver, the URL, the user-- and what is received is the ready-built source.
 *
 * <p>Returning <b>null</b> is normal and not an error: it means "this is not mine", and the
 * platform asks the next factory. A factory that returns something for everything breaks the
 * chain.
 */
public interface ObjectFactory {

    /**
     * The object that corresponds to that data.
     *
     * @param obj what was stored
     * @param name its name relative to {@code nameCtx}, or null
     * @param nameCtx the context the name is relative to; null is the initial one
     * @param environment the operation's environment
     * @return null if this factory does not recognize that data; see the class note
     */
    Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment)
        throws Exception;
}
