package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * The element visitor for Java 8. See {@link AbstractElementVisitor6} for the mechanism.
 *
 * <p>Java 8 did not add kinds of declaration either. Its things were lambdas and `default` methods,
 * and neither is a new declaration for the model: a lambda has no element of its own — it is an
 * expression — and a `default` is an `ExecutableElement` with one more modifier.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public abstract class AbstractElementVisitor8<R, P> extends AbstractElementVisitor7<R, P> {

    protected AbstractElementVisitor8() {
        super();
    }
}
