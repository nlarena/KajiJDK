package javax.annotation.processing;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;
public interface RoundEnvironment {
    boolean processingOver();
    Set<? extends Element> getRootElements();
    Set<? extends Element> getElementsAnnotatedWith(TypeElement a);
}
