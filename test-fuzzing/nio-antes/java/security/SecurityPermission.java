package java.security;

// A permission over the security subsystem itself: adding providers, touching the policy, reading
// or writing properties of `Security`.
//
// It is the permission to look at twice in a policy, because almost all of its names are ladders:
// `insertProvider` lets one put in a provider of one's own and therefore reimplement any algorithm;
// `setPolicy` lets one rewrite the rest of the permissions. Granting it amounts to granting
// `AllPermission` by a longer road.
//
// With no actions —it inherits the "" of `BasicPermission`— and `final`, as in the JDK: the set of
// names belongs to the system and a subclass that widened it would be inventing authority.
public final class SecurityPermission extends BasicPermission {

    public SecurityPermission(String name) {
        super(name);
    }

    // `actions` is ignored; it exists so that the policy loader can build it by reflection with the
    // same signature as any other permission.
    public SecurityPermission(String name, String actions) {
        super(name, actions);
    }
}
