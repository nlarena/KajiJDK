package java.lang.foreign;

/**
 * KajiLibrary's java.lang.foreign.UnionLayout -- the members **overlaid**, all from offset zero.
 *
 * <p>The size is that of the largest and the alignment the strictest. Unlike {@link StructLayout}
 * there is no offset rule to respect: if they all start at zero and the union's alignment is the
 * greatest of theirs, they all fall right by construction.
 */
public interface UnionLayout extends GroupLayout {

    UnionLayout withName(String name);

    UnionLayout withoutName();

    UnionLayout withByteAlignment(long byteAlignment);
}
