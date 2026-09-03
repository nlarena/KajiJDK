package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.IntersectionType;

/**
 * El visitante simple de tipos de Java 8. Ver {@link SimpleTypeVisitor6} por el mecanismo.
 *
 * <p>Le toca al tipo interseccion lo que en la de 7 le toco al union: `visitIntersection` deja de caer
 * en `visitUnknown` y pasa a `defaultAction`.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class SimpleTypeVisitor8<R, P> extends SimpleTypeVisitor7<R, P> {

    protected SimpleTypeVisitor8() {
        super(null);
    }

    protected SimpleTypeVisitor8(R defaultValue) {
        super(defaultValue);
    }

    public R visitIntersection(IntersectionType t, P p) {
        return this.defaultAction(t, p);
    }
}
