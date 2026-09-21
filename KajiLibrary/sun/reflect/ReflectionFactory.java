package sun.reflect;

import java.io.Externalizable;
import java.io.ObjectStreamField;
import java.io.OptionalDataException;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * The serialisation hooks a library that serialises on its own needs.
 *
 * <p>It exists for one single kind of client: the one that reimplements the format of {@code
 * ObjectOutputStream} --a CORBA ORB, a persistence framework-- and needs to do the same things the
 * JDK does and that **no** public API allows: building an object without running its constructor,
 * calling a private {@code readObject}, knowing whether a class has a static initialiser.
 *
 * <p>That is why the package is called `sun.` and is still exported: it is not API for anybody, but
 * taking it away would break those clients, which have no substitute.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Most of this class **cannot be implemented from Java**. The three holes:
 *
 * <ul>
 *   <li>The `newConstructorForSerialization` manufacture a {@link Constructor} that, when invoked,
 *       allocates an instance of one class and runs the constructor of **another**. The VM does
 *       that with a generated accessor; there is no way of expressing it in Java.
 *   <li>The ones that return {@link MethodHandle} need a lookup with private access over somebody
 *       else's class ({@code MethodHandles.privateLookupIn}), which is precisely the permission the
 *       language does not give.
 *   <li>{@link #hasStaticInitializerForSerialization} asks whether a class has a {@code <clinit>},
 *       which reflection does not expose: {@code <clinit>} is not a {@link
 *       java.lang.reflect.Method}.
 * </ul>
 *
 * <p>Those throw {@link UnsupportedOperationException} with the reason, and so does
 * {@link #newOptionalDataExceptionForSerialization}, for a fourth reason given in its own javadoc.
 * The note said "those ten"; they are eleven. Returning `null` --which is what the JDK returns when
 * the class really does not have the method-- would be worse: the client would read it as "this
 * class does not define `readObject`" and go on serialising wrongly, instead of finding out.
 *
 * <p>The three that can be done are done for real: {@link #getReflectionFactory},
 * {@link #newConstructorForExternalization} --which is only looking up the public constructor with
 * no arguments-- and {@link #serialPersistentFields}, which is reading a static field.
 */
public class ReflectionFactory {

    private static final ReflectionFactory soleInstance = new ReflectionFactory();

    /**
     * The inner one, the one that does the work in the JDK. Here it does not have the serialisation
     * methods --see the note of the class-- but the field is kept because it is where the bridge
     * would go the day it has them.
     */
    private static final jdk.internal.reflect.ReflectionFactory delegate =
            jdk.internal.reflect.ReflectionFactory.getReflectionFactory();

    private ReflectionFactory() {
    }

    /** The only instance. */
    public static ReflectionFactory getReflectionFactory() {
        return soleInstance;
    }

    /**
     * A constructor that allocates an instance of `cl` and runs the body of `constructorToCall`.
     *
     * <p><b>Not implemented.</b> See the note of the class.
     *
     * @throws UnsupportedOperationException always, in this library
     */
    public Constructor<?> newConstructorForSerialization(Class<?> cl,
            Constructor<?> constructorToCall) {
        throw new UnsupportedOperationException(
                "cannot synthesize a serialization constructor for " + nameOf(cl)
                + ": allocating an instance without running its constructor needs VM support");
    }

    /**
     * Like {@link #newConstructorForSerialization(Class, Constructor)}, taking the constructor with
     * no arguments of the non-serialisable superclass.
     *
     * <p><b>Not implemented.</b> See the note of the class.
     *
     * @throws UnsupportedOperationException always, in this library
     */
    public final Constructor<?> newConstructorForSerialization(Class<?> cl) {
        throw new UnsupportedOperationException(
                "cannot synthesize a serialization constructor for " + nameOf(cl)
                + ": allocating an instance without running its constructor needs VM support");
    }

    /**
     * The public constructor with no arguments {@link Externalizable} demands.
     *
     * <p>This one is done: there is nothing magic about it, it is the constructor the class itself
     * declares. The JDK returns it accessible even if the class is not, and so does this.
     *
     * @return the constructor, or `null` if the class does not have it
     * @throws NullPointerException if `cl` is null
     */
    public final Constructor<?> newConstructorForExternalization(Class<?> cl) {
        if (cl == null) {
            throw new NullPointerException("cl");
        }
        if (!Externalizable.class.isAssignableFrom(cl)) {
            return null;
        }
        try {
            Constructor<?> c = cl.getDeclaredConstructor();
            c.setAccessible(true);
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * A handle to the private {@code readObject} of `cl`.
     *
     * <p><b>Not implemented.</b> See the note of the class.
     *
     * @throws UnsupportedOperationException always, in this library
     */
    public final MethodHandle readObjectForSerialization(Class<?> cl) {
        throw noHandle("readObject", cl);
    }

    /**
     * A handle to the private {@code readObjectNoData} of `cl`.
     *
     * <p><b>Not implemented.</b> See the note of the class.
     *
     * @throws UnsupportedOperationException always, in this library
     */
    public final MethodHandle readObjectNoDataForSerialization(Class<?> cl) {
        throw noHandle("readObjectNoData", cl);
    }

    /**
     * A handle to what reads the default fields of `cl` from the stream.
     *
     * <p><b>Not implemented.</b> See the note of the class.
     *
     * @throws UnsupportedOperationException always, in this library
     */
    public final MethodHandle defaultReadObjectForSerialization(Class<?> cl) {
        throw noHandle("defaultReadObject", cl);
    }

    /**
     * A handle to the private {@code writeObject} of `cl`.
     *
     * <p><b>Not implemented.</b> See the note of the class.
     *
     * @throws UnsupportedOperationException always, in this library
     */
    public final MethodHandle writeObjectForSerialization(Class<?> cl) {
        throw noHandle("writeObject", cl);
    }

    /**
     * A handle to what writes the default fields of `cl` to the stream.
     *
     * <p><b>Not implemented.</b> See the note of the class.
     *
     * @throws UnsupportedOperationException always, in this library
     */
    public final MethodHandle defaultWriteObjectForSerialization(Class<?> cl) {
        throw noHandle("defaultWriteObject", cl);
    }

    /**
     * A handle to the {@code readResolve} of `cl`.
     *
     * <p><b>Not implemented.</b> See the note of the class.
     *
     * @throws UnsupportedOperationException always, in this library
     */
    public final MethodHandle readResolveForSerialization(Class<?> cl) {
        throw noHandle("readResolve", cl);
    }

    /**
     * A handle to the {@code writeReplace} of `cl`.
     *
     * <p><b>Not implemented.</b> See the note of the class.
     *
     * @throws UnsupportedOperationException always, in this library
     */
    public final MethodHandle writeReplaceForSerialization(Class<?> cl) {
        throw noHandle("writeReplace", cl);
    }

    /**
     * Whether `cl` has a static initialiser.
     *
     * <p><b>Not implemented.</b> {@code <clinit>} is not a {@link java.lang.reflect.Method} and
     * reflection does not list it. Returning `false` would be a concrete and wrong answer: the one
     * who asks uses it to decide whether deserialisation has to force the initialisation of the
     * class, and answering no when it is yes leaves it half initialised.
     *
     * @throws UnsupportedOperationException always, in this library
     */
    public final boolean hasStaticInitializerForSerialization(Class<?> cl) {
        throw new UnsupportedOperationException(
                "cannot tell whether " + nameOf(cl) + " has a static initializer: <clinit> is not "
                + "reachable through java.lang.reflect");
    }

    /**
     * An {@link OptionalDataException} with the end-of-data flag set.
     *
     * <p><b>Not implemented.</b> The two constructors of `OptionalDataException` are
     * package-private --on purpose: only the serialisation machinery has any reason to manufacture
     * it-- and from `sun.reflect` they cannot be seen.
     *
     * @throws UnsupportedOperationException always, in this library
     */
    public final OptionalDataException newOptionalDataExceptionForSerialization(boolean bool) {
        throw new UnsupportedOperationException(
                "cannot build an OptionalDataException: its constructors are package-private to "
                + "java.io");
    }

    /**
     * The {@code serialPersistentFields} `cl` declares, or `null` if it declares none.
     *
     * <p>This one is done: it is reading a static field. It only counts if it is declared as the
     * specification says --{@code private static final ObjectStreamField[]}-- because a field with
     * that name and other modifiers is not the contract and the serialisation of the JDK does not
     * look at it either.
     *
     * @throws NullPointerException if `cl` is null
     */
    public final ObjectStreamField[] serialPersistentFields(Class<?> cl) {
        if (cl == null) {
            throw new NullPointerException("cl");
        }
        try {
            Field f = cl.getDeclaredField("serialPersistentFields");
            int m = f.getModifiers();
            if (!Modifier.isPrivate(m) || !Modifier.isStatic(m) || !Modifier.isFinal(m)) {
                return null;
            }
            if (f.getType() != ObjectStreamField[].class) {
                return null;
            }
            f.setAccessible(true);
            ObjectStreamField[] fields = (ObjectStreamField[]) f.get(null);
            return fields == null ? null : fields.clone();
        } catch (Exception e) {
            return null;
        }
    }

    /** The message the seven that return a handle share. */
    private static UnsupportedOperationException noHandle(String method, Class<?> cl) {
        return new UnsupportedOperationException(
                "cannot bind a MethodHandle to " + nameOf(cl) + "." + method
                + ": a private lookup into another class is not available in this library");
    }

    /** The name of the class, tolerating null: this is for an error message. */
    private static String nameOf(Class<?> cl) {
        return cl == null ? "null" : cl.getName();
    }
}
