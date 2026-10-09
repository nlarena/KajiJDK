package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.IntersectionType;

/**
 * The type-kind visitor for Java 8. See {@link TypeKindVisitor6} for the mechanism.
 *
 * <p>`visitIntersection` enters the funnel, for the same branch reason {@link TypeKindVisitor7}
 * explains for the union.
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
