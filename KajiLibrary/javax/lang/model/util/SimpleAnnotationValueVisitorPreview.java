package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * The simple annotation value visitor for **preview** constructs. See {@link
 * SimpleAnnotationValueVisitor6} for the mechanism and {@link AbstractElementVisitorPreview} for
 * what "preview" means here.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class SimpleAnnotationValueVisitorPreview<R, P>
        extends SimpleAnnotationValueVisitor14<R, P> {

    protected SimpleAnnotationValueVisitorPreview() {
        super(null);
    }

    protected SimpleAnnotationValueVisitorPreview(R defaultValue) {
        super(defaultValue);
    }
}
