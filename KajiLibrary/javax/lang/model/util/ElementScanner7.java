package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.VariableElement;

/**
 * The element scanner for Java 7. See {@link ElementScanner6} for the walk mechanism.
 *
 * <p>With `RESOURCE_VARIABLE` in the language, `visitVariable` stops setting it aside and walks it
 * like any other variable.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class ElementScanner7<R, P> extends ElementScanner6<R, P> {

    @Deprecated(since = "12")
    protected ElementScanner7() {
        super(null);
    }

    @Deprecated(since = "12")
    protected ElementScanner7(R defaultValue) {
        super(defaultValue);
    }

    public R visitVariable(VariableElement e, P p) {
        return this.scan(e.getEnclosedElements(), p);
    }
}
