package java.security;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

// The mirror of `DigestInputStream`: it hashes everything that is written as it is written.
//
// Careful with a detail inherited from `FilterOutputStream` that matters here more than there: the
// three-argument version of `write` **is** overridden, so a `write(byte[])` —which
// `FilterOutputStream` implements by writing byte by byte in some versions— neither duplicates nor
// skips anything. Both roads feed the digest exactly once per byte.
//
// Both `write`s declare `throws IOException`, just as in the JDK and for the same reason explained
// in `DigestInputStream`: the restriction that prevented it was of `java.io.FilterOutputStream` and
// is no longer there.
public class DigestOutputStream extends FilterOutputStream {

    protected MessageDigest digest;

    private boolean on = true;

    public DigestOutputStream(OutputStream stream, MessageDigest digest) {
        super(stream);
        this.setMessageDigest(digest);
    }

    public MessageDigest getMessageDigest() {
        return this.digest;
    }

    public void setMessageDigest(MessageDigest digest) {
        this.digest = digest;
    }

    @Override
    public void write(int b) throws IOException {
        this.out.write(b);
        if (this.on) {
            this.digest.update((byte) b);
        }
    }

    // It is written first and hashed afterwards: if the writing fails, what did not reach the
    // destination does not enter the digest either.
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        this.out.write(b, off, len);
        if (this.on) {
            this.digest.update(b, off, len);
        }
    }

    public void on(boolean on) {
        this.on = on;
    }

    @Override
    public String toString() {
        return "[Digest Output Stream] " + this.digest.toString();
    }
}
