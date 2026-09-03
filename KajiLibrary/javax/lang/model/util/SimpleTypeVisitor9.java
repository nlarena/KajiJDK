package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * El visitante simple de tipos de Java 9. Ver {@link SimpleTypeVisitor6} por el mecanismo.
 *
 * <p>El pseudotipo `MODULE` que trajo Java 9 llega por `visitNoType`, que ya estaba en el embudo desde
 * la de 6, asi que no hay nada que reencaminar. Es {@link TypeKindVisitor9} — el que si desarma
 * `visitNoType` por kind — el que tiene que hacer algo con esto.
 */
// RELEASE_14 y no RELEASE_9: la anotacion dice la ultima version del lenguaje que este visitante
// **soporta**, no aquella en la que aparecio. Entre 9 y 14 no llego ninguna construccion que este
// no sepa tratar, asi que sigue siendo adecuado para las dos. Es el mismo valor que llevan
// `TypeKindVisitor9` y `ElementScanner9` en el JDK.
@SupportedSourceVersion(SourceVersion.RELEASE_14)
public class SimpleTypeVisitor9<R, P> extends SimpleTypeVisitor8<R, P> {

    protected SimpleTypeVisitor9() {
        super(null);
    }

    protected SimpleTypeVisitor9(R defaultValue) {
        super(defaultValue);
    }
}
