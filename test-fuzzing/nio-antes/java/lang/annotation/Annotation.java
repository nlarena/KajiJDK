package java.lang.annotation;

// KajiLibrary's java.lang.annotation.Annotation — the common interface extended by all annotation
// types. Not itself an annotation type. Declares the identity/string contract plus annotationType().
public interface Annotation {

    boolean equals(Object obj);

    int hashCode();

    String toString();

    Class<? extends Annotation> annotationType();
}
