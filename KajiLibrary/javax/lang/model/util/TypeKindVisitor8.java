package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.IntersectionType;

/**
 * El visitante por kind de tipo de Java 8. Ver {@link TypeKindVisitor6} por el mecanismo.
 *
 * <p>`visitIntersection` entra al embudo, por la misma razon de rama que explica {@link TypeKindVisitor7}
 * para el union.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class TypeKindVisitor8<R, P> extends TypeKindVisitor7<R, P> {

    protected TypeKindVisitor8() {
        super(null);
    }

    protected TypeKindVisitor8(R defaultValue) {
        super(defaultValue);
    }

    public R visitIntersection(IntersectionType t, P p) {
        return this.defaultAction(t, p);
    }
}
