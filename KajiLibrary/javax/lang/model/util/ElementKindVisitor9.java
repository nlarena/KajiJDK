package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ModuleElement;

/**
 * El visitante por kind de elemento de Java 9. Ver {@link ElementKindVisitor6} por el mecanismo.
 *
 * <p>`visitModule` entra al embudo. No se abre en `visitModuleAsXxx` porque `MODULE` es un solo kind:
 * no hay nada que repartir, igual que con los parametros de tipo.
 */
// RELEASE_14 y no RELEASE_9: la anotacion dice la ultima version del lenguaje que este visitante
// **soporta**, no aquella en la que aparecio. Entre 9 y 14 no llego ninguna construccion que este
// no sepa tratar, asi que sigue siendo adecuado para las dos. Es el mismo valor que llevan
// `TypeKindVisitor9` y `ElementScanner9` en el JDK.
@SupportedSourceVersion(SourceVersion.RELEASE_14)
public class ElementKindVisitor9<R, P> extends ElementKindVisitor8<R, P> {

    protected ElementKindVisitor9() {
        super(null);
    }

    protected ElementKindVisitor9(R defaultValue) {
        super(defaultValue);
    }

    public R visitModule(ModuleElement e, P p) {
        return this.defaultAction(e, p);
    }
}
