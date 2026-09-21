package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * The type visitor for **preview** constructs. See {@link AbstractTypeVisitor6} for the mechanism
 * and {@link AbstractElementVisitorPreview} for what "preview" means here — it is reflective API,
 * used without `--enable-preview`.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public abstract class AbstractTypeVisitorPreview<R, P> extends AbstractTypeVisitor14<R, P> {

    protected AbstractTypeVisitorPreview() {
        super();
    }
}
