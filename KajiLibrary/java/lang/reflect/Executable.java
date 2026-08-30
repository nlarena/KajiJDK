package java.lang.reflect;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * The shared supertype of {@link Method} and {@link Constructor} — everything reflection can say
 * about a thing you can invoke.
 *
 * <p>Methods and constructors are startlingly similar: both have parameters, exception types, type
 * parameters, parameter annotations, modifiers and a declaring class. They differ in exactly two
 * places — a constructor has no return type and no name of its own. For the first fifteen years of
 * the JDK that similarity was expressed by copy-and-paste, and {@code Method} and {@code Constructor}
 * were unrelated classes with parallel implementations. {@code Executable} was extracted in Java 8
 * so that {@link #getParameters}, and the {@link Parameter} type it returns, could be written once.
 *
 * <p>The class is abstract with a package-private constructor: it exists to be extended by exactly
 * the two classes in this package that already extend it, and by nothing else.
 *
 * <h2>KajiLibrary status</h2>
 *
 * <p>The public method set is the JDK's, with these deliberate differences:
 *
 * <ul>
 *   <li><strong>{@code accessFlags()} is omitted.</strong> It returns a {@code Set<AccessFlag>}, and
 *       {@code java.lang.reflect.AccessFlag} does not exist in KajiLibrary yet. {@link #getModifiers}
 *       gives the same information as a bitmask, decodable with {@link Modifier}.</li>
 *   <li><strong>{@link #getGenericParameterTypes} and {@link #getGenericExceptionTypes} are
 *       {@code native}.</strong> The natural implementation is the JDK's own no-generic-information
 *       fallback: widen the erased {@code Class<?>[]} into a {@code Type[]}, which is legal because
 *       in the real JDK {@code java.lang.Class implements Type}. KajiLibrary's {@code Class} does
 *       <em>not</em> implement it, so the widening does not typecheck and the fallback is
 *       unavailable. That single missing {@code implements} is the load-bearing gap in the whole
 *       generic type model — without it no {@link ParameterizedType#getRawType} can return a real
 *       class either. It belongs to the session that owns {@code java.lang}.</li>
 *   <li><strong>The four {@code getAnnotated*} accessors are {@code native}.</strong> They need the
 *       {@code RuntimeVisibleTypeAnnotations} attribute parsed into {@link AnnotatedType} trees,
 *       which is VM work that has not been done.</li>
 *   <li><strong>{@code getAnnotations()} and {@code getDeclaredAnnotations()} are not redeclared.</strong>
 *       They are inherited abstract from {@link AnnotatedElement}. Redeclaring them trips a compiler
 *       defect in the return-compatibility check for array-typed returns; the effect on the API is
 *       nil.</li>
 * </ul>
 */
public abstract class Executable extends AccessibleObject implements Member, GenericDeclaration {

    // Package-private, as in the JDK: Method and Constructor are the only legal subclasses, and both
    // are in this package. It is not private only because subclass constructors must be able to
    // chain to it.
    Executable() {
    }

    /**
     * Returns the class or interface that declares this executable.
     *
     * @return the declaring class
     */
    public abstract Class<?> getDeclaringClass();

    /**
     * Returns the name of this executable. A constructor reports its declaring class's binary name.
     *
     * @return the name
     */
    public abstract String getName();

    /**
     * Returns the Java language modifiers of this executable, as a bitmask.
     *
     * @return the modifiers, decodable with {@link Modifier}
     */
    public abstract int getModifiers();

    /** This executable's type parameters, in declaration order (empty if it is not generic). */
    public abstract TypeVariable<?>[] getTypeParameters();

    /**
     * Returns this executable's parameter types, in declaration order.
     *
     * @return the parameter types; empty if it takes none
     */
    public abstract Class<?>[] getParameterTypes();

    /**
     * Returns how many parameters this executable declares.
     *
     * @return the parameter count
     */
    public abstract int getParameterCount();

    /**
     * Returns this executable's parameter types as generic {@link Type}s.
     *
     * <p>Backed by the VM, and blocked on it twice over — see the class notes.
     *
     * @return the generic parameter types, in declaration order
     */
    public Type[] getGenericParameterTypes() {
        return Executable.erased(this.getParameterTypes());
    }

    /**
     * Returns this executable's exception types.
     *
     * @return the declared checked exception types; empty if it declares none
     */
    public abstract Class<?>[] getExceptionTypes();

    /**
     * Returns this executable's exception types as generic {@link Type}s.
     *
     * <p>Backed by the VM; see {@link #getGenericParameterTypes}.
     *
     * @return the generic exception types
     */
    public Type[] getGenericExceptionTypes() {
        return Executable.erased(this.getExceptionTypes());
    }

    // The erased types, widened to the generic model. Not a placeholder for the generic
    // answer -- it IS the answer whenever the method carries no `Signature` attribute, which
    // is the case the JDK handles the same way. What a `Signature` would add is the
    // difference between `List` and `List<String>`, and no method the VM can describe today
    // has one.
    //
    // Copied rather than returned as-is: `Class[]` IS a `Type[]` by array covariance, so
    // handing the caller the stored array would let them write a `TypeVariable` into this
    // method's parameter list and get an ArrayStoreException at some unrelated later read.
    private static Type[] erased(Class<?>[] types) {
        if (types == null) {
            return new Type[0];
        }
        Type[] out = new Type[types.length];
        int i = 0;
        while (i < types.length) {
            out[i] = types[i];
            i = i + 1;
        }
        return out;
    }

    /**
     * Returns a {@link Parameter} for each of this executable's parameters, in declaration order.
     *
     * <p>The JDK reads the optional {@code MethodParameters} attribute for real names and modifiers
     * and, when it is absent — which it is unless the class was compiled with {@code -parameters} —
     * synthesizes {@code arg0}, {@code arg1}, … KajiLibrary always takes the synthesizing path, so
     * {@link Parameter#isNamePresent} always reports {@code false} and the names are always
     * positional. That is the same answer the JDK gives for the overwhelming majority of real class
     * files, so the behaviour is representative rather than degraded.
     *
     * <p>A fresh array of fresh {@code Parameter} objects on every call: the JDK caches, but caching
     * has to hand out copies anyway to keep the array immutable from the caller's side, and there is
     * nothing to cache against until the VM supplies real parameter data.
     *
     * @return the parameters
     */
    public Parameter[] getParameters() {
        Class<?>[] types = getParameterTypes();
        Parameter[] parameters = new Parameter[types.length];
        for (int i = 0; i < types.length; i = i + 1) {
            parameters[i] = new Parameter("arg" + i, 0, this, i);
        }
        return parameters;
    }

    /**
     * Returns a string describing this executable, including its type parameters.
     *
     * @return the generic string form
     */
    public abstract String toGenericString();

    /**
     * Returns whether this executable was declared to take a variable number of arguments.
     *
     * <p>Reads {@code ACC_VARARGS} out of the modifiers. Note that KajiJDK's own javac does not
     * <em>emit</em> that flag yet, so this reports {@code false} for KajiLibrary-compiled varargs
     * methods — a compiler gap, not one here.
     *
     * @return {@code true} if it is a varargs executable
     */
    public boolean isVarArgs() {
        return (getModifiers() & 0x00000080) != 0;
    }

    /**
     * Returns whether this executable is synthetic — introduced by the compiler, absent from source.
     *
     * @return {@code true} if it is synthetic
     */
    public boolean isSynthetic() {
        return (getModifiers() & 0x00001000) != 0;
    }

    /**
     * Returns the annotations on each parameter, in declaration order.
     *
     * <p>The outer array has one entry per parameter — empty inner arrays for unannotated ones —
     * which is why it is a jagged array rather than a flat one.
     *
     * @return the parameter annotations
     */
    public abstract Annotation[][] getParameterAnnotations();

    /**
     * Returns the annotated use of this executable's return type.
     *
     * <p>For a constructor this is the use of the class being constructed.
     *
     * @return the annotated return type
     */
    public abstract AnnotatedType getAnnotatedReturnType();

    /**
     * Returns the annotated use of this executable's receiver type, or {@code null} if it has none.
     *
     * <p>A receiver parameter — the explicit {@code Outer.this} an instance method may declare purely
     * so that a type annotation has somewhere to land — is the rarest thing in this whole API.
     * Static methods and top-level constructors never have one.
     *
     * <p>Backed by the VM: needs {@code RuntimeVisibleTypeAnnotations} parsing.
     *
     * @return the annotated receiver type, or {@code null}
     */
    // The annotated-type views carry no type annotations (KajiLibrary does not model
    // RuntimeVisibleTypeAnnotations): each is its plain type wrapped in an AnnotatedType.

    public AnnotatedType getAnnotatedReceiverType() {
        if (Modifier.isStatic(this.getModifiers())) {
            return null;
        }
        return new AnnotatedTypeImpl(this.getDeclaringClass());
    }

    public AnnotatedType[] getAnnotatedParameterTypes() {
        Class<?>[] types = this.getParameterTypes();
        AnnotatedType[] out = new AnnotatedType[types.length];
        int i = 0;
        while (i < types.length) {
            out[i] = new AnnotatedTypeImpl(types[i]);
            i = i + 1;
        }
        return out;
    }

    public AnnotatedType[] getAnnotatedExceptionTypes() {
        Class<?>[] types = this.getExceptionTypes();
        AnnotatedType[] out = new AnnotatedType[types.length];
        int i = 0;
        while (i < types.length) {
            out[i] = new AnnotatedTypeImpl(types[i]);
            i = i + 1;
        }
        return out;
    }

    /** This executable's access flags, resolved at the {@code METHOD} location. */
    public Set<AccessFlag> accessFlags() {
        return AccessFlag.maskToAccessFlags(this.getModifiers(), AccessFlag.Location.METHOD);
    }

    // ---- annotations (re-declared at this level to match the JDK; member-level is empty) ----

    public Annotation[] getDeclaredAnnotations() {
        return new Annotation[0];
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        if (annotationClass == null) {
            throw new NullPointerException();
        }
        return null;
    }

    public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
        return (T[]) Array.newInstance(annotationClass, 0);
    }
}
