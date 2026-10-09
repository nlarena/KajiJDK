package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.UnionType;

/**
 * The type-kind visitor for Java 7. See {@link TypeKindVisitor6} for the mechanism.
 *
 * <p>`visitUnion` enters the funnel, just as in {@link SimpleTypeVisitor7}. It has to be repeated
 * here because this branch of the family goes down through `TypeKindVisitor6`, which inherits the
 * throwing `visitUnion` from {@link SimpleTypeVisitor6} — version 7 of the simple visitor is not
 * among its ancestors.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class TypeKindVisitor7<R, P> extends TypeKindVisitor6<R, P> {

    @Deprecated(since = "12")
    protected TypeKindVisitor7() {
        super(null);
    }

    @Deprecated(since = "12")
    protected TypeKindVisitor7(R defaultValue) {
        super(defaultValue);
    }

    public R visitUnion(UnionType t, P p) {
        return this.defaultAction(t, p);
    }
}
