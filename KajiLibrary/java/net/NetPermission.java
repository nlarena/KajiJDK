package java.net;

import java.security.BasicPermission;

// The "network" permissions that are not really about the network: they are permissions to
// **reconfigure the platform**.
//
// The names the JDK defines are things like "setDefaultAuthenticator", "setCookieHandler" or
// "setProxySelector": every one of them authorizes installing a global callback. And there lies the
// reason they exist -- whoever can replace the VM's `Authenticator` sees everyone's passwords, and
// whoever can replace the `ProxySelector` diverts all the traffic. No network is needed for that to
// be dangerous, and none is needed to represent it.
//
// All the logic --hierarchical names, the `*` wildcard, no actions-- is `BasicPermission`'s. This
// class exists in order to be a **distinct type**: a policy granting "setDefaultAuthenticator" should
// not also grant a system property with the same name.
//
// Nothing omitted.
//
// @deprecated The Security Manager is deprecated for removal; these permissions are no longer
// checked.
@Deprecated
public final class NetPermission extends BasicPermission {

    private static final long serialVersionUID = -8343910153355041693L;

    /**
     * @throws IllegalArgumentException if the name is empty
     * @throws NullPointerException if the name is null
     */
    public NetPermission(String name) {
        super(name);
    }

    /**
     * Like the other constructor; {@code actions} is ignored because this class has no actions.
     *
     * <p>It exists for deserialization and for chaining from subclasses, not because the argument
     * means anything.
     */
    public NetPermission(String name, String actions) {
        super(name, actions);
    }
}
