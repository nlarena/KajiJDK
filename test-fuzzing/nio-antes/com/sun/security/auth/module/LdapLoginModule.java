package com.sun.security.auth.module;

import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.security.auth.x500.X500Principal;

import com.sun.security.auth.UserPrincipal;

/**
 * The JAAS module that authenticates against an LDAP directory.
 *
 * <h2>How one authenticates against LDAP: by binding, not by comparing</h2>
 *
 * <p>The password is not read from the directory in order to compare it -- that is not even
 * possible, because the directory returns the attribute encrypted or does not return it at
 * all. What is done is to <strong>bind</strong> to the directory with the user's name and
 * password: if the server accepts the binding, the password was good; if it rejects it, no.
 *
 * <p>The consequence is that the password encryption algorithm is the server's problem and not
 * this code's, and that the password is never compared here.
 *
 * <h2>The two modes</h2>
 *
 * <p><strong>Authentication with a search</strong>: first a binding with a service account in
 * order to <em>find</em> the user's DN from its name, and afterwards the real binding with
 * that DN. It is needed when the name the user writes is not its DN, which is the usual
 * thing.
 *
 * <p><strong>Direct authentication</strong>: the DN is built with a template
 * ({@code userDNPattern}), with no previous search. It is one binding instead of three
 * operations, but it demands that all the users should be under the same branch.
 *
 * <h2>State on this VM</h2>
 *
 * <p>The code here is the real and complete one: it builds the JNDI environment, does the
 * search if it is called for, binds with the user's credentials and translates the rejection
 * into {@link FailedLoginException}. What is needed underneath is an <strong>LDAP JNDI
 * provider</strong>, which is who talks the protocol; without it, {@link InitialDirContext}
 * fails with {@link NamingException} and {@link #login} wraps it in {@link LoginException}
 * with the cause.
 *
 * @since 1.6
 */
public class LdapLoginModule implements LoginModule {

    private static final String LDAP_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";

    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, ?> sharedState;
    private Map<String, ?> options;

    private boolean debug;
    private boolean useSSL = true;
    private String userProvider;
    private String userFilter;
    private String authIdentity;
    private String authzIdentity;
    private String userDNPattern;

    private String userName;
    private UserPrincipal userPrincipal;
    private X500Principal dnPrincipal;
    private UserPrincipal authzPrincipal;

    private boolean succeeded;
    private boolean commitSucceeded;

    /** For the JAAS configuration, which instantiates it by reflection. */
    public LdapLoginModule() {
    }

    /** {@inheritDoc} */
    public void initialize(final Subject subject, final CallbackHandler callbackHandler,
            final Map<String, ?> sharedState, final Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;

        this.debug = "true".equalsIgnoreCase((String) options.get("debug"));
        final String ssl = (String) options.get("useSSL");
        this.useSSL = ssl == null || "true".equalsIgnoreCase(ssl);
        this.userProvider = (String) options.get("userProvider");
        this.userFilter = (String) options.get("userFilter");
        this.authIdentity = (String) options.get("authIdentity");
        this.authzIdentity = (String) options.get("authzIdentity");
        this.userDNPattern = (String) options.get("userDNPattern");
    }

    /**
     * It asks for a name and a password, and binds to the directory with them.
     *
     * @return {@code true} if the binding was accepted
     * @throws FailedLoginException if the directory rejected the credentials
     * @throws LoginException if configuration is missing or the directory could not be talked to
     */
    public boolean login() throws LoginException {
        if (userProvider == null) {
            throw new LoginException("the userProvider option is missing");
        }
        if (userFilter == null && userDNPattern == null) {
            throw new LoginException("userFilter or userDNPattern is needed");
        }

        final NameCallback nc = new NameCallback("User name: ");
        final PasswordCallback pc = new PasswordCallback("Contrasena: ", false);
        ask(new Callback[] { nc, pc });
        userName = nc.getName();
        final char[] pass = pc.getPassword();
        pc.clearPassword();
        if (userName == null || userName.length() == 0 || pass == null) {
            throw new FailedLoginException("the name or the password is missing");
        }

        try {
            final String dn = userDNPattern != null
                    ? userDNPattern.replace("{USERNAME}", userName)
                    : findDN(userName);
            bind(dn, pass);

            userPrincipal = new UserPrincipal(userName);
            dnPrincipal = new X500Principal(dn);
            if (authzIdentity != null) {
                authzPrincipal = new UserPrincipal(authzIdentity);
            }
        } catch (final NamingException e) {
            reset();
            final LoginException le =
                    new LoginException("the directory could not be talked to: " + userProvider);
            le.initCause(e);
            throw le;
        } finally {
            Arrays.fill(pass, ' ');
        }

        if (debug) {
            System.out.println("\t\t[LdapLoginModule]: entro " + userName);
        }
        succeeded = true;
        return true;
    }

    /**
     * The user's DN, looking it up with a service account.
     *
     * <p>Two bindings are needed: this one, with the service or anonymous identity, serves only
     * in order to <em>find</em> the user. The one that authenticates is the one afterwards, with
     * its DN and its password.
     */
    private String findDN(final String user) throws NamingException, LoginException {
        final Hashtable<String, Object> env = environment();
        if (authIdentity != null) {
            env.put(Context.SECURITY_PRINCIPAL, authIdentity);
        }
        final DirContext ctx = new InitialDirContext(env);
        try {
            final SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            sc.setReturningAttributes(new String[0]);
            sc.setCountLimit(2);
            final NamingEnumeration<SearchResult> r =
                    ctx.search("", userFilter.replace("{USERNAME}", user), sc);
            if (!r.hasMore()) {
                throw new FailedLoginException("the user was not found: " + user);
            }
            final SearchResult first = r.next();
            if (r.hasMore()) {
                // Two matches is not "to choose the first": the filter identifies nobody in
                                // particular, and binding with either of the two would be
                                // authenticating the wrong user.
                throw new FailedLoginException(
                        "the filter found more than one user for " + user);
            }
            final String dn = first.getNameInNamespace();
            return dn != null && dn.length() > 0 ? dn : first.getName();
        } finally {
            ctx.close();
        }
    }

    /** The binding that authenticates: with the user's DN and its password. */
    private void bind(final String dn, final char[] pass) throws NamingException, LoginException {
        final Hashtable<String, Object> env = environment();
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, dn);
        env.put(Context.SECURITY_CREDENTIALS, new String(pass));
        try {
            new InitialDirContext(env).close();
        } catch (final javax.naming.AuthenticationException e) {
            final FailedLoginException f = new FailedLoginException("credenciales rechazadas");
            f.initCause(e);
            throw f;
        }
    }

    private Hashtable<String, Object> environment() {
        final Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, LDAP_FACTORY);
        env.put(Context.PROVIDER_URL, userProvider);
        if (useSSL) {
            env.put(Context.SECURITY_PROTOCOL, "ssl");
        }
        return env;
    }

    private void ask(final Callback[] cbs) throws LoginException {
        if (callbackHandler == null) {
            throw new LoginException("a CallbackHandler is needed");
        }
        try {
            callbackHandler.handle(cbs);
        } catch (final IOException e) {
            final LoginException le = new LoginException("the CallbackHandler failed");
            le.initCause(e);
            throw le;
        } catch (final UnsupportedCallbackException e) {
            final LoginException le =
                    new LoginException("the CallbackHandler does not support " + e.getCallback());
            le.initCause(e);
            throw le;
        }
    }

    /**
     * It puts the principals in the {@link Subject}.
     *
     * @return {@code true} if this module had had success in {@link #login}
     * @throws LoginException if the {@code Subject} is read-only
     */
    public boolean commit() throws LoginException {
        if (!succeeded) {
            return false;
        }
        if (subject.isReadOnly()) {
            reset();
            throw new LoginException("Subject is ReadOnly");
        }
        addPrincipal(userPrincipal);
        addPrincipal(dnPrincipal);
        addPrincipal(authzPrincipal);
        commitSucceeded = true;
        return true;
    }

    private void addPrincipal(final java.security.Principal p) {
        if (p != null && !subject.getPrincipals().contains(p)) {
            subject.getPrincipals().add(p);
        }
    }

    /**
     * It undoes what this module did, because the authentication as a whole failed.
     *
     * @return {@code true} if this module had had success in {@link #login}
     * @throws LoginException if the {@code Subject} is read-only
     */
    public boolean abort() throws LoginException {
        if (!succeeded) {
            return false;
        }
        if (!commitSucceeded) {
            succeeded = false;
            reset();
        } else {
            logout();
        }
        return true;
    }

    /**
     * It takes out of the {@link Subject} the principals this module had put in.
     *
     * @return {@code true} always
     * @throws LoginException if the {@code Subject} is read-only
     */
    public boolean logout() throws LoginException {
        if (subject.isReadOnly()) {
            reset();
            throw new LoginException("Subject is ReadOnly");
        }
        removePrincipal(userPrincipal);
        removePrincipal(dnPrincipal);
        removePrincipal(authzPrincipal);
        succeeded = false;
        commitSucceeded = false;
        reset();
        return true;
    }

    private void removePrincipal(final java.security.Principal p) {
        if (p != null) {
            subject.getPrincipals().remove(p);
        }
    }

    private void reset() {
        userName = null;
        userPrincipal = null;
        dnPrincipal = null;
        authzPrincipal = null;
    }
}
