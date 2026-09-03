package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * El visitante simple de valores de anotacion de las construcciones en **vista previa**. Ver
 * {@link SimpleAnnotationValueVisitor6} por el mecanismo y {@link AbstractElementVisitorPreview} por que
 * significa "vista previa" aca.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class SimpleAnnotationValueVisitorPreview<R, P>
        extends SimpleAnnotationValueVisitor14<R, P> {

    protected SimpleAnnotationValueVisitorPreview() {
        super(null);
    }

    protected SimpleAnnotationValueVisitorPreview(R defaultValue) {
        super(defaultValue);
    }
}
