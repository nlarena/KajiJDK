package javax.lang.model.util;

import java.util.List;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 * KajiLibrary's javax.lang.model.util.SimpleAnnotationValueVisitor6 — el visitante de valores de
 * anotacion para el que casi todos los casos dan lo mismo.
 *
 * <p>Mismo embudo que {@link SimpleElementVisitor6}, con una diferencia de firma que vale explicar:
 * {@link #defaultAction} toma **`Object`** y no un tipo del modelo. Es lo unico que puede tomar, porque
 * los trece `visitXxx` de esta familia no comparten un supertipo util — un `boolean`, un `String`, un
 * `TypeMirror` y una `List` no tienen nada en comun salvo `Object`.
 *
 * <p>La consecuencia es concreta y hay que verla venir: los ocho primitivos **se autoboxean** al entrar
 * al embudo. Una `defaultAction` que reciba `o` va a ver un `Integer`, no un `int`, y si quiere
 * distinguir el tipo original tiene que redefinir el `visitXxx` puntual en vez de mirar el `Object`.
 * `visitInt` y `visitShort` autoboxeados no se distinguen del todo por `instanceof`.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class SimpleAnnotationValueVisitor6<R, P> extends AbstractAnnotationValueVisitor6<R, P> {

    /** Lo que devuelve `defaultAction` mientras no la redefinan. */
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

    /** El embudo. Ver el encabezado por que toma `Object`. */
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
