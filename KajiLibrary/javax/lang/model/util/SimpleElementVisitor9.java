package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ModuleElement;

/**
 * El visitante simple de elementos de Java 9. Ver {@link SimpleElementVisitor6} por el mecanismo.
 *
 * <p>Con los modulos ya en el lenguaje, `visitModule` entra al embudo: hereda de
 * {@link AbstractElementVisitor6} un cuerpo que tira, y aca pasa a `defaultAction`. Notar que esta clase
 * **no** extiende {@link AbstractElementVisitor9} — extiende `SimpleElementVisitor8` — asi que
 * `visitModule` no se vuelve abstracto. Es la diferencia entre las dos ramas: la abstracta obliga a
 * decidir, la simple decide por vos.
 */
// RELEASE_14 y no RELEASE_9: la anotacion dice la ultima version del lenguaje que este visitante
// **soporta**, no aquella en la que aparecio. Entre 9 y 14 no llego ninguna construccion que este
// no sepa tratar, asi que sigue siendo adecuado para las dos. Es el mismo valor que llevan
// `TypeKindVisitor9` y `ElementScanner9` en el JDK.
@SupportedSourceVersion(SourceVersion.RELEASE_14)
public class SimpleElementVisitor9<R, P> extends SimpleElementVisitor8<R, P> {

    protected SimpleElementVisitor9() {
        super(null);
    }

    protected SimpleElementVisitor9(R defaultValue) {
        super(defaultValue);
    }

    public R visitModule(ModuleElement e, P p) {
        return this.defaultAction(e, p);
    }
}
