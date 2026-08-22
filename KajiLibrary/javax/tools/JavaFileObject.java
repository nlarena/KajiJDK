package javax.tools;
import java.io.Writer;
import java.io.IOException;
public interface JavaFileObject {
    Writer openWriter() throws IOException;
}
