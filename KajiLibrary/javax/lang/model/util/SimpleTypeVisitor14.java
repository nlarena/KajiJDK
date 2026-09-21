package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * The simple type visitor for Java 14 onwards. See {@link SimpleTypeVisitor6} for the mechanism.
 *
 * <p>It adds nothing: no new form of type has appeared since Java 8.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class SimpleTypeVisitor14<R, P> extends SimpleTypeVisitor9<R, P> {

    protected SimpleTypeVisitor14() {
        super(null);
    }

    protected SimpleTypeVisitor14(R defaultValue) {
        super(defaultValue);
    }
}
