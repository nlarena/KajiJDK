package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * El visitante simple de valores de anotacion de Java 7. Ver {@link SimpleAnnotationValueVisitor6} por
 * el mecanismo y {@link AbstractAnnotationValueVisitor6} por que ninguna version de esta familia agrega
 * nada.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class SimpleAnnotationValueVisitor7<R, P> extends SimpleAnnotationValueVisitor6<R, P> {

    @Deprecated(since = "12")
    protected SimpleAnnotationValueVisitor7() {
        super(null);
    }

    @Deprecated(since = "12")
    protected SimpleAnnotationValueVisitor7(R defaultValue) {
        super(defaultValue);
    }
}
