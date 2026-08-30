package java.lang.reflect;

import java.util.Set;

// KajiLibrary's java.lang.reflect.Member — the reflective view shared by Field, Method and Constructor.
public interface Member {

    public static final int PUBLIC = 0;

    public static final int DECLARED = 1;

    Class<?> getDeclaringClass();

    String getName();

    int getModifiers();

    boolean isSynthetic();

    /**
     * This member's access flags. The default throws {@link UnsupportedOperationException}; a member
     * type that knows its raw flags (a {@code Field}, {@code Method} or {@code Constructor}) overrides
     * it. Matches the JDK, whose {@code Member.accessFlags} default does the same.
     */
    default Set<AccessFlag> accessFlags() {
        throw new UnsupportedOperationException();
    }
}
