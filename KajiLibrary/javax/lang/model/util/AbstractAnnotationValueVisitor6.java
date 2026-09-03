package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.UnknownAnnotationValueException;

/**
 * KajiLibrary's javax.lang.model.util.AbstractAnnotationValueVisitor6 — la base de la familia de
 * visitantes de valores de anotacion.
 *
 * <p>Mismo mecanismo que {@link AbstractElementVisitor6}, aplicado a {@link AnnotationValueVisitor}.
 *
 * <p>Es la familia mas quieta de las tres, y por una razon que vale decir: el conjunto de cosas que
 * pueden ser el valor de un elemento de anotacion esta **cerrado por la especificacion** desde que
 * existen las anotaciones. Son los ocho primitivos, `String`, una clase, una constante de enum, otra
 * anotacion, o un arreglo de esos. El lenguaje nunca lo amplio — ni los registros ni los modulos ni los
 * tipos sellados agregaron una forma de valor — asi que de la 7 a la `Preview` no hay una sola
 * diferencia de contrato.
 *
 * <p>Que las cinco subclases sean identicas no es descuido de la API: son el numero de version, que
 * sirve para fechar el visitante y para que {@code @SupportedSourceVersion} diga la verdad.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public abstract class AbstractAnnotationValueVisitor6<R, P>
        implements AnnotationValueVisitor<R, P> {

    protected AbstractAnnotationValueVisitor6() {
    }

    /** El despacho: la que sabe que forma de valor es, es la implementacion de `accept`. */
    public final R visit(AnnotationValue av, P p) {
        return av.accept(this, p);
    }

    /** Igual, con parametro nulo. */
    public final R visit(AnnotationValue av) {
        return av.accept(this, null);
    }

    public R visitUnknown(AnnotationValue av, P p) {
        throw new UnknownAnnotationValueException(av, p);
    }
}
