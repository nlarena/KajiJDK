package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.IntersectionType;

/**
 * The type visitor for Java 8. See {@link AbstractTypeVisitor6} for the mechanism.
 *
 * <p>Java 8 made the **intersection** type denotable — that of `&lt;T extends A &amp; B&gt;` and
 * that of casts with multiple bounds — so `visitIntersection` becomes abstract.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public abstract class AbstractTypeVisitor8<R, P> extends AbstractTypeVisitor7<R, P> {

    protected AbstractTypeVisitor8() {
        super();
    }

    public abstract R visitIntersection(IntersectionType t, P p);
}
