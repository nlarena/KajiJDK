package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.RecordComponentElement;

/**
 * El visitante simple de elementos de Java 14 en adelante. Ver {@link SimpleElementVisitor6} por el
 * mecanismo.
 *
 * <p>Los componentes de registro entran al embudo: `visitRecordComponent` deja de tirar y pasa a
 * `defaultAction`.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class SimpleElementVisitor14<R, P> extends SimpleElementVisitor9<R, P> {

    protected SimpleElementVisitor14() {
        super(null);
    }

    protected SimpleElementVisitor14(R defaultValue) {
        super(defaultValue);
    }

    public R visitRecordComponent(RecordComponentElement e, P p) {
        return this.defaultAction(e, p);
    }
}
