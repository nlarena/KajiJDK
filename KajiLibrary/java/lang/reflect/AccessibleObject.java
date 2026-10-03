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

    // ---- AnnotatedElement (member level) ----
    //
    // The three queries all answer off `getDeclaredAnnotations()`, which is the one a subclass
    // overrides with a native. That indirection is the whole point: before the #505 fix each of them
    // returned "nothing" on its own, so `Method.getDeclaredAnnotations()` could read the attribute
    // while `Method.getAnnotations()` still answered 0 -- two functions answering the same question,
    // and only one of them up to date.
    //
    // **On a member, `getAnnotations()` IS `getDeclaredAnnotations()`.** @Inherited applies only to a
    // class (JLS 9.6.4.3), so there is nothing to walk up: this is the contract, not a subset.

    public Annotation[] getAnnotations() {
        return this.getDeclaredAnnotations();
    }

    public Annotation[] getDeclaredAnnotations() {
        return new Annotation[0];
    }

    /**
     * The annotation of that type, or null.
     *
     * <p>It compares by {@code isInstance} and not by name: the synthetic class the VM builds for an
     * annotation implements the @interface, so the type check is the right one and does not need to
     * look at {@code annotationType()}.
     */
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        if (annotationClass == null) {
            throw new NullPointerException();
        }
        Annotation[] all = this.getDeclaredAnnotations();
        int i = 0;
        while (i < all.length) {
            if (annotationClass.isInstance(all[i])) {
                return (T) all[i];
            }
            i = i + 1;
        }
        return null;
    }

    public <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationClass) {
        return this.getAnnotation(annotationClass);
    }

    /**
     * The annotations of that type that are directly present.
     *
     * <p><strong>A declared subset:</strong> for a {@code @Repeatable} annotation the JDK also unwraps
     * the container and returns what is inside it. KajiLibrary does not model the container, so a
     * repeated annotation comes back as the container itself and this reports none of that type. For
     * every annotation that is not repeatable -- all of them in this library -- the answer is the
     * JDK's: one element when it is present, zero when it is not. Before the #505 fix it was zero in
     * both cases, which was wrong for the common one.
     */
    public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
        Annotation[] all = this.getDeclaredAnnotations();
        int n = 0;
        int i = 0;
        while (i < all.length) {
            if (annotationClass.isInstance(all[i])) {
                n = n + 1;
            }
            i = i + 1;
        }
        T[] out = (T[]) Array.newInstance(annotationClass, n);
        int k = 0;
        i = 0;
        while (i < all.length) {
            if (annotationClass.isInstance(all[i])) {
                out[k] = (T) all[i];
                k = k + 1;
            }
            i = i + 1;
        }
        return out;
    }

    public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass) {
        return this.getAnnotationsByType(annotationClass);
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return this.getAnnotation(annotationClass) != null;
    }
}
