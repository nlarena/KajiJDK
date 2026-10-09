package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * The annotation value visitor for Java 8. See {@link AbstractAnnotationValueVisitor6}.
 *
 * <p>Java 8 brought type annotations and repeatable ones, and neither touches this family: they are
 * annotations in new places, not **values** of a new form. A repeated annotation is modelled with
 * the container, whose value is an array — which already existed.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public abstract class AbstractAnnotationValueVisitor8<R, P>
        extends AbstractAnnotationValueVisitor7<R, P> {

    protected AbstractAnnotationValueVisitor8() {
        super();
    }
}
