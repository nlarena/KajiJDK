package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * El visitante simple de valores de anotacion de Java 14 en adelante. Ver
 * {@link SimpleAnnotationValueVisitor6}.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class SimpleAnnotationValueVisitor14<R, P> extends SimpleAnnotationValueVisitor9<R, P> {

    protected SimpleAnnotationValueVisitor14() {
        super(null);
    }

    protected SimpleAnnotationValueVisitor14(R defaultValue) {
        super(defaultValue);
    }
}
