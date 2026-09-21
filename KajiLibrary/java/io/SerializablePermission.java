package java.io;

import java.security.BasicPermission;

// KajiLibrary's java.io.SerializablePermission -- the permission for touching the serialization
// machinery.
//
// The two names that exist say well why a permission is needed:
//
//   - `enableSubclassImplementation`, for subclassing
//     `ObjectOutputStream`/`ObjectInputStream` and overriding how objects are written or read.
//     Whoever can do that can change what an object *says* it is when deserialized.
//   - `enableSubstitution`, for `enableReplaceObject`/`enableResolveObject`, that is, for swapping
//     one object for another in mid-flight.
//
// Both break the guarantee that what goes into a stream is what comes out, and that is why they are
// permissions and not ordinary methods.
//
// **No actions**: it inherits from `BasicPermission`, so `implies` is a name comparison with
// wildcards (a trailing `*`) and `getActions()` returns the empty string. The two-argument
// constructor exists only because `Permission`'s contract asks for it; it ignores the second one,
// as in the JDK.
public final class SerializablePermission extends BasicPermission {

    public SerializablePermission(String name) {
        super(name);
    }

    /** @param actions unused; it is here for uniformity with the rest of the permissions */
    public SerializablePermission(String name, String actions) {
        super(name, actions);
    }
}
