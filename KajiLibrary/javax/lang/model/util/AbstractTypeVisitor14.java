package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * El visitante de tipos de Java 14 en adelante. Ver {@link AbstractTypeVisitor6} por el mecanismo.
 *
 * <p>Los registros tampoco trajeron una forma de tipo nueva: el tipo de un registro es un
 * `DeclaredType` como el de cualquier clase. Desde Java 8 que no aparece una, y por eso esta clase y la
 * de 9 no agregan nada — la familia de tipos se estabilizo mucho antes que la de elementos.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public abstract class AbstractTypeVisitor14<R, P> extends AbstractTypeVisitor9<R, P> {

    protected AbstractTypeVisitor14() {
        super();
    }
}
