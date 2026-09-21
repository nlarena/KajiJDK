package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * The type visitor for Java 14 onwards. See {@link AbstractTypeVisitor6} for the mechanism.
 *
 * <p>Records did not bring a new form of type either: a record's type is a `DeclaredType` like any
 * class's. None has appeared since Java 8, and that is why this class and the 9 one add nothing —
 * the type family settled much earlier than the element one.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public abstract class AbstractTypeVisitor14<R, P> extends AbstractTypeVisitor9<R, P> {

    protected AbstractTypeVisitor14() {
        super();
    }
}
