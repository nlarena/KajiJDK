package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * El visitante por kind de tipo de Java 14 en adelante. Ver {@link TypeKindVisitor6} por el mecanismo.
 *
 * <p>No agrega nada: ni los registros ni nada posterior trajeron un `TypeKind` nuevo.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class TypeKindVisitor14<R, P> extends TypeKindVisitor9<R, P> {

    protected TypeKindVisitor14() {
        super(null);
    }

    protected TypeKindVisitor14(R defaultValue) {
        super(defaultValue);
    }
}
