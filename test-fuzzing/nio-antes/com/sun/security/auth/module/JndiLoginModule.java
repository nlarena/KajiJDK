package com.sun.security.auth.module;

import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
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

import com.sun.security.auth.UnixNumericGroupPrincipal;
import com.sun.security.auth.UnixNumericUserPrincipal;
import com.sun.security.auth.UnixPrincipal;

/**
 * The JAAS module that authenticates against an NIS- or LDAP-style directory read over JNDI.
 *
 * <h2>How it differs from {@link LdapLoginModule}</h2>
 *
 * <p>In where the password is checked. {@code LdapLoginModule} <strong>binds</strong> itself
 * to the directory with the user's credentials and lets the server decide. This module, on the
 * other hand, <strong>reads</strong> the {@code userPassword} attribute and compares it here.
 *
 * <p>That difference is not one of style: reading the encrypted password forces the client to
 * know the algorithm it was encrypted with, which in a classic NIS map is {@code crypt(3)}.
 * Binding forces nothing because the algorithm stays on the server's side. That is why the
 * LDAP one aged better.
 *
 * <h2>The two URLs</h2>
 *
 * <p>{@link #USER_PROVIDER} points at the map of users and {@link #GROUP_PROVIDER} at the one
 * of groups. They are separate because in NIS they are two different maps and may live on
 * different servers.
 *
 * <h2>State on this VM</h2>
 *
 * <p>Everything that is seen here is real: the callbacks, the two queries to the directory,
 * the reading of the {@code uid}, {@code gid} and {@code userPassword} attributes, and the
 * building of the principals. Two pieces are missing underneath, and the two fail saying which
 * they are:
 *
 * <ul>
 *   <li>a <strong>JNDI provider</strong> that talks to the directory -- without it,
 *       {@link InitialDirContext} throws {@link NamingException};
 *   <li><strong>{@code crypt(3)}</strong>, the Unix password encryption. It is not written on
 *       purpose: it is DES with big tables, and a badly transcribed table would produce a
 *       module that compiles, runs, and accepts or rejects the wrong passwords without saying
 *       so. The comparison throws {@link LoginException} naming what is missing, which is the
 *       only safe way of not having it.
 * </ul>
 *
 * @since 1.4
 */
public class JndiLoginModule implements LoginModule {

    /** The name of the option with the URL of the map of users. */
    public final String USER_PROVIDER = "user.provider.url";

    /** The name of the option with the URL of the map of groups. */
    public final String GROUP_PROVIDER = "group.provider.url";

    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, ?> sharedState;
    private Map<String, ?> options;

    private boolean debug;
    private String userProvider;
    private String groupProvider;

    private String userName;
    private UnixPrincipal userPrincipal;
    private UnixNumericUserPrincipal uidPrincipal;
    private UnixNumericGroupPrincipal gidPrincipal;
    private final List<UnixNumericGroupPrincipal> extraGroups =
            new LinkedList<UnixNumericGroupPrincipal>();

    private boolean succeeded;
    private boolean commitSucceeded;

    /** For the JAAS configuration, which instantiates it by reflection. */
    public JndiLoginModule() {
    }

    /** {@inheritDoc} */
    public void initialize(final Subject subject, final CallbackHandler callbackHandler,
            final Map<String, ?> sharedState, final Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;
        this.debug = "true".equalsIgnoreCase((String) options.get("debug"));
        this.userProvider = (String) options.get(USER_PROVIDER);
        this.groupProvider = (String) options.get(GROUP_PROVIDER);
    }

    /**
     * It asks for a name and a password, looks the user up in the directory and checks the
     * password.
     *
     * @return {@code true} if the password was good
     * @throws FailedLoginException if the user is not there or the password does not match
     * @throws LoginException if configuration is missing or the directory could not be talked to
     */
    public boolean login() throws LoginException {
        if (userProvider == null) {
            throw new LoginException("the option is missing: " + USER_PROVIDER);
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
            final Attributes attrs = findUser(userName);
            final String encrypted = value(attrs, "userPassword");
            if (encrypted == null) {
                throw new FailedLoginException(
                        "the directory did not return the password of " + userName);
            }
            verify(pass, encrypted);

            userPrincipal = new UnixPrincipal(userName);
            final String uid = value(attrs, "uidNumber");
            if (uid == null) {
                uidPrincipal = null;
            } else {
                uidPrincipal = new UnixNumericUserPrincipal(uid);
            }
            final String gid = value(attrs, "gidNumber");
            if (gid != null) {
                gidPrincipal = new UnixNumericGroupPrincipal(gid, true);
            }
            if (groupProvider != null) {
                for (final String g : findGroups(userName)) {
                    extraGroups.add(new UnixNumericGroupPrincipal(g, false));
                }
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
            System.out.println("\t\t[JndiLoginModule]: entro " + userName);
        }
        succeeded = true;
        return true;
    }

    /**
     * It compares the password that was written against the encrypted one the directory
     * returned.
     *
     * @throws LoginException always, in this library; see the class note about {@code crypt(3)}
     */
    private static void verify(final char[] escrita, final String encrypted)
            throws LoginException {
        throw new LoginException(
                "the check needs crypt(3), which this library does not implement; a badly "
                + "transcribed DES table would accept or reject passwords without saying so, "
                + "so it is missing on purpose");
    }

    private Attributes findUser(final String user)
            throws NamingException, LoginException {
        final DirContext ctx = new InitialDirContext(environment(userProvider));
        try {
            final SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            sc.setCountLimit(2);
            final NamingEnumeration<SearchResult> r = ctx.search("", "(uid=" + user + ")", sc);
            if (!r.hasMore()) {
                throw new FailedLoginException("the user was not found: " + user);
            }
            final SearchResult first = r.next();
            if (r.hasMore()) {
                throw new FailedLoginException("there is more than one entry for " + user);
            }
            return first.getAttributes();
        } finally {
            ctx.close();
        }
    }

    private List<String> findGroups(final String user) throws NamingException {
        final List<String> out = new LinkedList<String>();
        final DirContext ctx = new InitialDirContext(environment(groupProvider));
        try {
            final SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            final NamingEnumeration<SearchResult> r =
                    ctx.search("", "(memberUid=" + user + ")", sc);
            while (r.hasMore()) {
                final String gid = value(r.next().getAttributes(), "gidNumber");
                if (gid != null) {
                    out.add(gid);
                }
            }
            return out;
        } finally {
            ctx.close();
        }
    }

    private static Hashtable<String, Object> environment(final String url) {
        final Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put(Context.PROVIDER_URL, url);
        return env;
    }

    /** An attribute's first value, or {@code null} if it did not come. */
    private static String value(final Attributes attrs, final String name)
            throws NamingException {
        if (attrs == null) {
            return null;
        }
        final Attribute a = attrs.get(name);
        if (a == null || a.size() == 0) {
            return null;
        }
        final Object v = a.get();
        if (v instanceof byte[]) {
            // A binary attribute, which is how userPassword travels on several servers.
            return new String((byte[]) v, java.nio.charset.StandardCharsets.UTF_8);
        }
        return v == null ? null : v.toString();
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
        addPrincipal(uidPrincipal);
        addPrincipal(gidPrincipal);
        for (final UnixNumericGroupPrincipal g : extraGroups) {
            addPrincipal(g);
        }
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
        removePrincipal(uidPrincipal);
        removePrincipal(gidPrincipal);
        for (final UnixNumericGroupPrincipal g : extraGroups) {
            removePrincipal(g);
        }
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
        uidPrincipal = null;
        gidPrincipal = null;
        extraGroups.clear();
    }
}
