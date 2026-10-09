package javax.security.auth.login;

import java.util.HashMap;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.spi.LoginModule;

/**
 * KajiLibrary's javax.security.auth.login.LoginContext -- runs the authentication chain.
 *
 * <p>It asks the {@link Configuration} for the list of modules of a name, instantiates them and
 * runs them in two phases. The result, if it goes well, is a {@link Subject} full of principals and
 * credentials.
 *
 * <h2>Why two phases</h2>
 *
 * <p>First {@code login()} is called on the modules, and only afterwards {@code commit()}. No
 * module writes to the subject during the first phase.
 *
 * <p>It is what prevents a half-filled subject. With a single phase, a chain of three modules where
 * the third fails would leave the principals of the first two inside the subject, and the
 * application would receive a subject that looks authenticated and is not. With two, either all get
 * in or none does: failure calls {@code abort()}.
 *
 * <h2>How the chain decides</h2>
 *
 * <p>The rules of {@link AppConfigurationEntry.LoginModuleControlFlag} are applied like this:
 *
 * <ul>
 *   <li>a module that succeeds and is <b>SUFFICIENT</b> cuts the walk, unless some mandatory one
 *       failed before -- in that case the login is already lost and cutting would only hide the
 *       reason;
 *   <li>a <b>REQUISITE</b> that fails cuts right there;
 *   <li>a <b>REQUIRED</b> that fails is noted and the chain <b>goes on</b>. Going on costs time and
 *       is on purpose: if it cut, the response time would tell which of the modules rejected, which
 *       is just what should not be told to whoever is probing;
 *   <li>if at the end no mandatory one failed but none succeeded either, {@link LoginException} is
 *       thrown: a whole chain of optional ones that stay out of it is not a successful login.
 * </ul>
 *
 * <p>The first mandatory error is the one thrown, not the last: it is the one that says where the
 * problem began.
 *
 * <h2>Four walks with the same shape</h2>
 *
 * <p>{@code login}, {@code commit}, {@code abort} and {@code logout} walk the same list with the
 * same rules. The first two cut at a SUFFICIENT that succeeds and the last two do not. The
 * asymmetry is necessary. Cutting on the way in is the definition of "sufficient"; cutting on the
 * way back would leave modules with state without finding out that the chain ended.
 *
 * <p>A failing REQUISITE, on the other hand, cuts in <b>all four</b> here. The note said the walks
 * differ in one thing only; in the JDK they differ in two, because there a REQUISITE that fails in
 * {@code abort} or {@code logout} is noted like a REQUIRED and the walk goes on, for the same
 * reason as above. Here the modules after it are not aborted or logged out.
 *
 * <p>From there comes a detail that surprises the first time: a module that never got to run <b>is
 * instantiated all the same</b> when the chain has to be aborted or logged out. It is the right
 * thing -- the cleanup walk has to reach all the configured ones-- and it is what the JDK does.
 *
 * <h2>The shared state</h2>
 *
 * <p>The modules of a chain share a map that survives between them. That is what it is for: the
 * first module asks the person for the password once and leaves it there, and the ones that follow
 * use it without asking again.
 */
public class LoginContext {

    /** The security property with the default handler. */
    private static final String DEFAULT_HANDLER = "auth.login.defaultCallbackHandler";

    private final String name;
    private final CallbackHandler callbackHandler;
    private final Configuration configuration;
    private final AppConfigurationEntry[] entries;

    /** One per entry; null until that module is needed. */
    private final LoginModule[] modules;

    /** Shared between the modules of this chain; see the class note. */
    private final Map<String, Object> sharedState = new HashMap<String, Object>();

    /** Null until the first {@link #login}, unless the caller gave it. */
    private Subject subject;

    /**
     * Whether the caller brought the subject; it decides what {@link #getSubject} returns on
     * failure.
     */
    private final boolean subjectProvided;

    /** Whether the last chain went through. */
    private boolean loginSucceeded = false;

    /** With a new subject and no handler of its own. */
    public LoginContext(String name) throws LoginException {
        this(name, null, null, null);
    }

    /** On an existing subject. */
    public LoginContext(String name, Subject subject) throws LoginException {
        this(name, subject, null, null);
    }

    /** With a handler that knows how to ask the person. */
    public LoginContext(String name, CallbackHandler callbackHandler) throws LoginException {
        this(name, null, callbackHandler, null);
    }

    /** Both things. */
    public LoginContext(String name, Subject subject, CallbackHandler callbackHandler)
        throws LoginException {
        this(name, subject, callbackHandler, null);
    }

    /**
     * Everything explicit, including the configuration.
     *
     * <p>Passing it here is the way not to depend on the global one: two parts of the same process
     * can authenticate with different rules.
     *
     * @throws LoginException if the name is null or has no configured modules
     */
    public LoginContext(String name, Subject subject, CallbackHandler callbackHandler,
                        Configuration config) throws LoginException {
        if (name == null) {
            throw new LoginException("Invalid null input: name");
        }
        this.name = name;
        this.subject = subject;
        this.subjectProvided = subject != null;
        this.callbackHandler = (callbackHandler == null) ? defaultHandler() : callbackHandler;
        this.configuration = (config == null) ? Configuration.getConfiguration() : config;
        AppConfigurationEntry[] found = this.configuration.getAppConfigurationEntry(name);
        if (found == null || found.length == 0) {
            throw new LoginException("No LoginModules configured for " + name);
        }
        this.entries = found;
        this.modules = new LoginModule[found.length];
    }

    /**
     * Runs the chain: {@code login} on all, then {@code commit}.
     *
     * <p>If either of the two phases fails, the whole chain is aborted and the <b>first</b> error
     * is thrown; the one that comes out of the abort does not cover up the original.
     *
     * @throws LoginException if the authentication does not go through
     */
    public void login() throws LoginException {
        this.loginSucceeded = false;
        if (this.subject == null) {
            this.subject = new Subject();
        }
        try {
            invoke(Phase.LOGIN);
            invoke(Phase.COMMIT);
            this.loginSucceeded = true;
        } catch (LoginException first) {
            try {
                invoke(Phase.ABORT);
            } catch (LoginException ignored) {
                // The abort's error adds nothing and would cover up the one that really matters.
            }
            throw first;
        }
    }

    /**
     * Undoes the authentication.
     *
     * <p>It walks <b>all</b> the configured modules, including the ones that never ran; see the
     * class note.
     *
     * @throws LoginException if there was never a login, or if a module fails to log out
     */
    public void logout() throws LoginException {
        if (this.subject == null) {
            throw new LoginException("null subject - logout called before login");
        }
        invoke(Phase.LOGOUT);
        this.loginSucceeded = false;
    }

    /**
     * The authenticated subject.
     *
     * @return null if the login did not go through and the caller did not bring the subject --
     *     returning an empty subject would invite confusing it with an authenticated one without
     *     permissions
     */
    public Subject getSubject() {
        if (!this.loginSucceeded && !this.subjectProvided) {
            return null;
        }
        return this.subject;
    }

    /**
     * The walk, which is the same for the four phases.
     *
     * <p>The differences are in {@link Phase}: which method is called and whether a SUFFICIENT that
     * succeeds cuts. A failing REQUISITE cuts in all of them; see the class note.
     */
    private void invoke(Phase phase) throws LoginException {
        boolean anySucceeded = false;
        LoginException firstRequiredError = null;
        LoginException firstOtherError = null;
        int i = 0;
        while (i < this.entries.length) {
            AppConfigurationEntry entry = this.entries[i];
            AppConfigurationEntry.LoginModuleControlFlag flag = entry.getControlFlag();
            boolean status = false;
            LoginException failure = null;
            try {
                LoginModule module = moduleAt(i);
                status = phase.call(module);
            } catch (LoginException e) {
                failure = e;
            }
            if (failure == null && status) {
                anySucceeded = true;
                if (phase.stopsAtSufficient()
                        && flag == AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT
                        && firstRequiredError == null) {
                    return;
                }
            } else if (failure != null) {
                if (flag == AppConfigurationEntry.LoginModuleControlFlag.REQUISITE) {
                    if (firstRequiredError == null) {
                        firstRequiredError = failure;
                    }
                    break;
                }
                if (flag == AppConfigurationEntry.LoginModuleControlFlag.REQUIRED) {
                    if (firstRequiredError == null) {
                        firstRequiredError = failure;
                    }
                } else if (firstOtherError == null) {
                    firstOtherError = failure;
                }
            }
            i = i + 1;
        }
        if (firstRequiredError != null) {
            throw firstRequiredError;
        }
        if (!anySucceeded) {
            if (firstOtherError != null) {
                throw firstOtherError;
            }
            throw new LoginException("Login Failure: all modules ignored");
        }
    }

    /** The module of that entry, creating and initializing it the first time. */
    private LoginModule moduleAt(int index) throws LoginException {
        if (this.modules[index] != null) {
            return this.modules[index];
        }
        AppConfigurationEntry entry = this.entries[index];
        LoginModule made = instantiate(entry);
        made.initialize(this.subject, this.callbackHandler, this.sharedState, entry.getOptions());
        this.modules[index] = made;
        return made;
    }

    /** Loads and instantiates a module by class name. */
    private LoginModule instantiate(AppConfigurationEntry entry) throws LoginException {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader == null) {
                loader = LoginContext.class.getClassLoader();
            }
            Class<?> found = Class.forName(entry.getLoginModuleName(), true, loader);
            Object made = found.getConstructor(new Class<?>[0]).newInstance(new Object[0]);
            if (!(made instanceof LoginModule)) {
                throw new LoginException(
                    entry.getLoginModuleName() + " is not a javax.security.auth.spi.LoginModule");
            }
            return (LoginModule) made;
        } catch (LoginException e) {
            throw e;
        } catch (Exception e) {
            throw new LoginException(
                "Unable to instantiate LoginModule " + entry.getLoginModuleName() + ": " + e);
        }
    }

    /**
     * The handler named in the security property, or null.
     *
     * <p>Null is valid: a module that does not need to ask anything --one that reads a token from
     * the environment, for example-- works all the same without a handler.
     */
    private static CallbackHandler defaultHandler() {
        String className = null;
        try {
            className = java.security.Security.getProperty(DEFAULT_HANDLER);
        } catch (SecurityException e) {
            // Without permission to read it: carry on without a handler.
        }
        if (className == null || className.length() == 0) {
            return null;
        }
        try {
            Class<?> found = Class.forName(className);
            Object made = found.getConstructor(new Class<?>[0]).newInstance(new Object[0]);
            return (CallbackHandler) made;
        } catch (Exception e) {
            // A badly configured handler cannot bring down the start of the authentication.
            return null;
        }
    }

    /** The four phases, with the only thing that tells them apart. */
    private static final class Phase {

        /** Way in: verifies. */
        static final Phase LOGIN = new Phase(0, true);

        /** Way in: writes to the subject. */
        static final Phase COMMIT = new Phase(1, true);

        /** Way back: undoes whatever was started. */
        static final Phase ABORT = new Phase(2, false);

        /** Way back: logs out. */
        static final Phase LOGOUT = new Phase(3, false);

        private final int which;
        private final boolean stopsAtSufficient;

        private Phase(int which, boolean stopsAtSufficient) {
            this.which = which;
            this.stopsAtSufficient = stopsAtSufficient;
        }

        /** Whether a SUFFICIENT that succeeds cuts the walk. See the class note. */
        boolean stopsAtSufficient() {
            return this.stopsAtSufficient;
        }

        /** Calls this phase's method. */
        boolean call(LoginModule module) throws LoginException {
            if (this.which == 0) {
                return module.login();
            }
            if (this.which == 1) {
                return module.commit();
            }
            if (this.which == 2) {
                return module.abort();
            }
            return module.logout();
        }
    }
}
