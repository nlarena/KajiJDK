package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.RecordComponentElement;

/**
 * El visitante de elementos de Java 14 en adelante. Ver {@link AbstractElementVisitor6} por el
 * mecanismo.
 *
 * <p>Los **componentes de registro** son la segunda clase de declaracion nueva, y aca
 * `visitRecordComponent` pasa a abstracto por la misma razon que `visitModule` en la de 9.
 *
 * <p>El numero se quedo en 14 aunque el visitante cubre hasta 25: los registros fueron vista previa en
 * 14 y definitivos en 16, y la familia no volvio a crecer desde entonces. Que no exista un
 * `AbstractElementVisitor16` no es un olvido — es que no hizo falta, porque no aparecio ninguna clase de
 * declaracion mas.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public abstract class AbstractElementVisitor14<R, P> extends AbstractElementVisitor9<R, P> {

    protected AbstractElementVisitor14() {
        super();
    }

    public abstract R visitRecordComponent(RecordComponentElement e, P p);
}
