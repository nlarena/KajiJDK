package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * The type-kind visitor for **preview** constructs. See {@link TypeKindVisitor6} for the mechanism
 * and {@link AbstractElementVisitorPreview} for what "preview" means here.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class TypeKindVisitorPreview<R, P> extends TypeKindVisitor14<R, P> {

    protected TypeKindVisitorPreview() {
        super(null);
    }

    protected TypeKindVisitorPreview(R defaultValue) {
        super(defaultValue);
    }
}
