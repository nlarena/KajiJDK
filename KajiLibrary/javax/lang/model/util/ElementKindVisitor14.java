package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

/**
 * El visitante por kind de elemento de Java 14 en adelante. Ver {@link ElementKindVisitor6} por el
 * mecanismo.
 *
 * <p>Es la version que cierra los tres kinds que la de 6 tenia que apartar. Los registros trajeron dos
 * de ellos: `RECORD`, que es un `TypeElement` mas, y `RECORD_COMPONENT`, que es una declaracion propia.
 * El tercero, `BINDING_VARIABLE`, vino con `instanceof` con patron. Los tres pasan de `visitUnknown` a
 * `defaultAction`.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class ElementKindVisitor14<R, P> extends ElementKindVisitor9<R, P> {

    protected ElementKindVisitor14() {
        super(null);
    }

    protected ElementKindVisitor14(R defaultValue) {
        super(defaultValue);
    }

    public R visitRecordComponent(RecordComponentElement e, P p) {
        return this.defaultAction(e, p);
    }

    public R visitTypeAsRecord(TypeElement e, P p) {
        return this.defaultAction(e, p);
    }

    public R visitVariableAsBindingVariable(VariableElement e, P p) {
        return this.defaultAction(e, p);
    }
}
