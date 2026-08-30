package java.lang.reflect;

import java.lang.annotation.Annotation;

/**
 * A reflective constructor.
 *
 * <p>KajiLibrary subset, as {@link Method}: the object is populated by the VM, the getters read that
 * metadata, and {@link #newInstance} is a native.
 *
 * <h2>Now a subclass of {@link Executable}</h2>
 *
 * <p>Everything shared with {@code Method} — parameters, exception types, modifiers, and
 * {@link Executable#getParameters} — moved one level up. What is left here is what makes a
 * constructor different from a method, and it is a short list: it has no return type, its
 * {@link #getName} borrows the declaring class's, and instead of invoking on a receiver it
 * <em>produces</em> one.
 *
 * <p>{@link #getDeclaringClass} narrows the inherited {@code Class<?>} to {@code Class<T>} — the
 * covariant override the JDK also uses, and the reason {@code Constructor<T>} is generic at all:
 * {@code newInstance} can then return a {@code T} instead of an {@code Object}.
 *
 * <h2>What is still missing</h2>
 *
 * <p>{@code getAnnotation}, {@code getAnnotations}, {@code getDeclaredAnnotations} and
 * {@code getParameterAnnotations} are inherited abstract and not defined here; our javac rejects
 * every spelling of an override returning {@code Annotation[]} or {@code Annotation[][]}. See
 * {@link Parameter} for the full write-up. All four would be {@code native} and unimplemented by the
 * VM regardless.
 */
public final class Constructor<T> extends Executable {

    private Class<T> clazz;
    private Class<?>[] parameterTypes;
    private Class<?>[] exceptionTypes;
    private int modifiers;
    private int slot;

    private Constructor() {
    }

    /**
     * Returns the class that declares this constructor.
     *
     * @return the declaring class
     */
    public Class<T> getDeclaringClass() {
        return this.clazz;
    }

    /**
     * Returns the binary name of the class this constructor constructs.
     *
     * <p>A constructor has no name of its own — {@code <init>} is the class file's spelling, not the
     * language's — so reflection reports the class's name, which is also what source code writes at
     * the {@code new} site.
     *
     * @return the declaring class's binary name
     */
    public String getName() {
        return this.clazz.getName();
    }

    /**
     * Returns this constructor's Java language modifiers, as a bitmask.
     *
     * @return the modifiers, decodable with {@link Modifier}
     */
    public int getModifiers() {
        return this.modifiers;
    }

    /**
     * Returns this constructor's parameter types, in declaration order.
     *
     * @return the parameter types; empty if it takes none
     */
    public Class<?>[] getParameterTypes() {
        return this.parameterTypes;
    }

    /**
     * Returns how many parameters this constructor declares.
     *
     * @return the parameter count
     */
    public int getParameterCount() {
        if (this.parameterTypes == null) {
            return 0;
        }
        return this.parameterTypes.length;
    }

    /**
     * Returns the checked exception types this constructor declares.
     *
     * @return the exception types; empty if it declares none
     */
    public Class<?>[] getExceptionTypes() {
        return this.exceptionTypes;
    }

    /**
     * Returns a string describing this constructor, including modifiers and fully-qualified type
     * names.
     *
     * <p>Format: {@code modifiers declaringClass(paramTypes) throws excTypes} — the JDK's, minus the
     * type parameters, which need the {@code Signature} attribute the VM cannot yet supply. There is
     * no return type in the output, because there is none to print.
     *
     * @return the generic string form
     */
    public String toGenericString() {
        StringBuilder out = new StringBuilder();
        String mods = Modifier.toString(this.modifiers & Modifier.constructorModifiers());
        if (mods.length() > 0) {
            out.append(mods);
            out.append(' ');
        }
        out.append(this.clazz.getName());
        out.append('(');
        appendTypeList(out, this.parameterTypes);
        out.append(')');
        if (this.exceptionTypes != null && this.exceptionTypes.length > 0) {
            out.append(" throws ");
            appendTypeList(out, this.exceptionTypes);
        }
        return out.toString();
    }

    // As Method.appendTypeList: comma-separated binary names, null-tolerant so that a partially
    // populated Constructor is still printable.
    private static void appendTypeList(StringBuilder out, Class<?>[] types) {
        if (types == null) {
            return;
        }
        for (int i = 0; i < types.length; i = i + 1) {
            if (i > 0) {
                out.append(',');
            }
            out.append(types[i].getName());
        }
    }

    /**
     * Returns a string describing this constructor. Same format as {@link #toGenericString}.
     *
     * @return the string form
     */
    public String toString() {
        return toGenericString();
    }

    /**
     * Returns the type parameters this constructor declares.
     *
     * <p>Backed by the VM: needs the {@code Signature} attribute parsed into {@link TypeVariable}s.
     *
     * @return the type variables; empty if it is not generic
     */
    public TypeVariable<Constructor<T>>[] getTypeParameters() {
        // Vacio, y es la respuesta correcta: los parametros de tipo viven solo en
        // el atributo `Signature`, la VM no lo lee, y un constructor sin
        // `Signature` no declara ninguno.
        return new TypeVariable[0];
    }

    /**
     * Returns the annotated use of the type this constructor constructs.
     *
     * <p>Backed by the VM: needs {@code RuntimeVisibleTypeAnnotations} parsing.
     *
     * @return the annotated return type
     */
    public AnnotatedType getAnnotatedReturnType() {
        return new AnnotatedTypeImpl(this.clazz);
    }

    /** A constructor's receiver type, or {@code null} for one that takes no receiver. */
    public AnnotatedType getAnnotatedReceiverType() {
        if (Modifier.isStatic(this.getModifiers())) {
            return null;
        }
        return new AnnotatedTypeImpl(this.clazz);
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

    /** One (empty) annotation row per parameter. */
    public Annotation[][] getParameterAnnotations() {
        int n = this.getParameterTypes().length;
        return (Annotation[][]) Array.newInstance(Annotation.class, n, 0);
    }

    /**
     * Whether this constructor was declared with a variable arity parameter.
     */
    public boolean isVarArgs() {
        return (this.modifiers & 0x0080) != 0;
    }

    /** Whether the compiler generated this constructor rather than a programmer writing it. */
    public boolean isSynthetic() {
        return (this.modifiers & 0x1000) != 0;
    }

    /**
     * Whether {@code obj} is a {@code Constructor} describing the same constructor.
     *
     * <p>No return type in the comparison, unlike {@link Method#equals(Object)}: a constructor
     * has none, which is also why a class cannot declare two that differ only there.
     *
     * @param obj the object to compare against
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Constructor)) {
            return false;
        }
        Constructor<?> other = (Constructor<?>) obj;
        if (this.clazz != other.getDeclaringClass()) {
            return false;
        }
        Class<?>[] mine = this.parameterTypes;
        Class<?>[] theirs = other.getParameterTypes();
        if (mine.length != theirs.length) {
            return false;
        }
        int i = 0;
        while (i < mine.length) {
            if (mine[i] != theirs[i]) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /** The declaring class's name hashed, which is the JDK's formula. */
    public int hashCode() {
        return this.clazz.getName().hashCode();
    }

    /**
     * Suppresses the access check for this constructor.
     *
     * <p>A no-op here, because KajiJDK performs no access check on a reflective call in the first
     * place. Declared rather than merely inherited because the JDK declares it too.
     *
     * @param flag whether to suppress the check
     */
    public void setAccessible(boolean flag) {
        super.setAccessible(flag);
    }

    /** The parameter types in the generic model, erased. */
    public Type[] getGenericParameterTypes() {
        return super.getGenericParameterTypes();
    }

    /** The declared exception types in the generic model, erased. */
    public Type[] getGenericExceptionTypes() {
        return super.getGenericExceptionTypes();
    }

    /**
     * Creates a new instance by invoking this constructor with {@code args}.
     *
     * <p>Backed by the VM.
     *
     * @param args the arguments
     * @return the new instance
     */
    public T newInstance(Object... args) {
        // Not native: the VM intercepts this call (Intrinsic::ConstructorNewInstance), allocates and
        // runs the constructor directly, so this body is never reached. It exists so the surface
        // matches the JDK's (whose newInstance is an ordinary method).
        throw new UnsupportedOperationException("Constructor.newInstance is intercepted by the VM");
    }
}
