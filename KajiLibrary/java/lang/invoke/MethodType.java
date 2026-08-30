package java.lang.invoke;

import java.lang.constant.Constable;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.TypeDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// The type of a method handle: a return type plus parameter types, all of them `Class` objects.
// It is the LOADED counterpart of `java.lang.constant.MethodTypeDesc` — same information, except
// a `MethodType` holds resolved classes while a `MethodTypeDesc` holds names. `describeConstable`
// below is the bridge from one to the other, and it is the reason this class can exist here at
// all: the nominal half was already written.
//
// Immutable and interned in the JDK, which matters because method handle linkage compares types
// by identity. Ours is immutable but not interned: `equals` compares structurally, and nothing in
// KajiLibrary can link a handle anyway.
//
// The descriptor of a `Class` is derived from `getName()` alone, which turns out to be enough —
// the JDK's `Class.getName()` already returns `int` for a primitive and `[Ljava.lang.String;` for
// an array, so the three cases fall out of the first character. That avoids needing
// `isPrimitive`/`isArray`/`getComponentType`, none of which our `Class` has yet.
//
// `MethodType implements TypeDescriptor.OfMethod` — the loaded counterpart of the nominal
// `MethodTypeDesc`, which implements the same interface. The `OfField`-returning views
// (`returnType`, `parameterType`, `parameterArray`, `dropParameterTypes`) are covariant narrowings
// to `Class`/`MethodType`, so the compiler synthesises their bridges; the three operations whose
// parameter is `OfField` rather than `Class` are spelled out below as delegations (a `Class` is the
// only `OfField` this library hands to a method type). This became possible once `java.lang.Class`
// began implementing `TypeDescriptor.OfField` and `TypeDescriptor` was modelled raw — both true now.
//
// `Constable` va por import y nombre simple: una referencia CALIFICADA a un tipo del classpath no
// resuelve (finding #106a).
public final class MethodType implements Constable, TypeDescriptor.OfMethod {

    private final Class<?> rtype;
    private final Class<?>[] ptypes;

    private MethodType(Class<?> rtype, Class<?>[] ptypes) {
        this.rtype = rtype;
        this.ptypes = ptypes;
    }

    public static MethodType methodType(Class<?> rtype, Class<?>[] ptypes) {
        return new MethodType(rtype, copy(ptypes));
    }

    public static MethodType methodType(Class<?> rtype, List<Class<?>> ptypes) {
        Class<?>[] arr = new Class<?>[ptypes.size()];
        int i = 0;
        while (i < arr.length) {
            arr[i] = ptypes.get(i);
            i = i + 1;
        }
        return new MethodType(rtype, arr);
    }

    public static MethodType methodType(Class<?> rtype) {
        return new MethodType(rtype, new Class<?>[0]);
    }

    public static MethodType methodType(Class<?> rtype, Class<?> ptype0) {
        Class<?>[] arr = new Class<?>[1];
        arr[0] = ptype0;
        return new MethodType(rtype, arr);
    }

    public static MethodType methodType(Class<?> rtype, Class<?> ptype0, Class<?>... ptypes) {
        Class<?>[] arr = new Class<?>[1 + ptypes.length];
        arr[0] = ptype0;
        int i = 0;
        while (i < ptypes.length) {
            arr[1 + i] = ptypes[i];
            i = i + 1;
        }
        return new MethodType(rtype, arr);
    }

    // Takes the parameters from another type and a fresh return type — the common shape when
    // adapting a handle.
    public static MethodType methodType(Class<?> rtype, MethodType ptypes) {
        return new MethodType(rtype, copy(ptypes.ptypes));
    }

    // All-`Object` of the given arity: what an erased, unadapted handle looks like.
    public static MethodType genericMethodType(int objectArgCount) {
        return genericMethodType(objectArgCount, false);
    }

    public static MethodType genericMethodType(int objectArgCount, boolean finalArray) {
        int count = objectArgCount;
        if (finalArray) {
            count = count + 1;
        }
        Class<?>[] arr = new Class<?>[count];
        int i = 0;
        while (i < objectArgCount) {
            arr[i] = objectClass();
            i = i + 1;
        }
        if (finalArray) {
            arr[count - 1] = arrayOfObjectClass();
        }
        return new MethodType(objectClass(), arr);
    }

    /**
     * Parses a method descriptor (e.g. {@code (ILjava/lang/String;)V}) into a method type,
     * resolving each named class through {@code loader} (or the system loader when it is null).
     *
     * @throws IllegalArgumentException if the descriptor is malformed or a named class is absent
     */
    public static MethodType fromMethodDescriptorString(String descriptor, ClassLoader loader) {
        int close = descriptor.lastIndexOf(')');
        if (descriptor.length() < 2 || descriptor.charAt(0) != '(' || close < 0) {
            throw new IllegalArgumentException("not a method descriptor: " + descriptor);
        }
        String params = descriptor.substring(1, close);
        Class<?> rtype = classForDescriptor(descriptor.substring(close + 1), loader);
        List<Class<?>> ptypes = new ArrayList<Class<?>>();
        int i = 0;
        while (i < params.length()) {
            int start = i;
            while (params.charAt(i) == '[') {
                i = i + 1;
            }
            if (params.charAt(i) == 'L') {
                while (params.charAt(i) != ';') {
                    i = i + 1;
                }
            }
            i = i + 1;
            ptypes.add(classForDescriptor(params.substring(start, i), loader));
        }
        Class<?>[] arr = new Class<?>[ptypes.size()];
        int j = 0;
        while (j < arr.length) {
            arr[j] = ptypes.get(j);
            j = j + 1;
        }
        return new MethodType(rtype, arr);
    }

    // Resolves one field descriptor to its `Class`. Primitives come from the boxes' `TYPE` mirror
    // (the only source-level handle on a primitive `Class` here); references and arrays load by
    // name through the loader.
    private static Class<?> classForDescriptor(String desc, ClassLoader loader) {
        char c = desc.charAt(0);
        if (c == 'L') {
            return classByName(swap(desc.substring(1, desc.length() - 1), '/', '.'), loader);
        }
        if (c == '[') {
            return classByName(swap(desc, '/', '.'), loader);
        }
        return primitiveClass(c);
    }

    private static Class<?> classByName(String binary, ClassLoader loader) {
        try {
            if (loader == null) {
                return Class.forName(binary);
            }
            return loader.loadClass(binary);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("cannot resolve " + binary);
        }
    }

    private static Class<?> primitiveClass(char tag) {
        if (tag == 'I') {
            return Integer.TYPE;
        } else if (tag == 'J') {
            return Long.TYPE;
        } else if (tag == 'D') {
            return Double.TYPE;
        } else if (tag == 'F') {
            return Float.TYPE;
        } else if (tag == 'S') {
            return Short.TYPE;
        } else if (tag == 'B') {
            return Byte.TYPE;
        } else if (tag == 'C') {
            return Character.TYPE;
        } else if (tag == 'Z') {
            return Boolean.TYPE;
        } else if (tag == 'V') {
            return Void.TYPE;
        }
        throw new IllegalArgumentException("bad descriptor char: " + tag);
    }

    public MethodType changeParameterType(int num, Class<?> nptype) {
        Class<?>[] arr = copy(ptypes);
        arr[num] = nptype;
        return new MethodType(rtype, arr);
    }

    public MethodType insertParameterTypes(int num, Class<?>... ptypesToInsert) {
        Class<?>[] arr = new Class<?>[ptypes.length + ptypesToInsert.length];
        int to = 0;
        int i = 0;
        while (i < num) {
            arr[to] = ptypes[i];
            to = to + 1;
            i = i + 1;
        }
        int k = 0;
        while (k < ptypesToInsert.length) {
            arr[to] = ptypesToInsert[k];
            to = to + 1;
            k = k + 1;
        }
        while (i < ptypes.length) {
            arr[to] = ptypes[i];
            to = to + 1;
            i = i + 1;
        }
        return new MethodType(rtype, arr);
    }

    public MethodType appendParameterTypes(Class<?>... ptypesToInsert) {
        return insertParameterTypes(ptypes.length, ptypesToInsert);
    }

    public MethodType insertParameterTypes(int num, List<Class<?>> ptypesToInsert) {
        Class<?>[] arr = new Class<?>[ptypesToInsert.size()];
        int i = 0;
        while (i < arr.length) {
            arr[i] = ptypesToInsert.get(i);
            i = i + 1;
        }
        return insertParameterTypes(num, arr);
    }

    public MethodType appendParameterTypes(List<Class<?>> ptypesToInsert) {
        return insertParameterTypes(ptypes.length, ptypesToInsert);
    }

    public MethodType dropParameterTypes(int start, int end) {
        Class<?>[] arr = new Class<?>[ptypes.length - (end - start)];
        int to = 0;
        int i = 0;
        while (i < ptypes.length) {
            if (i < start || i >= end) {
                arr[to] = ptypes[i];
                to = to + 1;
            }
            i = i + 1;
        }
        return new MethodType(rtype, arr);
    }

    public MethodType changeReturnType(Class<?> nrtype) {
        return new MethodType(nrtype, copy(ptypes));
    }

    // ---- TypeDescriptor.OfMethod operations whose parameter is the interface `OfField` rather
    // than `Class`. The covariant-return views (returnType, parameterType, parameterArray,
    // dropParameterTypes) are bridged by the compiler; these three delegate, since the only
    // `OfField` a method type ever receives here is a `Class`.

    public TypeDescriptor.OfMethod changeReturnType(TypeDescriptor.OfField newReturn) {
        return changeReturnType((Class<?>) newReturn);
    }

    public TypeDescriptor.OfMethod changeParameterType(int num, TypeDescriptor.OfField newType) {
        return changeParameterType(num, (Class<?>) newType);
    }

    public TypeDescriptor.OfMethod insertParameterTypes(int num, TypeDescriptor.OfField[] typesToInsert) {
        Class<?>[] arr = new Class<?>[typesToInsert.length];
        int i = 0;
        while (i < typesToInsert.length) {
            arr[i] = (Class<?>) typesToInsert[i];
            i = i + 1;
        }
        return insertParameterTypes(num, arr);
    }

    public boolean hasPrimitives() {
        boolean any = isPrimitiveName(rtype.getName());
        int i = 0;
        while (i < ptypes.length) {
            if (isPrimitiveName(ptypes[i].getName())) {
                any = true;
            }
            i = i + 1;
        }
        return any;
    }

    public boolean hasWrappers() {
        boolean any = isWrapperName(rtype.getName());
        int i = 0;
        while (i < ptypes.length) {
            if (isWrapperName(ptypes[i].getName())) {
                any = true;
            }
            i = i + 1;
        }
        return any;
    }

    // Every reference becomes `Object`; primitives stay. This is what the JVM actually links
    // against once generics are gone.
    public MethodType erase() {
        Class<?>[] arr = new Class<?>[ptypes.length];
        int i = 0;
        while (i < ptypes.length) {
            arr[i] = eraseOne(ptypes[i]);
            i = i + 1;
        }
        return new MethodType(eraseOne(rtype), arr);
    }

    // Everything becomes `Object`, primitives included — the shape of a fully boxed invocation.
    public MethodType generic() {
        return genericMethodType(ptypes.length);
    }

    private static Class<?> eraseOne(Class<?> type) {
        Class<?> result = type;
        if (!isPrimitiveName(type.getName())) {
            result = objectClass();
        }
        return result;
    }

    // Primitive to its box, and back. The pair is what an `asType` adaptation walks when it has
    // to cross the primitive/reference divide.
    public MethodType wrap() {
        return convertAll(true);
    }

    public MethodType unwrap() {
        return convertAll(false);
    }

    private MethodType convertAll(boolean toWrapper) {
        Class<?>[] arr = new Class<?>[ptypes.length];
        int i = 0;
        while (i < ptypes.length) {
            arr[i] = convertOne(ptypes[i], toWrapper);
            i = i + 1;
        }
        return new MethodType(convertOne(rtype, toWrapper), arr);
    }

    private static Class<?> convertOne(Class<?> type, boolean toWrapper) {
        Class<?> result = type;
        if (toWrapper) {
            result = wrapperFor(type.getName(), type);
        }
        // The `unwrap` direction stays the identity, and the reason is worth stating precisely
        // because it is NOT the same reason as before. It is not that a name cannot be turned
        // back into a `Class` — every wrapper has a class literal, so `wrap` above works. It is
        // that a PRIMITIVE `Class` object has no source spelling our compiler accepts: `int.class`
        // is rejected at the parser ("se esperaba una expresion, se encontro Int"), and there is
        // no other expression whose value is the mirror for `int`. `Integer.TYPE` would be the
        // classic escape hatch, and it is declared as `Integer.TYPE = int.class` — the same
        // literal, one file away. So `unwrap` returns what it was given rather than lying about
        // it, and the two directions are asymmetric until that literal parses.
        return result;
    }

    // Primitive keyword to its box. Kept as a name switch rather than a map because a `Class`
    // cannot be a key here — there is no primitive `Class` to key on, which is the whole problem.
    private static Class<?> wrapperFor(String name, Class<?> fallback) {
        Class<?> box = fallback;
        if (name.equals("int")) {
            box = Integer.class;
        } else if (name.equals("long")) {
            box = Long.class;
        } else if (name.equals("double")) {
            box = Double.class;
        } else if (name.equals("float")) {
            box = Float.class;
        } else if (name.equals("short")) {
            box = Short.class;
        } else if (name.equals("byte")) {
            box = Byte.class;
        } else if (name.equals("char")) {
            box = Character.class;
        } else if (name.equals("boolean")) {
            box = Boolean.class;
        } else if (name.equals("void")) {
            box = Void.class;
        }
        return box;
    }

    public Class<?> parameterType(int num) {
        return ptypes[num];
    }

    public int parameterCount() {
        return ptypes.length;
    }

    public Class<?> returnType() {
        return rtype;
    }

    public List<Class<?>> parameterList() {
        List<Class<?>> list = new ArrayList<Class<?>>();
        int i = 0;
        while (i < ptypes.length) {
            list.add(ptypes[i]);
            i = i + 1;
        }
        return list;
    }

    public Class<?> lastParameterType() {
        Class<?> last = objectClass();
        if (ptypes.length > 0) {
            last = ptypes[ptypes.length - 1];
        }
        return last;
    }

    public Class<?>[] parameterArray() {
        return copy(ptypes);
    }

    public boolean equals(Object other) {
        boolean same = false;
        if (other instanceof MethodType) {
            MethodType that = (MethodType) other;
            same = toMethodDescriptorString().equals(that.toMethodDescriptorString());
        }
        return same;
    }

    public int hashCode() {
        return toMethodDescriptorString().hashCode();
    }

    // `(int,String)void` — the readable form, unlike `descriptorString`.
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        int i = 0;
        while (i < ptypes.length) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(simpleName(ptypes[i].getName()));
            i = i + 1;
        }
        sb.append(")");
        sb.append(simpleName(rtype.getName()));
        return sb.toString();
    }

    public String toMethodDescriptorString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        int i = 0;
        while (i < ptypes.length) {
            sb.append(descriptorOf(ptypes[i]));
            i = i + 1;
        }
        sb.append(")");
        sb.append(descriptorOf(rtype));
        return sb.toString();
    }

    public String descriptorString() {
        return toMethodDescriptorString();
    }

    // The bridge to the nominal world: a `MethodType` (loaded classes) described as a
    // `MethodTypeDesc` (names). Always present, because a method type is always describable.
    // Retorno `Optional` CRUDO y no `Optional<MethodTypeDesc>`: el chequeo de override contra
    // `Constable.describeConstable()` —que vive en el classpath y devuelve
    // `Optional<? extends ConstantDesc>`— falla con "Optional no es un subtipo de Optional",
    // el mismo sintoma de fuente-vs-classpath del #123. La ERASURE es identica
    // (`()Ljava/util/Optional;`), asi que el descriptor emitido es el correcto y el gate lo matchea.
    public Optional describeConstable() {
        // Con el type witness explicito: la inferencia del argumento erasa a `Optional<Object>` y
        // el retorno queda incompatible (familia del #100). `Optional.<T>of(...)` fija el parametro.
        MethodTypeDesc desc = MethodTypeDesc.ofDescriptor(toMethodDescriptorString());
        return Optional.<MethodTypeDesc>of(desc);
    }

    // ---- nombres y descriptores ----

    // The three cases fall out of `getName()`: an array already comes back in descriptor form
    // with dots, a primitive comes back as its keyword, and anything else is a binary name.
    static String descriptorOf(Class<?> type) {
        String name = type.getName();
        String desc;
        if (name.charAt(0) == '[') {
            desc = swap(name, '.', '/');
        } else if (isPrimitiveName(name)) {
            desc = primitiveTag(name);
        } else {
            desc = "L" + swap(name, '.', '/') + ";";
        }
        return desc;
    }

    static boolean isPrimitiveName(String name) {
        return name.equals("int") || name.equals("long") || name.equals("double")
                || name.equals("float") || name.equals("short") || name.equals("byte")
                || name.equals("char") || name.equals("boolean") || name.equals("void");
    }

    static boolean isWrapperName(String name) {
        return name.equals("java.lang.Integer") || name.equals("java.lang.Long")
                || name.equals("java.lang.Double") || name.equals("java.lang.Float")
                || name.equals("java.lang.Short") || name.equals("java.lang.Byte")
                || name.equals("java.lang.Character") || name.equals("java.lang.Boolean")
                || name.equals("java.lang.Void");
    }

    private static String primitiveTag(String name) {
        String tag = "V";
        if (name.equals("int")) {
            tag = "I";
        } else if (name.equals("long")) {
            tag = "J";
        } else if (name.equals("double")) {
            tag = "D";
        } else if (name.equals("float")) {
            tag = "F";
        } else if (name.equals("short")) {
            tag = "S";
        } else if (name.equals("byte")) {
            tag = "B";
        } else if (name.equals("char")) {
            tag = "C";
        } else if (name.equals("boolean")) {
            tag = "Z";
        }
        return tag;
    }

    private static String simpleName(String binary) {
        String simple = binary;
        int at = -1;
        int i = 0;
        while (i < binary.length()) {
            if (binary.charAt(i) == '.') {
                at = i;
            }
            i = i + 1;
        }
        if (at >= 0) {
            simple = binary.substring(at + 1, binary.length());
        }
        return simple;
    }

    private static String swap(String s, char from, char to) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == from) {
                sb.append(to);
            } else {
                sb.append(c);
            }
            i = i + 1;
        }
        return sb.toString();
    }

    private static Class<?>[] copy(Class<?>[] src) {
        Class<?>[] arr = new Class<?>[src.length];
        int i = 0;
        while (i < src.length) {
            arr[i] = src[i];
            i = i + 1;
        }
        return arr;
    }

    private static Class<?> objectClass() {
        return Object.class;
    }

    private static Class<?> arrayOfObjectClass() {
        // `Object[].class` no parsea (el literal de clase de un ARRAY todavia no esta soportado),
        // asi que la clase se saca del objeto en vez de del literal. Equivalente y andando.
        Object[] empty = new Object[0];
        return empty.getClass();
    }
}
