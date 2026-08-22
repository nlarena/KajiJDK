package javax.annotation.processing;
import javax.tools.Diagnostic;
public interface Messager {
    void printMessage(Diagnostic.Kind kind, CharSequence msg);
}
