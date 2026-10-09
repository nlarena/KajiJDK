package java.nio.file.attribute;

// A loose attribute --name and value-- handed to `Files.createFile` and company so it is set **at
// the moment of creation**, atomically.
//
// KajiJDK accepts the type but cannot honour any attribute: `jdk.internal.io.Fs`'s natives create
// files and directories with no permission parameter. The methods that take `FileAttribute<?>...`
// document that a non-empty array ends in `UnsupportedOperationException` -- which is exactly what
// the spec requires when the attribute cannot be set, so the lie never comes into existence.
//
// @param <T> the type of the attribute's value
public interface FileAttribute<T> {

    /** The attribute's name, in the form `"view:attribute"`. */
    String name();

    /** The value. */
    T value();
}
