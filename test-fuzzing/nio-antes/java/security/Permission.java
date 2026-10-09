package java.security;

import java.io.Serializable;

// A permission: a name, some optional actions, and the rule of when one permission **implies**
// another.
//
// `implies` is the heart of the whole model and what tells it apart from a simple list of labels:
// having `FilePermission("/tmp/*", "read")` implies having
// `FilePermission("/tmp/x.txt", "read")`, without anybody having enumerated the second. An access
// check is always "does any of the permissions I have imply the one that is needed?".
//
// The class is abstract because that rule cannot be written in general: each class of permission
// has its own notion of "wider than". `BasicPermission` gives the one of hierarchical names with a
// wildcard, which is the one almost all of them use.
//
// **Note on the state of the model**: since JDK 24 the `SecurityManager` is permanently disabled,
// so these classes are still in the signatures of the JDK but no longer govern anything at runtime.
// They are implemented because they are contract —20 packages of `java.base` name them— not because
// they enforce anything. `checkGuard` says so explicitly further down.
public abstract class Permission implements Guard, Serializable {

    // The name of the permission. Its meaning depends on the subclass: a path, a property, a host.
    // Final: changing it would turn the permission into another one.
    private final String name;

    // A permission with the given name.
    public Permission(String name) {
        this.name = name;
    }

    // It watches over the access to `object`. **It always throws.**
    //
    // It looks the wrong way round and it is not. Since the `SecurityManager` was left permanently
    // disabled (JDK 24) there is nobody to ask whether the permission is granted, and faced with
    // that question there are two possible answers: let it through or deny. JDK 25 denies —it
    // throws `SecurityException("checking permissions is not supported")`— and it is the only right
    // one: a `GuardedObject` exists **so that** somebody decides, and a guard that cannot decide
    // and lets things through turns each of those objects into an unprotected object, silently and
    // without the code that built it finding out.
    //
    // This class said the opposite until the behaviour test compared it against the real JDK: it
    // returned without doing anything, with a comment asserting that that was what the JDK did. It
    // was not. The difference showed exactly where it matters: a `GuardedObject` the JDK closes was
    // opened here.
    public void checkGuard(Object object) throws SecurityException {
        throw new SecurityException("checking permissions is not supported");
    }

    // Whether this permission implies the other. It is the only question an access check asks.
    public abstract boolean implies(Permission permission);

    // Abstract on purpose, although `Object` has it already: two permissions of the same class with
    // the same name and actions **must** be equal, because if not a collection of permissions would
    // keep duplicates that imply the same thing. Forcing the subclass to write it is the way of
    // keeping nobody from inheriting `Object`'s by oversight.
    public abstract boolean equals(Object obj);

    // Just like `equals`: abstract so that it is coherent with it.
    public abstract int hashCode();

    // The name of this permission.
    public final String getName() {
        return this.name;
    }

    // The actions, as a canonical string; "" if this class does not use them.
    public abstract String getActions();

    // An empty collection suitable for keeping permissions of this class, or null if any will do.
    //
    // It exists because some classes can answer `implies` much faster over a set than by asking
    // each permission one at a time. `null` means "I have nothing better", and the caller uses a
    // generic collection.
    public PermissionCollection newPermissionCollection() {
        return null;
    }

    // `("class" "name")`, or `("class" "name" "actions")` if it has actions.
    public String toString() {
        String actions = this.getActions();
        if (actions == null || actions.length() == 0) {
            return "(\"" + this.getClass().getName() + "\" \"" + this.name + "\")";
        }
        return "(\"" + this.getClass().getName() + "\" \"" + this.name + "\" \"" + actions + "\")";
    }
}
