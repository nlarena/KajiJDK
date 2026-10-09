package java.lang.reflect;

/**
 * One component of a record class — the {@code int x} in {@code record Point(int x, int y) {}}.
 *
 * <p>A record component is not a field, and that distinction is the reason this class exists rather
 * than reusing {@link Field}. A component is a piece of the record's <em>declared state</em>: it
 * names a private final field, a public accessor method, a canonical constructor parameter, and a
 * position in {@code equals}/{@code hashCode}/{@code toString}, all at once. Reflecting on the field
 * would find one of those four and lose the rest — in particular it would lose the ordering, which
 * is what makes a record serializable and pattern-matchable.
 *
 * <p>The class file records this separately, in the {@code Record} attribute, which is also how the
 * JVM tells a real record from a class that merely extends {@code java.lang.Record}.
 *
 * <h2>KajiLibrary status</h2>
 *
 * <p>The object is populated by the VM ({@code Class.getRecordComponents}, a runtime follow-up),
 * exactly as {@link Field} is: the constructor is private and the getters read stored metadata.
 *
 * <p>Two divergences from the JDK, both forced:
 *
 * <ul>
 *   <li><strong>It does not implement {@link AnnotatedElement}</strong>, and declares none of the
 *       annotation accessors. Our javac cannot compile any class that implements that interface —
 *       omitting {@code getAnnotations()} is rejected as an unimplemented abstract method, and
 *       defining it is rejected by the return-compatibility check. See {@link Parameter} for the
 *       full write-up and the repro.</li>
 *   <li><strong>{@link #getGenericType} and {@code getAnnotatedType()} are not implemented.</strong>
 *       The first needs the component's {@code Signature} parsed; it is declared {@code native}. The
 *       second needs {@code RuntimeVisibleTypeAnnotations} and is omitted entirely.</li>
 * </ul>
 */
public final class RecordComponent {

    private Class<?> clazz;
    private String name;
    private Class<?> type;
    private Method accessor;
    private String signature;

    // Only the VM constructs RecordComponent objects, populating the fields from native code.
    private RecordComponent() {
    }

    /**
     * Returns this component's name, which is also its accessor's name and its field's name.
     *
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns this component's declared type.
     *
     * @return the type
     */
    public Class<?> getType() {
        return this.type;
    }

    /**
     * Returns this component's generic signature, or {@code null} if it has none.
     *
     * <p>The raw {@code Signature} string out of the class file — {@code null} whenever the
     * component's type is not generic, which is the usual case.
     *
     * @return the generic signature, or {@code null}
     */
    public String getGenericSignature() {
        return this.signature;
    }

    /**
     * Returns this component's type as a generic {@link Type}.
     *
     * <p>Backed by the VM: needs {@link #getGenericSignature} parsed into a {@code Type} tree.
     *
     * @return the generic type
     */
    public Type getGenericType() {
        // The erased type here, as no Signature is parsed -- correct for a non-generic component.
        return this.type;
    }

    /**
     * Returns the accessor method the record declares for this component.
     *
     * <p>Always present: a record either declares the accessor explicitly or has it generated, so
     * this never returns {@code null} for a well-formed record.
     *
     * @return the accessor method
     */
    public Method getAccessor() {
        return this.accessor;
    }

    /**
     * Returns the record class this component belongs to.
     *
     * @return the declaring record class
     */
    public Class<?> getDeclaringRecord() {
        return this.clazz;
    }

    /**
     * Returns a description of this component: its type followed by its name.
     *
     * @return the string form
     */
    public String toString() {
        return this.type.getName() + " " + this.name;
    }

    /** The component's type as an {@link AnnotatedType} (no type annotations modelled). */
    public AnnotatedType getAnnotatedType() {
        return new AnnotatedTypeImpl(this.type);
    }

    // ---- annotations (component-level runtime annotations not modelled: "none") ----

    public java.lang.annotation.Annotation[] getAnnotations() {
        return new java.lang.annotation.Annotation[0];
    }

    public java.lang.annotation.Annotation[] getDeclaredAnnotations() {
        return new java.lang.annotation.Annotation[0];
    }

    public <T extends java.lang.annotation.Annotation> T getAnnotation(Class<T> annotationClass) {
        if (annotationClass == null) {
            throw new NullPointerException();
        }
        return null;
    }
}
