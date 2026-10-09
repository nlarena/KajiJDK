package java.nio.file;

// The modes `FileSystemProvider.checkAccess` can be asked about.
//
// **KajiJDK answers two of the three.** `jdk.internal.io.Fs`'s `stat` brings the read and write
// flags but **not** the execute one: there is no execute bit in the native's answer.
// `checkAccess(EXECUTE)` throws `UnsupportedOperationException`. This note used to add that
// `Files.isExecutable` does not exist; it does, and it returns `false` -- which the spec makes the
// answer for "the access cannot be determined" as well as for "it would be denied", so it asserts
// nothing false. The two differ because only `checkAccess`'s signature can say "cannot".
public enum AccessMode {

    /** It can be read. */
    READ,

    /** It can be written. */
    WRITE,

    /** It can be executed. */
    EXECUTE
}
