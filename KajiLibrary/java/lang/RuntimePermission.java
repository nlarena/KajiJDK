package java.lang;

import java.security.BasicPermission;

// The permission over the runtime's operations: "exitVM.0", "setIO", "createClassLoader",
// "getClassLoader", "accessDeclaredMembers", "loadLibrary.<name>", etc.
//
// It is a pure `BasicPermission`: all the logic --parsing the trailing wildcard, hierarchical
// `implies`, `equals`/`hashCode` by canonical name, a collection of its own-- already lives in the
// base class, and this one adds nothing but the type. That is not laziness: in
// `BasicPermission.implies` the first test is `p.getClass() != this.getClass()`, so **the type is
// what separates the name spaces**. Without a distinct class, a `RuntimePermission("exitVM")` and a
// `PropertyPermission("exitVM")` would be the same permission. That is why the class exists even
// though its body is empty.
//
// No actions: `getActions()` inherits the base's "". The targets with a variable part simulate them
// by putting it into the name --`"exitVM.0"`, `"loadLibrary.awt"`-- precisely so as not to need them,
// and out of that it falls for free that `"exitVM.*"` implies every exit code.
//
// `final`, as in the JDK: were it extensible, the subclass would fall into another name space and
// stop being implied by the `RuntimePermission`s that ought to cover it.
//
// What it does not do here: KajiJDK installs no `SecurityManager` (its constructor throws), so
// nobody consults these permissions at run time. The class is here for the type and because the
// permission model --constructing, comparing, putting into a `Permissions`-- does genuinely work. For
// that same reason it is marked for removal since 25, just as in the JDK: with no security manager
// evaluating them, the whole type is surface on its way out.
@Deprecated(since = "25", forRemoval = true)
public final class RuntimePermission extends BasicPermission {

    public RuntimePermission(String name) {
        super(name);
    }

    // `actions` is ignored, as in the JDK. The two-argument constructor exists because the policy
    // file format always passes an actions field, even though for this class it is
    // null o "".
    public RuntimePermission(String name, String actions) {
        super(name, actions);
    }
}
