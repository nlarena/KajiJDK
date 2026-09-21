package java.lang.foreign;

import java.util.List;

/**
 * KajiLibrary's java.lang.foreign.GroupLayout -- several layouts together.
 *
 * <p>What a `struct` and a `union` have in common; what tells them apart is **where each member
 * starts**, and that is decided by {@link StructLayout} and {@link UnionLayout}. This interface only
 * says that there are members.
 */
public interface GroupLayout extends MemoryLayout {

    /** The members, in order. */
    List<MemoryLayout> memberLayouts();

    GroupLayout withName(String name);

    GroupLayout withoutName();

    GroupLayout withByteAlignment(long byteAlignment);
}
