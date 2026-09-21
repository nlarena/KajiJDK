package javax.naming.ldap;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ReferralException;

/**
 * The server does not have what it was asked for and says where to look for it.
 *
 * <h2>Why an exception and not a return value</h2>
 *
 * <p>Because a referral interrupts the operation: what was asked for is not <em>here</em>.
 * Modelling it as a result would force every call to return "either the data or a redirection", and
 * that would pollute the whole API for a case that almost never happens.
 *
 * <p>The odd thing about this exception is that it is <strong>continued</strong>:
 * {@link #getReferralContext} returns a context already pointing at the other server, and the
 * operation is repeated there. There may be several chained referrals, so the pattern is a loop
 * that catches, follows and retries.
 *
 * <p>The overload with {@link Control}{@code []} is what LDAP adds over {@link ReferralException}:
 * the original context's controls do not travel on their own to the new server, and you have to
 * decide which ones to take.
 */
public abstract class LdapReferralException extends ReferralException {

    private static final long serialVersionUID = -1668992791764950804L;

    /** With a message. */
    protected LdapReferralException(String explanation) {
        super(explanation);
    }

    /** Without a message. */
    protected LdapReferralException() {
        super();
    }

    /** A context pointing at the referred server. */
    public abstract Context getReferralContext() throws NamingException;

    /** Same, with another environment. */
    public abstract Context getReferralContext(Hashtable<?, ?> env) throws NamingException;

    /**
     * Same, with another environment and those connection controls.
     *
     * <p>The controls are not inherited from the original context: the new server may not support
     * them, and sending them as critical there would make the operation you were trying to rescue
     * fail.
     */
    public abstract Context getReferralContext(Hashtable<?, ?> env, Control[] reqCtls)
            throws NamingException;
}
