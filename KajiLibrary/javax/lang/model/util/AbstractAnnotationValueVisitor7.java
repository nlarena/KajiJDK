package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * El visitante de valores de anotacion de Java 7. Ver {@link AbstractAnnotationValueVisitor6} por el
 * mecanismo y por que ninguna version de esta familia agrega nada.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public abstract class AbstractAnnotationValueVisitor7<R, P>
        extends AbstractAnnotationValueVisitor6<R, P> {

    @Deprecated(since = "12")
    protected AbstractAnnotationValueVisitor7() {
        super();
    }
}
