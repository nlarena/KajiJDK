package java.lang;

// Por import y nombre simple: calificar el tipo en el uso no resuelve desde java.lang
// (finding #210).
import java.lang.constant.ClassDesc;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeImpl;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.security.ProtectionDomain;
import java.util.Optional;
import java.util.Set;

/**
 * KajiLibrary's java.lang.Class -- the runtime mirror of a type.
 *
 * <p>In KajiJDK a {@code Class<T>} object <em>is</em> the heap mirror the VM keeps for each
 * loaded class, so {@code getClass()} hands back that very reference and {@code ==} on two
 * mirrors is type identity. There are mirrors for three kinds of thing and only one of them has
 * a class file behind it: an ordinary class, a synthetic array class ({@code [I}), and a
 * primitive ({@code int}), whose mirror the VM mints from the keyword because there is nowhere
 * else it could come from.
 *
 * <p><strong>What is here and what is not.</strong> This is the METADATA half: everything
 * answerable from the class file's own header -- names, modifiers, the superclass, the
 * interfaces, the component type -- plus the {@code Field} and {@code Method} mirrors. What is
 * absent needs machinery that does not exist yet: {@code Constructor}, the annotation proxies
 * (one generated class per {@code @interface}), the generic type model, and the module and
 * protection-domain machinery. What used to be missing for a third reason -- the class file
 * attributes nobody read -- is here now: {@code InnerClasses}, {@code EnclosingMethod},
 * {@code NestHost}, {@code NestMembers}, {@code PermittedSubclasses} and {@code Record} are
 * what reconstructs the structure the language has and the class file loses.
 *
 * <p>Only four methods are {@code native}, and they are the four the JDK declares native too.
 * Everything else is Java over private seams, for the same reason {@code String} ended up with
 * four private seams: logic that lives in the VM is logic no Java test can step through, and
 * every method that was native for convenience rather than necessity also differed from the
 * reference in its modifiers.
 */
public final class Class<T> implements Type, java.lang.invoke.TypeDescriptor.OfField {

    // Only the VM constructs mirrors -- as in the JDK, whose constructor is private and called
    // from native code. Private also suppresses the synthesized public default one.
    private Class() {
    }

    // ---- the four the VM has to answer ----

    /**
     * Whether {@code obj} is an instance of this type.
     *
     * <p>The dynamic form of {@code instanceof}: the type is a value here rather than a name in
     * the bytecode, which is the whole point. {@code null} is never an instance of anything.
     *
     * @param obj the object to test
     */
    public native boolean isInstance(Object obj);

    /**
     * The direct superclass, or {@code null}.
     *
     * <p>Null in three separate cases worth telling apart: {@code Object}, which has none; every
     * interface, whose superclass is <em>not</em> {@code Object}; and every primitive. An array
     * answers {@code Object}.
     */
    public native Class<? super T> getSuperclass();

    /**
     * Whether a reference of type {@code cls} can be assigned to a reference of this type.
     *
     * <p>The direction reads backwards the first time: {@code Object.class.isAssignableFrom(
     * String.class)} is true. A primitive is assignable only to itself -- it takes part in no
     * hierarchy at all.
     *
     * @param cls the type being assigned from
     */
    public native boolean isAssignableFrom(Class<?> cls);

    /**
     * Whether this is a hidden class, one defined by {@code Lookup.defineHiddenClass} and
     * unreachable by name.
     *
     * <p>Always false here: nothing in KajiJDK defines one.
     */
    public native boolean isHidden();

    // The private seams. Each one reads the class file and nothing else; the shape of the answer
    // is decided in Java, above.
    private native String name0();

    private native int modifiers0();

    private native boolean isPrimitive0();

    private native Class<?>[] interfaces0();

    private native Class<?> componentType0();

    private native Class<?> arrayType0();

    private native Field[] declaredFields0();

    private native Method[] declaredMethods0();

    private native Constructor<?>[] declaredConstructors0();

    private native Class<?> nestHost0();

    private native Class<?>[] nestMembers0();

    private native Class<?>[] permittedSubclasses0();

    private native Class<?> declaringClass0();

    private native Class<?> enclosingClass0();

    private native String innerName0();

    private native boolean isInnerClass0();

    private native boolean hasEnclosingMethod0();

    private native Class<?>[] declaredClasses0();

    private native String[] enclosingMethodInfo0();

    private native java.lang.reflect.RecordComponent[] recordComponents0();

    private native boolean annotationPresent0(Class<?> annotationClass);

    // Materialises this class's directly-present RUNTIME annotations as objects. The VM spins one
    // class per annotation implementing its @interface (element methods return the values written at
    // the use site, or the @interface's defaults) and allocates an instance. No @Inherited walk.
    private native java.lang.annotation.Annotation[] declaredAnnotations0();

    private static native Class<?> forName0(String name);

    // The mirror of a PRIMITIVE type, by its keyword. Package-private and native, exactly as the
    // JDK declares it, and its only callers are the `TYPE` fields of the wrapper classes, which
    // live in this package.
    static native <T> Class<T> getPrimitiveClass(String name);

    // ---- what kind of thing this is ----

    /** The Java language modifiers, as a bitmask for {@link java.lang.reflect.Modifier}. */
    public int getModifiers() {
        return this.modifiers0();
    }

    /**
     * This class's access flags as a set of {@link AccessFlag}, resolved at the {@code CLASS}
     * location — the symbolic form of the {@code access_flags} the {@link #getModifiers() modifier}
     * bits encode.
     */
    public Set<AccessFlag> accessFlags() {
        return AccessFlag.maskToAccessFlags(this.getModifiers(), AccessFlag.Location.CLASS);
    }

    /** Whether this type is an interface. An annotation type is one too. */
    public boolean isInterface() {
        return (this.modifiers0() & 0x0200) != 0;
    }

    /**
     * Whether this type is an array.
     *
     * <p>Read off the name, which for an array is its descriptor: no other type name can begin
     * with a bracket.
     */
    public boolean isArray() {
        return this.name0().charAt(0) == '[';
    }

    /**
     * Whether this is one of the nine primitive types.
     *
     * <p>{@code void} counts, which is the surprise: {@code void.class.isPrimitive()} is true
     * even though no value ever has that type.
     */
    public boolean isPrimitive() {
        return this.isPrimitive0();
    }

    /** Whether this type is an annotation type -- an interface with the annotation flag. */
    public boolean isAnnotation() {
        return this.isInterface() && (this.modifiers0() & 0x2000) != 0;
    }

    /**
     * Whether this type is an enum type.
     *
     * <p>Note what it excludes: an enum constant with a body compiles to an anonymous SUBCLASS
     * of the enum, and that subclass answers false. The flag alone would say true, so the
     * superclass has to be checked as well.
     */
    public boolean isEnum() {
        if ((this.modifiers0() & 0x4000) == 0) {
            return false;
        }
        Class<?> parent = this.getSuperclass();
        // Compared by NAME rather than against `Enum.class`, which is what the JDK writes. A
        // class literal here would make loading `java.lang.Class` drag in `java.lang.Enum`, and
        // this class is loaded before almost everything else. The answer is the same.
        if (parent == null) {
            return false;
        }
        return parent.name0().equals("java/lang/Enum");
    }

    /** Whether this type is a record. */
    public boolean isRecord() {
        Class<?> parent = this.getSuperclass();
        if (parent == null) {
            return false;
        }
        return parent.name0().equals("java/lang/Record");
    }

    /** Whether the compiler generated this type rather than a programmer writing it. */
    public boolean isSynthetic() {
        return (this.modifiers0() & 0x1000) != 0;
    }

    // ---- names, of which there are five ----
    //
    // The JDK gives a type five different names and they disagree on purpose. For the member
    // class `Map.Entry` in an array: getName() is "[Ljava.util.Map$Entry;", getTypeName() is
    // "java.util.Map$Entry[]", getSimpleName() is "Entry[]", getCanonicalName() is
    // "java.util.Map.Entry[]" and descriptorString() is "[Ljava/util/Map$Entry;". Each one is
    // the right answer to a different question, and using the wrong one is how a class name
    // ends up unparseable at the other end.

    /**
     * The binary name: {@code java.lang.String}, {@code int}, {@code [Ljava.lang.String;}.
     *
     * <p>The name the class loader uses, and the one an array reports in DESCRIPTOR form -- so
     * it is not the name a programmer would write for an array type.
     */
    public String getName() {
        return this.name0().replace('/', '.');
    }

    /**
     * The name as source would spell the type: {@code java.lang.String[]} rather than
     * {@code [Ljava.lang.String;}.
     */
    public String getTypeName() {
        if (!this.isArray()) {
            return this.getName();
        }
        Class<?> element = this;
        int dimensions = 0;
        while (element.isArray()) {
            element = element.componentType0();
            dimensions = dimensions + 1;
        }
        StringBuilder out = new StringBuilder(element.getName());
        int i = 0;
        while (i < dimensions) {
            out.append("[]");
            i = i + 1;
        }
        return out.toString();
    }

    /**
     * The name without its package or its enclosing types: {@code Entry}, {@code int[]}.
     *
     * <p>Derived from the binary name by cutting at the last {@code '.'} and the last
     * {@code '$'}. That is exact for every type javac produces and it has one blind spot: a
     * TOP-LEVEL class whose source name really contains a dollar sign reports only the part
     * after it. Telling the two apart needs the {@code InnerClasses} attribute, which the VM
     * does not read yet.
     */
    public String getSimpleName() {
        if (this.isArray()) {
            return this.componentType0().getSimpleName() + "[]";
        }
        if (this.isInnerClass0()) {
            String inner = this.innerName0();
            if (inner == null) {
                return ""; // anonima: no tiene nombre que dar
            }
            return inner;
        }
        String binary = this.name0();
        return binary.substring(binary.lastIndexOf('/') + 1);
    }

    /**
     * The name as it could be written in source from anywhere: {@code java.util.Map.Entry}.
     *
     * <p>{@code null} when there is no such name -- an anonymous or local class cannot be named
     * from outside, and neither can an array of one.
     */
    public String getCanonicalName() {
        if (this.isArray()) {
            String element = this.componentType0().getCanonicalName();
            if (element == null) {
                return null;
            }
            return element + "[]";
        }
        String simple = this.getSimpleName();
        if (simple.length() == 0) {
            return null;
        }
        return this.getName().replace('$', '.');
    }

    /**
     * The package name, or {@code ""} for the default package.
     *
     * <p>An array reports its ELEMENT type's package, and a primitive reports
     * {@code java.lang} -- both of which are the specified answers and neither of which is the
     * obvious one.
     */
    public String getPackageName() {
        Class<?> element = this;
        while (element.isArray()) {
            element = element.componentType0();
        }
        if (element.isPrimitive()) {
            return "java.lang";
        }
        String binary = element.name0();
        int cut = binary.lastIndexOf('/');
        if (cut < 0) {
            return "";
        }
        return binary.substring(0, cut).replace('/', '.');
    }

    /**
     * The {@link Package} this class belongs to. Built from {@link #getPackageName()}; carries no
     * manifest attributes, because KajiJDK loads classes from directories, not versioned JARs.
     */
    public Package getPackage() {
        return new Package(getPackageName());
    }

    /**
     * The {@link Module} this class belongs to. KajiJDK has no module system, so every class is in
     * its loader's single, permissive unnamed module.
     */
    public Module getModule() {
        return new Module(getClassLoader());
    }

    // Package-private internal the reference exposes: the raw `RuntimeVisibleTypeAnnotations` bytes
    // for a member. KajiLibrary does not model type annotations, so there are none to hand back.
    static byte[] getExecutableTypeAnnotationBytes(java.lang.reflect.Executable ex) {
        return null;
    }

    // Package-private internal: the bounds clause of a type variable, as `toGenericString` would
    // render it. Reduced to the variable's name here — the bounds are erased in this library's
    // generic model, so there is nothing beyond the name to print.
    static String typeVarBounds(java.lang.reflect.TypeVariable<?> typeVar) {
        return typeVar.getName();
    }

    /** {@code "class java.lang.String"}, {@code "interface java.lang.Runnable"}, {@code "int"}. */
    public String toString() {
        if (this.isPrimitive()) {
            return this.getName();
        }
        String kind = "class ";
        if (this.isInterface()) {
            kind = "interface ";
        }
        return kind + this.getName();
    }

    /** The same with the modifiers in front: {@code "public final class java.lang.String"}. */
    public String toGenericString() {
        if (this.isPrimitive()) {
            return this.getName();
        }
        // ACC_INTERFACE is not a modifier a programmer writes -- it is what the word `interface`
        // already says -- and ACC_ABSTRACT is implied by it, so both come out of the list before
        // it is printed.
        int flags = this.getModifiers();
        int printable = flags & 0x0d1f;
        if (this.isInterface()) {
            printable = printable & ~0x0400;
        }
        StringBuilder out = new StringBuilder();
        String modifiers = Modifier.toString(printable);
        if (modifiers.length() > 0) {
            out.append(modifiers).append(' ');
        }
        if (this.isAnnotation()) {
            out.append('@');
        }
        if (this.isInterface()) {
            out.append("interface");
        } else if (this.isEnum()) {
            out.append("enum");
        } else if (this.isRecord()) {
            out.append("record");
        } else {
            out.append("class");
        }
        out.append(' ').append(this.getName());
        return out.toString();
    }

    /** The field descriptor: {@code Ljava/lang/String;}, {@code I}, {@code [I}. */
    public String descriptorString() {
        String binary = this.name0();
        if (binary.charAt(0) == '[') {
            return binary;
        }
        if (this.isPrimitive()) {
            if (binary.equals("int")) {
                return "I";
            }
            if (binary.equals("long")) {
                return "J";
            }
            if (binary.equals("double")) {
                return "D";
            }
            if (binary.equals("float")) {
                return "F";
            }
            if (binary.equals("short")) {
                return "S";
            }
            if (binary.equals("byte")) {
                return "B";
            }
            if (binary.equals("char")) {
                return "C";
            }
            if (binary.equals("boolean")) {
                return "Z";
            }
            return "V";
        }
        return "L" + binary + ";";
    }

    /**
     * This type as a constant that can be written into a class file's constant pool.
     *
     * <p>Empty only for a hidden class, which has no name to write.
     */
    public Optional<ClassDesc> describeConstable() {
        if (this.isHidden()) {
            return Optional.empty();
        }
        return Optional.of(ClassDesc.ofDescriptor(this.descriptorString()));
    }

    // ---- the type graph ----

    /**
     * The interfaces this type declares directly, in source order.
     *
     * <p>Directly: an interface a superclass implements is not in the list. Every array reports
     * {@code Cloneable} and {@code Serializable}, which no class file says anywhere -- the
     * language grants them (JLS 10.7) and the VM has to supply them.
     */
    public Class<?>[] getInterfaces() {
        return this.interfaces0();
    }

    /** The element type of an array, or {@code null} if this is not one. */
    public Class<?> getComponentType() {
        return this.componentType0();
    }

    /** The element type of an array, or {@code null}. The same as {@link #getComponentType()}. */
    public Class<?> componentType() {
        return this.componentType0();
    }

    /** The array type whose element type is this one: {@code int} answers {@code int[]}. */
    public Class<?> arrayType() {
        return this.arrayType0();
    }

    // ---- casting ----

    /**
     * {@code obj} narrowed to this type, or a {@code ClassCastException}.
     *
     * <p>The dynamic form of a cast, and the one that actually checks: an ordinary
     * {@code (T) obj} on a type variable is erased and checks nothing at runtime.
     *
     * @param obj the object to cast; {@code null} always passes
     */
    public T cast(Object obj) {
        if (obj != null && !this.isInstance(obj)) {
            throw new ClassCastException("cannot cast " + obj.getClass().getName() + " to "
                    + this.getName());
        }
        return (T) obj;
    }

    /**
     * This mirror, retyped as a subtype of {@code clazz}.
     *
     * <p>Checks the relationship rather than asserting it, so the unchecked cast it performs is
     * one that has just been proven.
     *
     * @param clazz the supertype to narrow against
     */
    public <U> Class<? extends U> asSubclass(Class<U> clazz) {
        if (!clazz.isAssignableFrom(this)) {
            throw new ClassCastException(this.getName() + " is not a subtype of "
                    + clazz.getName());
        }
        return (Class<? extends U>) this;
    }

    // ---- finding a class by name ----

    /**
     * The mirror of the type named {@code className}, loading it if necessary.
     *
     * @param className the binary name, with dots
     * @throws ClassNotFoundException if there is no such class
     */
    public static Class<?> forName(String className) throws ClassNotFoundException {
        Class<?> found = Class.forName0(className);
        if (found == null) {
            throw new ClassNotFoundException(className);
        }
        return found;
    }

    /**
     * The mirror of the type named {@code name}.
     *
     * <p>Both extra arguments are accepted and ignored, and that is honest rather than lazy:
     * there is exactly one class loader here, so naming another one cannot change the answer,
     * and every class is initialized on first active use whatever {@code initialize} says.
     *
     * @param name the binary name, with dots
     * @param initialize whether to run the static initializer
     * @param loader the loader to search
     * @throws ClassNotFoundException if there is no such class
     */
    public static Class<?> forName(String name, boolean initialize, ClassLoader loader)
            throws ClassNotFoundException {
        return Class.forName(name);
    }

    /**
     * The class {@code name} in {@code module}, or {@code null} if there is none — this overload
     * reports absence with {@code null} rather than an exception. The module is ignored: with a
     * single unnamed module over one class path, naming one cannot change which class is found.
     *
     * @param module the module to search (ignored)
     * @param name the binary name, with dots
     */
    public static Class<?> forName(Module module, String name) {
        Class<?> found;
        try {
            found = Class.forName(name);
        } catch (ClassNotFoundException e) {
            found = null;
        }
        return found;
    }

    /**
     * The mirror of a primitive type by its keyword, or {@code null} if that is not one.
     *
     * @param primitiveName {@code "int"}, {@code "void"}, and the rest
     */
    public static Class<?> forPrimitiveName(String primitiveName) {
        if (primitiveName.equals("int") || primitiveName.equals("long")
                || primitiveName.equals("double") || primitiveName.equals("float")
                || primitiveName.equals("short") || primitiveName.equals("byte")
                || primitiveName.equals("char") || primitiveName.equals("boolean")
                || primitiveName.equals("void")) {
            return Class.getPrimitiveClass(primitiveName);
        }
        return null;
    }

    // ---- the fields ----

    /**
     * The fields this type declares, of every visibility, in no particular order.
     *
     * <p>Declares: inherited fields are not here, which is the difference from
     * {@link #getFields()}. An array and a primitive declare none.
     */
    public Field[] getDeclaredFields() {
        return this.declaredFields0();
    }

    /**
     * The field this type declares under {@code name}.
     *
     * @param name the field name
     * @throws NoSuchFieldException if this type declares no such field
     */
    public Field getDeclaredField(String name) throws NoSuchFieldException {
        Field[] declared = this.declaredFields0();
        int i = 0;
        while (i < declared.length) {
            if (declared[i].getName().equals(name)) {
                return declared[i];
            }
            i = i + 1;
        }
        throw new NoSuchFieldException(this.getName() + "." + name);
    }

    /**
     * Every PUBLIC field reachable through this type, its own and its inherited ones.
     *
     * <p>Interfaces are walked too, and not as an afterthought: an interface's constants are
     * public fields, and a class that implements it exposes them.
     */
    public Field[] getFields() {
        StringBuilder seen = new StringBuilder();
        Field[] found = new Field[0];
        Class<?> walk = this;
        while (walk != null) {
            found = Class.appendPublicFields(found, walk, seen);
            Class<?>[] interfaces = walk.interfaces0();
            int i = 0;
            while (i < interfaces.length) {
                found = Class.appendPublicFields(found, interfaces[i], seen);
                i = i + 1;
            }
            walk = walk.getSuperclass();
        }
        return found;
    }

    // The public fields `owner` declares, appended to `found`, skipping any name already in
    // `seen`. Hiding is by NAME and not by signature: a field in a subclass hides one in a
    // superclass whatever its type, so the first one found along the walk is the one that wins.
    private static Field[] appendPublicFields(Field[] found, Class<?> owner, StringBuilder seen) {
        Field[] declared = owner.declaredFields0();
        Field[] out = found;
        int i = 0;
        while (i < declared.length) {
            Field candidate = declared[i];
            if (Modifier.isPublic(candidate.getModifiers())) {
                String marker = "|" + candidate.getName() + "|";
                if (seen.indexOf(marker) < 0) {
                    seen.append(marker);
                    Field[] bigger = new Field[out.length + 1];
                    System.arraycopy(out, 0, bigger, 0, out.length);
                    bigger[out.length] = candidate;
                    out = bigger;
                }
            }
            i = i + 1;
        }
        return out;
    }

    /**
     * The public field named {@code name}, declared here or inherited.
     *
     * @param name the field name
     * @throws NoSuchFieldException if no public field has that name
     */
    public Field getField(String name) throws NoSuchFieldException {
        Field[] fields = this.getFields();
        int i = 0;
        while (i < fields.length) {
            if (fields[i].getName().equals(name)) {
                return fields[i];
            }
            i = i + 1;
        }
        throw new NoSuchFieldException(this.getName() + "." + name);
    }

    // ---- the methods ----

    /**
     * The methods this type declares, of every visibility, in no particular order.
     *
     * <p>Constructors are not here -- they are not methods -- and neither is the static
     * initializer, which no caller could name anyway. What IS here and often surprises: the
     * bridge methods the compiler synthesized, which are real entries in the class file with the
     * erased signature a caller invokes.
     */
    public Method[] getDeclaredMethods() {
        return this.declaredMethods0();
    }

    /**
     * The method this type declares under {@code name} with exactly these parameter types.
     *
     * <p>Exactly: the parameter types are matched by identity, not by assignability, so asking
     * for {@code (Object)} does not find a method taking {@code (String)}. Overload resolution
     * is a compile-time thing and this is not it.
     *
     * @param name the method name
     * @param parameterTypes the erased parameter types, in order
     * @throws NoSuchMethodException if this type declares no such method
     */
    public Method getDeclaredMethod(String name, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Method[] declared = this.declaredMethods0();
        int i = 0;
        while (i < declared.length) {
            if (Class.matches(declared[i], name, parameterTypes)) {
                return declared[i];
            }
            i = i + 1;
        }
        throw new NoSuchMethodException(this.getName() + "." + name + "()");
    }

    /**
     * Every PUBLIC method reachable through this type, its own and its inherited ones.
     *
     * <p>Superclasses and interfaces both, and the interfaces transitively: a default method
     * declared in a superinterface is callable on this type, so it belongs in the list. A method
     * found earlier in the walk hides one with the same name and parameter types found later,
     * which is what makes an override appear once rather than twice.
     */
    public Method[] getMethods() {
        StringBuilder seen = new StringBuilder();
        Method[] found = new Method[0];
        Class<?> walk = this;
        while (walk != null) {
            found = Class.appendPublicMethods(found, walk, seen);
            found = Class.appendInterfaceMethods(found, walk, seen);
            walk = walk.getSuperclass();
        }
        return found;
    }

    // Every interface `owner` implements, and every interface those implement, contributing their
    // public methods. Written as a walk rather than a set because a diamond just re-offers a
    // method whose signature is already in `seen`, and `seen` rejects it.
    private static Method[] appendInterfaceMethods(Method[] found, Class<?> owner,
            StringBuilder seen) {
        Method[] out = found;
        Class<?>[] interfaces = owner.interfaces0();
        int i = 0;
        while (i < interfaces.length) {
            out = Class.appendPublicMethods(out, interfaces[i], seen);
            out = Class.appendInterfaceMethods(out, interfaces[i], seen);
            i = i + 1;
        }
        return out;
    }

    // The public methods `owner` declares, appended to `found`, skipping any signature already in
    // `seen`. The signature is name plus parameter types and NOT the return type: two methods
    // that differ only in return type are one override pair, and only the first one found is the
    // one a caller reaches.
    private static Method[] appendPublicMethods(Method[] found, Class<?> owner,
            StringBuilder seen) {
        Method[] declared = owner.declaredMethods0();
        Method[] out = found;
        int i = 0;
        while (i < declared.length) {
            Method candidate = declared[i];
            if (Modifier.isPublic(candidate.getModifiers())) {
                String marker = Class.signatureOf(candidate);
                if (seen.indexOf(marker) < 0) {
                    seen.append(marker);
                    Method[] bigger = new Method[out.length + 1];
                    System.arraycopy(out, 0, bigger, 0, out.length);
                    bigger[out.length] = candidate;
                    out = bigger;
                }
            }
            i = i + 1;
        }
        return out;
    }

    // A method's name and parameter types as one string, bracketed so that no signature can be a
    // substring of another -- `|f(int)|` cannot hide inside `|ff(int)|`.
    private static String signatureOf(Method method) {
        StringBuilder out = new StringBuilder("|");
        out.append(method.getName()).append('(');
        Class<?>[] parameters = method.getParameterTypes();
        int i = 0;
        while (i < parameters.length) {
            out.append(parameters[i].getName()).append(',');
            i = i + 1;
        }
        out.append(")|");
        return out.toString();
    }

    // Whether `method` is named `name` and takes exactly `parameterTypes`. A null array means no
    // parameters, which is what `getMethod("x")` passes.
    private static boolean matches(Method method, String name, Class<?>[] parameterTypes) {
        if (!method.getName().equals(name)) {
            return false;
        }
        Class<?>[] wanted = parameterTypes;
        if (wanted == null) {
            wanted = new Class<?>[0];
        }
        Class<?>[] actual = method.getParameterTypes();
        if (actual.length != wanted.length) {
            return false;
        }
        int i = 0;
        while (i < actual.length) {
            if (actual[i] != wanted[i]) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /**
     * The public method named {@code name} with exactly these parameter types, declared here or
     * inherited.
     *
     * @param name the method name
     * @param parameterTypes the erased parameter types, in order
     * @throws NoSuchMethodException if no public method matches
     */
    public Method getMethod(String name, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Method[] methods = this.getMethods();
        int i = 0;
        while (i < methods.length) {
            if (Class.matches(methods[i], name, parameterTypes)) {
                return methods[i];
            }
            i = i + 1;
        }
        throw new NoSuchMethodException(this.getName() + "." + name + "()");
    }

    // ---- the constructors ----
    //
    // Un constructor es un metodo llamado `<init>`: en el archivo de clase vive en la misma tabla
    // que los demas. La reflexion lo separa porque se INVOCA distinto -- aloca antes de correr --
    // y esa diferencia es la unica razon por la que `Constructor` no es un `Method`.

    /**
     * The constructors this type declares, of every visibility.
     *
     * <p>An interface, an array and a primitive declare none, and neither does an enum's
     * anonymous constant subclass.
     */
    public Constructor<?>[] getDeclaredConstructors() {
        return this.declaredConstructors0();
    }

    /**
     * The constructor this type declares with exactly these parameter types.
     *
     * @param parameterTypes the erased parameter types, in order
     * @throws NoSuchMethodException if this type declares no such constructor
     */
    public Constructor<T> getDeclaredConstructor(Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Constructor<?>[] declared = this.declaredConstructors0();
        int i = 0;
        while (i < declared.length) {
            if (Class.matchesConstructor(declared[i], parameterTypes)) {
                return (Constructor<T>) declared[i];
            }
            i = i + 1;
        }
        throw new NoSuchMethodException(this.getName() + ".<init>()");
    }

    /** The PUBLIC constructors this type declares. */
    public Constructor<?>[] getConstructors() {
        Constructor<?>[] declared = this.declaredConstructors0();
        int found = 0;
        int i = 0;
        while (i < declared.length) {
            if (Modifier.isPublic(declared[i].getModifiers())) {
                found = found + 1;
            }
            i = i + 1;
        }
        Constructor<?>[] out = new Constructor<?>[found];
        int at = 0;
        i = 0;
        while (i < declared.length) {
            if (Modifier.isPublic(declared[i].getModifiers())) {
                out[at] = declared[i];
                at = at + 1;
            }
            i = i + 1;
        }
        return out;
    }

    /**
     * The public constructor with exactly these parameter types.
     *
     * <p>No inheritance here, unlike {@link #getMethod(String, Class...)}: constructors are not
     * inherited, so "public and declared here" is the whole search.
     *
     * @param parameterTypes the erased parameter types, in order
     * @throws NoSuchMethodException if no public constructor matches
     */
    public Constructor<T> getConstructor(Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Constructor<?>[] declared = this.declaredConstructors0();
        int i = 0;
        while (i < declared.length) {
            if (Modifier.isPublic(declared[i].getModifiers())
                    && Class.matchesConstructor(declared[i], parameterTypes)) {
                return (Constructor<T>) declared[i];
            }
            i = i + 1;
        }
        throw new NoSuchMethodException(this.getName() + ".<init>()");
    }

    // Whether `candidate` takes exactly `parameterTypes`. Null means none, which is what
    // `getConstructor()` with no arguments passes.
    private static boolean matchesConstructor(Constructor<?> candidate,
            Class<?>[] parameterTypes) {
        Class<?>[] wanted = parameterTypes;
        if (wanted == null) {
            wanted = new Class<?>[0];
        }
        Class<?>[] actual = candidate.getParameterTypes();
        if (actual.length != wanted.length) {
            return false;
        }
        int i = 0;
        while (i < actual.length) {
            if (actual[i] != wanted[i]) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /**
     * A new instance, made with the no-argument constructor.
     *
     * @throws InstantiationException if there is no accessible no-argument constructor, or the
     *         type cannot be instantiated at all
     * @throws IllegalAccessException never here, since no access check is performed
     * @deprecated the JDK deprecates it, and the reason is the exception handling: a checked
     *         exception thrown by the constructor comes out of this method WITHOUT being
     *         declared, which defeats the compiler's whole checked-exception analysis.
     *         {@code getDeclaredConstructor().newInstance()} wraps it in
     *         {@link java.lang.reflect.InvocationTargetException} instead, which is what a caller
     *         can actually handle.
     */
    @Deprecated(since = "9")
    public T newInstance() throws InstantiationException, IllegalAccessException {
        Constructor<T> ctor = null;
        try {
            ctor = this.getDeclaredConstructor();
        } catch (NoSuchMethodException ex) {
            throw new InstantiationException(this.getName() + " has no no-argument constructor");
        }
        try {
            return ctor.newInstance();
        } catch (java.lang.reflect.InvocationTargetException ex) {
            // Y aca esta la misfeature, reproducida a proposito: la excepcion del constructor
            // sale SIN envolver y sin estar declarada. `sneak` es lo que lo hace posible -- el
            // borrado de tipos deja lanzar una chequeada donde el compilador espera otra cosa --
            // y es exactamente lo que el JDK hace con `Unsafe.throwException`.
            Class.sneak(ex.getTargetException());
            return null; // inalcanzable
        }
    }

    // Lanza `t` tal cual, sea chequeada o no. Legal por borrado: `E` se infiere como
    // RuntimeException en el sitio de llamada, el compilador deja de exigir el `throws`, y en
    // tiempo de ejecucion no hay chequeo ninguno porque el cast a `E` se borro.
    private static <E extends Throwable> void sneak(Throwable t) throws E {
        throw (E) t;
    }

    // ---- the generic model ----
    //
    // Erased, and that is the answer rather than a placeholder: a type with no `Signature`
    // attribute has no generic information, and the VM does not read one. What a `Signature`
    // would add is the difference between `List` and `List<String>`.

    /** The type parameters this type declares: none, since no `Signature` is read. */
    public TypeVariable<Class<T>>[] getTypeParameters() {
        return new TypeVariable[0];
    }

    /** The superclass in the generic model, erased. */
    public Type getGenericSuperclass() {
        return this.getSuperclass();
    }

    /** The directly implemented interfaces in the generic model, erased. */
    public Type[] getGenericInterfaces() {
        Class<?>[] erased = this.interfaces0();
        Type[] out = new Type[erased.length];
        int i = 0;
        while (i < erased.length) {
            out[i] = erased[i];
            i = i + 1;
        }
        return out;
    }

    /**
     * The annotated use of this class's superclass, or {@code null} when there is none (this class is
     * {@code Object}, an interface, a primitive, {@code void} or an array). No type annotations are
     * modelled, so it wraps the plain superclass.
     */
    public AnnotatedType getAnnotatedSuperclass() {
        Type superclass = this.getGenericSuperclass();
        if (superclass == null) {
            return null;
        }
        return new AnnotatedTypeImpl(superclass);
    }

    /** The annotated uses of this class's directly-implemented interfaces. */
    public AnnotatedType[] getAnnotatedInterfaces() {
        Type[] ifaces = this.getGenericInterfaces();
        AnnotatedType[] out = new AnnotatedType[ifaces.length];
        int i = 0;
        while (i < ifaces.length) {
            out[i] = new AnnotatedTypeImpl(ifaces[i]);
            i = i + 1;
        }
        return out;
    }

    /**
     * This class's protection domain. KajiJDK does not enforce access control, so every class shares
     * one domain keyed by its (bootstrap) class loader.
     */
    public java.security.ProtectionDomain getProtectionDomain() {
        // Cuatro argumentos y no dos: el de dos crea un dominio de permisos **estaticos**, y decir
        // que este dominio tiene fijados sus permisos para siempre seria afirmar algo que nadie
        // comprobo. Con el de cuatro queda dinamico, que es lo que hace el JDK, y sin codesource ni
        // permisos —los dos en null— porque es lo unico que se sabe: quien lo cargo.
        return new ProtectionDomain(null, null, this.getClassLoader(), null);
    }

    // ---- three that answer without a table ----

    /**
     * The constants of this enum type, in declaration order, or {@code null} if it is not one.
     *
     * <p>Read by CALLING the type's own {@code values()} method, which is the only place the list
     * exists: the compiler synthesises it, and the constants themselves are created by the
     * class's static initialiser. Note that an enum constant with a body compiles to an anonymous
     * subclass, and that subclass answers null -- it is not an enum type, its superclass is.
     */
    public T[] getEnumConstants() {
        if (!this.isEnum()) {
            return null;
        }
        try {
            Method values = this.getDeclaredMethod("values");
            values.setAccessible(true);
            Object[] all = (Object[]) values.invoke(null);
            return (T[]) all.clone();
        } catch (NoSuchMethodException ex) {
            return null;
        } catch (IllegalAccessException ex) {
            return null;
        } catch (java.lang.reflect.InvocationTargetException ex) {
            return null;
        }
    }

    /**
     * The signers of this class: always {@code null}.
     *
     * <p>Null is what an unsigned class reports, and nothing in KajiJDK signs one -- so this is
     * the specified answer rather than a stub.
     */
    public Object[] getSigners() {
        return null;
    }

    /**
     * Whether an annotation of type {@code annotationClass} is directly present on this type.
     *
     * <p>Only RUNTIME-retention annotations are in the class file at all, which is what makes
     * this answerable without an annotation object: the question is whether the type's name
     * appears in the {@code RuntimeVisibleAnnotations} attribute, and that is a string compare.
     * Getting the annotation ITSELF would need a generated proxy class per {@code @interface},
     * which is why {@code getAnnotation} is not here and this is.
     *
     * @param annotationClass the annotation type to look for
     */
    public boolean isAnnotationPresent(Class<? extends java.lang.annotation.Annotation>
            annotationClass) {
        return this.annotationPresent0(annotationClass);
    }

    // ---- reading annotations as objects (JSR 175 reflection) ----
    //
    // `isAnnotationPresent` above answers presence; these hand the annotation back as an OBJECT of
    // the @interface type. The VM does the materialisation (`declaredAnnotations0`, one spun class
    // per annotation); the filtering by type is plain Java here. There is no @Inherited walk up the
    // superclass chain, so the "declared" and the plain forms return the same set -- matching what
    // `isAnnotationPresent` already reports. equals/hashCode/toString on the returned objects are
    // Object's (identity), not the value-based equality the spec asks for: a documented limit of
    // this subset. Element values that are themselves nested annotations come back as null.

    /** This class's directly-present annotations, as objects; empty if none. */
    public java.lang.annotation.Annotation[] getAnnotations() {
        return this.declaredAnnotations0();
    }

    /** The same set as {@link #getAnnotations()} — no @Inherited walk distinguishes them here. */
    public java.lang.annotation.Annotation[] getDeclaredAnnotations() {
        return this.declaredAnnotations0();
    }

    /**
     * This class's annotation of type {@code annotationClass}, or {@code null} if not present.
     *
     * @throws NullPointerException if {@code annotationClass} is null
     */
    public <A extends java.lang.annotation.Annotation> A getAnnotation(Class<A> annotationClass) {
        if (annotationClass == null) {
            throw new NullPointerException();
        }
        java.lang.annotation.Annotation[] all = this.declaredAnnotations0();
        int i = 0;
        while (i < all.length) {
            if (all[i].annotationType() == annotationClass) {
                return (A) all[i];
            }
            i = i + 1;
        }
        return null;
    }

    /** Directly-present only; the same as {@link #getAnnotation} here (no @Inherited walk). */
    public <A extends java.lang.annotation.Annotation> A getDeclaredAnnotation(Class<A> annotationClass) {
        return this.getAnnotation(annotationClass);
    }

    /**
     * This class's annotations of type {@code annotationClass}. For a non-{@code @Repeatable}
     * annotation this is the one present (or an empty array); container unwrapping for repeatable
     * annotations is not modelled in this subset.
     */
    public <A extends java.lang.annotation.Annotation> A[] getAnnotationsByType(Class<A> annotationClass) {
        A single = this.getAnnotation(annotationClass);
        int n = single == null ? 0 : 1;
        A[] array = (A[]) Array.newInstance(annotationClass, n);
        if (single != null) {
            array[0] = single;
        }
        return array;
    }

    /** Directly-present only; the same as {@link #getAnnotationsByType} here (no @Inherited walk). */
    public <A extends java.lang.annotation.Annotation> A[] getDeclaredAnnotationsByType(Class<A> annotationClass) {
        return this.getAnnotationsByType(annotationClass);
    }

    // ---- where this type was declared ----
    //
    // Un archivo de clase no sabe que estaba adentro de otro: `Outer` y `Outer$Inner` son dos
    // archivos sueltos con un `$` en el nombre, y el `$` es un caracter legal en un identificador.
    // La estructura del LENGUAJE la reconstruye el atributo `InnerClasses`, y por eso todo lo que
    // sigue lo consulta en vez de mirar el nombre.

    /**
     * Whether this type is declared inside a method or a constructor.
     *
     * <p>Local and anonymous classes both are; a member class is not, even though it is nested.
     */
    private boolean isLocalOrAnonymous() {
        return this.hasEnclosingMethod0();
    }

    /** The type that declares this one, or {@code null} if it is not a member of another. */
    public Class<?> getDeclaringClass() {
        if (this.isLocalOrAnonymous()) {
            return null;
        }
        return this.declaringClass0();
    }

    /**
     * The type this one is declared inside, or {@code null}.
     *
     * <p>Wider than {@link #getDeclaringClass()}: a local class is declared inside a METHOD, so
     * it has an enclosing class and no declaring one. That is the whole difference between the
     * two methods, and it is why both exist.
     */
    public Class<?> getEnclosingClass() {
        return this.enclosingClass0();
    }

    /** Whether this type is a member of another -- nested, but not inside a method. */
    public boolean isMemberClass() {
        return !this.isLocalOrAnonymous() && this.declaringClass0() != null;
    }

    /** Whether this type is anonymous -- declared and instantiated in one expression. */
    public boolean isAnonymousClass() {
        return !this.isArray() && this.isLocalOrAnonymous() && this.innerName0() == null;
    }

    /** Whether this type is a named class declared inside a method. */
    public boolean isLocalClass() {
        return this.isLocalOrAnonymous() && this.innerName0() != null;
    }

    /** The types this one declares as members, of every visibility. */
    public Class<?>[] getDeclaredClasses() {
        return this.declaredClasses0();
    }

    /**
     * Every PUBLIC member type reachable through this one, its own and its inherited ones.
     */
    public Class<?>[] getClasses() {
        Class<?>[] found = new Class<?>[0];
        Class<?> walk = this;
        while (walk != null) {
            Class<?>[] declared = walk.declaredClasses0();
            int i = 0;
            while (i < declared.length) {
                if (Modifier.isPublic(declared[i].getModifiers())) {
                    Class<?>[] bigger = new Class<?>[found.length + 1];
                    System.arraycopy(found, 0, bigger, 0, found.length);
                    bigger[found.length] = declared[i];
                    found = bigger;
                }
                i = i + 1;
            }
            walk = walk.getSuperclass();
        }
        return found;
    }

    /**
     * The method this type was declared inside, or {@code null}.
     *
     * <p>Null for three different reasons worth telling apart: this is not a local or anonymous
     * class, or it is one but was declared in a field initialiser rather than in a method, or it
     * was declared in a CONSTRUCTOR -- which {@link #getEnclosingConstructor()} answers instead.
     */
    public Method getEnclosingMethod() {
        String[] info = this.enclosingMethodInfo0();
        if (info == null || info[1].equals("<init>")) {
            return null;
        }
        Class<?> owner = this.enclosingClass0();
        if (owner == null) {
            return null;
        }
        Method[] declared = owner.declaredMethods0();
        int i = 0;
        while (i < declared.length) {
            if (declared[i].getName().equals(info[1])
                    && Class.descriptorOf(declared[i].getParameterTypes(),
                            declared[i].getReturnType()).equals(info[2])) {
                return declared[i];
            }
            i = i + 1;
        }
        return null;
    }

    /** The constructor this type was declared inside, or {@code null}. */
    public Constructor<?> getEnclosingConstructor() {
        String[] info = this.enclosingMethodInfo0();
        if (info == null || !info[1].equals("<init>")) {
            return null;
        }
        Class<?> owner = this.enclosingClass0();
        if (owner == null) {
            return null;
        }
        Constructor<?>[] declared = owner.declaredConstructors0();
        int i = 0;
        while (i < declared.length) {
            if (Class.descriptorOf(declared[i].getParameterTypes(), void.class).equals(info[2])) {
                return declared[i];
            }
            i = i + 1;
        }
        return null;
    }

    // El descriptor de un metodo, rearmado desde los mirrors. Es lo que permite comparar contra
    // el que el atributo `EnclosingMethod` guarda, que es texto.
    private static String descriptorOf(Class<?>[] parameters, Class<?> returns) {
        StringBuilder out = new StringBuilder("(");
        int i = 0;
        while (i < parameters.length) {
            out.append(parameters[i].descriptorString());
            i = i + 1;
        }
        out.append(')').append(returns.descriptorString());
        return out.toString();
    }

    // ---- the nest ----
    //
    // Un nido son las clases que comparten acceso `private` entre si: una externa y todas sus
    // anidadas. Existe desde Java 11 y reemplazo a los metodos puente sinteticos que el compilador
    // generaba antes para el mismo fin, que es por que un `.class` moderno tiene menos basura.

    /** The nest this type belongs to, named by its host. */
    public Class<?> getNestHost() {
        return this.nestHost0();
    }

    /** Every type in this type's nest, the host included. */
    public Class<?>[] getNestMembers() {
        return this.getNestHost().nestMembers0();
    }

    /**
     * Whether this type and {@code c} share a nest, and therefore each other's private members.
     *
     * @param c the other type
     */
    public boolean isNestmateOf(Class<?> c) {
        return this == c || this.nestHost0() == c.nestHost0();
    }

    // ---- sealing ----

    /** Whether this type restricts who may extend or implement it. */
    public boolean isSealed() {
        if (this.isArray() || this.isPrimitive()) {
            return false;
        }
        return this.permittedSubclasses0() != null;
    }

    /**
     * The types allowed to extend or implement this one, or {@code null} if it is not sealed.
     *
     * <p>Null and an empty array are different answers: null means "anyone may", and an empty
     * array means "nobody may", which is a legal and very restrictive thing for a sealed type to
     * say.
     */
    public Class<?>[] getPermittedSubclasses() {
        return this.permittedSubclasses0();
    }

    // ---- records ----

    /**
     * The components of this record type, in declaration order, or {@code null}.
     *
     * <p>The declaration order is the part that matters: it is the order of the canonical
     * constructor's parameters, so this is what lets a serialiser rebuild a record without
     * knowing its source.
     */
    public java.lang.reflect.RecordComponent[] getRecordComponents() {
        return this.recordComponents0();
    }

    // ---- the two that answer the same thing whatever is asked ----

    /**
     * The loader that defined this type: always {@code null}, the bootstrap loader.
     *
     * <p>Not a placeholder. KajiJDK loads every class through one loader, and {@code null} is
     * precisely what the specification says that loader reports.
     */
    public ClassLoader getClassLoader() {
        return null;
    }

    /**
     * Whether assertions would be enabled for this type.
     *
     * <p>Read at class initialisation and stored in a synthetic field the desugared {@code assert}
     * then guards on, which is why changing the setting afterwards does not affect a type that is
     * already initialised -- and why this method exists at all instead of the compiler asking
     * directly.
     */
    public boolean desiredAssertionStatus() {
        return ClassLoader.getSystemClassLoader().assertionStatusOf(this.getName());
    }

    // ---- classpath resources ----
    //
    // El JDK busca el recurso por el classpath a traves del ClassLoader. KajiJDK no tiene carga de
    // recursos (no hay un classpath de datos que recorrer), asi que la busqueda no encuentra nada y
    // devuelve `null` -- que es exactamente el resultado que el contrato define para "no hallado".
    // No es un placeholder: para toda entrada, el resultado honesto aqui es "no hay tal recurso".

    /**
     * Find the resource named {@code name} on the class path, as a {@link java.net.URL} — always
     * {@code null} here: KajiJDK carries no class-path resources, so nothing is ever found.
     */
    public java.net.URL getResource(String name) {
        return null;
    }

    /**
     * Open the resource named {@code name} for reading — always {@code null} here, for the same
     * reason as {@link #getResource(String)}: there are no class-path resources to open.
     */
    public java.io.InputStream getResourceAsStream(String name) {
        return null;
    }
}
