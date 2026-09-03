package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.VariableElement;

/**
 * El escaner de elementos de Java 7. Ver {@link ElementScanner6} por el mecanismo del recorrido.
 *
 * <p>Con `RESOURCE_VARIABLE` ya en el lenguaje, `visitVariable` deja de apartarla y la recorre como a
 * cualquier otra variable.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class ElementScanner7<R, P> extends ElementScanner6<R, P> {

    @Deprecated(since = "12")
    protected ElementScanner7() {
        super(null);
    }

    @Deprecated(since = "12")
    protected ElementScanner7(R defaultValue) {
        super(defaultValue);
    }

    public R visitVariable(VariableElement e, P p) {
        return this.scan(e.getEnclosedElements(), p);
    }
}
