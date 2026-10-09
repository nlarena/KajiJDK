package jdk.internal.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * KajiLibrary's jdk.internal.reflect.ReflectionFactory -- the factory of the three accessors.
 *
 * <h2>What there is and why it is enough</h2>
 *
 * <p>The three {@code new*Accessor} are there, and they return accessors that
 * <strong>work</strong>: each one delegates to the reflective machinery this VM already has (the
 * intrinsics {@code Intrinsic::MethodInvoke} and {@code Intrinsic::ConstructorNewInstance} of the
 * interpreter, and the native seams of {@link Field}). They are not a second plumbing parallel to
 * the one that works: they are the way that plumbing lets itself be named from an interface.
 *
 * <p>{@link #newInstance(Constructor, Object[], Class)} is there for the same reason, and it is the
 * only member of this class the JDK writes in terms of the accessors instead of the other way
 * round.
 *
 * <h2>What is not there, and why it cannot be</h2>
 *
 * <p>There are three blockages, and none of them is fixed by writing more Java here.
 *
 * <ul>
 * <li><strong>The four copies</strong> --{@code copyMethod}, {@code leafCopyMethod}, {@code
 *     copyField}, {@code copyConstructor}-- and the two raw accesses ({@code
 *     getExecutableTypeAnnotationBytes}, {@code getExecutableSharedParameterTypes}). The JDK writes
 *     them with <em>package-private</em> members of {@code java.lang.reflect} ({@code
 *     Method.copy()}, {@code Executable.getSharedParameterTypes()}), which only exist because that
 *     factory and those classes are on the same side of the border of the package. From here there
 *     is no public way of manufacturing a {@link Method} nor a {@link Field}: the VM builds them.
 *     Bringing them in would not be implementing this class but changing {@code
 *     java.lang.reflect}.</li>
 * <li><strong>The whole half of serialisation</strong> --the seven {@code *ForSerialization} that
 *     return {@link java.lang.invoke.MethodHandle}, the three
 *     {@code newConstructorFor(Externalization|Serialization)},
 *     {@code hasStaticInitializerForSerialization},
 *     {@code newOptionalDataExceptionForSerialization} and {@code serialPersistentFields}--. It
 *     needs two things that are not there: a {@code MethodHandles.Lookup} with private access over
 *     somebody else's class, which is a privilege of the VM and not a method, and
 *     {@code java.io.ObjectStreamField}, which is not in this library. All of them
 *     <em>manufacture</em> something, so there is no honest version that returns less: either they
 *     build the handle or they lie.</li>
 * <li><strong>{@code parseAccessFlags(int, AccessFlag.Location, Class)}</strong>. The form of two
 *     arguments exists ({@code AccessFlag.maskToAccessFlags}), but the third one is not decorative:
 *     it is what disambiguates the bits that mean different things according to which class carries
 *     them. Delegating to the one of two and throwing the class away would give the right answer
 *     almost always, and "almost always" is exactly the kind of member that is not written.</li>
 * </ul>
 *
 * <p>The criterion is the one of the house: a member that is missing is a legal subset and does not
 * compile on the other side; one that lies compiles and blows up later.
 */
public class ReflectionFactory {

    // The JDK hides it as well: nobody manufactures a factory, one asks for the one there is. Being
    // `private` is what makes `getReflectionFactory()` able to promise identity.
    private static final ReflectionFactory THE_ONLY_ONE = new ReflectionFactory();

    private ReflectionFactory() {
    }

    /** The factory of the process. Always the same instance. */
    public static ReflectionFactory getReflectionFactory() {
        return ReflectionFactory.THE_ONLY_ONE;
    }

    /**
     * An accessor for {@code field}.
     *
     * @param field the field
     * @param override whether whoever asks for it has already suppressed the control of access;
     *                 with {@code false} a {@code final} field comes out read-only and its writers
     *                 throw
     * @return the accessor
     */
    public FieldAccessor newFieldAccessor(Field field, boolean override) {
        return new FieldAccessorImpl(field, override);
    }

    /**
     * An accessor for {@code method}.
     *
     * @param method the method
     * @param callerSensitive whether the destination looks at who called it. The accessor does not
     *                        change shape because of it: this VM interposes no frame when invoking
     *                        reflectively, so the destination sees the real caller either way. The
     *                        note used to say that the hook did not exist; it does, see
     *                        {@link Reflection#getCallerClass()}.
     * @return the accessor
     */
    public MethodAccessor newMethodAccessor(Method method, boolean callerSensitive) {
        return new MethodAccessorImpl(method);
    }

    /**
     * An accessor for {@code c}.
     *
     * @param c the constructor
     * @return the accessor
     */
    public ConstructorAccessor newConstructorAccessor(Constructor<?> c) {
        return new ConstructorAccessorImpl(c);
    }

    /**
     * It builds an instance with {@code ctor}, saying who calls.
     *
     * <p>The {@code caller} is for the check of access, which in the JDK is done here and not in
     * the accessor. This VM does not do it on either of the two sides --{@code
     * Constructor.newInstance} does not do it either--, so the argument is accepted and changes
     * nothing; it is the same situation as the {@code caller} of {@link
     * MethodAccessor#invoke(Object, Object[], Class)}.
     *
     * @param <T> the type built
     * @param ctor the constructor
     * @param args the arguments
     * @param caller whoever says they are building
     * @return the instance
     * @throws IllegalAccessException if the check of access were to fail
     * @throws InstantiationException if the class cannot be instantiated
     * @throws InvocationTargetException wrapping whatever the constructor threw
     */
    public <T> T newInstance(Constructor<T> ctor, Object[] args, Class<?> caller)
            throws IllegalAccessException, InstantiationException, InvocationTargetException {
        return (T) this.newConstructorAccessor(ctor).newInstance(args);
    }
}
