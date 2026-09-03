package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * El visitante de valores de anotacion de Java 9. Ver {@link AbstractAnnotationValueVisitor6}.
 */
// RELEASE_14 y no RELEASE_9: la anotacion dice la ultima version del lenguaje que este visitante
// **soporta**, no aquella en la que aparecio. Entre 9 y 14 no llego ninguna construccion que este
// no sepa tratar, asi que sigue siendo adecuado para las dos. Es el mismo valor que llevan
// `TypeKindVisitor9` y `ElementScanner9` en el JDK.
@SupportedSourceVersion(SourceVersion.RELEASE_14)
public abstract class AbstractAnnotationValueVisitor9<R, P>
        extends AbstractAnnotationValueVisitor8<R, P> {

    protected AbstractAnnotationValueVisitor9() {
        super();
    }
}
