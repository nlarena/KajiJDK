/**
 * Exercises java.lang.Class. Every method returns the number of things that came out wrong, so 0
 * is a pass.
 *
 * <p>The same source compiles against the JDK 25, where {@code main} prints the same counts.
 *
 * <p>One rule shapes the whole file: anything about the CONTENT of a specific class is asked of
 * a class defined here, never of a JDK one. Our {@code java.lang.String} implements three
 * interfaces and the reference's implements five, and both are correct for their own library --
 * so a probe that counted {@code String.class.getInterfaces()} would be measuring how far the
 * library has got, not whether {@code getInterfaces} works. JDK classes are asked only what the
 * LANGUAGE fixes: that an array implements exactly Cloneable and Serializable, that
 * {@code Object} has no superclass, that an interface's superclass is null.
 */
public class ClassTest {

    static int eq(String got, String want) {
        if (got == null) {
            if (want == null) {
                return 0;
            }
            return 1;
        }
        if (got.equals(want)) {
            return 0;
        }
        return 1;
    }

    /** The five names a type has, and where they disagree. */
    public static int nombres() {
        int bad = 0;
        Class<?> string = String.class;
        bad = bad + ClassTest.eq(string.getName(), "java.lang.String");
        bad = bad + ClassTest.eq(string.getTypeName(), "java.lang.String");
        bad = bad + ClassTest.eq(string.getSimpleName(), "String");
        bad = bad + ClassTest.eq(string.getCanonicalName(), "java.lang.String");
        bad = bad + ClassTest.eq(string.getPackageName(), "java.lang");
        bad = bad + ClassTest.eq(string.toString(), "class java.lang.String");
        bad = bad + ClassTest.eq(string.descriptorString(), "Ljava/lang/String;");

        // A primitive: the name IS the keyword, and the package is java.lang.
        Class<?> integer = int.class;
        bad = bad + ClassTest.eq(integer.getName(), "int");
        bad = bad + ClassTest.eq(integer.getTypeName(), "int");
        bad = bad + ClassTest.eq(integer.getSimpleName(), "int");
        bad = bad + ClassTest.eq(integer.getCanonicalName(), "int");
        bad = bad + ClassTest.eq(integer.getPackageName(), "java.lang");
        bad = bad + ClassTest.eq(integer.toString(), "int");
        bad = bad + ClassTest.eq(integer.descriptorString(), "I");

        // An array: getName is a DESCRIPTOR and getTypeName is not, which is the pair that
        // catches code using the wrong one.
        Class<?> strings = String[].class;
        bad = bad + ClassTest.eq(strings.getName(), "[Ljava.lang.String;");
        bad = bad + ClassTest.eq(strings.getTypeName(), "java.lang.String[]");
        bad = bad + ClassTest.eq(strings.getSimpleName(), "String[]");
        bad = bad + ClassTest.eq(strings.getCanonicalName(), "java.lang.String[]");
        bad = bad + ClassTest.eq(strings.getPackageName(), "java.lang");
        bad = bad + ClassTest.eq(strings.toString(), "class [Ljava.lang.String;");
        bad = bad + ClassTest.eq(strings.descriptorString(), "[Ljava/lang/String;");

        Class<?> ints = int[][].class;
        bad = bad + ClassTest.eq(ints.getName(), "[[I");
        bad = bad + ClassTest.eq(ints.getTypeName(), "int[][]");
        bad = bad + ClassTest.eq(ints.getSimpleName(), "int[][]");
        bad = bad + ClassTest.eq(ints.getCanonicalName(), "int[][]");
        bad = bad + ClassTest.eq(ints.descriptorString(), "[[I");

        // A class of ours, in the default package: no package name at all.
        Class<?> subject = ClassTestSubject.class;
        bad = bad + ClassTest.eq(subject.getName(), "ClassTestSubject");
        bad = bad + ClassTest.eq(subject.getSimpleName(), "ClassTestSubject");
        bad = bad + ClassTest.eq(subject.getPackageName(), "");
        bad = bad + ClassTest.eq(subject.descriptorString(), "LClassTestSubject;");
        return bad;
    }

    /** What kind of thing a mirror describes. */
    public static int clases() {
        int bad = 0;
        if (String.class.isInterface() || !ClassTestIface.class.isInterface()) {
            bad = bad + 1;
        }
        if (String.class.isArray() || !String[].class.isArray() || !int[].class.isArray()) {
            bad = bad + 1;
        }
        if (!int.class.isPrimitive() || String.class.isPrimitive()
                || int[].class.isPrimitive()) {
            bad = bad + 1;
        }
        // void counts as a primitive even though no value has that type.
        if (!void.class.isPrimitive()) {
            bad = bad + 1;
        }
        if (String.class.isAnnotation() || ClassTestIface.class.isAnnotation()) {
            bad = bad + 1;
        }
        if (String.class.isEnum() || int.class.isEnum() || String[].class.isEnum()) {
            bad = bad + 1;
        }
        if (String.class.isRecord() || int.class.isRecord()) {
            bad = bad + 1;
        }
        if (String.class.isSynthetic()) {
            bad = bad + 1;
        }
        if (String.class.isHidden() || int.class.isHidden()) {
            bad = bad + 1;
        }
        // The modifiers of things the language pins down.
        if (String.class.getModifiers() != 17) { // public final
            bad = bad + 1;
        }
        if (int.class.getModifiers() != 1041) { // public abstract final
            bad = bad + 1;
        }
        if (String[].class.getModifiers() != 1041) { // takes String's visibility
            bad = bad + 1;
        }
        if (Object.class.getModifiers() != 1) { // public
            bad = bad + 1;
        }
        if (ClassTestIface.class.getModifiers() != 1536) { // interface abstract, package-private
            bad = bad + 1;
        }
        return bad;
    }

    /** Superclasses, interfaces, component types, and the two relations over them. */
    public static int grafo() {
        int bad = 0;
        if (String.class.getSuperclass() != Object.class) {
            bad = bad + 1;
        }
        // Three separate reasons to be null, and they are easy to conflate.
        if (Object.class.getSuperclass() != null) {
            bad = bad + 1;
        }
        if (ClassTestIface.class.getSuperclass() != null) {
            bad = bad + 1;
        }
        if (int.class.getSuperclass() != null) {
            bad = bad + 1;
        }
        // An array's superclass is Object, not an array of the component's superclass.
        if (String[].class.getSuperclass() != Object.class) {
            bad = bad + 1;
        }
        if (ClassTestSubject.class.getSuperclass() != ClassTestBase.class) {
            bad = bad + 1;
        }

        // Every array implements exactly Cloneable and Serializable -- granted by the language,
        // written in no class file.
        Class<?>[] arrayIfaces = String[].class.getInterfaces();
        if (arrayIfaces.length != 2) {
            bad = bad + 1;
        } else {
            bad = bad + ClassTest.eq(arrayIfaces[0].getName(), "java.lang.Cloneable");
            bad = bad + ClassTest.eq(arrayIfaces[1].getName(), "java.io.Serializable");
        }
        if (int.class.getInterfaces().length != 0) {
            bad = bad + 1;
        }
        Class<?>[] mine = ClassTestSubject.class.getInterfaces();
        if (mine.length != 1 || mine[0] != ClassTestIface.class) {
            bad = bad + 1;
        }
        // Directly: the interface the SUPERCLASS implements is not in the subclass's list.
        if (ClassTestBase.class.getInterfaces().length != 0) {
            bad = bad + 1;
        }

        if (String[].class.getComponentType() != String.class) {
            bad = bad + 1;
        }
        if (int[][].class.getComponentType() != int[].class) {
            bad = bad + 1;
        }
        if (int[].class.getComponentType() != int.class) {
            bad = bad + 1;
        }
        if (String.class.getComponentType() != null || int.class.getComponentType() != null) {
            bad = bad + 1;
        }
        if (String.class.componentType() != null
                || String[].class.componentType() != String.class) {
            bad = bad + 1;
        }
        if (String.class.arrayType() != String[].class) {
            bad = bad + 1;
        }
        if (int.class.arrayType() != int[].class) {
            bad = bad + 1;
        }
        if (int[].class.arrayType() != int[][].class) {
            bad = bad + 1;
        }

        // isAssignableFrom reads backwards, and that is the point of testing both directions.
        if (!Object.class.isAssignableFrom(String.class)) {
            bad = bad + 1;
        }
        if (String.class.isAssignableFrom(Object.class)) {
            bad = bad + 1;
        }
        if (!String.class.isAssignableFrom(String.class)) {
            bad = bad + 1;
        }
        if (!ClassTestIface.class.isAssignableFrom(ClassTestSubject.class)) {
            bad = bad + 1;
        }
        if (!Object.class.isAssignableFrom(String[].class)) {
            bad = bad + 1;
        }
        // A primitive takes part in no hierarchy: only itself.
        if (!int.class.isAssignableFrom(int.class)) {
            bad = bad + 1;
        }
        if (Object.class.isAssignableFrom(int.class) || int.class.isAssignableFrom(long.class)) {
            bad = bad + 1;
        }

        Object text = "hola";
        if (!String.class.isInstance(text) || !Object.class.isInstance(text)) {
            bad = bad + 1;
        }
        if (String.class.isInstance(null) || Integer.class.isInstance(text)) {
            bad = bad + 1;
        }
        Object subject = new ClassTestSubject();
        if (!ClassTestIface.class.isInstance(subject)
                || !ClassTestBase.class.isInstance(subject)) {
            bad = bad + 1;
        }
        // getClass hands back the very same mirror, so identity is type identity.
        if (text.getClass() != String.class || subject.getClass() != ClassTestSubject.class) {
            bad = bad + 1;
        }
        return bad;
    }

    /** The two casts that check at runtime. */
    public static int casteo() {
        int bad = 0;
        Object text = "hola";
        String back = String.class.cast(text);
        bad = bad + ClassTest.eq(back, "hola");
        if (String.class.cast(null) != null) {
            bad = bad + 1;
        }
        if (ClassTestSubject.class.asSubclass(ClassTestBase.class) != ClassTestSubject.class) {
            bad = bad + 1;
        }
        if (String.class.asSubclass(Object.class) != String.class) {
            bad = bad + 1;
        }
        bad = bad + ClassTest.expectCast(1);
        bad = bad + ClassTest.expectCast(2);
        return bad;
    }

    static int expectCast(int which) {
        try {
            ClassTest.castCase(which);
        } catch (ClassCastException ex) {
            return 0;
        }
        return 1;
    }

    static Object castCase(int which) {
        Object text = "hola";
        if (which == 1) {
            return Integer.class.cast(text);
        }
        return Object.class.asSubclass(String.class);
    }

    /** Finding a mirror by name. */
    public static int buscar() {
        int bad = 0;
        try {
            if (Class.forName("java.lang.String") != String.class) {
                bad = bad + 1;
            }
            if (Class.forName("java.lang.String", true, null) != String.class) {
                bad = bad + 1;
            }
            if (Class.forName("ClassTestSubject") != ClassTestSubject.class) {
                bad = bad + 1;
            }
        } catch (ClassNotFoundException ex) {
            bad = bad + 1;
        }
        bad = bad + ClassTest.expectNotFound();
        // A primitive keyword is NOT a class name -- forName cannot find `int`.
        if (Class.forPrimitiveName("int") != int.class) {
            bad = bad + 1;
        }
        if (Class.forPrimitiveName("void") != void.class) {
            bad = bad + 1;
        }
        if (Class.forPrimitiveName("java.lang.String") != null) {
            bad = bad + 1;
        }
        if (Class.forPrimitiveName("Int") != null) {
            bad = bad + 1;
        }
        return bad;
    }

    static int expectNotFound() {
        try {
            Class.forName("no.existe.NingunaClase");
        } catch (ClassNotFoundException ex) {
            return 0;
        }
        return 1;
    }

    /** The field mirrors. */
    public static int campos() {
        int bad = 0;
        java.lang.reflect.Field[] declared = ClassTestSubject.class.getDeclaredFields();
        if (declared.length != 3) {
            bad = bad + 1;
        }
        int seenPublic = 0;
        int seenPrivate = 0;
        int seenStatic = 0;
        int i = 0;
        while (i < declared.length) {
            java.lang.reflect.Field f = declared[i];
            if (f.getDeclaringClass() != ClassTestSubject.class) {
                bad = bad + 1;
            }
            if (f.getName().equals("visible")) {
                seenPublic = seenPublic + 1;
                if (f.getType() != int.class) {
                    bad = bad + 1;
                }
            }
            if (f.getName().equals("oculto")) {
                seenPrivate = seenPrivate + 1;
                if (f.getType() != String.class) {
                    bad = bad + 1;
                }
            }
            if (f.getName().equals("COMPARTIDO")) {
                seenStatic = seenStatic + 1;
                if (f.getType() != long[].class) {
                    bad = bad + 1;
                }
            }
            i = i + 1;
        }
        if (seenPublic != 1 || seenPrivate != 1 || seenStatic != 1) {
            bad = bad + 1;
        }
        // An array and a primitive declare no fields.
        if (String[].class.getDeclaredFields().length != 0
                || int.class.getDeclaredFields().length != 0) {
            bad = bad + 1;
        }
        try {
            java.lang.reflect.Field one = ClassTestSubject.class.getDeclaredField("oculto");
            bad = bad + ClassTest.eq(one.getName(), "oculto");
        } catch (NoSuchFieldException ex) {
            bad = bad + 1;
        }
        bad = bad + ClassTest.expectNoField(1);
        bad = bad + ClassTest.expectNoField(2);
        // getFields walks up: the subclass's public field plus the base's, and not the private
        // one of either.
        java.lang.reflect.Field[] visible = ClassTestSubject.class.getFields();
        int publicCount = 0;
        int j = 0;
        while (j < visible.length) {
            if (visible[j].getName().equals("visible")
                    || visible[j].getName().equals("heredado")
                    || visible[j].getName().equals("COMPARTIDO")) {
                publicCount = publicCount + 1;
            }
            j = j + 1;
        }
        if (publicCount != 3 || visible.length != 3) {
            bad = bad + 1;
        }
        try {
            if (!ClassTestSubject.class.getField("heredado").getName().equals("heredado")) {
                bad = bad + 1;
            }
        } catch (NoSuchFieldException ex) {
            bad = bad + 1;
        }
        return bad;
    }

    static int expectNoField(int which) {
        try {
            ClassTest.noFieldCase(which);
        } catch (NoSuchFieldException ex) {
            return 0;
        }
        return 1;
    }

    static java.lang.reflect.Field noFieldCase(int which) throws NoSuchFieldException {
        if (which == 1) {
            return ClassTestSubject.class.getDeclaredField("noExiste");
        }
        // A private field is declared but not visible: getField must not find it.
        return ClassTestSubject.class.getField("oculto");
    }

    /** The rest. */
    public static int varios() {
        int bad = 0;
        if (String.class.getClassLoader() != null) {
            bad = bad + 1;
        }
        if (String.class.desiredAssertionStatus()) {
            bad = bad + 1;
        }
        if (!String.class.isNestmateOf(String.class)) {
            bad = bad + 1;
        }
        if (!String.class.describeConstable().isPresent()) {
            bad = bad + 1;
        }
        if (!int.class.describeConstable().isPresent()) {
            bad = bad + 1;
        }
        bad = bad + ClassTest.eq(String.class.toGenericString(),
                "public final class java.lang.String");
        bad = bad + ClassTest.eq(int.class.toGenericString(), "int");
        bad = bad + ClassTest.eq(Object.class.toGenericString(), "public class java.lang.Object");
        return bad;
    }

    public static int todo() {
        return ClassTest.nombres() + ClassTest.clases() + ClassTest.grafo() + ClassTest.casteo()
                + ClassTest.buscar() + ClassTest.campos() + ClassTest.varios();
    }

    public static void main(String[] args) {
        System.out.println("nombres   " + ClassTest.nombres());
        System.out.println("clases    " + ClassTest.clases());
        System.out.println("grafo     " + ClassTest.grafo());
        System.out.println("casteo    " + ClassTest.casteo());
        System.out.println("buscar    " + ClassTest.buscar());
        System.out.println("campos    " + ClassTest.campos());
        System.out.println("varios    " + ClassTest.varios());
        System.out.println("TOTAL     " + ClassTest.todo());
    }
}

// The subjects. Named with the probe's prefix on purpose: `java/` is one flat directory shared by
// every fixture, and a plain name like `Base` would collide with somebody else's.
interface ClassTestIface {
}

class ClassTestBase {
    public int heredado;

    private int reservado;
}

class ClassTestSubject extends ClassTestBase implements ClassTestIface {
    public int visible;

    private String oculto;

    public static long[] COMPARTIDO;
}
