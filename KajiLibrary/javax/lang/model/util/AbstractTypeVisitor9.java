package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * El visitante de tipos de Java 9. Ver {@link AbstractTypeVisitor6} por el mecanismo.
 *
 * <p>Java 9 agrego los modulos, pero **no una forma de tipo nueva**: un modulo no es un tipo. Lo que
 * agrego fue el pseudotipo `MODULE`, y eso es un `TypeKind` mas dentro de `NoType`, que ya tenia su
 * `visitNoType`. Por eso esta clase no cambia el contrato — la asimetria con
 * {@link AbstractElementVisitor9}, donde los modulos si obligaron a un metodo nuevo, es justamente el
 * punto: un modulo es una declaracion, no un tipo.
 */
// RELEASE_14 y no RELEASE_9: la anotacion dice la ultima version del lenguaje que este visitante
// **soporta**, no aquella en la que aparecio. Entre 9 y 14 no llego ninguna construccion que este
// no sepa tratar, asi que sigue siendo adecuado para las dos. Es el mismo valor que llevan
// `TypeKindVisitor9` y `ElementScanner9` en el JDK.
@SupportedSourceVersion(SourceVersion.RELEASE_14)
public abstract class AbstractTypeVisitor9<R, P> extends AbstractTypeVisitor8<R, P> {

    protected AbstractTypeVisitor9() {
        super();
    }
}
