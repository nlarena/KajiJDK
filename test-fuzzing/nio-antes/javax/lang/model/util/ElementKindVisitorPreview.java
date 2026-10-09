package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * The element-kind visitor for **preview** constructs. See {@link ElementKindVisitor6} for the
 * mechanism and {@link AbstractElementVisitorPreview} for what "preview" means here.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class ElementKindVisitorPreview<R, P> extends ElementKindVisitor14<R, P> {

    protected ElementKindVisitorPreview() {
        super(null);
    }

    protected ElementKindVisitorPreview(R defaultValue) {
        super(defaultValue);
    }
}
