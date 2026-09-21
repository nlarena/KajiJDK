package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.VariableElement;

/**
 * The simple element visitor for Java 7. See {@link SimpleElementVisitor6} for the funnel
 * mechanism.
 *
 * <p>The only thing Java 7 adds here is that `RESOURCE_VARIABLE` **exists now**, so `visitVariable`
 * stops setting it aside and sends it to `defaultAction` like any other variable. The 6 class's
 * logic would be lying about what the visitor could have foreseen; in 7 it no longer does.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class SimpleElementVisitor7<R, P> extends SimpleElementVisitor6<R, P> {

    @Deprecated(since = "12")
    protected SimpleElementVisitor7() {
        super(null);
    }

    @Deprecated(since = "12")
    protected SimpleElementVisitor7(R defaultValue) {
        super(defaultValue);
    }

    public R visitVariable(VariableElement e, P p) {
        return this.defaultAction(e, p);
    }
}
