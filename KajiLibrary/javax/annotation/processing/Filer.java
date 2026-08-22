package javax.annotation.processing;
import javax.tools.JavaFileObject;
import java.io.IOException;
public interface Filer {
    JavaFileObject createSourceFile(CharSequence name) throws IOException;
}
