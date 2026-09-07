package java.awt.font;

import java.awt.geom.AffineTransform;
import java.io.Serializable;

/**
 * An {@link AffineTransform} wrapped so that it can be used as a text attribute's value.
 *
 * <p>The wrapper is not bureaucracy. `AffineTransform` is **mutable**, and a text attribute is kept
 * in maps and shared between stretches: if the value were the transform itself, changing it from
 * outside would change the style of everything using it. Here it is copied on the way in and on the
 * way out, so what is kept is a value.
 *
 * <p>An identity transform is kept as `null` inside. It is the way for {@link #isIdentity} to be a
 * comparison against `null` and not a walk over six coefficients, which is a question asked for every
 * stretch of text that is drawn.
 */
public final class TransformAttribute implements Serializable {

    private static final long serialVersionUID = 3356247357827709530L;

    /** The transform that does nothing. */
    public static final TransformAttribute IDENTITY = new TransformAttribute(null);

    private final AffineTransform transform;

    /**
     * With the given transform, which is copied.
     *
     * <p>It accepts `null` and takes it as the identity. The JDK's documentation says it throws, but
     * its implementation accepts it, and what it does is what counts.
     */
    public TransformAttribute(AffineTransform transform) {
        if (transform != null && !transform.isIdentity()) {
            this.transform = new AffineTransform(transform);
        } else {
            this.transform = null;
        }
    }

    /** A copy of the transform; the identity if there is none. */
    public AffineTransform getTransform() {
        AffineTransform at = this.transform;
        if (at == null) {
            return new AffineTransform();
        }
        return new AffineTransform(at);
    }

    /** Whether it does nothing. */
    public boolean isIdentity() {
        return this.transform == null;
    }

    public int hashCode() {
        if (this.transform == null) {
            return 0;
        }
        return this.transform.hashCode();
    }

    /** Equality by the transform it wraps. */
    public boolean equals(Object rhs) {
        if (rhs == null) {
            return false;
        }
        if (rhs == this) {
            return true;
        }
        if (rhs.getClass() != this.getClass()) {
            return false;
        }
        TransformAttribute that = (TransformAttribute) rhs;
        if (this.transform == null) {
            return that.transform == null;
        }
        return this.transform.equals(that.transform);
    }
}
