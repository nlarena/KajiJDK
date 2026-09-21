package javax.naming.spi;

import java.io.Serializable;
import javax.naming.CompositeName;
import javax.naming.InvalidNameException;
import javax.naming.Name;

/**
 * KajiLibrary's javax.naming.spi.ResolveResult -- how far it got, and what is left.
 *
 * <p>What a {@link Resolver} returns: the object it could resolve to and the piece of the name left
 * unresolved. With both, the caller carries on resolving against the new object.
 *
 * <p>The two {@code append} methods exist because resolution goes <b>backwards</b> as it unwinds: a
 * context that cannot carry on adds to the remainder what it did not consume itself, and so the
 * final result accumulates everything missing from the point where it stopped.
 *
 * <p>The fields are {@code protected}, as in the JDK, for subclasses to touch directly. (An earlier
 * note named {@code CannotProceedException} as one of them; it extends {@code NamingException},
 * not this class.)
 */
public class ResolveResult implements Serializable {

    private static final long serialVersionUID = -4552108072002407559L;

    /** What it resolved to. */
    protected Object resolvedObj;

    /** What was left unresolved. */
    protected Name remainingName;

    /** Empty, for subclasses that fill themselves in later. */
    protected ResolveResult() {
        this.resolvedObj = null;
        this.remainingName = null;
    }

    /**
     * With the remainder as text.
     *
     * <p>The text is parsed as a {@link CompositeName}, which is the format of names that cross
     * different namespaces.
     */
    public ResolveResult(Object robj, String rcomp) {
        this.resolvedObj = robj;
        try {
            this.remainingName = new CompositeName(rcomp);
        } catch (InvalidNameException e) {
            // The specification does not allow throwing here. A name that does not parse is left
            // with no remainder, which is the only coherent thing: there is nowhere to carry on.
            this.remainingName = null;
        }
    }

    /** With the remainder already built. */
    public ResolveResult(Object robj, Name rname) {
        this.resolvedObj = robj;
        setRemainingName(rname);
    }

    /** What was left unresolved. */
    public Name getRemainingName() {
        return this.remainingName;
    }

    /** What it resolved to. */
    public Object getResolvedObj() {
        return this.resolvedObj;
    }

    /** Replaces the remainder. A copy is kept: the name is mutable. */
    public void setRemainingName(Name name) {
        if (name == null) {
            this.remainingName = null;
            return;
        }
        this.remainingName = (Name) name.clone();
    }

    /** Appends that to the end of the remainder. See the class note. */
    public void appendRemainingName(Name name) {
        if (name == null) {
            return;
        }
        if (this.remainingName == null) {
            this.remainingName = (Name) name.clone();
            return;
        }
        try {
            this.remainingName.addAll(name);
        } catch (InvalidNameException e) {
            // Cannot happen: it is appending to the end of a name of the same type.
        }
    }

    /** Same, with a single component. */
    public void appendRemainingComponent(String name) {
        if (name == null) {
            return;
        }
        try {
            if (this.remainingName == null) {
                this.remainingName = new CompositeName();
            }
            this.remainingName.add(name);
        } catch (InvalidNameException e) {
            // Same.
        }
    }

    /** Replaces the resolved object. */
    public void setResolvedObj(Object obj) {
        this.resolvedObj = obj;
    }
}
