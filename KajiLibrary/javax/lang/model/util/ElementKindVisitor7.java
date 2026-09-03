package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.VariableElement;

/**
 * El visitante por kind de elemento de Java 7. Ver {@link ElementKindVisitor6} por el mecanismo del
 * reparto.
 *
 * <p>Con el `try` con recursos ya en el lenguaje, `visitVariableAsResourceVariable` deja de caer en
 * `visitUnknown` y entra al embudo.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class ElementKindVisitor7<R, P> extends ElementKindVisitor6<R, P> {

    @Deprecated(since = "12")
    protected ElementKindVisitor7() {
        super(null);
    }

    @Deprecated(since = "12")
    protected ElementKindVisitor7(R defaultValue) {
        super(defaultValue);
    }

    public R visitVariableAsResourceVariable(VariableElement e, P p) {
        return this.defaultAction(e, p);
    }
}
