package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * El visitante simple de elementos de Java 8. Ver {@link SimpleElementVisitor6} por el mecanismo.
 *
 * <p>Java 8 no agrego kinds de elemento ni clases de declaracion, asi que no hay nada que reencaminar.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class SimpleElementVisitor8<R, P> extends SimpleElementVisitor7<R, P> {

    protected SimpleElementVisitor8() {
        super(null);
    }

    protected SimpleElementVisitor8(R defaultValue) {
        super(defaultValue);
    }
}
