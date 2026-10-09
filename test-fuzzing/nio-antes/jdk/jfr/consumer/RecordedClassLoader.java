package jdk.jfr.consumer;

import java.util.List;

import jdk.jfr.ValueDescriptor;

/**
 * A class loader, as it was recorded.
 *
 * <p>{@link #getType} returns the class <strong>of the loader</strong>, not what it loaded. It is
 * the distinction one has to keep in mind when reading this: a {@code RecordedClassLoader} of a web
 * application says that it is a {@code WebAppClassLoader}, and the classes it loaded are in the
 * events that reference it.
 *
 * @since 9
 */
public final class RecordedClassLoader extends RecordedObject {

    RecordedClassLoader(List<ValueDescriptor> descriptors, Object[] values) {
        super(descriptors, values);
    }

    /**
     * The class of the loader.
     *
     * @return the class, or {@code null} if it is the bootstrap loader
     */
    public RecordedClass getType() {
        return getClass("type");
    }

    /**
     * The name of the loader.
     *
     * @return the name, or {@code null} if it has none
     */
    public String getName() {
        return getString("name");
    }

    /**
     * The identifier the VM that recorded gave this loader.
     *
     * @return the identifier
     */
    public long getId() {
        return getLong("id");
    }
}
