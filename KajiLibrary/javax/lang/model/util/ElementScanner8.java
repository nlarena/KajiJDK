package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * The element scanner for Java 8. See {@link ElementScanner6} for the mechanism.
 *
 * <p>Java 8 added no declarations nor changed who is a child of whom, so the walk is the same.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ElementScanner8<R, P> extends ElementScanner7<R, P> {

    protected ElementScanner8() {
        super(null);
    }

    protected ElementScanner8(R defaultValue) {
        super(defaultValue);
    }
}
