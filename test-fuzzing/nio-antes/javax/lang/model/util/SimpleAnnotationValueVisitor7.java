package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * The simple annotation value visitor for Java 7. See {@link SimpleAnnotationValueVisitor6} for the
 * mechanism and {@link AbstractAnnotationValueVisitor6} for why no version of this family adds
 * anything.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class SimpleAnnotationValueVisitor7<R, P> extends SimpleAnnotationValueVisitor6<R, P> {

    @Deprecated(since = "12")
    protected SimpleAnnotationValueVisitor7() {
        super(null);
    }

    @Deprecated(since = "12")
    protected SimpleAnnotationValueVisitor7(R defaultValue) {
        super(defaultValue);
    }
}
