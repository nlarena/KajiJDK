package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.UnionType;

/**
 * The simple type visitor for Java 7. See {@link SimpleTypeVisitor6} for the mechanism.
 *
 * <p>With the union type in the language, `visitUnion` enters the funnel: it inherits from
 * {@link AbstractTypeVisitor6} a body that falls into `visitUnknown`, and here it goes to
 * `defaultAction`.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class SimpleTypeVisitor7<R, P> extends SimpleTypeVisitor6<R, P> {

    @Deprecated(since = "12")
    protected SimpleTypeVisitor7() {
        super(null);
    }

    @Deprecated(since = "12")
    protected SimpleTypeVisitor7(R defaultValue) {
        super(defaultValue);
    }

    public R visitUnion(UnionType t, P p) {
        return this.defaultAction(t, p);
    }
}
