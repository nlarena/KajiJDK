package com.sun.security.auth.module;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

/**
 * The JAAS module that authenticates against Kerberos.
 *
 * <h2>What makes Kerberos different</h2>
 *
 * <p>That the password does not travel. The client asks the key distribution centre for a
 * <strong>ticket</strong> with which to grant tickets, and the answer comes encrypted with a
 * key derived from the user's password. If the client can decrypt it, it knew the password --
 * and the server never saw it go past.
 *
 * <p>From there comes the other property, the one that makes it worth while: with that ticket
 * the user obtains tickets for each service without writing anything again. It is the single
 * sign-on, and it is not an addition but a direct consequence of the design.
 *
 * <h2>What it leaves in the {@link Subject}</h2>
 *
 * <p>A {@link KerberosPrincipal} with the full name ({@code user@REALM}) and, as a
 * <strong>private</strong> credential, the {@link KerberosTicket}. The division is the usual
 * one: the principal says who it is, the ticket is what allows it to act.
 *
 * <h2>State on this VM</h2>
 *
 * <p>This module is not implemented, and unlike its companions in the package it is not
 * missing a step but <strong>all the work</strong>: talking Kerberos is implementing the
 * protocol -- the exchange with the distribution centre, the ASN.1 encoding of the messages,
 * the key derivation functions of each kind of encryption, the credential cache and the
 * analysis of the realm's configuration file. In the JDK that lives in
 * {@code sun.security.krb5}, which is dozens of classes and is not public API.
 *
 * <p>That is why {@link #login} throws {@link LoginException} saying this very thing, and the
 * other three answer what the contract asks of a module that did not authenticate. The
 * alternative -- a state machine that returns {@code true} without having authenticated
 * anybody -- would be a module that compiles, runs and lets anybody through.
 *
 * @since 1.4
 */
public class Krb5LoginModule implements LoginModule {

    private static final String MISSING =
            "Krb5LoginModule needs an implementation of the Kerberos protocol (the exchange "
            + "with the KDC, ASN.1, key derivation and a credential cache), which this library "
            + "does not have";

    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, ?> sharedState;
    private Map<String, ?> options;

    /** For the JAAS configuration, which instantiates it by reflection. */
    public Krb5LoginModule() {
    }

    /**
     * {@inheritDoc}
     *
     * <p>It keeps what it receives. It does not fail: the signature does not allow it to say so,
     * and failing here would keep a configuration with several modules from even getting to
     * initialize the others.
     */
    public void initialize(final Subject subject, final CallbackHandler callbackHandler,
            final Map<String, ?> sharedState, final Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;
    }

    /**
     * {@inheritDoc}
     *
     * @throws LoginException always; see the class note
     */
    public boolean login() throws LoginException {
        throw new LoginException(MISSING);
    }

    /**
     * {@inheritDoc}
     *
     * <p>It returns {@code false}, which is what the JAAS contract asks of a module whose
     * {@link #login} did not succeed. In the normal flow it is not even called -- a {@code login}
     * that fails leads to {@link #abort} -- but a direct caller has to receive the contract's
     * answer and not an exception.
     *
     * @return {@code false}
     */
    public boolean commit() throws LoginException {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * <p>It returns {@code false} instead of failing: {@link #login} never succeeded, and the
     * JAAS contract says that a module that did not authenticate answers {@code false} on
     * aborting. Making it fail would break the abort of the whole configuration because of a
     * module that did nothing.
     *
     * @return {@code false}
     */
    public boolean abort() throws LoginException {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * <p>It returns {@code false} for the same reason as {@link #abort}: there is nothing to take
     * out of the {@link Subject} because this module never put anything in.
     *
     * @return {@code false}
     */
    public boolean logout() throws LoginException {
        return false;
    }
}
