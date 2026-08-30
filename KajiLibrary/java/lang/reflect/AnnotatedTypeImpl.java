package java.lang.reflect;

// KajiLibrary's package-private AnnotatedType implementation — the plain wrapper the reflection API
// hands back for a `Type` that carries no type annotations (which is every type in a library that
// does not model `RuntimeVisibleTypeAnnotations`). `getType()` is the wrapped type; the annotation
// queries answer "none". `Field.getAnnotatedType`, and the class/method annotated-type accessors,
// return one of these.
// Public (not package-private as its JDK analogue) so java.lang.Class can build one too -- it has no
// JDK counterpart, so it is invisible to the public-surface accounting.
public final class AnnotatedTypeImpl implements AnnotatedType {

    private final Type type;

    public AnnotatedTypeImpl(Type type) {
        this.type = type;
    }

    public Type getType() {
        return this.type;
    }

    public <T extends java.lang.annotation.Annotation> T getAnnotation(Class<T> annotationClass) {
        return null;
    }

    public java.lang.annotation.Annotation[] getAnnotations() {
        return new java.lang.annotation.Annotation[0];
    }

    public java.lang.annotation.Annotation[] getDeclaredAnnotations() {
        return new java.lang.annotation.Annotation[0];
    }
}
