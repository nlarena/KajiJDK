package javax.lang.model.element;
import java.util.List;
public interface Element {
    Name getSimpleName();
    ElementKind getKind();
    Element getEnclosingElement();
    List<? extends Element> getEnclosedElements();
}
