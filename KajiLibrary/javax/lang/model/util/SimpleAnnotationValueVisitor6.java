package javax.lang.model.util;

import java.util.List;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 * KajiLibrary's javax.lang.model.util.SimpleAnnotationValueVisitor6 — the annotation value visitor
 * for which almost every case gives the same.
 *
 * <p>Same funnel as {@link SimpleElementVisitor6}, with a difference of signature worth explaining:
 * {@link #defaultAction} takes an **`Object`** and not a model type. It is the only thing it can
 * take, because the thirteen `visitXxx` of this family share no useful supertype — a `boolean`, a
 * `String`, a `TypeMirror` and a `List` have nothing in common but `Object`.
 *
 * <p>The consequence is concrete and has to be seen coming: the eight primitives **are autoboxed**
 * on entering the funnel. A `defaultAction` that receives `o` is going to see an `Integer`, not an
 * `int`, and if it wants to tell the original type it has to override the particular `visitXxx`
 * instead of looking at the `Object`. Autoboxed, `visitInt` and `visitShort` cannot be fully told
 * apart with `instanceof`.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class SimpleAnnotationValueVisitor6<R, P> extends AbstractAnnotationValueVisitor6<R, P> {

    /** What `defaultAction` returns until it is overridden. */
    protected final R DEFAULT_VALUE;

    @Deprecated(since = "9")
    protected SimpleAnnotationValueVisitor6() {
        super();
        this.DEFAULT_VALUE = null;
    }

    @Deprecated(since = "9")
    protected SimpleAnnotationValueVisitor6(R defaultValue) {
        super();
        this.DEFAULT_VALUE = defaultValue;
    }

    /** The funnel. See the header for why it takes `Object`. */
    protected R defaultAction(Object o, P p) {
        return this.DEFAULT_VALUE;
    }

    public R visitBoolean(boolean b, P p) {
        return this.defaultAction(b, p);
    }

    public R visitByte(byte b, P p) {
        return this.defaultAction(b, p);
    }

    public R visitChar(char c, P p) {
        return this.defaultAction(c, p);
    }

    public R visitDouble(double d, P p) {
        return this.defaultAction(d, p);
    }

    public R visitFloat(float f, P p) {
        return this.defaultAction(f, p);
    }

    public R visitInt(int i, P p) {
        return this.defaultAction(i, p);
    }

    public R visitLong(long i, P p) {
        return this.defaultAction(i, p);
    }

    public R visitShort(short s, P p) {
        return this.defaultAction(s, p);
    }

    public R visitString(String s, P p) {
        return this.defaultAction(s, p);
    }

    public R visitType(TypeMirror t, P p) {
        return this.defaultAction(t, p);
    }

    public R visitEnumConstant(VariableElement c, P p) {
        return this.defaultAction(c, p);
    }

    public R visitAnnotation(AnnotationMirror a, P p) {
        return this.defaultAction(a, p);
    }

    public R visitArray(List<? extends AnnotationValue> vals, P p) {
        return this.defaultAction(vals, p);
    }
}
