package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.NoType;

/**
 * El visitante por kind de tipo de Java 9. Ver {@link TypeKindVisitor6} por el mecanismo.
 *
 * <p>Con los modulos en el lenguaje, `visitNoTypeAsModule` pasa de `visitUnknown` a `defaultAction`.
 * Esta es la clase por la que la rama de kinds necesita una version 9 y la rama simple no: es el unico
 * lugar donde el pseudotipo `MODULE` se distingue de los otros tres `NoType`.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_14)
public class TypeKindVisitor9<R, P> extends TypeKindVisitor8<R, P> {

    protected TypeKindVisitor9() {
        super(null);
    }

    protected TypeKindVisitor9(R defaultValue) {
        super(defaultValue);
    }

    public R visitNoTypeAsModule(NoType t, P p) {
        return this.defaultAction(t, p);
    }
}
