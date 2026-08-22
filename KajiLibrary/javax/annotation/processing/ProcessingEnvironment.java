package javax.annotation.processing;
public interface ProcessingEnvironment {
    Messager getMessager();
    Filer getFiler();
}
