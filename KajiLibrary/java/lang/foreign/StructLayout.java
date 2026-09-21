package java.lang.foreign;

/**
 * KajiLibrary's java.lang.foreign.StructLayout -- the members **one after the other**.
 *
 * <p>Each member's offset is the sum of the sizes of the previous ones, and there lies the most
 * surprising rule: **each member has to fall at an offset that is a multiple of its own
 * alignment**, or the construction fails. A `long` after an `int` does not form a struct; a
 * `MemoryLayout.paddingLayout(4)` has to go in between.
 *
 * <p>That it fails instead of arranging things is deliberate. A struct that inserts padding by
 * itself describes a different thing depending on the platform, and this package exists to describe
 * memory exactly.
 */
public interface StructLayout extends GroupLayout {

    StructLayout withName(String name);

    StructLayout withoutName();

    StructLayout withByteAlignment(long byteAlignment);
}
