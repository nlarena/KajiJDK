package java.lang.reflect;

// KajiLibrary's java.lang.reflect.Member — the reflective view shared by Field, Method and Constructor.
// A KajiLibrary subset: the accessFlags() default (needs java.lang.reflect.AccessFlag) is omitted.
public interface Member {

    public static final int PUBLIC = 0;

    public static final int DECLARED = 1;

    Class<?> getDeclaringClass();

    String getName();

    int getModifiers();

    boolean isSynthetic();
}
