package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * The simple type visitor for **preview** constructs. See {@link SimpleTypeVisitor6} for the
 * mechanism and {@link AbstractElementVisitorPreview} for what "preview" means here.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class SimpleTypeVisitorPreview<R, P> extends SimpleTypeVisitor14<R, P> {

    protected SimpleTypeVisitorPreview() {
        super(null);
    }

    protected SimpleTypeVisitorPreview(R defaultValue) {
        super(defaultValue);
    }
}
