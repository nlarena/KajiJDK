package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * The annotation value visitor for Java 7. See {@link AbstractAnnotationValueVisitor6} for the
 * mechanism and for why no version of this family adds anything.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public abstract class AbstractAnnotationValueVisitor7<R, P>
        extends AbstractAnnotationValueVisitor6<R, P> {

    @Deprecated(since = "12")
    protected AbstractAnnotationValueVisitor7() {
        super();
    }
}
