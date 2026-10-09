package javax.naming.spi;

import java.util.Hashtable;
import javax.naming.CannotProceedException;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

/**
 * KajiLibrary's javax.naming.spi.DirectoryManager -- the same as {@link NamingManager}, with
 * attributes.
 *
 * <p>It extends {@link NamingManager} and adds the three operations that need to see an entry's
 * attributes. The inheritance is for convenience --it is inherited to have the static methods at
 * hand under a single name-- and means nothing more: everything in both classes is static.
 *
 * <p>The overloads with {@link Attributes} prefer the factories that understand them --
 * {@link DirObjectFactory} and {@link DirStateFactory}-- and fall back to the common ones when
 * there are none. That order matters: a factory that ignores the attributes may accept an entry
 * that is not its business, and in a directory the attributes are what identifies it.
 */
public class DirectoryManager extends NamingManager {

    /**
     * Package-private, as in the JDK: the class is static methods only. (An earlier note said
     * public for compatibility.)
     */
    DirectoryManager() {
    }

    /**
     * The directory context where an interrupted operation carries on.
     *
     * <p>Unlike the JDK, which wraps the exception in a {@code DirContext} that resolves the
     * continuation lazily (and never throws {@code NotContextException} here), this asks
     * {@link #getContinuationContext} right away and checks the result.
     *
     * @throws NotContextException if what it resolves to is not a {@link DirContext}
     */
    public static DirContext getContinuationDirContext(CannotProceedException cpe)
        throws NamingException {
        Context ctx = getContinuationContext(cpe);
        if (ctx instanceof DirContext) {
            return (DirContext) ctx;
        }
        throw new NotContextException(
            "Not an instance of DirContext: " + ctx.getClass().getName());
    }

    /**
     * The object that corresponds to that data and those attributes.
     *
     * <p>See the order of preference in the class note.
     *
     * @return the object, or {@code refInfo} as is if no factory recognized it
     */
    public static Object getObjectInstance(Object refInfo, Name name, Context nameCtx,
                                           Hashtable<?, ?> environment, Attributes attrs)
        throws Exception {
        String list = property(environment, Context.OBJECT_FACTORIES);
        if (list != null) {
            String[] names = list.split(":");
            int i = 0;
            while (i < names.length) {
                Object factory = instantiate(names[i].trim());
                if (factory instanceof DirObjectFactory) {
                    Object made = ((DirObjectFactory) factory)
                        .getObjectInstance(refInfo, name, nameCtx, environment, attrs);
                    if (made != null) {
                        return made;
                    }
                } else if (factory instanceof ObjectFactory) {
                    Object made = ((ObjectFactory) factory)
                        .getObjectInstance(refInfo, name, nameCtx, environment);
                    if (made != null) {
                        return made;
                    }
                }
                i = i + 1;
            }
        }
        // Without directory factories, the common path applies: it includes the reference's own.
        return NamingManager.getObjectInstance(refInfo, name, nameCtx, environment);
    }

    /**
     * What to store and with which attributes.
     *
     * @param inAttrs the ones that were going to be written, or null
     * @return never null: if no factory recognizes the object, what came in is returned
     */
    public static DirStateFactory.Result getStateToBind(Object obj, Name name, Context nameCtx,
                                                        Hashtable<?, ?> environment,
                                                        Attributes inAttrs)
        throws NamingException {
        String list = property(environment, Context.STATE_FACTORIES);
        if (list != null) {
            String[] names = list.split(":");
            int i = 0;
            while (i < names.length) {
                Object factory = instantiate(names[i].trim());
                if (factory instanceof DirStateFactory) {
                    DirStateFactory.Result made = ((DirStateFactory) factory)
                        .getStateToBind(obj, name, nameCtx, environment, inAttrs);
                    if (made != null) {
                        return made;
                    }
                } else if (factory instanceof StateFactory) {
                    Object made = ((StateFactory) factory)
                        .getStateToBind(obj, name, nameCtx, environment);
                    if (made != null) {
                        return new DirStateFactory.Result(made, inAttrs);
                    }
                }
                i = i + 1;
            }
        }
        return new DirStateFactory.Result(obj, inAttrs);
    }

    /**
     * Instantiates that class, or null.
     *
     * <p>It duplicates {@link NamingManager}'s private helper because that one is private. (An
     * earlier note said the JDK does not share it either; the JDK 25 does, through
     * {@code com.sun.naming.internal.NamingManagerHelper}.) It swallows the failure for the same
     * reason: a factory that does not load is one factory fewer.
     */
    private static Object instantiate(String className) {
        try {
            Class<?> found = Class.forName(className, true, contextLoader());
            return found.getConstructor(new Class<?>[0]).newInstance(new Object[0]);
        } catch (Exception e) {
            return null;
        }
    }
}
