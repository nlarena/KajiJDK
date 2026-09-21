package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * The annotation value visitor for **preview** constructs. See {@link
 * AbstractAnnotationValueVisitor6} for the mechanism and {@link AbstractElementVisitorPreview} for
 * what "preview" means here.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public abstract class AbstractAnnotationValueVisitorPreview<R, P>
        extends AbstractAnnotationValueVisitor14<R, P> {

    protected AbstractAnnotationValueVisitorPreview() {
        super();
    }
}
