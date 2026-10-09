package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * The type-kind visitor for Java 14 onwards. See {@link TypeKindVisitor6} for the mechanism.
 *
 * <p>It adds nothing: neither records nor anything later brought a new `TypeKind`.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class TypeKindVisitor14<R, P> extends TypeKindVisitor9<R, P> {

    protected TypeKindVisitor14() {
        super(null);
    }

    protected TypeKindVisitor14(R defaultValue) {
        super(defaultValue);
    }
}
