package java.lang.reflect;

import java.lang.annotation.Annotation;

/**
 * A reflective method.
 *
 * <p>KajiLibrary subset: the object is meant to be created and populated by the VM
 * ({@code Class.getDeclaredMethods}, a runtime follow-up); the getters read the stored metadata and
 * {@link #invoke} is a native.
 *
 * <h2>Now a subclass of {@link Executable}</h2>
 *
 * <p>It used to extend {@link AccessibleObject} directly, which is what the JDK did until Java 8 and
 * for the same reason: nothing forced the shared shape out into its own type. Everything a method
 * and a constructor have in common — parameters, exception types, modifiers, declaring class, and
 * above all {@link Executable#getParameters} — now lives one level up, and this class is what is
 * genuinely method-specific: a return type, a name of its own, and {@code invoke}.
 *
 * <p>The field layout is unchanged apart from {@code exceptionTypes}, which {@link #getExceptionTypes}
 * needs; {@code Executable} declares no instance state of its own, so a VM that populates
 * {@code clazz}/{@code name}/{@code returnType}/… still sees the same slots in the same order.
 *
 * <h2>What is still missing</h2>
 *
 * <p>{@code getAnnotation}, {@code getAnnotations} and {@code getDeclaredAnnotations} are inherited
 * abstract, from {@link AnnotatedElement} by way of {@link GenericDeclaration}, and are
 * <strong>not</strong> defined here — our javac rejects every spelling of a method returning
 * {@code Annotation[]} that overrides an inherited one (see {@link Parameter} for the full write-up).
 * They would be {@code native} and the VM implements none of them, so the observable difference is
 * an {@code AbstractMethodError} instead of an {@code UnsatisfiedLinkError}.
 */
public final class Method extends Executable {

    private Class<?> clazz;
    private String name;
    private Class<?> returnType;
    private Class<?>[] parameterTypes;
    private Class<?>[] exceptionTypes;
    private int modifiers;
    private int slot;

    private Method() {
    }

    /**
     * Returns the class or interface that declares this method.
     *
     * @return the declaring class
     */
    public Class<?> getDeclaringClass() {
        return this.clazz;
    }

    /**
     * Returns this method's simple name.
     *
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns this method's Java language modifiers, as a bitmask.
     *
     * @return the modifiers, decodable with {@link Modifier}
     */
    public int getModifiers() {
        return this.modifiers;
    }

    /**
     * Returns this method's return type.
     *
     * @return the return type
     */
    public Class<?> getReturnType() {
        return this.returnType;
    }

    /**
     * Returns this method's parameter types, in declaration order.
     *
     * @return the parameter types; empty if it takes none
     */
    public Class<?>[] getParameterTypes() {
        return this.parameterTypes;
    }

    /**
     * Returns how many parameters this method declares.
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
     * Returns the checked exception types this method declares.
     *
     * @return the exception types; empty if it declares none
     */
    public Class<?>[] getExceptionTypes() {
        return this.exceptionTypes;
    }

    /**
     * Returns whether this method is a bridge — a synthetic forwarder the compiler generates so that
     * an overriding method with a covariant return type, or a generic method after erasure, still
     * has an entry with the erased signature the caller invokes.
     *
     * @return {@code true} if it is a bridge method
     */
    public boolean isBridge() {
        return (this.modifiers & 0x00000040) != 0;
    }

    /**
     * Returns whether this method is a default method — a non-abstract instance method declared in
     * an interface.
     *
     * @return {@code true} if it is a default method
     */
    public boolean isDefault() {
        // The JDK's own test, verbatim: in an interface, "public and neither abstract nor static"
        // is exactly what `default` compiles to; there is no ACC_DEFAULT flag to read.
        return (this.modifiers & (Modifier.ABSTRACT | Modifier.PUBLIC | Modifier.STATIC)) == Modifier.PUBLIC
                && this.clazz.isInterface();
    }

    /**
     * Returns a string describing this method, including modifiers and fully-qualified type names.
     *
     * <p>The format is {@code modifiers returnType declaringClass.name(paramTypes) throws excTypes},
     * which is the JDK's. Type parameters are not printed: reading them needs the {@code Signature}
     * attribute, and {@link #getTypeParameters} is still VM-blocked. For a non-generic method — every
     * method the VM can currently describe — the output is identical to the JDK's.
     *
     * @return the generic string form
     */
    public String toGenericString() {
        StringBuilder out = new StringBuilder();
        String mods = Modifier.toString(this.modifiers & Modifier.methodModifiers());
        if (mods.length() > 0) {
            out.append(mods);
            out.append(' ');
        }
        out.append(this.returnType.getName());
        out.append(' ');
        out.append(this.clazz.getName());
        out.append('.');
        out.append(this.name);
        out.append('(');
        appendTypeList(out, this.parameterTypes);
        out.append(')');
        if (this.exceptionTypes != null && this.exceptionTypes.length > 0) {
            out.append(" throws ");
            appendTypeList(out, this.exceptionTypes);
        }
        return out.toString();
    }

    // Comma-separated binary names. Tolerates a null array because the VM populates these fields and
    // a half-populated Method should still be printable -- toString-family methods that throw are
    // the worst kind to debug.
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
     * Returns a string describing this method. Same format as {@link #toGenericString}.
     *
     * @return the string form
     */
    public String toString() {
        return toGenericString();
    }

    /**
     * Returns the type parameters this method declares.
     *
     * <p>Backed by the VM: needs the {@code Signature} attribute parsed into {@link TypeVariable}s.
     *
     * @return the type variables; empty if it is not generic
     */
    public TypeVariable<Method>[] getTypeParameters() {
        // Empty, and that is the right answer rather than a stub: a method's type
        // parameters live only in its `Signature` attribute, the VM does not read one, and a
        // method without a `Signature` declares none. The gap this leaves is real and
        // narrow -- a genuinely generic method reports zero instead of its variables -- and
        // it is the same gap as `getGenericParameterTypes`, not a separate one.
        return new TypeVariable[0];
    }

    // getParameterAnnotations() stays inherited abstract from Executable: `Annotation[][]` overriding
    // `Annotation[][]` is rejected by the same compiler defect described in the class comment.

    /**
     * Returns the annotated use of this method's return type (carrying no type annotations here,
     * as KajiLibrary does not model {@code RuntimeVisibleTypeAnnotations}).
     */
    public AnnotatedType getAnnotatedReturnType() {
        return new AnnotatedTypeImpl(this.returnType);
    }

    /**
     * Whether this method was declared with a variable arity parameter.
     *
     * <p>Nothing about the parameter list says so: the last parameter is an array either way, and
     * {@code ACC_VARARGS} is the only thing that tells {@code f(int...)} from {@code f(int[])}.
     */
    public boolean isVarArgs() {
        return (this.modifiers & 0x0080) != 0;
    }

    /** Whether the compiler generated this method rather than a programmer writing it. */
    public boolean isSynthetic() {
        return (this.modifiers & 0x1000) != 0;
    }

    /**
     * Whether {@code obj} is a {@code Method} describing the same method.
     *
     * <p>Two separate {@code Method} objects can describe the same method -- every call to
     * {@code getDeclaredMethods} mints fresh ones -- so identity is the wrong test and this one
     * is needed. The RETURN type is part of the comparison: two methods that differ only there
     * are two distinct entries in a class file, and one of them is usually a bridge.
     *
     * @param obj the object to compare against
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Method)) {
            return false;
        }
        Method other = (Method) obj;
        if (this.clazz != other.getDeclaringClass() || this.returnType != other.getReturnType()) {
            return false;
        }
        if (!this.name.equals(other.getName())) {
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

    /**
     * The declaring class's name hashed against this method's name.
     *
     * <p>The JDK's formula exactly, and worth keeping identical: it deliberately leaves the
     * parameter types out, so all the overloads of one name collide. That is a choice about hash
     * tables of methods, which are small and rarely hold two overloads at once.
     */
    public int hashCode() {
        return this.clazz.getName().hashCode() ^ this.name.hashCode();
    }

    /**
     * Suppresses the access check for this method.
     *
     * <p>A no-op here, because KajiJDK performs no access check on a reflective call in the first
     * place -- so every method behaves as if the flag were already set. Declared rather than
     * merely inherited because the JDK declares it too.
     *
     * @param flag whether to suppress the check
     */
    public void setAccessible(boolean flag) {
        super.setAccessible(flag);
    }

    /**
     * The return type in the generic model.
     *
     * <p>The erased type, for the reason spelled out in {@code Executable.getGenericParameterTypes}:
     * that is what a method with no {@code Signature} attribute reports, and none of ours has one.
     */
    public Type getGenericReturnType() {
        return this.returnType;
    }

    /** The parameter types in the generic model. */
    public Type[] getGenericParameterTypes() {
        return super.getGenericParameterTypes();
    }

    /** The declared exception types in the generic model. */
    public Type[] getGenericExceptionTypes() {
        return super.getGenericExceptionTypes();
    }

    /**
     * Invokes this method on {@code obj} with {@code args}.
     *
     * <p>Backed by the VM (a runtime follow-up).
     *
     * @param obj the receiver, or {@code null} for a static method
     * @param args the arguments
     * @return the result, boxed if the return type is primitive
     */
    public Object invoke(Object obj, Object... args) {
        // Not native: the VM intercepts this call (Intrinsic::MethodInvoke) and runs the target
        // method directly, so this body is never reached. It exists so the surface matches the JDK's
        // (whose invoke is also an ordinary method, not native).
        throw new UnsupportedOperationException("Method.invoke is intercepted by the VM");
    }

    /**
     * The default value of this annotation-interface element, or {@code null} if it has none (or is
     * not an annotation element). KajiLibrary does not parse the {@code AnnotationDefault} attribute
     * through reflection, so this reports "no default" -- correct for every non-annotation method.
     */
    public Object getDefaultValue() {
        return null;
    }

    // ---- annotations ----
    //
    // A KajiLibrary subset, as on Field: method-level RUNTIME annotation reflection is not wired.
    // A method with no runtime annotations -- the common case -- gets the right empty answers.

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
        int n = this.parameterTypes == null ? 0 : this.parameterTypes.length;
        // `new Annotation[n][0]` is not accepted by this javac; the reflective multi-dim create is.
        return (Annotation[][]) Array.newInstance(Annotation.class, n, 0);
    }
}
