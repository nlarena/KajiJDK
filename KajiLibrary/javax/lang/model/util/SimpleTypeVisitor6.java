package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

/**
 * KajiLibrary's javax.lang.model.util.SimpleTypeVisitor6 — the type visitor for which almost every
 * case gives the same.
 *
 * <p>Same funnel as {@link SimpleElementVisitor6}: each `visitXxx` falls into {@link
 * #defaultAction}, which returns `DEFAULT_VALUE`, and whoever extends overrides `defaultAction`
 * plus the particular case they care about. Why the funnel is explained there, and not repeated.
 *
 * <p>Unlike the element one, here **no** `visitXxx` sets a case aside: there is no equivalent of
 * `RESOURCE_VARIABLE`. The types that arrived after Java 6 — union and intersection — do not share
 * a method with an old one, they have their own, so they can be left in the `visitUnknown`
 * inherited from {@link AbstractTypeVisitor6} without ambiguity. A new kind coming in through an
 * old method, as happens with variables, is the exception and not the rule.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class SimpleTypeVisitor6<R, P> extends AbstractTypeVisitor6<R, P> {

    /** What `defaultAction` returns until it is overridden. */
    protected final R DEFAULT_VALUE;

    @Deprecated(since = "9")
    protected SimpleTypeVisitor6() {
        this.DEFAULT_VALUE = null;
    }

    @Deprecated(since = "9")
    protected SimpleTypeVisitor6(R defaultValue) {
        this.DEFAULT_VALUE = defaultValue;
    }

    /** The funnel. Overriding it is the way to treat all types alike. */
    protected R defaultAction(TypeMirror t, P p) {
        return this.DEFAULT_VALUE;
    }

    public R visitPrimitive(PrimitiveType t, P p) {
        return this.defaultAction(t, p);
    }

    public R visitNull(NullType t, P p) {
        return this.defaultAction(t, p);
    }

    public R visitArray(ArrayType t, P p) {
        return this.defaultAction(t, p);
    }

    public R visitDeclared(DeclaredType t, P p) {
        return this.defaultAction(t, p);
    }

    public R visitError(ErrorType t, P p) {
        return this.defaultAction(t, p);
    }

    public R visitTypeVariable(TypeVariable t, P p) {
        return this.defaultAction(t, p);
    }

    public R visitWildcard(WildcardType t, P p) {
        return this.defaultAction(t, p);
    }

    public R visitExecutable(ExecutableType t, P p) {
        return this.defaultAction(t, p);
    }

    public R visitNoType(NoType t, P p) {
        return this.defaultAction(t, p);
    }
}
