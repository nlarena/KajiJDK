package java.lang.reflect;

// KajiLibrary's java.lang.reflect.AccessibleObject — the common base of Field/Method/Constructor,
// carrying the "suppress access checks" flag. A KajiLibrary subset (the AnnotatedElement reflection
// methods are added with runtime annotation support).
public class AccessibleObject {

    protected AccessibleObject() {
    }

    public void setAccessible(boolean flag) {
    }

    public boolean isAccessible() {
        return false;
    }
}
