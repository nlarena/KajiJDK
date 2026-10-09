package javax.naming.spi;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.spi.StateFactory -- turns an object into something that can be
 * stored.
 *
 * <p>The reverse path of {@link ObjectFactory}: the application passes an object to {@code bind}
 * and this factory turns it into what the directory knows how to store --typically a {@code
 * Reference}.
 *
 * <p>That they are two interfaces and not one with two methods is on purpose: storing and
 * retrieving are usually done by different people. Whoever publishes a service writes the state
 * one; whoever consumes it needs the object one, which almost always comes with the service's
 * library.
 *
 * <p>Returning null means "not mine" and the platform carries on with the next, same as on the
 * other side.
 */
public interface StateFactory {

    /**
     * What to store in place of that object.
     *
     * @return null if this factory does not recognize it
     */
    Object getStateToBind(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment)
        throws NamingException;
}
