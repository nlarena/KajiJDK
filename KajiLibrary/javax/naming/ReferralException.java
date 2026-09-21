package javax.naming;

import java.util.Hashtable;

/**
 * "I don't have that, ask that one": a reference to another server, thrown as an exception.
 *
 * <p>A referral is not an error, it is a **redirection**. LDAP uses it all the time: the server
 * answers "that branch lives at `ldap://other/...`" and the client decides whether to follow.
 * That is why the class has methods no other exception in the package has: `getReferralContext()`
 * returns the other server's context, `skipReferral()` discards this one and moves to the next, and
 * `retryReferral()` tries the same one again --the typical case is retrying after changing the
 * credentials.
 *
 * <p><strong>It is abstract, and that is what keeps it honest here.</strong> Following a referral
 * is opening a connection to another server, and only a provider can do that. The class declares
 * the shape of the contract --which is what a provider has to implement-- and does not promise to
 * fulfil it: this library ships no provider, and the only subclass is the equally abstract
 * `javax.naming.ldap.LdapReferralException`, so nothing here throws it. A `catch` naming it
 * compiles and is correct; it simply never runs. (An earlier note said nobody extends it.)
 *
 * <p>The rest of the hierarchy is explained in `NamingException`.
 */
public abstract class ReferralException extends NamingException {

    private static final long serialVersionUID = -2881363844695698876L;

    protected ReferralException(String explanation) {
        super(explanation);
    }

    protected ReferralException() {
        super();
    }

    /** The raw referral information, in whatever form the provider uses (a URL, typically). */
    public abstract Object getReferralInfo();

    /**
     * The context to carry on in, already pointing at the other server.
     *
     * <p>The operation that failed has to be requested again on this context: the exception carries
     * the redirection, not the result.
     */
    public abstract Context getReferralContext() throws NamingException;

    /**
     * Same as the other, but with an environment of its own --the case of retrying with other
     * credentials.
     */
    public abstract Context getReferralContext(Hashtable<?, ?> env) throws NamingException;

    /**
     * Discards this referral and moves to the next one, if any.
     *
     * <p>Returns whether there are more. A server may answer with several and the client try them
     * in order until one works.
     */
    public abstract boolean skipReferral();

    /**
     * Leaves the same referral ready to be retried.
     *
     * <p>It does not retry: it **prepares**. The caller then asks for `getReferralContext()` again.
     */
    public abstract void retryReferral();
}
