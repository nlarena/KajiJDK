package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.UnionType;

/**
 * The type visitor for Java 7. See {@link AbstractTypeVisitor6} for the mechanism.
 *
 * <p>Java 7 introduced the **union** type with multi-catch, so `visitUnion` becomes abstract:
 * whoever extends this class has to say what they do with `catch (A | B e)`.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public abstract class AbstractTypeVisitor7<R, P> extends AbstractTypeVisitor6<R, P> {

    @Deprecated(since = "12")
    protected AbstractTypeVisitor7() {
        super();
    }

    public abstract R visitUnion(UnionType t, P p);
}
