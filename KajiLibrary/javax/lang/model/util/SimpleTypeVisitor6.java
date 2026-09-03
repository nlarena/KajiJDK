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
 * KajiLibrary's javax.lang.model.util.SimpleTypeVisitor6 — el visitante de tipos para el que casi todos
 * los casos dan lo mismo.
 *
 * <p>Mismo embudo que {@link SimpleElementVisitor6}: cada `visitXxx` cae en {@link #defaultAction}, que
 * devuelve `DEFAULT_VALUE`, y el que extiende redefine `defaultAction` mas el caso puntual que le
 * interesa. Ahi esta explicado por que el embudo, y no se repite.
 *
 * <p>A diferencia del de elementos, aca **ningun** `visitXxx` aparta un caso: no hay un equivalente de
 * `RESOURCE_VARIABLE`. Los tipos que llegaron despues de Java 6 — union e interseccion — no comparten
 * metodo con uno viejo, tienen el suyo propio, asi que se pueden dejar en el `visitUnknown` que hereda
 * de {@link AbstractTypeVisitor6} sin ambiguedad. Que un kind nuevo entre por un metodo viejo, como
 * pasa con las variables, es la excepcion y no la regla.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class SimpleTypeVisitor6<R, P> extends AbstractTypeVisitor6<R, P> {

    /** Lo que devuelve `defaultAction` mientras no la redefinan. */
    protected final R DEFAULT_VALUE;

    @Deprecated(since = "9")
    protected SimpleTypeVisitor6() {
        this.DEFAULT_VALUE = null;
    }

    @Deprecated(since = "9")
    protected SimpleTypeVisitor6(R defaultValue) {
        this.DEFAULT_VALUE = defaultValue;
    }

    /** El embudo. Redefinirla es la manera de tratar todos los tipos igual. */
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
