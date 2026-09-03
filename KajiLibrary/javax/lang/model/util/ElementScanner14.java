package javax.lang.model.util;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;

/**
 * El escaner de elementos de Java 14 en adelante. Ver {@link ElementScanner6} por el mecanismo.
 *
 * <p>Trae los componentes de registro al recorrido, como se espera. Pero ademas **arregla un agujero
 * viejo**, y esa es la parte que lo distingue de verdad de las versiones anteriores.
 *
 * <p>El agujero: los **parametros de tipo** de una clase o de un metodo no estan en
 * `getEnclosedElements()` ni en `getParameters()`. En `&lt;T&gt; void f(T x)`, `T` es un
 * `TypeParameterElement` que ningun escaner anterior a esta version visitaba nunca, aunque el visitante
 * tuviera un `visitTypeParameter` escrito y esperando. Desde aca, `visitType` y `visitExecutable` los
 * anteponen a lo que ya recorrian.
 *
 * <p>Van **antes** y no despues porque es el orden en que se declaran y en que hacen falta: el parametro
 * de tipo esta en alcance para los miembros que lo usan.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class ElementScanner14<R, P> extends ElementScanner9<R, P> {

    protected ElementScanner14() {
        super(null);
    }

    protected ElementScanner14(R defaultValue) {
        super(defaultValue);
    }

    public R visitType(TypeElement e, P p) {
        return this.scan(this.createScanningList(e, e.getEnclosedElements()), p);
    }

    public R visitExecutable(ExecutableElement e, P p) {
        return this.scan(this.createScanningList(e, e.getParameters()), p);
    }

    // Sin parametros de tipo se devuelve la lista original y no una copia: es el caso comun con
    // diferencia, y copiar por nada seria una asignacion por cada elemento visitado.
    private List<? extends Element> createScanningList(Parameterizable element,
            List<? extends Element> toBeScanned) {
        List<? extends TypeParameterElement> typeParameters = element.getTypeParameters();
        if (typeParameters.isEmpty()) {
            return toBeScanned;
        }
        List<Element> scanningList = new ArrayList<Element>(typeParameters);
        scanningList.addAll(toBeScanned);
        return scanningList;
    }

    public R visitRecordComponent(RecordComponentElement e, P p) {
        return this.scan(e.getEnclosedElements(), p);
    }
}
