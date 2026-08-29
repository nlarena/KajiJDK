package java.lang.reflect;

/**
 * One parameter of a {@link Method} or {@link Constructor}.
 *
 * <p>Before Java 8 a parameter was not a thing reflection could point at — only
 * {@code getParameterTypes()[i]} and {@code getParameterAnnotations()[i]}, two parallel arrays the
 * caller had to keep in step by hand. {@code Parameter} bundles the position, so a framework that
 * wants "the type and the annotations of argument 2" asks one object instead of indexing two arrays
 * and hoping they agree.
 *
 * <p>The catch, and the reason {@link #isNamePresent} exists at all, is that parameter <em>names</em>
 * are optional in a class file. {@code javac} emits the {@code MethodParameters} attribute only under
 * {@code -parameters}, so most class files in the world carry no names and reflection has to invent
 * {@code arg0}, {@code arg1}, … {@code isNamePresent} is how a caller tells a real name from an
 * invented one — which matters, because binding a JSON field or a web request parameter to
 * {@code arg0} is a bug, not a feature.
 *
 * <h2>KajiLibrary status</h2>
 *
 * <p>Objects are created only by {@link Executable#getParameters}, which always synthesizes: there is
 * no {@code MethodParameters} data to read, so {@link #isNamePresent} always reports {@code false},
 * {@link #getName} always returns {@code argN}, and {@link #getModifiers} always returns 0.
 * {@link #getType} and {@link #getParameterizedType} are real — they index the declaring
 * executable's parameter types.
 *
 * <p>Omitted from the JDK's set: {@code accessFlags()} (needs {@code AccessFlag}, absent) and
 * {@code getAnnotatedType()} (needs {@code RuntimeVisibleTypeAnnotations} parsing in the VM).
 *
 * <h2>Why this does not implement {@code AnnotatedElement}</h2>
 *
 * <p>In the JDK it does, and it should here. It does not, because <strong>our javac cannot compile
 * any class that implements {@link AnnotatedElement}</strong> — the two halves of the check
 * contradict each other and leave no legal program in between:
 *
 * <ul>
 *   <li>Omitting {@code getAnnotations()} is rejected: "{@code Parameter} no es abstracta y no
 *       implementa {@code getAnnotations} de {@code AnnotatedElement}". Correct, and exactly what
 *       real javac says.</li>
 *   <li>Defining it is <em>also</em> rejected: "{@code Annotation[]} no es un subtipo de
 *       {@code ?[]}". The inherited signature, read back out of {@code AnnotatedElement.class},
 *       loses its array component type. Every spelling fails — {@code native} or with a body,
 *       simple name or fully qualified.</li>
 * </ul>
 *
 * <p>So the choice was between a class that does not compile and a class that compiles without the
 * three annotation methods. Dropping the {@code implements} clause is the honest version of the
 * second: the type no longer claims a contract it does not carry, and nothing silently degrades into
 * an {@code AbstractMethodError}. Nothing is lost in practice — all three would have been
 * {@code native}, and the VM implements none of them. The defect is reported to the compiler session
 * with a repro; when it is fixed, the clause and the three methods go back in together.
 */
public final class Parameter {

    private final String name;
    private final int modifiers;
    private final Executable executable;
    private final int index;

    // Package-private, matching the JDK: only Executable.getParameters may build these, because only
    // it knows the index is in range, and an out-of-range index would make getType() throw later,
    // far from the mistake.
    Parameter(String name, int modifiers, Executable executable, int index) {
        this.name = name;
        this.modifiers = modifiers;
        this.executable = executable;
        this.index = index;
    }

    /**
     * Compares this parameter to another: equal when they are the same position of the same
     * executable.
     *
     * @param obj the object to compare with
     * @return {@code true} if they denote the same parameter
     */
    public boolean equals(Object obj) {
        if (obj instanceof Parameter) {
            Parameter other = (Parameter) obj;
            return other.executable.equals(this.executable) && other.index == this.index;
        }
        return false;
    }

    /**
     * Returns a hash code consistent with {@link #equals}.
     *
     * @return the hash code
     */
    public int hashCode() {
        return this.executable.hashCode() ^ this.index;
    }

    /**
     * Returns whether the class file actually recorded a name for this parameter.
     *
     * <p>Always {@code false} in KajiLibrary — see the class notes.
     *
     * @return {@code true} if the name is real rather than synthesized
     */
    public boolean isNamePresent() {
        return false;
    }

    /**
     * Returns a description of this parameter: its type followed by its name.
     *
     * @return the string form
     */
    public String toString() {
        return getType().getName() + " " + getName();
    }

    /**
     * Returns the method or constructor that declares this parameter.
     *
     * @return the declaring executable
     */
    public Executable getDeclaringExecutable() {
        return this.executable;
    }

    /**
     * Returns this parameter's modifiers as a bitmask — {@code final}, {@code synthetic},
     * {@code mandated}.
     *
     * @return the modifiers, decodable with {@link Modifier}
     */
    public int getModifiers() {
        return this.modifiers;
    }

    /**
     * Returns this parameter's name, real or synthesized.
     *
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns this parameter's type as a generic {@link Type}.
     *
     * @return the generic parameter type
     */
    public Type getParameterizedType() {
        return this.executable.getGenericParameterTypes()[this.index];
    }

    /**
     * Returns this parameter's type.
     *
     * @return the parameter type
     */
    public Class<?> getType() {
        return this.executable.getParameterTypes()[this.index];
    }

    /**
     * Returns whether this parameter is implicitly declared — mandated by the language rather than
     * written in source, like the enclosing instance an inner class's constructor receives.
     *
     * @return {@code true} if it is mandated
     */
    public boolean isImplicit() {
        return (getModifiers() & 0x00008000) != 0;
    }

    /**
     * Returns whether this parameter is synthetic — neither written in source nor mandated.
     *
     * @return {@code true} if it is synthetic
     */
    public boolean isSynthetic() {
        return (getModifiers() & 0x00001000) != 0;
    }

    /**
     * Returns whether this parameter is the variable-arity parameter of a varargs executable — that
     * is, whether it is the last one and its executable is varargs.
     *
     * @return {@code true} if it is the varargs parameter
     */
    public boolean isVarArgs() {
        return this.executable.isVarArgs() && this.index == this.executable.getParameterCount() - 1;
    }

    // ------------------------------------------------------------------------------------------
    // AnnotatedElement is deliberately NOT implemented here -- see the class comment. When the
    // compiler defect is fixed, `implements AnnotatedElement` goes back on the declaration above and
    // getAnnotation / getAnnotations / getDeclaredAnnotations come back as natives, right here.
    // ------------------------------------------------------------------------------------------
}
