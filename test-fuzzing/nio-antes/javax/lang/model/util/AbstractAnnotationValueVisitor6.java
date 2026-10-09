package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.UnknownAnnotationValueException;

/**
 * KajiLibrary's javax.lang.model.util.AbstractAnnotationValueVisitor6 — the base of the annotation
 * value visitor family.
 *
 * <p>Same mechanism as {@link AbstractElementVisitor6}, applied to {@link AnnotationValueVisitor}.
 *
 * <p>It is the quietest family of the three, for a reason worth saying: the set of things that can
 * be the value of an annotation element has been **closed by the specification** since annotations
 * exist. They are the eight primitives, `String`, a class, an enum constant, another annotation, or
 * an array of those. The language never widened it — neither records nor modules nor sealed types
 * added a form of value — so from 7 to `Preview` there is not a single difference of contract.
 *
 * <p>That the five subclasses are identical is not carelessness of the API: they are the version
 * number, which serves to date the visitor and to let {@code @SupportedSourceVersion} tell the
 * truth.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public abstract class AbstractAnnotationValueVisitor6<R, P>
        implements AnnotationValueVisitor<R, P> {

    protected AbstractAnnotationValueVisitor6() {
    }

    /**
     * The dispatch: the one that knows which form of value it is, the implementation of `accept`.
     */
    public final R visit(AnnotationValue av, P p) {
        return av.accept(this, p);
    }

    /** The same, with a null parameter. */
    public final R visit(AnnotationValue av) {
        return av.accept(this, null);
    }

    public R visitUnknown(AnnotationValue av, P p) {
        throw new UnknownAnnotationValueException(av, p);
    }
}
