package jdk.jfr.consumer;

import java.util.List;

import jdk.jfr.ValueDescriptor;

/**
 * A Java class, as it was recorded.
 *
 * <p>It is not a {@code Class}: the process that reads the recording may not have that class
 * loaded, nor be able to load it. What is left is the name, the modifiers and which loader it came
 * from.
 *
 * <p>{@link #getId} is the identifier the VM that recorded gave the class. It serves for knowing
 * whether two events talk about the same class without comparing names, which is the only thing
 * that works when two different loaders loaded classes of the same name -- the normal case in an
 * application server.
 *
 * @since 9
 */
public final class RecordedClass extends RecordedObject {

    RecordedClass(List<ValueDescriptor> descriptors, Object[] values) {
        super(descriptors, values);
    }

    /**
     * The modifiers of the class, with the format of {@code java.lang.reflect.Modifier}.
     *
     * @return the modifiers
     */
    public int getModifiers() {
        return getInt("modifiers");
    }

    /**
     * The loader it came from.
     *
     * @return the loader, or {@code null} if it is the bootstrap one
     */
    public RecordedClassLoader getClassLoader() {
        return getValue("classLoader");
    }

    /**
     * The complete name of the class.
     *
     * @return the name
     */
    public String getName() {
        return getString("name");
    }

    /**
     * The identifier the VM that recorded gave this class.
     *
     * @return the identifier
     */
    public long getId() {
        return getLong("id");
    }
}
