package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * The simple element visitor for Java 8. See {@link SimpleElementVisitor6} for the mechanism.
 *
 * <p>Java 8 added no element kinds nor kinds of declaration, so there is nothing to reroute.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class SimpleElementVisitor8<R, P> extends SimpleElementVisitor7<R, P> {

    protected SimpleElementVisitor8() {
        super(null);
    }

    protected SimpleElementVisitor8(R defaultValue) {
        super(defaultValue);
    }
}
