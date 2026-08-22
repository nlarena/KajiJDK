package javax.annotation.processing;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;
import java.util.HashSet;
// Soporte mínimo del round loop de APT (JSR 269, fase 2): una implementación concreta de
// RoundEnvironment que el driver construye una vez por ronda. `over` distingue la ronda final
// (processingOver == true), que es lo único que el MVP necesita exponer; los conjuntos de
// elementos raíz / anotados devuelven vacío hasta que exista la reificación de elementos (fase 3).
public class RoundEnvironmentImpl implements RoundEnvironment {
    private final boolean over;
    public RoundEnvironmentImpl(boolean over) { this.over = over; }
    public boolean processingOver() { return over; }
    public Set<? extends Element> getRootElements() { return new HashSet<Element>(); }
    public Set<? extends Element> getElementsAnnotatedWith(TypeElement a) { return new HashSet<Element>(); }
}
