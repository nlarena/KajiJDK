package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * The simple annotation value visitor for Java 8. See {@link SimpleAnnotationValueVisitor6}.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class SimpleAnnotationValueVisitor8<R, P> extends SimpleAnnotationValueVisitor7<R, P> {

    protected SimpleAnnotationValueVisitor8() {
        super(null);
    }

    protected SimpleAnnotationValueVisitor8(R defaultValue) {
        super(defaultValue);
    }
}
