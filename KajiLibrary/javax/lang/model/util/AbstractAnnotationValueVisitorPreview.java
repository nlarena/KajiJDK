package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * El visitante de valores de anotacion de las construcciones en **vista previa**. Ver
 * {@link AbstractAnnotationValueVisitor6} por el mecanismo y {@link AbstractElementVisitorPreview} por
 * que significa "vista previa" aca.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public abstract class AbstractAnnotationValueVisitorPreview<R, P>
        extends AbstractAnnotationValueVisitor14<R, P> {

    protected AbstractAnnotationValueVisitorPreview() {
        super();
    }
}
