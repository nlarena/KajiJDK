package javax.naming.ldap;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;

/**
 * Turns a raw {@link Control} --OID and bytes-- into the type that knows how to interpret it.
 *
 * <h2>Why the step is needed</h2>
 *
 * <p>Because the LDAP provider receives an OID and a byte array from the server, and has no reason
 * to know what they mean: anyone can define a control. What it does is build a
 * {@link BasicControl} and ask the registered factories whether any recognizes it.
 *
 * <p>The first one that returns something other than {@code null} wins; if none recognizes the
 * control, the raw one remains -- still usable, just without meaningful accessors.
 *
 * <p>It is the same pattern that {@link ExtendedRequest#createExtendedResponse} solves on the other
 * side: whoever defined the extension is the only one who knows how to interpret it.
 */
public abstract class ControlFactory {

    /** For the implementations. */
    protected ControlFactory() {
    }

    /**
     * Interprets the control, or returns {@code null} if it does not recognize it.
     *
     * <p>Returning {@code null} is the normal answer: a factory recognizes one or two OIDs and has
     * no opinion on the rest.
     */
    public abstract Control getControlInstance(Control ctl) throws NamingException;

    /**
     * Tries all the registered factories.
     *
     * @param env the environment, where {@link LdapContext#CONTROL_FACTORIES} comes from
     * @return the interpreted control, or the same one that came in if nobody recognized it
     */
    public static Control getControlInstance(Control ctl, Context ctx, Hashtable<?, ?> env)
            throws NamingException {
        Object prop = env == null ? null : env.get(LdapContext.CONTROL_FACTORIES);
        if (prop == null) {
            return ctl;
        }
        java.util.StringTokenizer st = new java.util.StringTokenizer(prop.toString(), ":");
        while (st.hasMoreTokens()) {
            String className = st.nextToken();
            try {
                Class<?> c = Class.forName(className, true, ClassLoader.getSystemClassLoader());
                ControlFactory f = (ControlFactory) c.getDeclaredConstructor().newInstance();
                Control r = f.getControlInstance(ctl);
                if (r != null) {
                    return r;
                }
            } catch (NamingException e) {
                throw e;
            } catch (Exception e) {
                // A factory that does not load does not invalidate the following ones: the list's
                // order is a preference, not a dependency.
                continue;
            }
        }
        return ctl;
    }
}
