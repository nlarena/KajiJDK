package java.lang.foreign;

/**
 * KajiLibrary's java.lang.foreign.PaddingLayout -- space taken up that carries nothing.
 *
 * <p>It exists so the gaps a C compiler would insert by itself between a `struct`'s fields can be
 * written by hand. That they have to be written is the package's design decision: a layout that
 * arranges itself would describe a different thing on each platform, and all of this exists to
 * describe memory exactly.
 *
 * <p>Its alignment is 1 and cannot meaningfully be changed to anything greater: a constraint on
 * where nothing may start constrains nothing.
 */
public interface PaddingLayout extends MemoryLayout {

    PaddingLayout withName(String name);

    PaddingLayout withoutName();

    PaddingLayout withByteAlignment(long byteAlignment);
}
