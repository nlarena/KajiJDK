package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * The element-kind visitor for Java 8. See {@link ElementKindVisitor6} for the mechanism.
 *
 * <p>Java 8 added no kinds, so there is nothing to pass to the funnel.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ElementKindVisitor8<R, P> extends ElementKindVisitor7<R, P> {

    protected ElementKindVisitor8() {
        super(null);
    }

    protected ElementKindVisitor8(R defaultValue) {
        super(defaultValue);
    }
}
