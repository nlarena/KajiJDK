package com.sun.security.auth.module;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import com.sun.security.auth.UnixNumericGroupPrincipal;
import com.sun.security.auth.UnixNumericUserPrincipal;
import com.sun.security.auth.UnixPrincipal;

/**
 * The JAAS module that takes the identity the Unix system has already established.
 *
 * <h2>What it does not do: ask for a password</h2>
 *
 * <p>This module authenticates nobody. The user already authenticated when it entered the
 * system, and what this module does is <strong>import</strong> that fact into the
 * {@link Subject}: it reads who the process's owner is and adds the principals that represent
 * it.
 *
 * <p>That is why {@link #login} does not use the {@code CallbackHandler}. If it fails it is not
 * because the password is wrong, but because who the user is could not be found out.
 *
 * <h2>The two phases, and why it is not a single one</h2>
 *
 * <p>JAAS separates {@link #login} from {@link #commit} because a configuration has several
 * modules and the result depends on them all. First each one's {@code login} runs, and only if
 * the whole is acceptable does each one's {@code commit} run. That way the {@code Subject} is
 * never left half filled: either the principals of all the modules that were due to go in go
 * in, or none does.
 *
 * <p>{@link #abort} is the other branch: somebody failed, and this module has to undo. That is
 * why it tells whether it had already done {@code commit} -- if it did not, forgetting is
 * enough; if it did, what it put has to be taken out of the {@code Subject}, and for that it
 * calls {@link #logout}.
 *
 * <h2>State on this VM</h2>
 *
 * <p>The state machine above is complete and is the JDK's. What fails is the only step that
 * needs the operating system: building the {@link UnixSystem}, which comes out of
 * {@code getuid} and companions. {@link #login} turns it into {@link FailedLoginException}
 * with the cause chained, which is what the contract asks -- and it is besides the right
 * result, because not being able to establish who the user is has to be an authentication
 * failure and not a silent success.
 *
 * @since 1.4
 */
public class UnixLoginModule implements LoginModule {

    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, ?> sharedState;
    private Map<String, ?> options;

    private boolean debug;

    private UnixSystem ss;
    private UnixPrincipal userPrincipal;
    private UnixNumericUserPrincipal uidPrincipal;
    private UnixNumericGroupPrincipal gidPrincipal;
    private final List<UnixNumericGroupPrincipal> extraGroups =
            new LinkedList<UnixNumericGroupPrincipal>();

    private boolean succeeded;
    private boolean commitSucceeded;

    /** For the JAAS configuration, which instantiates it by reflection. */
    public UnixLoginModule() {
    }

    /** {@inheritDoc} */
    public void initialize(final Subject subject, final CallbackHandler callbackHandler,
            final Map<String, ?> sharedState, final Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;
        this.debug = "true".equalsIgnoreCase((String) options.get("debug"));
    }

    /**
     * It finds out who the process's user is and builds its principals.
     *
     * <p>It does not put them in the {@link Subject} yet: that is {@link #commit}.
     *
     * @return {@code true} if it could be done
     * @throws FailedLoginException if who the user is could not be found out
     */
    public boolean login() throws LoginException {
        try {
            ss = new UnixSystem();
        } catch (final UnsupportedOperationException e) {
            succeeded = false;
            final FailedLoginException f =
                    new FailedLoginException("Unable to get UNIX information");
            f.initCause(e);
            throw f;
        }

        userPrincipal = new UnixPrincipal(ss.getUsername());
        uidPrincipal = new UnixNumericUserPrincipal(ss.getUid());
        // gid 0 is the root group; the JDK does not add it as a principal principal, and this
                // class reproduces that decision.
        if (ss.getGid() != 0) {
            gidPrincipal = new UnixNumericGroupPrincipal(ss.getGid(), true);
        }
        final long[] groups = ss.getGroups();
        if (groups != null) {
            for (int i = 0; i < groups.length; i++) {
                extraGroups.add(new UnixNumericGroupPrincipal(groups[i], false));
            }
        }
        if (debug) {
            System.out.println("\t\t[UnixLoginModule]: usuario " + ss.getUsername());
        }
        succeeded = true;
        return true;
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
        // The contains is necessary: the Subject may bring principals of another module or of
                // a previous authentication, and adding the same one twice would leave duplicates
                // that afterwards would have to be taken out twice in the logout.
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
            // It never got to touch the Subject: forgetting what it found out is enough.
            succeeded = false;
            reset();
        } else {
            // It had already put the principals in; taking them out is exactly what logout does.
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
        ss = null;
        userPrincipal = null;
        uidPrincipal = null;
        gidPrincipal = null;
        extraGroups.clear();
    }
}
