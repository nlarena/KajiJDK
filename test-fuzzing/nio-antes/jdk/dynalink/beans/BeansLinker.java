package jdk.dynalink.beans;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import jdk.dynalink.linker.GuardedInvocation;
import jdk.dynalink.linker.GuardingDynamicLinker;
import jdk.dynalink.linker.LinkRequest;
import jdk.dynalink.linker.LinkerServices;
import jdk.dynalink.linker.TypeBasedGuardingDynamicLinker;

/**
 * The linker of ordinary Java objects: properties, methods, arrays and collections.
 *
 * <h2>What it exposes of an object</h2>
 *
 * <p>What a dynamic language expects to be able to write. {@code obj.prop} looks for a
 * {@code getProp()} or a field {@code prop}; {@code obj.method(1)} looks for a method; {@code arr[0]}
 * and {@code list[0]} index.
 *
 * <p>The name translation is JavaBeans's, oddity included: {@code getURL()} gives the property
 * {@code URL} and not {@code uRL}, because when the first two letters are uppercase the name is left
 * alone. That is what {@code Introspector.decapitalize} does and this class reproduces it.
 *
 * <h2>The static facet and the instance facet are not looked at the same way</h2>
 *
 * <p>It is the least obvious asymmetry of this class, and it is verified against the JDK. The
 * <strong>instance</strong> facet uses every public member, inherited ones included: a field of the
 * parent is a property of the child. The <strong>static</strong> facet uses only the
 * <strong>declared</strong> ones: a static field of the parent is not a static property of the
 * child, and a static method of the parent does not turn up among the child's.
 *
 * <p>It makes sense: static members are not really inherited —there is no dispatch— and exposing the
 * parent's on the child would suggest a relationship that does not exist. Nested classes are the
 * exception and are inherited, which is why {@code Point} exposes {@code Double} and {@code Float},
 * which are {@code Point2D}'s.
 *
 * <h2>Fine rules, all checked against JDK 25</h2>
 *
 * <ul> <li>{@code getX()} is a property whatever it returns: even {@code void getVoid()} counts.
 * What matters is that it takes no arguments. <li>{@code isX()} only counts if it returns a
 * <strong>primitive</strong> {@code boolean}; with {@code Boolean} it does not. <li>Bare
 * {@code get()} and {@code is()} are not properties: no name is left after the prefix. <li>A
 * {@code final} field is readable and not writable. <li>{@code class} is always among the readable
 * static properties, even when the class has no static member at all. <li>An array has the readable
 * property {@code length}, which does not come out of reflection —{@code getFields()} of an array
 * returns nothing— and is added by hand. </ul>
 *
 * <h2>Where this VM stands</h2>
 *
 * <p>The six introspection methods are real and are verified one by one against the JDK. Linking
 * proper —{@link #getLinkerForClass} and {@link #getGuardedInvocation}— needs {@code MethodHandle}
 * built, which this VM cannot do yet; they get that far and fail naming it.
 *
 * @since 9
 */
public class BeansLinker implements GuardingDynamicLinker {

    private static final String NO_HANDLES =
            "linking a bean needs MethodHandle built, which this VM does not support yet";

    private final MissingMemberHandlerFactory missingMemberFactory;

    /** A linker that lets members that do not exist fail. */
    public BeansLinker() {
        this(null);
    }

    /**
     * A linker with an answer of its own for members that do not exist.
     *
     * @param missingMemberHandlerFactory the factory, or {@code null} for the usual behaviour
     */
    public BeansLinker(final MissingMemberHandlerFactory missingMemberHandlerFactory) {
        this.missingMemberFactory = missingMemberHandlerFactory;
    }

    /**
     * The linker that belongs to that class.
     *
     * @param clazz the class
     * @return the linker
     * @throws UnsupportedOperationException on this VM; see the class note
     */
    public TypeBasedGuardingDynamicLinker getLinkerForClass(final Class<?> clazz) {
        throw new UnsupportedOperationException(NO_HANDLES);
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException on this VM; see the class note
     */
    public GuardedInvocation getGuardedInvocation(final LinkRequest linkRequest,
            final LinkerServices linkerServices) throws Exception {
        throw new UnsupportedOperationException(NO_HANDLES);
    }

    /**
     * Whether that object is one of the dynamic methods this linker produces.
     *
     * <p>Always {@code false}, and it is the right answer: dynamic methods are objects the linker
     * itself builds, this VM never gets as far as building one, and therefore no object anybody can
     * pass in here is one.
     *
     * @param obj the object
     * @return {@code false}
     */
    public static boolean isDynamicMethod(final Object obj) {
        return false;
    }

    /**
     * Whether that object is one of the dynamic constructors this linker produces.
     *
     * <p>Always {@code false}, for the same reason as {@link #isDynamicMethod}.
     *
     * @param obj the object
     * @return {@code false}
     */
    public static boolean isDynamicConstructor(final Object obj) {
        return false;
    }

    /**
     * That class's constructor with that signature, as a dynamic method object.
     *
     * <p>It fails rather than returning {@code null}: {@code null} means "that class has no
     * constructor with that signature", which would be a lie for nearly any input.
     *
     * @param clazz the class
     * @param signature the signature, with the types separated by commas
     * @return it does not return
     * @throws UnsupportedOperationException on this VM; see the class note
     */
    public static Object getConstructorMethod(final Class<?> clazz, final String signature) {
        throw new UnsupportedOperationException(
                "dynamic method objects are built by the linker, and linking needs MethodHandle, "
                + "which this VM does not support yet");
    }

    // ---- introspection ----

    /**
     * The instance properties that can be read.
     *
     * @param clazz the class
     * @return the names
     */
    public static Set<String> getReadableInstancePropertyNames(final Class<?> clazz) {
        final Set<String> out = new TreeSet<String>();
        if (clazz.isArray()) {
            // An array's length is not a Field: `getFields()` of an array returns nothing, and yet
            // `arr.length` is exactly what a dynamic language writes. It is added by hand, and only
            // as readable: an array's length cannot be changed.
            out.add("length");
        }
        for (final Field f : clazz.getFields()) {
            if (!Modifier.isStatic(f.getModifiers())) {
                out.add(f.getName());
            }
        }
        for (final Method m : clazz.getMethods()) {
            if (!Modifier.isStatic(m.getModifiers())) {
                addReader(out, m);
            }
        }
        return Collections.unmodifiableSet(out);
    }

    /**
     * The instance properties that can be written.
     *
     * @param clazz the class
     * @return the names
     */
    public static Set<String> getWritableInstancePropertyNames(final Class<?> clazz) {
        final Set<String> out = new TreeSet<String>();
        for (final Field f : clazz.getFields()) {
            final int mod = f.getModifiers();
            if (!Modifier.isStatic(mod) && !Modifier.isFinal(mod)) {
                out.add(f.getName());
            }
        }
        for (final Method m : clazz.getMethods()) {
            if (!Modifier.isStatic(m.getModifiers())) {
                addWriter(out, m);
            }
        }
        return Collections.unmodifiableSet(out);
    }

    /**
     * The names of the instance methods.
     *
     * <p>It includes the ones that are also accessors: {@code getProp} turns up here and
     * {@code prop} turns up among the properties. They are two ways to the same thing and both are
     * exposed.
     *
     * @param clazz the class
     * @return the names
     */
    public static Set<String> getInstanceMethodNames(final Class<?> clazz) {
        final Set<String> out = new TreeSet<String>();
        for (final Method m : clazz.getMethods()) {
            if (!Modifier.isStatic(m.getModifiers())) {
                out.add(m.getName());
            }
        }
        return Collections.unmodifiableSet(out);
    }

    /**
     * The static properties that can be read.
     *
     * <p>It always includes {@code class}, and the nested classes by their simple name.
     *
     * @param clazz the class
     * @return the names
     */
    public static Set<String> getReadableStaticPropertyNames(final Class<?> clazz) {
        final Set<String> out = new TreeSet<String>();
        // The pseudo-property that returns the Class being stood in for. It is always there, even on
        // a class with no static member at all.
        out.add("class");
        for (final Field f : clazz.getDeclaredFields()) {
            if (isPublicStatic(f.getModifiers())) {
                out.add(f.getName());
            }
        }
        for (final Method m : clazz.getDeclaredMethods()) {
            if (isPublicStatic(m.getModifiers())) {
                addReader(out, m);
            }
        }
        // Nested classes are inherited, unlike the rest of the static facet.
        for (final Class<?> k : clazz.getClasses()) {
            out.add(k.getSimpleName());
        }
        return Collections.unmodifiableSet(out);
    }

    /**
     * The static properties that can be written.
     *
     * @param clazz the class
     * @return the names
     */
    public static Set<String> getWritableStaticPropertyNames(final Class<?> clazz) {
        final Set<String> out = new TreeSet<String>();
        for (final Field f : clazz.getDeclaredFields()) {
            final int mod = f.getModifiers();
            if (isPublicStatic(mod) && !Modifier.isFinal(mod)) {
                out.add(f.getName());
            }
        }
        for (final Method m : clazz.getDeclaredMethods()) {
            if (isPublicStatic(m.getModifiers())) {
                addWriter(out, m);
            }
        }
        return Collections.unmodifiableSet(out);
    }

    /**
     * The names of the static methods declared by that class.
     *
     * @param clazz the class
     * @return the names
     */
    public static Set<String> getStaticMethodNames(final Class<?> clazz) {
        final Set<String> out = new TreeSet<String>();
        for (final Method m : clazz.getDeclaredMethods()) {
            if (isPublicStatic(m.getModifiers())) {
                out.add(m.getName());
            }
        }
        return Collections.unmodifiableSet(out);
    }

    private static boolean isPublicStatic(final int mod) {
        return Modifier.isStatic(mod) && Modifier.isPublic(mod);
    }

    /** If the method is a read accessor, adds the property it names. */
    private static void addReader(final Set<String> out, final Method m) {
        if (m.getParameterTypes().length != 0) {
            return;
        }
        final String n = m.getName();
        if (n.length() > 3 && n.startsWith("get")) {
            // Without looking at the return type: even a `void getVoid()` counts as a property.
            out.add(decapitalize(n.substring(3)));
        } else if (n.length() > 2 && n.startsWith("is") && m.getReturnType() == boolean.class) {
            // Here it does matter, and it has to be the primitive: with Boolean it does not count.
            out.add(decapitalize(n.substring(2)));
        }
    }

    /** If the method is a write accessor, adds the property it names. */
    private static void addWriter(final Set<String> out, final Method m) {
        if (m.getParameterTypes().length != 1) {
            return;
        }
        final String n = m.getName();
        if (n.length() > 3 && n.startsWith("set")) {
            out.add(decapitalize(n.substring(3)));
        }
    }

    /**
     * JavaBeans's rule for going from {@code getURL} to {@code URL} and from {@code getX} to
     * {@code x}.
     *
     * <p>If the first two letters are uppercase the name is left untouched. The reason is that such
     * names are usually acronyms —{@code URL}, {@code HTTP}, {@code ID}— and lowercasing the first
     * letter would make them unrecognizable.
     */
    private static String decapitalize(final String s) {
        if (s.length() > 1 && Character.isUpperCase(s.charAt(0))
                && Character.isUpperCase(s.charAt(1))) {
            return s;
        }
        final char[] c = s.toCharArray();
        c[0] = Character.toLowerCase(c[0]);
        return new String(c);
    }
}
