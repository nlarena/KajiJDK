package javax.security.auth;

import java.security.BasicPermission;

/**
 * KajiLibrary's javax.security.auth.AuthPermission -- permission for the authentication operations.
 *
 * <p>It is a {@link BasicPermission} and therefore has no actions: the name <b>is</b> the
 * permission. The names are {@code "doAs"}, {@code "getSubject"}, {@code
 * "createLoginContext.<name>"} and company, and as in every {@code BasicPermission} the final
 * {@code *} covers a prefix: {@code "createLoginContext.*"} implies {@code
 * "createLoginContext.Kaji"}.
 *
 * <p>There is a historical translation reproduced on purpose because it is observable: the bare
 * name {@code "createLoginContext"} is stored as {@code "createLoginContext.*"}. It comes from when
 * that permission did not carry the configuration's name; writing it without the dot today would
 * ask for a permission that does not exist, so the JDK interprets it as the wildcard instead of
 * leaving it useless.
 *
 * <p>A note on what it serves today: the security manager can no longer be enabled, so no check in
 * the library consults this permission. The class exists all the same because its form is part of
 * the API -- it is stored in policies, compared, serialized -- and because whoever writes their own
 * access control can use it like any other {@code Permission}.
 */
public final class AuthPermission extends BasicPermission {

    private static final long serialVersionUID = 5806031445061587174L;

    // See the class note: without this, the old name would imply nothing.
    private static String translated(String name) {
        return "createLoginContext".equals(name) ? "createLoginContext.*" : name;
    }

    public AuthPermission(String name) {
        super(translated(name));
    }

    /**
     * The actions are ignored: a {@code BasicPermission} has none. The constructor exists because
     * the policy loader builds every permission with two arguments and does not know which use
     * them.
     */
    public AuthPermission(String name, String actions) {
        super(translated(name), actions);
    }
}
