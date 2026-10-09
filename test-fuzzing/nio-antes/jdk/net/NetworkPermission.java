package jdk.net;

import java.security.BasicPermission;

/**
 * The permission that protects the socket options of {@link ExtendedSocketOptions}.
 *
 * <p>The extended options are not harmless: several touch the behaviour of the kernel, and
 * {@link ExtendedSocketOptions#SO_PEERCRED} returns the identity of another process. Hence using
 * them is an action with a permission of its own and not simply one more call.
 *
 * <p>It extends {@link BasicPermission}, so the name admits wildcards: {@code "*"} gives them all,
 * {@code "setOption.*"} gives those of one family. It has no actions — the second constructor
 * accepts them and ignores them, and it is there only because the permission mechanism builds by
 * reflection with two arguments.
 */
public final class NetworkPermission extends BasicPermission {

    private static final long serialVersionUID = -2004683231018171266L;

    /** A permission with that name. */
    public NetworkPermission(String name) {
        super(name);
    }

    /** The same; {@code actions} is ignored, and the JDK does likewise. */
    public NetworkPermission(String name, String actions) {
        super(name, actions);
    }
}
