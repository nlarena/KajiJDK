package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.IntersectionType;

/**
 * The simple type visitor for Java 8. See {@link SimpleTypeVisitor6} for the mechanism.
 *
 * <p>The intersection type gets what the union got in the 7 one: `visitIntersection` stops falling
 * into `visitUnknown` and goes to `defaultAction`.
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
