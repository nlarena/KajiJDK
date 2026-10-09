package javax.naming;

import java.util.Enumeration;

/**
 * An `Enumeration` that can fail **while** being walked, and that has to be closed.
 *
 * <p>Listing a context is not walking an in-memory collection: it is fetching results from a server
 * piece by piece. That breaks both assumptions of `Enumeration`: that advancing does not fail and
 * that dropping the reference is enough. Hence the three methods.
 *
 * <p>`next()` and `hasMore()` are `nextElement()` and `hasMoreElements()` **with `throws`**. The
 * inherited ones still exist because the interface extends `Enumeration` and there is old code that
 * uses it that way; when those fail they have to wrap the `NamingException` in an unchecked one,
 * which is exactly the problem the new methods are there to avoid. If you can choose, use
 * `hasMore`/`next`.
 *
 * <p>`close()` frees whatever there is on the server side. Walking to the end also closes;
 * `close()` is for whoever stops early, which is the common case when looking for a single entry.
 */
public interface NamingEnumeration<T> extends Enumeration<T> {

    T next() throws NamingException;

    boolean hasMore() throws NamingException;

    void close() throws NamingException;
}
