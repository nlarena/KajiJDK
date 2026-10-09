package java.lang.classfile;

import java.lang.reflect.AccessFlag;
import java.lang.reflect.AccessFlag.Location;
import java.util.Set;

// The `access_flags` mask of a class, a field or a method (JVMS §4.1, §4.5, §4.6), together with the
// location that gives each bit its meaning: the same `0x0020` is `ACC_SUPER` on a class and
// `ACC_SYNCHRONIZED` on a method, so without the location the mask cannot be read.
public interface AccessFlags extends ClassElement, MethodElement, FieldElement {

    /** The raw mask. */
    int flagsMask();

    /** The flags that are set, already interpreted for this location. */
    Set<AccessFlag> flags();

    /** Where this mask lives. */
    Location location();

    /** Whether `flag` is set. It throws `IllegalArgumentException` if `flag` is no good here. */
    boolean has(AccessFlag flag);
}
