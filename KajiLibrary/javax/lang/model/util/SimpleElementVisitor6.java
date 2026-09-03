package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

/**
 * KajiLibrary's javax.lang.model.util.SimpleElementVisitor6 — el visitante de elementos para el que casi
 * todos los casos dan lo mismo.
 *
 * <h2>Que agrega sobre el abstracto</h2>
 *
 * <p>{@link AbstractElementVisitor6} obliga a escribir los cinco `visitXxx`. Pero el visitante tipico no
 * quiere cinco: quiere uno. "Devolveme el nombre de cualquier elemento", "contame todo lo que sea
 * publico". Escribir cinco metodos con el mismo cuerpo es ruido.
 *
 * <p>Esta clase mete un embudo: cada `visitXxx` llama a {@link #defaultAction}, y el que extiende
 * redefine **una sola cosa** — `defaultAction` para el caso general, y ademas el `visitXxx` puntual que
 * quiera tratar distinto. La `defaultAction` de aca devuelve `DEFAULT_VALUE`, el valor que se le paso al
 * constructor, para el caso mas comun de todos: un visitante que solo se interesa por un tipo de
 * elemento y quiere un valor fijo para el resto.
 *
 * <h2>Por que `visitVariable` no siempre llama a `defaultAction`</h2>
 *
 * <p>Este es el unico lugar donde el "todo cae en el mismo embudo" tiene una excepcion, y no es un
 * capricho. Java 7 agrego `RESOURCE_VARIABLE` para el `try` con recursos. Es un `VariableElement`, asi
 * que le llega a `visitVariable` **sin que la firma cambie** — y un visitante escrito para Java 6 jamas
 * decidio que hacer con una variable de recurso, porque no existian.
 *
 * <p>Mandarla a `defaultAction` seria devolver en silencio la respuesta de un caso que nunca se
 * considero. Por eso va a `visitUnknown`, que tira: un kind que el visitante no puede haber previsto es
 * exactamente lo que `visitUnknown` significa. {@link SimpleElementVisitor7} lo saca, porque ahi si
 * existia.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class SimpleElementVisitor6<R, P> extends AbstractElementVisitor6<R, P> {

    /** Lo que devuelve `defaultAction` mientras no la redefinan. */
    protected final R DEFAULT_VALUE;

    @Deprecated(since = "9")
    protected SimpleElementVisitor6() {
        this.DEFAULT_VALUE = null;
    }

    @Deprecated(since = "9")
    protected SimpleElementVisitor6(R defaultValue) {
        this.DEFAULT_VALUE = defaultValue;
    }

    /** El embudo. Redefinirla es la manera de tratar todos los elementos igual. */
    protected R defaultAction(Element e, P p) {
        return this.DEFAULT_VALUE;
    }

    public R visitPackage(PackageElement e, P p) {
        return this.defaultAction(e, p);
    }

    public R visitType(TypeElement e, P p) {
        return this.defaultAction(e, p);
    }

    public R visitVariable(VariableElement e, P p) {
        // Ver el encabezado: una variable de recurso es de Java 7 y este visitante es de Java 6.
        if (e.getKind() != ElementKind.RESOURCE_VARIABLE) {
            return this.defaultAction(e, p);
        }
        return this.visitUnknown(e, p);
    }

    public R visitExecutable(ExecutableElement e, P p) {
        return this.defaultAction(e, p);
    }

    public R visitTypeParameter(TypeParameterElement e, P p) {
        return this.defaultAction(e, p);
    }
}
