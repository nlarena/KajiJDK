package java.lang.reflect;

import java.lang.annotation.Annotation;

// KajiLibrary's java.lang.reflect.AccessibleObject — the common base of Field/Method/Constructor,
// carrying the "suppress access checks" flag and the AnnotatedElement surface.
//
// KajiJDK performs no access check on a reflective call, so the "accessible" flag and canAccess/
// trySetAccessible are effectively always granted. Runtime annotation reflection is wired at the
// Class level (see Class.getAnnotation); member-level annotations report "none" here -- a subset
// that is correct for every member carrying no runtime annotation.
public class AccessibleObject implements AnnotatedElement {

    protected AccessibleObject() {
    }

    public void setAccessible(boolean flag) {
    }

    public boolean isAccessible() {
        return false;
    }

    public final boolean canAccess(Object obj) {
        return true;
    }

    public final boolean trySetAccessible() {
        return true;
    }

    public static void setAccessible(AccessibleObject[] array, boolean flag) {
        int i = 0;
        while (i < array.length) {
            array[i].setAccessible(flag);
            i = i + 1;
        }
    }

    // ---- AnnotatedElement (member-level: no runtime annotations modelled) ----

    public Annotation[] getAnnotations() {
        return new Annotation[0];
    }

    public Annotation[] getDeclaredAnnotations() {
        return new Annotation[0];
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        if (annotationClass == null) {
            throw new NullPointerException();
        }
        return null;
    }

    public <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationClass) {
        return this.getAnnotation(annotationClass);
    }

    public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
        return (T[]) Array.newInstance(annotationClass, 0);
    }

    public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass) {
        return this.getAnnotationsByType(annotationClass);
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return this.getAnnotation(annotationClass) != null;
    }
}
