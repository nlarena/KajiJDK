package javax.security.auth.login;

import java.util.Collections;
import java.util.Map;

/**
 * KajiLibrary's javax.security.auth.login.AppConfigurationEntry -- a module in the login chain.
 *
 * <p>Three things: which module to load, how mandatory it is, and with which options. An
 * application authenticates against a <b>list</b> of these, and the flags of {@link
 * LoginModuleControlFlag} are what make the list a program and not an enumeration.
 *
 * <h2>The map is not copied</h2>
 *
 * <p>{@link #getOptions} returns an <b>unmodifiable view</b> of the map that was passed, not a
 * copy. The difference shows: whoever built the entry can keep changing their map and those changes
 * are seen from here. It is how it is specified and so it is replicated, but it is as well to pass
 * it a map one is not going to touch any more.
 */
public class AppConfigurationEntry {

    private final String loginModuleName;
    private final LoginModuleControlFlag controlFlag;
    private final Map<String, ?> options;

    /**
     * @param loginModuleName the fully qualified name of the module's class
     * @param controlFlag how mandatory it is; see {@link LoginModuleControlFlag}
     * @param options what is passed to the module when initializing it
     * @throws IllegalArgumentException if the name is null or empty, or if the flag or the options
     *     are null
     */
    public AppConfigurationEntry(String loginModuleName, LoginModuleControlFlag controlFlag,
                                 Map<String, ?> options) {
        if (loginModuleName == null || loginModuleName.length() == 0) {
            throw new IllegalArgumentException("invalid null or empty LoginModule name");
        }
        if (controlFlag == null) {
            throw new IllegalArgumentException("invalid null ControlFlag");
        }
        if (options == null) {
            throw new IllegalArgumentException("invalid null options");
        }
        this.loginModuleName = loginModuleName;
        this.controlFlag = controlFlag;
        // A view, not a copy; see the class note.
        this.options = Collections.unmodifiableMap(options);
    }

    /** The name of the module's class. */
    public String getLoginModuleName() {
        return this.loginModuleName;
    }

    /** How mandatory it is. */
    public LoginModuleControlFlag getControlFlag() {
        return this.controlFlag;
    }

    /** The options, without being able to modify them. See the class note. */
    public Map<String, ?> getOptions() {
        return this.options;
    }

    /**
     * How mandatory a module is within the chain.
     *
     * <p>The four flags answer two independent questions: whether the module <b>has</b> to succeed
     * for the login to go through, and whether its result <b>cuts</b> the walk of the list.
     *
     * <ul>
     *   <li><b>REQUIRED</b> -- it has to succeed; if it fails, the chain goes on all the same.
     *       Going on is on purpose: if it cut, whoever tries to get in could deduce <b>which</b>
     *       module rejected them from how quickly the answer comes back;
     *   <li><b>REQUISITE</b> -- it has to succeed and it also cuts on failure. It is used when the
     *       modules that follow make no sense without this one;
     *   <li><b>SUFFICIENT</b> -- it does not need to succeed, but if it does --and no mandatory one
     *       failed before-- that is enough and the walk is cut there;
     *   <li><b>OPTIONAL</b> -- it neither needs to succeed nor cuts. It serves to gather data in
     *       the subject without that deciding anything.
     * </ul>
     *
     * <p>It is not an enum because it is older than enums: JAAS entered the platform in 1.4 and
     * enums arrived in Java 5. (The note said the class was from 1999 and that making it an enum
     * now would break the serialization of saved configurations; the JDK marks it {@code @since
     * 1.4} and it is not {@code Serializable}, neither there nor here.)
     */
    public static class LoginModuleControlFlag {

        private final String name;

        /** It has to succeed; it does not cut. */
        public static final LoginModuleControlFlag REQUIRED =
            new LoginModuleControlFlag("required");

        /** It has to succeed; it cuts on failure. */
        public static final LoginModuleControlFlag REQUISITE =
            new LoginModuleControlFlag("requisite");

        /** It does not need to; it cuts on success. */
        public static final LoginModuleControlFlag SUFFICIENT =
            new LoginModuleControlFlag("sufficient");

        /** It neither needs to succeed nor cuts. */
        public static final LoginModuleControlFlag OPTIONAL =
            new LoginModuleControlFlag("optional");

        /** Private: the four constants are the only ones there are. */
        private LoginModuleControlFlag(String name) {
            this.name = name;
        }

        /** The form whoever reads a log expects: {@code "LoginModuleControlFlag: required"}. */
        public String toString() {
            return "LoginModuleControlFlag: " + this.name;
        }
    }
}
