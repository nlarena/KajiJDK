package javax.naming;

import java.util.Hashtable;

/**
 * "This is as far as I go": the exception with which one provider hands resolution to another.
 *
 * <p>A composite name can cross namespaces of different providers --`ldap` up to a point and then a
 * file system, for example. When the first one reaches the edge of its own it can neither fail nor
 * make things up: it throws this, and what it carries is **exactly what another needs to carry
 * on**, which is more than an error message.
 *
 * <p>The four fields of its own are the four pieces of that continuation:
 *
 * <ul>
 *   <li>`remainingNewName`, for the operations that have **two** names (`rename`): the inherited
 *       `remainingName` tracks the source, this one tracks the destination.
 *   <li>`environment`, because whoever continues needs the properties of the one that was
 *       resolving.
 *   <li>`altName` and `altNameCtx`, which are the same cut point seen from the other side: the name
 *       of the resolved object **relative to** `altNameCtx`. Without that pair, whoever continues
 *       would have an object but would not know what it is called in the namespace it comes from.
 * </ul>
 *
 * <p>It is used by `javax.naming.spi.NamingManager.getContinuationContext`, the function that takes
 * this exception and returns the context to carry on in. That method is implemented in this
 * library: it asks the object factories for a `Context` built from `getResolvedObj()` and rethrows
 * this exception when none comes out. (An earlier note said the method was not declared here; the
 * `javax.naming.spi` package has since been added.)
 *
 * <p>The rest of the hierarchy is explained in `NamingException`.
 */
public class CannotProceedException extends NamingException {

    private static final long serialVersionUID = 1219724816191576813L;

    /** The "remaining name" of the **second** name, for two-name operations like `rename`. */
    protected Name remainingNewName;

    /** The environment resolution was running with; whoever continues needs it too. */
    protected Hashtable<?, ?> environment;

    /**
     * The name of the resolved object, but relative to `altNameCtx` and not the original context.
     */
    protected Name altName;

    /** The context `altName` is read against. */
    protected Context altNameCtx;

    public CannotProceedException(String explanation) {
        super(explanation);
    }

    public CannotProceedException() {
        super();
    }

    public Hashtable<?, ?> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Hashtable<?, ?> environment) {
        this.environment = environment;
    }

    public Name getRemainingNewName() {
        return remainingNewName;
    }

    /** Clones, like `NamingException`'s name setters and for the same reason. */
    public void setRemainingNewName(Name newName) {
        remainingNewName = (newName != null) ? (Name) newName.clone() : null;
    }

    public Name getAltName() {
        return altName;
    }

    public void setAltName(Name altName) {
        this.altName = altName;
    }

    public Context getAltNameCtx() {
        return altNameCtx;
    }

    public void setAltNameCtx(Context altNameCtx) {
        this.altNameCtx = altNameCtx;
    }
}
