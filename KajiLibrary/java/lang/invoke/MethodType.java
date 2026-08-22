package java.lang.invoke;

import java.lang.constant.Constable;
import java.lang.constant.MethodTypeDesc;
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
// OMITTED (subset): `fromMethodDescriptorString(String, ClassLoader)` — it must LOAD each named
// class, and KajiLibrary has no `Class.forName`; and the `TypeDescriptor.OfMethod` bridge methods,
// which are the erased overloads of the ones below.
// `Constable` va por import y nombre simple: una referencia CALIFICADA a un tipo del classpath no
// resuelve (finding #106a).
public final class MethodType implements Constable {

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

    public static MethodType methodType(Class<?> rtype, Class<?> ptype0, Class<?>[] ptypes) {
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

    public MethodType changeParameterType(int num, Class<?> nptype) {
        Class<?>[] arr = copy(ptypes);
        arr[num] = nptype;
        return new MethodType(rtype, arr);
    }

    public MethodType insertParameterTypes(int num, Class<?>[] ptypesToInsert) {
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

    public MethodType appendParameterTypes(Class<?>[] ptypesToInsert) {
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
        // Without `Class.forName` a name cannot be turned back into a `Class`, so the conversion
        // can only report the type it was given. The shape of the API is preserved; the mapping
        // returns once class lookup by name exists.
        return type;
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
