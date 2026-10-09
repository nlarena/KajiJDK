package jdk.internal.reflect;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * KajiLibrary's jdk.internal.reflect.Reflection -- the rules of access of reflection, and the
 * filter of members reflection must not hand over.
 *
 * <h2>Two independent halves</h2>
 *
 * <p>The <strong>first</strong> one is the check of access: {@link #verifyMemberAccess} answers the
 * question JLS 6.6 asks of every access --may this class touch this member?-- and it is pure
 * arithmetic over modifiers, packages, nests and the hierarchy of inheritance. It needs nothing of
 * the VM that this library does not already have, so it is whole and answers the same as the JDK.
 *
 * <p>The <strong>second</strong> one is the register of filtering: a class may declare that certain
 * fields or methods of its own <em>do not exist</em> for reflection, and {@link #filterFields} /
 * {@link #filterMethods} take them out of the array. The map starts with the same entries as that
 * of the JDK, which are the ones that keep {@code ClassLoader.getDeclaredFields()} from handing
 * over the guts of the class loader.
 *
 * <p>It is worth saying how it differs here: in the JDK that filter is <em>plugged in</em>, because
 * {@code Class.getDeclaredFields0} goes through it. In this VM {@code Class.getDeclaredFields} is a
 * native that consults nobody, so the filter is a function that has to be called and not one that
 * applies itself. The function does what it promises; what there is not is the hook that would call
 * it, and that belongs to {@code java.lang.Class}, not to this class.
 *
 * <h2>What the header used to say about missing members, and what is true</h2>
 *
 * <p>This javadoc listed five members as absent --{@code getCallerClass()},
 * {@code getClassAccessFlags(Class)}, {@code isCallerSensitive(Method)},
 * {@code isTrustedFinalField(Field)} and {@code ensureNativeAccess(...)}-- and gave a reason for
 * each. <strong>The five are in this file</strong>, declared and documented further down, and they
 * came in after that note was written. Their own javadoc is the one that holds: read
 * {@link #getCallerClass()}, {@link #getClassAccessFlags(Class)},
 * {@link #isCallerSensitive(Method)}, {@link #isTrustedFinalField(Field)} and
 * {@link #ensureNativeAccess(Class, Class, String, Class)}.
 *
 * <p>The same note listed three classes of the package as absent.
 * <strong>{@link CallerSensitive} is there</strong>, which is what makes
 * {@link #isCallerSensitive(Method)} able to answer. The other two are not, and for these reasons:
 *
 * <ul>
 * <li><strong>{@code CallerSensitiveAdapter}</strong>. It marks the alternative method the runtime
 *     calls with the real caller when a {@code @CallerSensitive} one is reached reflectively. It
 *     only means something when the machinery interposes frames, and this one does not: see the
 *     note of {@link #getCallerClass()}.</li>
 * <li><strong>{@code AccessorUtils}</strong>. Its public surface is, in its entirety, a constructor
 *     with no arguments: the only method it has --{@code isIllegalArgument}-- is package-private,
 *     that is to say it is not API even in the JDK. And what it does is decide whether a {@code
 *     ClassCastException} that came out of a {@code MethodHandle} was born of the accessor or of
 *     the destination method, which is a question that only exists if the accessors are made of
 *     {@code MethodHandle} -- and the ones here are not. Bringing it in would add a class to the
 *     count and zero behaviour, which is exactly what is not done.</li>
 * </ul>
 */
public class Reflection {

    // The wildcard. The JDK compares it by contents and not by identity, so any set that contains
    // it filters everything -- `ALL_MEMBERS` is the convenient way of writing it, not the only one.
    private static final String WILDCARD = "*";

    /** The set that, registered for a class, hides <em>all</em> of its members. */
    public static final Set<String> ALL_MEMBERS = Set.of(Reflection.WILDCARD);

    // Copy on write: `filterFields` reads with no lock and `registerFieldsToFilter` publishes a
    // whole new map. It is what allows the registration to be `synchronized` and the reading not,
    // which matters because filtering happens on every `getDeclaredFields` and registering a
    // handful of times in the life of the process.
    private static volatile Map<Class<?>, Set<String>> fieldFilter;
    private static volatile Map<Class<?>, Set<String>> methodFilter;

    static {
        // The same entries as the JDK. They are the classes whose internal fields, handed over by
        // reflection, would let one write the class loader or the bit of accessibility of an
        // `AccessibleObject` -- that is to say, skip everything else there is in this file.
        Map<Class<?>, Set<String>> fields = new HashMap<Class<?>, Set<String>>();
        fields.put(Reflection.class, Reflection.ALL_MEMBERS);
        fields.put(AccessibleObject.class, Reflection.ALL_MEMBERS);
        fields.put(Class.class, Set.of("classLoader", "classData", "modifiers", "protectionDomain",
                "primitive"));
        fields.put(ClassLoader.class, Reflection.ALL_MEMBERS);
        fields.put(Constructor.class, Reflection.ALL_MEMBERS);
        fields.put(Field.class, Reflection.ALL_MEMBERS);
        fields.put(Method.class, Reflection.ALL_MEMBERS);
        fields.put(Module.class, Reflection.ALL_MEMBERS);
        Reflection.fieldFilter = fields;
        Reflection.methodFilter = new HashMap<Class<?>, Set<String>>();
    }

    public Reflection() {
    }

    // ---- the check of access (JLS 6.6) ----

    /**
     * Whether {@code currentClass} may touch a member of {@code memberClass} with these modifiers.
     *
     * @param currentClass the one that accesses
     * @param memberClass the one that declares the member
     * @param targetClass the static type of the receiver, or {@code null} if the member is static.
     *                    Only the rule of the instance {@code protected} looks at it (JLS 6.6.2).
     * @param modifiers the modifiers of the member
     * @return whether the access is legal
     */
    public static boolean verifyMemberAccess(Class<?> currentClass, Class<?> memberClass,
                                             Class<?> targetClass, int modifiers) {
        Objects.requireNonNull(currentClass);
        Objects.requireNonNull(memberClass);

        // A class can always with itself, the private included.
        if (currentClass == memberClass) {
            return true;
        }
        if (!Reflection.verifyModuleAccess(currentClass.getModule(), memberClass)) {
            return false;
        }

        // Two different questions of package and the same answer: it is calculated at most once
        // because comparing packages touches the class loader.
        boolean wasAsked = false;
        boolean samePackage = false;

        if (!Modifier.isPublic(memberClass.getModifiers())) {
            samePackage = Reflection.sameClassPackage(currentClass, memberClass);
            wasAsked = true;
            if (!samePackage) {
                return false;
            }
        }

        // From here it is known that `currentClass` reaches `memberClass`; the member is left.
        if (Modifier.isPublic(modifiers)) {
            return true;
        }

        // A `private` crosses the border of the class if the two share a nest -- which is what a
        // nest is. `targetClass` may be left outside and it does not matter: what is accessed is
        // the member.
        if (Modifier.isPrivate(modifiers)) {
            if (Reflection.areNestMates(currentClass, memberClass)) {
                return true;
            }
        }

        boolean okSoFar = false;
        if (Modifier.isProtected(modifiers)) {
            if (Reflection.isSubclassOf(currentClass, memberClass)) {
                okSoFar = true;
            }
        }
        if (!okSoFar && !Modifier.isPrivate(modifiers)) {
            if (!wasAsked) {
                samePackage = Reflection.sameClassPackage(currentClass, memberClass);
                wasAsked = true;
            }
            if (samePackage) {
                okSoFar = true;
            }
        }
        if (!okSoFar) {
            return false;
        }

        // JLS 6.6.2: inheriting a `protected` from another package lets you use it on YOUR type,
        // not on any other subtype of the one that declares it. It is the rule that keeps a
        // subclass from using the protected `clone()` of `Object` on somebody else's object.
        if (targetClass != null && Modifier.isProtected(modifiers) && targetClass != currentClass) {
            if (!wasAsked) {
                samePackage = Reflection.sameClassPackage(currentClass, memberClass);
                wasAsked = true;
            }
            if (!samePackage) {
                if (!Reflection.isSubclassOf(targetClass, currentClass)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * {@link #verifyMemberAccess} but throwing instead of answering no.
     *
     * @param currentClass the one that accesses
     * @param memberClass the one that declares the member
     * @param targetClass the static type of the receiver, or {@code null}
     * @param modifiers the modifiers of the member
     * @throws IllegalAccessException if the access is not legal
     */
    public static void ensureMemberAccess(Class<?> currentClass, Class<?> memberClass,
                                          Class<?> targetClass, int modifiers)
            throws IllegalAccessException {
        if (!Reflection.verifyMemberAccess(currentClass, memberClass, targetClass, modifiers)) {
            throw Reflection.newIllegalAccessException(currentClass, memberClass, targetClass,
                    modifiers);
        }
    }

    /**
     * The cheap case of the check: a public member of a public type in a package exported with no
     * condition. It serves for skipping {@link #verifyMemberAccess} when the answer does not depend
     * on who asks.
     *
     * @param memberClass the one that declares the member
     * @param modifiers the modifiers of the member
     * @return whether anybody may
     */
    public static boolean verifyPublicMemberAccess(Class<?> memberClass, int modifiers) {
        Module m = memberClass.getModule();
        return Modifier.isPublic(modifiers)
                && m.isExported(memberClass.getPackageName())
                && Modifier.isPublic(memberClass.getModifiers());
    }

    /**
     * Whether the module of {@code memberClass} exports its package to {@code currentModule}.
     *
     * <p>In this runtime everything lives in the unnamed module, which exports everything; the
     * answer is always yes, and not by shortcut but because that <em>is</em> the graph of modules
     * there is.
     *
     * @param currentModule the module that accesses
     * @param memberClass the one that declares the member
     * @return whether the package is exported towards it
     */
    public static boolean verifyModuleAccess(Module currentModule, Class<?> memberClass) {
        Module memberModule = memberClass.getModule();
        if (currentModule == memberModule) {
            return true;
        }
        return memberModule.isExported(memberClass.getPackageName(), currentModule);
    }

    /**
     * The exception that describes an illegal access, already worded.
     *
     * @param currentClass the one that accesses
     * @param memberClass the one that declares the member
     * @param targetClass the static type of the receiver, or {@code null}
     * @param modifiers the modifiers of the member
     * @return the exception, without throwing it
     */
    public static IllegalAccessException newIllegalAccessException(Class<?> currentClass,
                                                                   Class<?> memberClass,
                                                                   Class<?> targetClass,
                                                                   int modifiers) {
        StringBuilder m = new StringBuilder();
        m.append("class ").append(currentClass.getName());
        m.append(" cannot access ");
        String visibility = Modifier.isPublic(modifiers) ? "public"
                : Modifier.isProtected(modifiers) ? "protected"
                : Modifier.isPrivate(modifiers) ? "private" : "package-private";
        m.append(visibility).append(" member of class ").append(memberClass.getName());
        if (targetClass != null && targetClass != memberClass) {
            m.append(" with modifiers \"").append(Modifier.toString(modifiers)).append('"');
        }
        return new IllegalAccessException(m.toString());
    }

    /**
     * Whether the two classes share a nest, and therefore the private members of one another.
     *
     * <p>In the JDK it is a {@code native} because the VM resolves the nest; here {@link
     * Class#isNestmateOf} already asks exactly that question against the same native, so this
     * method is the name the reflective machinery gives to that one.
     *
     * @param currentClass one class
     * @param memberClass the other
     * @return whether they are in the same nest
     */
    public static boolean areNestMates(Class<?> currentClass, Class<?> memberClass) {
        return currentClass.isNestmateOf(memberClass);
    }

    // ---- the register of filtering ----

    /**
     * It declares that reflection must not hand over those fields of {@code containingClass}.
     *
     * @param containingClass the class that declares them
     * @param fieldNames the names to hide, or {@link #ALL_MEMBERS} for all of them
     * @throws IllegalArgumentException if that class already has a filter; registering twice would
     *                                  <em>replace</em> the first one, which is how it would be
     *                                  skipped
     */
    public static synchronized void registerFieldsToFilter(Class<?> containingClass,
                                                           Set<String> fieldNames) {
        Reflection.fieldFilter = Reflection.register(Reflection.fieldFilter, containingClass,
                fieldNames);
    }

    /**
     * It declares that reflection must not hand over those methods of {@code containingClass}.
     *
     * @param containingClass the class that declares them
     * @param methodNames the names to hide, or {@link #ALL_MEMBERS} for all of them
     * @throws IllegalArgumentException if that class already has a filter
     */
    public static synchronized void registerMethodsToFilter(Class<?> containingClass,
                                                            Set<String> methodNames) {
        Reflection.methodFilter = Reflection.register(Reflection.methodFilter,
                containingClass, methodNames);
    }

    /** {@code fields} without the ones {@code containingClass} has declared hidden. */
    public static Field[] filterFields(Class<?> containingClass, Field[] fields) {
        if (Reflection.fieldFilter.isEmpty()) {
            return fields;
        }
        return (Field[]) Reflection.filter(fields, Reflection.fieldFilter.get(containingClass));
    }

    /** {@code methods} without the ones {@code containingClass} has declared hidden. */
    public static Method[] filterMethods(Class<?> containingClass, Method[] methods) {
        if (Reflection.methodFilter.isEmpty()) {
            return methods;
        }
        return (Method[]) Reflection.filter(methods,
                Reflection.methodFilter.get(containingClass));
    }

    // ---- the internal part ----

    static boolean isSubclassOf(Class<?> queryClass, Class<?> ofClass) {
        Class<?> c = queryClass;
        while (c != null) {
            if (c == ofClass) {
                return true;
            }
            c = c.getSuperclass();
        }
        return false;
    }

    // Two classes are in the same package only if the same loader also loaded them: two packages of
    // the same name from two different loaders are different packages, and confusing them would be
    // precisely the way of sneaking into somebody else's.
    private static boolean sameClassPackage(Class<?> c1, Class<?> c2) {
        if (c1.getClassLoader() != c2.getClassLoader()) {
            return false;
        }
        return Objects.equals(c1.getPackageName(), c2.getPackageName());
    }

    private static Map<Class<?>, Set<String>> register(Map<Class<?>, Set<String>> map,
                                                        Class<?> c, Set<String> names) {
        if (map.get(c) != null) {
            throw new IllegalArgumentException("Filter already registered: " + c);
        }
        Map<Class<?>, Set<String>> fresh = new HashMap<Class<?>, Set<String>>(map);
        fresh.put(c, names);
        return fresh;
    }

    private static Member[] filter(Member[] members, Set<String> hidden) {
        if (hidden == null || members.length == 0) {
            return members;
        }
        // The output array has to be of the type of the input one -- `Field[]` or `Method[]` -- and
        // the only place to get it from without an extra `Class` parameter is an element.
        Class<?> type = members[0].getClass();
        if (hidden.contains(Reflection.WILDCARD)) {
            return (Member[]) Array.newInstance(type, 0);
        }
        int howMany = 0;
        int i = 0;
        while (i < members.length) {
            if (!hidden.contains(members[i].getName())) {
                howMany = howMany + 1;
            }
            i = i + 1;
        }
        Member[] out = (Member[]) Array.newInstance(type, howMany);
        int target = 0;
        i = 0;
        while (i < members.length) {
            if (!hidden.contains(members[i].getName())) {
                out[target] = members[i];
                target = target + 1;
            }
            i = i + 1;
        }
        return out;
    }

    /**
     * The class that called the method that calls this one.
     *
     * <p>It is an intrinsic of the VM and not a native of the bridge: one has to look at <b>the
     * frames</b>, and the bridge only sees the metaspace and the heap. It returns null if there is
     * not that much stack -- calling it from the entry method is legitimate and the right answer
     * there is "nobody".
     *
     * <p><b>Difference noted with the JDK</b>: over there this method <b>throws</b> {@code
     * InternalError} if the method that calls it is not marked with {@link CallerSensitive}. Here
     * it does not. It is not a guard that is missing, it is one that is not needed: the JDK needs
     * it because its machinery of reflection <b>interposes generated frames</b> between the real
     * caller and the method, and its implementation counts a fixed depth; the mark is what tells
     * the runtime not to interpose them. This VM interposes none --{@code Method.invoke} is an
     * intrinsic that pushes the frame of the destination and nothing else-- so the walk gives the
     * right caller whether they are marked or not.
     *
     * <p>Marking the method that uses it is still the right thing: it is what documents that its
     * answer depends on the stack, and it is what {@link #isCallerSensitive} reads.
     */
    public static native Class<?> getCallerClass();

    /**
     * The <b>raw</b> {@code access_flags} of the class file.
     *
     * <p>It is not the same as {@code Class.getModifiers()}: for a nested class, that one returns
     * the modifiers the {@code InnerClasses} attribute declares --which is where the {@code
     * private} of an inner class lives-- and this one those of the header, where that {@code
     * private} cannot be represented. In order to decide accesses the raw ones are needed.
     */
    public static native int getClassAccessFlags(Class<?> c);

    /**
     * Whether that method is marked with {@link CallerSensitive}.
     *
     * <p>It is read from the {@code .class} and not from a list: the mark travels with the method,
     * so a method sensitive to the caller of a third-party library is recognised just like one of
     * the JDK.
     */
    public static boolean isCallerSensitive(Method m) {
        return m.getAnnotation(CallerSensitive.class) != null;
    }

    /**
     * Whether that field is a {@code final} one can <b>trust</b>: one that not even reflection with
     * {@code setAccessible} can write.
     *
     * <p>There are two: the fields of a {@code record} and those of a hidden class. In both cases
     * the immutability is part of the contract of the type --the {@code equals} of a record and the
     * taking apart of a lambda depend on it-- so letting them be written would break invariants the
     * compiler has already taken for granted when optimising.
     *
     * <p>A {@code static} field never is: the static ones are written in the {@code <clinit>}, and
     * there the writing is legitimate.
     */
    public static boolean isTrustedFinalField(Field f) {
        if (!Modifier.isFinal(f.getModifiers()) || Modifier.isStatic(f.getModifiers())) {
            return false;
        }
        Class<?> owner = f.getDeclaringClass();
        return owner.isRecord() || owner.isHidden();
    }

    /**
     * It checks that that module has native access enabled.
     *
     * <p>It is the hook the restricted methods of {@code java.lang.foreign} call: in the JDK it
     * throws {@code IllegalCallerException} if the module of the caller did not start with {@code
     * --enable-native-access}.
     *
     * <p><b>Here it never throws</b>, and one has to say why that is not a shortcut. The flag
     * exists so that an application can decide <b>which modules</b> may call native code; this
     * library has no system of modules, so there is nobody to ask and nobody to refuse. Simulating
     * a refusal would be inventing a policy; simulating an approval --which is what the JDK does
     * when the access is enabled-- is the only answer that corresponds to the real state of the
     * runtime.
     */
    public static void ensureNativeAccess(Class<?> currentClass, Class<?> owner, String methodName,
            boolean jni) {
    }
}
