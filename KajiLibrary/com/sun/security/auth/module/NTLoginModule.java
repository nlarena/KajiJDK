package com.sun.security.auth.module;

import java.security.Principal;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import com.sun.security.auth.NTDomainPrincipal;
import com.sun.security.auth.NTNumericCredential;
import com.sun.security.auth.NTSidDomainPrincipal;
import com.sun.security.auth.NTSidGroupPrincipal;
import com.sun.security.auth.NTSidPrimaryGroupPrincipal;
import com.sun.security.auth.NTSidUserPrincipal;
import com.sun.security.auth.NTUserPrincipal;

/**
 * The JAAS module that takes the identity Windows has already established.
 *
 * <h2>Why it adds so many principals</h2>
 *
 * <p>Because a name and a SID are not interchangeable and both are needed. The
 * {@link NTUserPrincipal} carries the name, which is what a human reads and what serves for a
 * message; the {@link NTSidUserPrincipal} carries the SID, which is the only thing that may be
 * compared against a Windows access control list.
 *
 * <p>A security policy written against the name is fragile -- the name may be reassigned -- and
 * one written against the SID is illegible. By putting both in, each use chooses the one that
 * serves it.
 *
 * <h2>The private credential</h2>
 *
 * <p>Besides the principals, {@link #commit} adds an {@link NTNumericCredential} with the
 * impersonation token. It is what allows Java code that runs under this {@code Subject} to ask
 * Windows to act as that user. It goes as a <strong>private</strong> credential and not as a
 * principal precisely because it is a capability and not an identity: whoever has it may act,
 * whoever has the principal may only be recognized.
 *
 * <h2>State on this VM</h2>
 *
 * <p>The JAAS state machine is complete and is the JDK's. The only step that fails is building
 * the {@link NTSystem}, which comes from the Windows API; {@link #login} turns it into
 * {@link FailedLoginException} with the cause chained.
 *
 * @since 1.4
 */
public class NTLoginModule implements LoginModule {

    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, ?> sharedState;
    private Map<String, ?> options;

    private boolean debug;
    private boolean debugNative;

    private NTSystem ntSystem;
    private NTUserPrincipal userPrincipal;
    private NTSidUserPrincipal userSid;
    private NTDomainPrincipal domainPrincipal;
    private NTSidDomainPrincipal domainSid;
    private NTSidPrimaryGroupPrincipal primaryGroupSid;
    private final List<NTSidGroupPrincipal> groupSids = new LinkedList<NTSidGroupPrincipal>();
    private NTNumericCredential credential;

    private boolean succeeded;
    private boolean commitSucceeded;

    /** For the JAAS configuration, which instantiates it by reflection. */
    public NTLoginModule() {
    }

    /** {@inheritDoc} */
    public void initialize(final Subject subject, final CallbackHandler callbackHandler,
            final Map<String, ?> sharedState, final Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;
        this.debug = "true".equalsIgnoreCase((String) options.get("debug"));
        this.debugNative = "true".equalsIgnoreCase((String) options.get("debugNative"));
        // debugNative implies debug: asking for the detail of the native layer without the rest
                // of the trace would give loose lines with no context.
        if (debugNative) {
            this.debug = true;
        }
    }

    /**
     * It finds out who the Windows user is and builds its principals.
     *
     * @return {@code true} if it could be done
     * @throws FailedLoginException if who the user is could not be found out
     */
    public boolean login() throws LoginException {
        try {
            ntSystem = new NTSystem();
        } catch (final UnsupportedOperationException e) {
            succeeded = false;
            final FailedLoginException f =
                    new FailedLoginException("Failed in attempt to import "
                            + "the underlying NT system identity information");
            f.initCause(e);
            throw f;
        }

        if (ntSystem.getName() != null) {
            userPrincipal = new NTUserPrincipal(ntSystem.getName());
        }
        if (ntSystem.getUserSID() != null) {
            userSid = new NTSidUserPrincipal(ntSystem.getUserSID());
        }
        if (ntSystem.getDomain() != null) {
            domainPrincipal = new NTDomainPrincipal(ntSystem.getDomain());
        }
        if (ntSystem.getDomainSID() != null) {
            domainSid = new NTSidDomainPrincipal(ntSystem.getDomainSID());
        }
        if (ntSystem.getPrimaryGroupID() != null) {
            primaryGroupSid = new NTSidPrimaryGroupPrincipal(ntSystem.getPrimaryGroupID());
        }
        final String[] groups = ntSystem.getGroupIDs();
        if (groups != null) {
            for (int i = 0; i < groups.length; i++) {
                if (groups[i] != null) {
                    groupSids.add(new NTSidGroupPrincipal(groups[i]));
                }
            }
        }
        credential = new NTNumericCredential(ntSystem.getImpersonationToken());
        succeeded = true;
        return true;
    }

    /**
     * It puts the principals and the credential in the {@link Subject}.
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
        addPrincipal(userSid);
        addPrincipal(domainPrincipal);
        addPrincipal(domainSid);
        addPrincipal(primaryGroupSid);
        for (final NTSidGroupPrincipal g : groupSids) {
            addPrincipal(g);
        }
        if (credential != null
                && !subject.getPrivateCredentials().contains(credential)) {
            subject.getPrivateCredentials().add(credential);
        }
        commitSucceeded = true;
        return true;
    }

    private void addPrincipal(final Principal p) {
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
     * It takes out of the {@link Subject} what this module had put in.
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
        removePrincipal(userSid);
        removePrincipal(domainPrincipal);
        removePrincipal(domainSid);
        removePrincipal(primaryGroupSid);
        for (final NTSidGroupPrincipal g : groupSids) {
            removePrincipal(g);
        }
        if (credential != null) {
            subject.getPrivateCredentials().remove(credential);
        }
        succeeded = false;
        commitSucceeded = false;
        reset();
        return true;
    }

    private void removePrincipal(final Principal p) {
        if (p != null) {
            subject.getPrincipals().remove(p);
        }
    }

    private void reset() {
        ntSystem = null;
        userPrincipal = null;
        userSid = null;
        domainPrincipal = null;
        domainSid = null;
        primaryGroupSid = null;
        groupSids.clear();
        credential = null;
    }
}
