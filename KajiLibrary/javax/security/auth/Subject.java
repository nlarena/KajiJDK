package javax.security.auth;

import java.io.Serializable;
import java.security.AccessControlContext;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionException;

/**
 * KajiLibrary's javax.security.auth.Subject -- who is acting: their identities and their
 * credentials.
 *
 * <p>A Subject brings together three sets and the distinction between them is the whole point of
 * the class:
 *
 * <ul>
 *   <li><b>principals</b>: the identities. The same Subject may be {@code cn=john} in X.500,
 *       {@code john@ACME} in Kerberos and {@code uid=1000} in the system, all at once. There is no
 *       "main" one -- they are all equally valid and each service looks at the one it understands.
 *   <li><b>public credentials</b>: what proves an identity and can be shown. A certificate.
 *   <li><b>private credentials</b>: what proves an identity and <b>cannot</b> be shown. A private
 *       key, a ticket. The separation exists so that a different permission can be required to read
 *       them.
 * </ul>
 *
 * <h2>The sets are live views</h2>
 *
 * <p>{@code getPrincipals()} does not return a copy: it returns the set, and adding there adds to
 * the Subject. That is on purpose -- it is how a Subject is put together -- and it brings two
 * surprising behaviours that are reproduced as they are:
 *
 * <ul>
 *   <li>Putting something that is not a {@code Principal} into the principals set throws {@code
 *       SecurityException}, not {@code ClassCastException}: the set defends itself at run time
 *       because generics are erased and the list may arrive raw.
 *   <li>The <b>four-argument constructor</b>, on the other hand, does <b>not</b> check that type.
 *       That is the JDK's too, and for the same reason: the constructor copies the elements without
 *       casting them, so a raw set with garbage inside gets in without protest. The difference
 *       between the two doors is an inherited oddity, not an oversight of this implementation.
 * </ul>
 *
 * <p>{@code setReadOnly()} is one way only: there is no going back. Without it, passing a Subject
 * to someone else's code would be giving it permission to add identities to itself.
 *
 * <h2>current(), callAs() and doAs()</h2>
 *
 * <p>{@code callAs} binds the Subject to the thread while the action runs, and {@code current()}
 * returns it. The JDK uses a {@code ScopedValue}; here it is a {@code ThreadLocal} restored in a
 * {@code finally}, which gives the same observable behaviour within one thread -- nesting included,
 * where the inner one hides the outer one and on leaving the outer one comes back --. <b>The noted
 * difference</b>: a {@code ScopedValue} is inherited by the threads structured concurrency starts
 * and a {@code ThreadLocal} is not, so a thread launched inside a {@code callAs} sees {@code null}
 * where the JDK could show it the Subject.
 *
 * <p>{@code doAs} and {@code doAsPrivileged} are deprecated in the JDK and marked for removal, but
 * <b>they still work</b> and still bind the Subject just like {@code callAs}; the only thing that
 * changes is how exceptions are wrapped. {@code getSubject(AccessControlContext)} is the only one
 * that no longer works: it throws {@code UnsupportedOperationException}, because its contract was
 * reading the Subject from an access control context and that mechanism no longer exists.
 */
public final class Subject implements Serializable {

    private static final long serialVersionUID = -8308522755600156056L;

    // A ThreadLocal and not a ScopedValue: see the class note about the difference.
    private static final ThreadLocal<Subject> CURRENT = new ThreadLocal<Subject>();

    // Lists and not sets: the insertion order is the one that comes out through `toString()`, and
    // the elements' equality is decided by each one's `equals`, not by a hash.
    private final List<Object> principals = new ArrayList<Object>();
    private final List<Object> pubCredentials = new ArrayList<Object>();
    private final List<Object> privCredentials = new ArrayList<Object>();

    private volatile boolean readOnly = false;

    /** An empty, modifiable Subject. */
    public Subject() {
    }

    /**
     * A Subject with those three sets, copied.
     *
     * <p>The sets are copied: changing them afterwards does not change the Subject. The elements
     * are <b>not</b> checked -- see the class note --, but none may be null.
     *
     * @throws NullPointerException if any set or any element is null
     */
    public Subject(boolean readOnly, Set<? extends Principal> principals,
            Set<?> pubCredentials, Set<?> privCredentials) {
        copy(principals, this.principals);
        copy(pubCredentials, this.pubCredentials);
        copy(privCredentials, this.privCredentials);
        this.readOnly = readOnly;
    }

    private static void copy(Collection<?> from, List<Object> into) {
        if (from == null) {
            throw new NullPointerException("invalid null input(s)");
        }
        Iterator<?> it = from.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if (o == null) {
                throw new NullPointerException("invalid null input(s)");
            }
            if (!into.contains(o)) {
                into.add(o);
            }
        }
    }

    /** Freezes the Subject. There is no going back. */
    public void setReadOnly() {
        this.readOnly = true;
    }

    public boolean isReadOnly() {
        return this.readOnly;
    }

    /** The identities. Live view: adding here adds to the Subject. */
    public Set<Principal> getPrincipals() {
        return new SecureSet<Principal>(this, this.principals, true);
    }

    /** The public credentials. Live view. */
    public Set<Object> getPublicCredentials() {
        return new SecureSet<Object>(this, this.pubCredentials, false);
    }

    /** The private credentials. Live view. */
    public Set<Object> getPrivateCredentials() {
        return new SecureSet<Object>(this, this.privCredentials, false);
    }

    /**
     * The identities of that class or a subclass. A <b>copy</b>, unlike {@link #getPrincipals()}:
     * adding to what comes out of here does not add to the Subject.
     *
     * @throws NullPointerException if the class is null
     */
    public <T extends Principal> Set<T> getPrincipals(Class<T> c) {
        return filterByClass(this.principals, c);
    }

    /** The public credentials of that class or subclass. A copy. */
    public <T> Set<T> getPublicCredentials(Class<T> c) {
        return filterByClass(this.pubCredentials, c);
    }

    /** The private credentials of that class or subclass. A copy. */
    public <T> Set<T> getPrivateCredentials(Class<T> c) {
        return filterByClass(this.privCredentials, c);
    }

    private static <T> Set<T> filterByClass(List<Object> list, Class<T> c) {
        if (c == null) {
            throw new NullPointerException("invalid null Class provided");
        }
        Set<T> out = new HashSet<T>();
        synchronized (list) {
            int i = 0;
            while (i < list.size()) {
                Object o = list.get(i);
                if (c.isInstance(o)) {
                    out.add(c.cast(o));
                }
                i = i + 1;
            }
        }
        return out;
    }

    /** The Subject bound to the current thread, or null if there is none. */
    public static Subject current() {
        return CURRENT.get();
    }

    /**
     * Runs the action with this Subject bound to the thread.
     *
     * <p>It wraps <b>any</b> exception of the action in {@code CompletionException}, runtime ones
     * included. It is what the JDK does and it has to be kept in mind: an
     * {@code IllegalStateException} seen inside comes out of here as something else.
     *
     * @throws NullPointerException if the action is null
     */
    public static <T> T callAs(Subject subject, Callable<T> action) throws CompletionException {
        if (action == null) {
            throw new NullPointerException();
        }
        Subject previous = CURRENT.get();
        CURRENT.set(subject);
        try {
            return action.call();
        } catch (Exception e) {
            throw new CompletionException(e);
        } finally {
            // In a `finally` and restoring the previous one --not clearing--: it is what makes
            // nesting two `callAs` leave the outer one intact on coming back from the inner one.
            restore(previous);
        }
    }

    /**
     * Runs the action with this Subject bound to the thread.
     *
     * @deprecated the access control mechanism it belonged to no longer exists; use {@link
     *     #callAs}. It still works and still binds the Subject.
     */
    @Deprecated
    public static <T> T doAs(Subject subject, PrivilegedAction<T> action) {
        if (action == null) {
            throw new NullPointerException("invalid null action provided");
        }
        Subject previous = CURRENT.get();
        CURRENT.set(subject);
        try {
            return action.run();
        } finally {
            restore(previous);
        }
    }

    /**
     * Likewise, for an action that may throw.
     *
     * <p>It wraps only the <b>declared</b> exceptions: a {@code RuntimeException} comes out as it
     * is. It is the difference from {@link #callAs}, which wraps everything.
     *
     * @deprecated see {@link #doAs(Subject, PrivilegedAction)}
     */
    @Deprecated
    public static <T> T doAs(Subject subject, PrivilegedExceptionAction<T> action)
            throws PrivilegedActionException {
        if (action == null) {
            throw new NullPointerException("invalid null action provided");
        }
        Subject previous = CURRENT.get();
        CURRENT.set(subject);
        try {
            return action.run();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new PrivilegedActionException(e);
        } finally {
            restore(previous);
        }
    }

    /**
     * Same as {@link #doAs(Subject, PrivilegedAction)}.
     *
     * <p>The {@code AccessControlContext} is ignored, and it is not a shortcut of this
     * implementation: without a security manager there is no context to combine, so in the JDK it
     * stops mattering too.
     *
     * @deprecated see {@link #doAs(Subject, PrivilegedAction)}
     */
    @Deprecated
    public static <T> T doAsPrivileged(Subject subject, PrivilegedAction<T> action,
            AccessControlContext acc) {
        return doAs(subject, action);
    }

    /**
     * Same as {@link #doAs(Subject, PrivilegedExceptionAction)}.
     *
     * @deprecated see {@link #doAs(Subject, PrivilegedAction)}
     */
    @Deprecated
    public static <T> T doAsPrivileged(Subject subject, PrivilegedExceptionAction<T> action,
            AccessControlContext acc) throws PrivilegedActionException {
        return doAs(subject, action);
    }

    private static void restore(Subject previous) {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }

    /**
     * It cannot: this method's contract was reading the Subject from an access control context, and
     * that mechanism no longer exists.
     *
     * <p>It throws instead of returning null on purpose. Null would read as "no Subject is acting",
     * which is an answer -- and the wrong one. Use {@link #current()}.
     *
     * @throws UnsupportedOperationException always
     * @deprecated it has no direct replacement; use {@link #current()}
     */
    @Deprecated
    public static Subject getSubject(AccessControlContext acc) {
        throw new UnsupportedOperationException("getSubject is not supported");
    }

    /** Two Subjects are the same if they have the same three sets. */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Subject)) {
            return false;
        }
        Subject other = (Subject) o;
        return sameElements(this.principals, other.principals)
            && sameElements(this.pubCredentials, other.pubCredentials)
            && sameElements(this.privCredentials, other.privCredentials);
    }

    private static boolean sameElements(List<Object> a, List<Object> b) {
        if (a.size() != b.size()) {
            return false;
        }
        int i = 0;
        while (i < a.size()) {
            if (!b.contains(a.get(i))) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /**
     * XOR of the hashes of the elements of the three sets.
     *
     * <p>It has to be an XOR and not a positional sum: {@code equals} does not look at the order,
     * so the hash cannot either.
     */
    @Override
    public int hashCode() {
        return hashOf(this.principals) ^ hashOf(this.pubCredentials) ^ hashOf(this.privCredentials);
    }

    private static int hashOf(List<Object> list) {
        int h = 0;
        int i = 0;
        while (i < list.size()) {
            h = h ^ list.get(i).hashCode();
            i = i + 1;
        }
        return h;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Subject:\n");
        listEach(sb, this.principals, "\tPrincipal: ");
        listEach(sb, this.pubCredentials, "\tPublic Credential: ");
        listEach(sb, this.privCredentials, "\tPrivate Credential: ");
        return sb.toString();
    }

    private static void listEach(StringBuilder sb, List<Object> list, String label) {
        int i = 0;
        while (i < list.size()) {
            sb.append(label).append(list.get(i)).append("\n");
            i = i + 1;
        }
    }

    /**
     * The live view of one of the three sets.
     *
     * <p>It does two things a normal {@code Set} does not: it rejects writes if the Subject is
     * frozen, and --in the identities set-- it rejects what is not a {@code Principal}. Both with
     * the exception the JDK uses, which in the second case is not the one one would expect.
     */
    private static final class SecureSet<E> extends AbstractSet<E> {

        private final Subject owner;
        private final List<Object> list;
        private final boolean ofPrincipals;

        SecureSet(Subject owner, List<Object> list, boolean ofPrincipals) {
            this.owner = owner;
            this.list = list;
            this.ofPrincipals = ofPrincipals;
        }

        @Override
        public int size() {
            return this.list.size();
        }

        @Override
        public boolean contains(Object o) {
            return this.list.contains(o);
        }

        @Override
        public boolean add(E o) {
            if (this.owner.readOnly) {
                throw new IllegalStateException("Subject is read-only");
            }
            if (o == null) {
                throw new NullPointerException("invalid null input(s)");
            }
            // SecurityException and not ClassCastException: it is what the JDK throws. The check
            // exists because the generic is erased and the list may arrive raw.
            if (this.ofPrincipals && !(o instanceof Principal)) {
                throw new SecurityException("attempting to add an object which is not an instance "
                    + "of java.security.Principal to a Subject's Principal Set");
            }
            if (this.list.contains(o)) {
                return false;
            }
            this.list.add(o);
            return true;
        }

        @Override
        public boolean remove(Object o) {
            if (this.owner.readOnly) {
                throw new IllegalStateException("Subject is read-only");
            }
            return this.list.remove(o);
        }

        @Override
        public void clear() {
            if (this.owner.readOnly) {
                throw new IllegalStateException("Subject is read-only");
            }
            this.list.clear();
        }

        @Override
        public Iterator<E> iterator() {
            final Iterator<Object> base = this.list.iterator();
            final Subject owner = this.owner;
            return new Iterator<E>() {
                public boolean hasNext() {
                    return base.hasNext();
                }

                @SuppressWarnings("unchecked")
                public E next() {
                    return (E) base.next();
                }

                public void remove() {
                    // The check goes here too and not only in `remove(Object)`: removing through
                    // the iterator is the other door to the same set, and leaving it open would
                    // make `setReadOnly()` useless.
                    if (owner.readOnly) {
                        throw new IllegalStateException("Subject is read-only");
                    }
                    base.remove();
                }
            };
        }
    }
}
