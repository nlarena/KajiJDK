package javax.naming;

/**
 * The root of JNDI's twenty-five exceptions.
 *
 * <h2>Why there are twenty-five and not one</h2>
 *
 * <p>A naming operation --resolving `ldap://host/cn=john`, binding an object, listing a context--
 * can fail for reasons the caller treats **differently**: if the name does not exist it is created,
 * if the server does not answer it is retried, if the credentials are wrong the user is asked, and
 * if the name is misspelled it is never retried. A single type with a code inside would force every
 * `catch` to look at the code; a hierarchy lets the `catch` pick the level that suits it. That is
 * why the package has one exception per cause and **five** branches:
 *
 * <ul>
 *   <li>The direct leaves of this class: `NameNotFoundException`, `NameAlreadyBoundException`,
 *       `CommunicationException`, `ConfigurationException`, ... Each one is a different cause and
 *       adds not a single member; all it contributes is **its type**.
 *   <li>`NamingSecurityException`, abstract, groups the three security ones, so that a `catch` can
 *       say "any authentication or permission problem" at once.
 *   <li>`LimitExceededException` groups the two limit ones (size and time).
 *   <li>`LinkException` groups the link ones and **does** add state: the link has its own resolved
 *       name and its own remaining name, apart from the context's.
 *   <li>`CannotProceedException` and `ReferralException` are the two the provider uses to say
 *       "you carry on somewhere else", and both carry the state needed to carry on.
 * </ul>
 *
 * <h2>What this class adds to `Exception`</h2>
 *
 * <p>A naming exception does not just say "it failed": it says **how far** it got. Resolving
 * `a/b/c/d` may resolve `a/b` and die at `c`; that is two names --the resolved one and the one
 * left-- plus the object reached. Those three fields are `protected` on purpose: providers fill
 * them in piece by piece while the exception climbs through the layers, and that is why
 * `appendRemainingComponent` and `appendRemainingName` exist, which **accumulate** the remaining
 * name on the way back.
 *
 * <p>The other peculiarity is `rootException`: JNDI brought cause chaining in 1.3, before
 * `Throwable` had it. When the JDK added `getCause`/`initCause`, this class was left with **both**
 * names for the same thing, wired to each other: `getCause` returns `getRootCause`, and `initCause`
 * also sets the root cause. It is kept that way because the asymmetry is observable: `setRootCause`
 * does not touch `Throwable`'s cause.
 *
 * <p>On serialization: the class is `Serializable` by inheritance, and its serial form is the
 * default one (the four fields), with the real JDK's `serialVersionUID`. An earlier note said this
 * tree had no `ObjectOutputStream`; it has one now, and a `NamingException` written and read back
 * on this VM keeps its explanation.
 */
public class NamingException extends Exception {

    private static final long serialVersionUID = -1299181962103167177L;

    /** How far it could be resolved. `null` if unknown or if it got nowhere. */
    protected Name resolvedName;

    /** The object reached by resolving `resolvedName`. */
    protected Object resolvedObj;

    /** What was left to resolve when it failed. */
    protected Name remainingName;

    /** The underlying cause, from when JNDI had to chain causes by hand. */
    protected Throwable rootException;

    public NamingException(String explanation) {
        super(explanation);
        // Two statements and not `resolvedName = remainingName = null`: chained assignment
        // to **fields** triggers #460 in COMPILER_FINDINGS (the `dup_x1` is missing, and the
        // second `putfield` empties the stack). Separate, it is the same.
        resolvedName = null;
        remainingName = null;
        resolvedObj = null;
        rootException = null;
    }

    public NamingException() {
        super();
        // Two statements and not `resolvedName = remainingName = null`: chained assignment
        // to **fields** triggers #460 in COMPILER_FINDINGS (the `dup_x1` is missing, and the
        // second `putfield` empties the stack). Separate, it is the same.
        resolvedName = null;
        remainingName = null;
        resolvedObj = null;
        rootException = null;
    }

    public Name getResolvedName() {
        return resolvedName;
    }

    public Name getRemainingName() {
        return remainingName;
    }

    public Object getResolvedObj() {
        return resolvedObj;
    }

    /**
     * It is `getMessage()` under another name; JNDI has called it the "explanation" since before.
     */
    public String getExplanation() {
        return getMessage();
    }

    // Both name setters **clone**. A `Name` is mutable, and the exception travels upwards while
    // the provider keeps using its copy: without cloning, the name the catcher reads could have
    // changed after it was thrown.

    public void setResolvedName(Name name) {
        resolvedName = (name != null) ? (Name) name.clone() : null;
    }

    public void setRemainingName(Name name) {
        remainingName = (name != null) ? (Name) name.clone() : null;
    }

    public void setResolvedObj(Object obj) {
        resolvedObj = obj;
    }

    /**
     * Adds a component to the conceptual **front** of the remaining name.
     *
     * <p>This is what the upper layer does while the exception climbs: each context that sees it
     * adds what **it** did not get to resolve, and in the end the remaining name is complete as
     * seen from the initial context. If there is no remaining name yet it starts a composite one,
     * which is the neutral type for names that cross different namespaces.
     */
    public void appendRemainingComponent(String name) {
        if (name != null) {
            try {
                if (remainingName == null) {
                    remainingName = new CompositeName();
                }
                remainingName.add(name);
            } catch (NamingException e) {
                // `CompositeName.add` only fails with invalid names, and here the component comes
                // already split: if it happens anyway, it is a programming error of the provider.
                throw new IllegalArgumentException(e.toString());
            }
        }
    }

    public void appendRemainingName(Name name) {
        if (name == null) {
            return;
        }
        if (remainingName != null) {
            try {
                remainingName.addAll(name);
            } catch (NamingException e) {
                throw new IllegalArgumentException(e.toString());
            }
        } else {
            remainingName = (Name) name.clone();
        }
    }

    public Throwable getRootCause() {
        return rootException;
    }

    /** The `if` avoids the trivial cycle: an exception caused by itself hangs any printer. */
    public void setRootCause(Throwable e) {
        if (e != this) {
            rootException = e;
        }
    }

    @Override
    public Throwable getCause() {
        return getRootCause();
    }

    /** Sets both: `Throwable`'s --which only allows it once-- and JNDI's. */
    @Override
    public Throwable initCause(Throwable cause) {
        super.initCause(cause);
        setRootCause(cause);
        return this;
    }

    @Override
    public String toString() {
        String answer = super.toString();
        if (rootException != null) {
            answer += " [Root exception is " + rootException + "]";
        }
        if (remainingName != null) {
            answer += "; remaining name '" + remainingName + "'";
        }
        return answer;
    }

    /**
     * Same as `toString()`, plus the resolved object if detail is asked for and there is one.
     *
     * <p>It is separate because the resolved object can be anything --a connection, a pool-- and
     * its `toString` may be huge or leak data; the default does not print it.
     */
    public String toString(boolean detail) {
        if (!detail || resolvedObj == null) {
            return toString();
        }
        return toString() + "; resolved object " + resolvedObj;
    }
}
