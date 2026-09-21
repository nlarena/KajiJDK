package com.sun.security.jgss;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;

/**
 * A {@link GSSContext} with what standard GSS-API does not give.
 *
 * <p>The three operations have the same reason for being: the standard leaves out things a
 * program that uses Kerberos for real needs --looking inside the context, and constrained
 * delegation-- and this interface adds them without touching the portable interface.
 *
 * <p>It is not implemented: it is returned by the provider. A `GSSContext` obtained from
 * {@code GSSManager.createContext} may be tried with `instanceof` and used as an
 * `ExtendedGSSContext` if the mechanism supports it.
 */
public interface ExtendedGSSContext extends GSSContext {

    /**
     * It asks for one of the context's internal data.
     *
     * <p>It only makes sense with the context already established; before, the answer does not
     * exist yet. The type of what it returns depends on `type`: see {@link InquireType}.
     *
     * @param type what is asked
     * @return the answer, of the type `type` documents
     * @throws GSSException if the context is not established, or if the mechanism does not know
     *     how to answer that query
     */
    Object inquireSecContext(InquireType type) throws GSSException;

    /**
     * It asks that the delegation should be subject to the KDC's policy.
     *
     * <p>It is stricter than {@code requestCredDeleg}: there the client decides to delegate and the
     * service receives the complete credential. Here the decision is taken by the KDC, which marks
     * the ticket as `OK-AS-DELEGATE` only for the services it trusts. It serves so as not to hand
     * the user's identity over to any service it connects to.
     *
     * <p>It has to be called **before** establishing the context; afterwards it has no effect.
     *
     * @throws GSSException if the mechanism does not support the delegation policy
     */
    void requestDelegPolicy(boolean state) throws GSSException;

    /**
     * Whether the delegation policy came into effect.
     *
     * <p>Before establishing the context it reports what was asked for; afterwards, what was got,
     * which may be less.
     */
    boolean getDelegPolicyState();
}
