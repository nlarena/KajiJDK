package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.UnionType;

/**
 * El visitante simple de tipos de Java 7. Ver {@link SimpleTypeVisitor6} por el mecanismo.
 *
 * <p>Con el tipo union ya en el lenguaje, `visitUnion` entra al embudo: hereda de
 * {@link AbstractTypeVisitor6} un cuerpo que cae en `visitUnknown`, y aca pasa a `defaultAction`.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class SimpleTypeVisitor7<R, P> extends SimpleTypeVisitor6<R, P> {

    @Deprecated(since = "12")
    protected SimpleTypeVisitor7() {
        super(null);
    }

    @Deprecated(since = "12")
    protected SimpleTypeVisitor7(R defaultValue) {
        super(defaultValue);
    }

    public R visitUnion(UnionType t, P p) {
        return this.defaultAction(t, p);
    }
}
