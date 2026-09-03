package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * El visitante simple de tipos de Java 14 en adelante. Ver {@link SimpleTypeVisitor6} por el mecanismo.
 *
 * <p>No agrega nada: desde Java 8 que no aparece una forma de tipo nueva.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class SimpleTypeVisitor14<R, P> extends SimpleTypeVisitor9<R, P> {

    protected SimpleTypeVisitor14() {
        super(null);
    }

    protected SimpleTypeVisitor14(R defaultValue) {
        super(defaultValue);
    }
}
