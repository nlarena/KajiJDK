package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * El visitante de valores de anotacion de Java 8. Ver {@link AbstractAnnotationValueVisitor6}.
 *
 * <p>Java 8 trajo las anotaciones de tipo y las repetibles, y ninguna de las dos toca esta familia: son
 * anotaciones en lugares nuevos, no **valores** de forma nueva. Una anotacion repetida se modela con la
 * contenedora, cuyo valor es un arreglo — que ya existia.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public abstract class AbstractAnnotationValueVisitor8<R, P>
        extends AbstractAnnotationValueVisitor7<R, P> {

    protected AbstractAnnotationValueVisitor8() {
        super();
    }
}
