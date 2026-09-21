package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * The simple annotation value visitor for Java 14 onwards. See {@link
 * SimpleAnnotationValueVisitor6}.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class SimpleAnnotationValueVisitor14<R, P> extends SimpleAnnotationValueVisitor9<R, P> {

    protected SimpleAnnotationValueVisitor14() {
        super(null);
    }

    protected SimpleAnnotationValueVisitor14(R defaultValue) {
        super(defaultValue);
    }
}
