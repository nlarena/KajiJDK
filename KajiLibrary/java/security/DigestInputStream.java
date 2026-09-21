package java.security;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

// A stream that passes on to the digest everything that is read from it.
//
// It serves for hashing something without reading it twice and without having it whole in memory:
// it is read normally, and when it finishes the digest is computed already. The trap is that **only
// what was read counts**: if the consumer stops before end of file, or does a `skip`, the digest is
// that of the prefix that went through here, not that of the file. This class cannot detect the
// difference, and comparing that digest against that of the complete file gives a different result
// without anything having failed.
//
// `skip()` is not overridden —it is inherited from `FilterInputStream`, which reads and discards—
// so the skipped bytes **do** enter the digest. It is the behaviour of the JDK and it is the least
// surprising: skipping should not change the hash of the content walked.
//
// Both `read`s declare `throws IOException`, just as in the JDK. Until recently they could not: the
// `java.io.FilterInputStream` of this library did not declare it in its own and Java forbids a
// subclass to declare **more** checked exceptions than the method it overrides. Now that java.io
// has aligned its signatures, the difference has disappeared.
public class DigestInputStream extends FilterInputStream {

    // Protected, as in the JDK: a subclass may need to touch it.
    protected MessageDigest digest;

    // Whether the digest is listening. It starts on.
    private boolean on = true;

    public DigestInputStream(InputStream stream, MessageDigest digest) {
        super(stream);
        this.setMessageDigest(digest);
    }

    public MessageDigest getMessageDigest() {
        return this.digest;
    }

    // Changing the digest halfway is legal and sometimes it is the point: a header is read with one
    // and the body with another.
    public void setMessageDigest(MessageDigest digest) {
        this.digest = digest;
    }

    @Override
    public int read() throws IOException {
        int ch = this.in.read();
        if (this.on && ch != -1) {
            this.digest.update((byte) ch);
        }
        return ch;
    }

    // It feeds only the bytes that were **really** read, not `len`.
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int result = this.in.read(b, off, len);
        if (this.on && result > 0) {
            this.digest.update(b, off, result);
        }
        return result;
    }

    // It turns the feeding of the digest on or off.
    //
    // It is what allows a piece of the stream to be hashed and not another: for example, skipping a
    // signature field that is embedded in the same file that is being verified.
    public void on(boolean on) {
        this.on = on;
    }

    @Override
    public String toString() {
        return "[Digest Input Stream] " + this.digest.toString();
    }
}
