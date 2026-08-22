package javax.annotation.processing;
import javax.lang.model.SourceVersion;
import java.util.Set;
import java.util.HashSet;
public abstract class AbstractProcessor implements Processor {
    protected ProcessingEnvironment processingEnv;
    public void init(ProcessingEnvironment env) { this.processingEnv = env; }
    public Set<String> getSupportedAnnotationTypes() { return new HashSet<String>(); }
    public SourceVersion getSupportedSourceVersion() { return SourceVersion.RELEASE_25; }
}
