package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * The annotation value visitor for Java 9. See {@link AbstractAnnotationValueVisitor6}.
 */
// RELEASE_14 and not RELEASE_9: the annotation states the latest language version this visitor
// **supports**, not the one in which it appeared. Between 9 and 14 no construct arrived that it
// cannot handle, so it is still adequate for both. It is the same value `TypeKindVisitor9` and
// `ElementScanner9` carry in the JDK.
@SupportedSourceVersion(SourceVersion.RELEASE_14)
public abstract class AbstractAnnotationValueVisitor9<R, P>
        extends AbstractAnnotationValueVisitor8<R, P> {

    protected AbstractAnnotationValueVisitor9() {
        super();
    }
}
