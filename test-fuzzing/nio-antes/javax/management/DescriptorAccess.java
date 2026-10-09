package javax.management;

/**
 * Reading and writing the descriptor.
 *
 * <p>Only the pieces of {@code javax.management.modelmbean} implement it, which are the ones
 * configured at run time. The {@code MBean*Info} of this package are immutable and that is why
 * they stay at {@link DescriptorRead}.
 */
public interface DescriptorAccess extends DescriptorRead {

    /** Replaces the whole descriptor. */
    void setDescriptor(Descriptor inDescriptor);
}
