package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * The element visitor for Java 7. See {@link AbstractElementVisitor6} for the family's mechanism.
 *
 * <p>Java 7 added no kind of declaration. Try-with-resources added an `ElementKind`,
 * `RESOURCE_VARIABLE`, but a resource variable is still a `VariableElement` and `visitVariable`
 * takes it: a new kind is not a new method. That is why this class adds nothing — it exists so that
 * a visitor can **date** which language version it was written against, which is information even
 * if the contract does not change.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public abstract class AbstractElementVisitor7<R, P> extends AbstractElementVisitor6<R, P> {

    @Deprecated(since = "12")
    protected AbstractElementVisitor7() {
        super();
    }
}
