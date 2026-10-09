package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.VariableElement;

/**
 * The element-kind visitor for Java 7. See {@link ElementKindVisitor6} for the dispatch mechanism.
 *
 * <p>With try-with-resources in the language, `visitVariableAsResourceVariable` stops falling into
 * `visitUnknown` and enters the funnel.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class ElementKindVisitor7<R, P> extends ElementKindVisitor6<R, P> {

    @Deprecated(since = "12")
    protected ElementKindVisitor7() {
        super(null);
    }

    @Deprecated(since = "12")
    protected ElementKindVisitor7(R defaultValue) {
        super(defaultValue);
    }

    public R visitVariableAsResourceVariable(VariableElement e, P p) {
        return this.defaultAction(e, p);
    }
}
