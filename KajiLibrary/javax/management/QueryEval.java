package javax.management;

import java.io.Serializable;

/**
 * The base of the query expressions: it carries the server the attributes are resolved against.
 *
 * <p>The server lives in a <b>static</b> {@code ThreadLocal}, and that is what has to be understood
 * about this class. The obvious alternative --a field per expression-- would force walking the
 * whole tree setting it on every node before each evaluation, and would also mean the same query
 * could not be evaluated on two threads at once. With the {@code ThreadLocal}, whoever evaluates
 * sets it once and every node finds it, each thread its own.
 *
 * <p>That is also why {@code setMBeanServer} is deprecated in {@link QueryExp} and in
 * {@link ValueExp}: there is no need to call it.
 */
public abstract class QueryEval implements Serializable {

    private static final long serialVersionUID = 2675899265640874796L;

    private static ThreadLocal<MBeanServer> server = new ThreadLocal<MBeanServer>();

    /** Sets the server <b>of this thread</b>. */
    public void setMBeanServer(MBeanServer s) {
        server.set(s);
    }

    /** This thread's server, or {@code null} if nobody set it. */
    public static MBeanServer getMBeanServer() {
        return server.get();
    }
}
