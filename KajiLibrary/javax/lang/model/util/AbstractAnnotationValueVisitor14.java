package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * The annotation value visitor for Java 14 onwards. See {@link AbstractAnnotationValueVisitor6}.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public abstract class AbstractAnnotationValueVisitor14<R, P>
        extends AbstractAnnotationValueVisitor9<R, P> {

    protected AbstractAnnotationValueVisitor14() {
        super();
    }
}
