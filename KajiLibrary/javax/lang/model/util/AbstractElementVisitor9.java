package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ModuleElement;

/**
 * El visitante de elementos de Java 9. Ver {@link AbstractElementVisitor6} por el mecanismo.
 *
 * <p>Java 9 trajo los **modulos**, la primera clase de declaracion nueva desde que existe el modelo. Por
 * eso aca `visitModule` deja de tener un cuerpo que tira y pasa a ser **abstracto**: extender esta clase
 * en vez de la de 8 es exactamente la manera de que el compilador te obligue a decidir que hacer con un
 * modulo, en lugar de descubrirlo en tiempo de ejecucion con una excepcion.
 */
// RELEASE_14 y no RELEASE_9: la anotacion dice la ultima version del lenguaje que este visitante
// **soporta**, no aquella en la que aparecio. Entre 9 y 14 no llego ninguna construccion que este
// no sepa tratar, asi que sigue siendo adecuado para las dos. Es el mismo valor que llevan
// `TypeKindVisitor9` y `ElementScanner9` en el JDK.
@SupportedSourceVersion(SourceVersion.RELEASE_14)
public abstract class AbstractElementVisitor9<R, P> extends AbstractElementVisitor8<R, P> {

    protected AbstractElementVisitor9() {
        super();
    }

    public abstract R visitModule(ModuleElement e, P p);
}
