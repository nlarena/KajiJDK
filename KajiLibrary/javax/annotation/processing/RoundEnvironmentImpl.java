package javax.annotation.processing;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

// Soporte mínimo del round loop de APT (JSR 269, fase 2): una implementación concreta de
// RoundEnvironment que el driver construye una vez por ronda. `over` distingue la ronda final
// (processingOver == true), que es lo único que el MVP necesita exponer; los conjuntos de
// elementos raíz / anotados devuelven vacío hasta que exista la reificación de elementos (fase 3).
//
// `errorRaised()` devuelve `false` siempre, y es la respuesta correcta y no un placeholder: el
// round loop de este proyecto **aborta** apenas una excepción escapa de un `process()`, así que
// nunca hay una ronda siguiente que pueda ver un error de la anterior. Además el `Messager` de acá
// escribe a la consola de trazas y no lleva la cuenta de errores, con lo cual no hay ninguna otra
// fuente de la que un error pudiera venir.
public class RoundEnvironmentImpl implements RoundEnvironment {
    private final boolean over;
    public RoundEnvironmentImpl(boolean over) { this.over = over; }
    public boolean processingOver() { return over; }
    public boolean errorRaised() { return false; }
    public Set<? extends Element> getRootElements() { return new HashSet<Element>(); }
    public Set<? extends Element> getElementsAnnotatedWith(TypeElement a) { return new HashSet<Element>(); }
    public Set<? extends Element> getElementsAnnotatedWith(Class<? extends Annotation> a) { return new HashSet<Element>(); }
}
