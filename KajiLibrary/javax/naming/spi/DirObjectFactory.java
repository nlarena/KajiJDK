package javax.naming.spi;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.directory.Attributes;

/**
 * KajiLibrary's javax.naming.spi.DirObjectFactory -- an {@link ObjectFactory} that also sees the
 * attributes.
 *
 * <p>It adds an overload with {@link Attributes}, and it is not a convenience: in a directory,
 * <b>what distinguishes</b> an entry is usually in its attributes --its object class, its fields--
 * and not in what {@code lookup} returns as the value. Without the attributes, the factory has
 * nothing to decide with whether the entry is its business.
 *
 * <p>It also saves a round trip to the server: the platform already read them to resolve the name,
 * and passing them spares the factory asking for them again.
 *
 * <p>The method inherited from {@link ObjectFactory} still exists and is called when there are no
 * attributes to pass.
 */
public interface DirObjectFactory extends ObjectFactory {

    /**
     * The object that corresponds to that data and those attributes.
     *
     * @param attrs the entry's, or null if they were not read
     * @return null if this factory does not recognize them
     */
    Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment,
                             Attributes attrs) throws Exception;
}
