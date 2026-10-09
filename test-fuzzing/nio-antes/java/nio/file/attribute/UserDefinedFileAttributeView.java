package java.nio.file.attribute;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

// The `"user"` view: extended attributes, name-bytes pairs the user hangs off the file and the
// system does not interpret.
//
// `read` and `write` take a `ByteBuffer` rather than a `byte[]` because the value can be large and
// that way the caller chooses where the buffer lives; the value is read or written **whole at
// once**, there is no position within the attribute.
//
// Without an implementation in KajiJDK: there is no native for extended attributes.
public interface UserDefinedFileAttributeView extends FileAttributeView {

    /** Always `"user"`. */
    String name();

    /** The names of the attributes the file has. */
    List<String> list() throws IOException;

    /** The size in bytes of the attribute `name`. */
    int size(String name) throws IOException;

    /** It copies `name`'s value into `dst`; it returns how many bytes it copied. */
    int read(String name, ByteBuffer dst) throws IOException;

    /** It writes `name`'s value from `src`; it returns how many bytes it wrote. */
    int write(String name, ByteBuffer src) throws IOException;

    /** It deletes the attribute `name`. */
    void delete(String name) throws IOException;
}
