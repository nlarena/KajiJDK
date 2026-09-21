package java.util.logging;

/**
 * KajiLibrary's java.util.logging.LoggingPermission -- the package's only permission.
 *
 * <p>It has a single valid name, `"control"`, and no actions. That poverty is the design: the
 * package does not tell "may read the configuration" from "may change it", because changing one
 * logger's level is already enough to switch off another's audit log. If there is a single thing to
 * protect, there is a single permission.
 *
 * <p>The constructor **rejects** any other name and any non-empty action rather than ignoring them.
 * It is the right thing for a permission: a misspelled `new LoggingPermission("controll", null)`
 * that was constructed in silence would be a permission that never implies anything and a policy
 * that seems to say something and says nothing.
 *
 * <p>It is deprecated for removal in the JDK along with the security manager, which was the only
 * thing that consulted it. It is brought in all the same because it is still part of the API and
 * because {@link java.security.BasicPermission} --where all the implication logic comes from-- is
 * complete in this tree: there is nothing to simulate here.
 */
@Deprecated(since = "17", forRemoval = true)
public final class LoggingPermission extends java.security.BasicPermission {

    /**
     * @throws NullPointerException if `name` is `null`
     * @throws IllegalArgumentException if `name` is not `"control"`, or if `actions` is not empty
     */
    public LoggingPermission(String name, String actions) throws IllegalArgumentException {
        super(name);
        if (!name.equals("control")) {
            throw new IllegalArgumentException("name: " + name);
        }
        if (actions != null && actions.length() > 0) {
            throw new IllegalArgumentException("actions: " + actions);
        }
    }
}
