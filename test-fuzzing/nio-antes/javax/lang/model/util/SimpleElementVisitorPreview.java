package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * The simple element visitor for **preview** constructs. See {@link SimpleElementVisitor6} for the
 * mechanism and {@link AbstractElementVisitorPreview} for what "preview" means here.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class SimpleElementVisitorPreview<R, P> extends SimpleElementVisitor14<R, P> {

    protected SimpleElementVisitorPreview() {
        super(null);
    }

    protected SimpleElementVisitorPreview(R defaultValue) {
        super(defaultValue);
    }
}
