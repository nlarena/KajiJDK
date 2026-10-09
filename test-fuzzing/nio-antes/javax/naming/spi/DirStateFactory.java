package javax.naming.spi;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;

/**
 * KajiLibrary's javax.naming.spi.DirStateFactory -- a {@link StateFactory} that also produces
 * attributes.
 *
 * <p>The flip side of {@link DirObjectFactory}. What it returns is not an object but a
 * {@link Result}: the value to store <b>and</b> the attributes to store it with.
 *
 * <p>They have to be two things because in a directory both are written together and atomically.
 * Returning only the object would force a second {@code modifyAttributes}, and between the two
 * calls the entry would exist without its object class -- which is exactly what the schema forbids.
 */
public interface DirStateFactory extends StateFactory {

    /**
     * What to store and with which attributes.
     *
     * @param inAttrs the ones that were going to be written, or null
     * @return null if this factory does not recognize the object
     */
    Result getStateToBind(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment,
                          Attributes inAttrs) throws NamingException;

    /**
     * The pair {@link DirStateFactory#getStateToBind} returns.
     *
     * <p>Immutable and logic-free: it exists only because Java has no tuples. Either field may be
     * null, and that means "use what you already had".
     */
    public static class Result {

        /** What to store. */
        private final Object obj;

        /** With which attributes. */
        private final Attributes attrs;

        /**
         * @param obj the value to store, or null to keep the original
         * @param outAttrs the attributes, or null to keep the ones there were
         */
        public Result(Object obj, Attributes outAttrs) {
            this.obj = obj;
            this.attrs = outAttrs;
        }

        /** The value to store. */
        public Object getObject() {
            return this.obj;
        }

        /** The attributes. */
        public Attributes getAttributes() {
            return this.attrs;
        }
    }
}
