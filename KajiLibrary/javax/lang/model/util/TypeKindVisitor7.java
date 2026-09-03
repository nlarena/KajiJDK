package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.UnionType;

/**
 * El visitante por kind de tipo de Java 7. Ver {@link TypeKindVisitor6} por el mecanismo.
 *
 * <p>`visitUnion` entra al embudo, igual que en {@link SimpleTypeVisitor7}. Hace falta repetirlo aca
 * porque esta rama de la familia baja por `TypeKindVisitor6`, que hereda el `visitUnion` que tira de
 * {@link SimpleTypeVisitor6} — la version 7 del visitante simple no esta en su ancestro.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class TypeKindVisitor7<R, P> extends TypeKindVisitor6<R, P> {

    @Deprecated(since = "12")
    protected TypeKindVisitor7() {
        super(null);
    }

    @Deprecated(since = "12")
    protected TypeKindVisitor7(R defaultValue) {
        super(defaultValue);
    }

    public R visitUnion(UnionType t, P p) {
        return this.defaultAction(t, p);
    }
}
