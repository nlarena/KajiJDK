package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * El escaner de elementos de Java 8. Ver {@link ElementScanner6} por el mecanismo.
 *
 * <p>Java 8 no agrego declaraciones ni cambio de quien es hijo de quien, asi que el recorrido es el
 * mismo.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ElementScanner8<R, P> extends ElementScanner7<R, P> {

    protected ElementScanner8() {
        super(null);
    }

    protected ElementScanner8(R defaultValue) {
        super(defaultValue);
    }
}
