package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.UnknownTypeException;

/**
 * KajiLibrary's javax.lang.model.util.AbstractTypeVisitor6 — the base of the type visitor family.
 *
 * <p>Same mechanism as {@link AbstractElementVisitor6}, applied to {@link TypeVisitor}: one class
 * per language version, what existed in that version abstract, what came later with a body that
 * falls into {@link #visitUnknown} and throws. Why is explained there, and not repeated.
 *
 * <p>What changes is **which** forms of type kept arriving, which are not the same as the kinds of
 * declaration:
 *
 * <ul>
 * <li>The **union** type is from Java 7, from multi-catch: in `catch (A | B e)`, the type of `e` is
 *     neither `A` nor `B` but the union of both.</li>
 * <li>The **intersection** type is from Java 8, from multiple bounds: in `&lt;T extends A &amp;
 *     B&gt;`, the type of `T` is the intersection.</li>
 * </ul>
 *
 * <p>Both appear here with a body that delegates to `visitUnknown`, and become abstract in the
 * class of the version that introduced them — 7 and 8 respectively.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public abstract class AbstractTypeVisitor6<R, P> implements TypeVisitor<R, P> {

    protected AbstractTypeVisitor6() {
    }

    /**
     * The dispatch: the one that knows which form of type it is, the implementation of `accept`.
     */
    public final R visit(TypeMirror t, P p) {
        return t.accept(this, p);
    }

    /** The same, with a null parameter. */
    public final R visit(TypeMirror t) {
        return t.accept(this, null);
    }

    public R visitUnion(UnionType t, P p) {
        return this.visitUnknown(t, p);
    }

    public R visitIntersection(IntersectionType t, P p) {
        return this.visitUnknown(t, p);
    }

    public R visitUnknown(TypeMirror t, P p) {
        throw new UnknownTypeException(t, p);
    }
}
