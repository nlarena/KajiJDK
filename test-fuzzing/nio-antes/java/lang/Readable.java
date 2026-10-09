package java.lang;

import java.io.IOException;
import java.nio.CharBuffer;

/**
 * KajiLibrary's java.lang.Readable — a source of characters that can fill a buffer. It is
 * the narrowest possible reading contract: one method, no positioning, no closing. That is
 * what lets a scanner accept anything able to produce characters without caring where they
 * come from.
 */
public interface Readable {

    // Reads characters into `cb`, up to its remaining capacity.
    //
    // Returns how many were read, or -1 at end of input. ZERO is possible — the buffer had
    // no room — so a caller must not read zero as end of input.
    int read(CharBuffer cb) throws IOException;
}
