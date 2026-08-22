package javax.annotation.processing;
import javax.lang.model.element.TypeElement;
import javax.lang.model.SourceVersion;
import java.util.Set;
public interface Processor {
    Set<String> getSupportedAnnotationTypes();
    SourceVersion getSupportedSourceVersion();
    void init(ProcessingEnvironment env);
    boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv);
}
