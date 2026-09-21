package jdk.incubator.vector;

/**
 * The message shared by the operations that need the VM's intrinsics. It is not API.
 *
 * <p>It is in a single place and not repeated in each class for a concrete reason: the classes of
 * this package form a hierarchy --{@code Vector}, {@code AbstractVector}, {@code IntVector}-- and a
 * constant with the same name at several levels hides the others, which is exactly what is not
 * wanted of a text that has to be identical everywhere.
 */
final class Msg {

    /** Why a vector operation cannot work in this library. */
    static final String NOT_THERE =
            "the vector API relies on VM intrinsics --each operation is replaced by a vector "
            + "machine instruction-- and this VM lacks them; without them there is no "
            + "way to create or operate on a vector";

    private Msg() {
    }
}
