package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.VariableElement;

/**
 * El visitante simple de elementos de Java 7. Ver {@link SimpleElementVisitor6} por el mecanismo del
 * embudo.
 *
 * <p>Lo unico que agrega Java 7 aca es que `RESOURCE_VARIABLE` **ya existe**, asi que `visitVariable`
 * deja de apartarla y la manda a `defaultAction` como a cualquier otra variable. La logica de la clase
 * de 6 quedaba mintiendo sobre lo que el visitante podia haber previsto; en 7 ya no.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class SimpleElementVisitor7<R, P> extends SimpleElementVisitor6<R, P> {

    @Deprecated(since = "12")
    protected SimpleElementVisitor7() {
        super(null);
    }

    @Deprecated(since = "12")
    protected SimpleElementVisitor7(R defaultValue) {
        super(defaultValue);
    }

    public R visitVariable(VariableElement e, P p) {
        return this.defaultAction(e, p);
    }
}
