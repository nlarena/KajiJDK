package javax.management;

/**
 * Implemented by everything that carries a {@link Descriptor}.
 *
 * <p>It exists so that code that only wants to read metadata does not have to know which of the
 * {@code MBean*Info} classes it has in front of it.
 */
public interface DescriptorRead {

    /**
     * The descriptor. Never {@code null}: if there are no fields, it returns an empty one.
     *
     * <p>It is a copy, because the descriptor of an {@code MBeanInfo} is immutable in fact even
     * though its declared type is not.
     */
    Descriptor getDescriptor();
}
