package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * El visitante por kind de elemento de Java 8. Ver {@link ElementKindVisitor6} por el mecanismo.
 *
 * <p>Java 8 no agrego kinds, asi que no hay nada que pasar al embudo.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ElementKindVisitor8<R, P> extends ElementKindVisitor7<R, P> {

    protected ElementKindVisitor8() {
        super(null);
    }

    protected ElementKindVisitor8(R defaultValue) {
        super(defaultValue);
    }
}
