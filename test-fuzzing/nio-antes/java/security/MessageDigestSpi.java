package java.security;

import java.nio.ByteBuffer;

// The face a provider gives to a digest algorithm.
//
// The separation between this type and `MessageDigest` is the Service Provider Interface pattern,
// and its reason for being is that the two faces change for different reasons: `MessageDigest` is
// what the API promises and cannot be touched, while this is what whoever implements a new
// algorithm writes. A provider that adds a hash writes a subclass of this and never sees the other
// half.
//
// (A historical detail that confuses everybody: `MessageDigest extends MessageDigestSpi`. It is not
// a design error of the library but a deliberate shortcut — it allows an implementation that lives
// inside the JDK to be both things at once and save itself the delegate object. When the spi and
// the public face are different objects, `MessageDigest` uses an internal delegate.)
public abstract class MessageDigestSpi {

    public MessageDigestSpi() {
    }

    // The length of the digest in bytes, or 0 if this implementation does not know it beforehand.
    //
    // It returns 0 and does not throw because this method was added after the class: an old
    // subclass does not write it, and 0 is how "I do not know" is said. Every new implementation
    // should override it.
    protected int engineGetDigestLength() {
        return 0;
    }

    protected abstract void engineUpdate(byte input);

    protected abstract void engineUpdate(byte[] input, int offset, int len);

    // It consumes what is left in the buffer and leaves it positioned at the end.
    //
    // Concrete and not abstract out of compatibility: the array version is enough to fulfil it, and
    // an implementation that knows how to take advantage of a direct buffer overrides it.
    protected void engineUpdate(ByteBuffer input) {
        if (!input.hasRemaining()) {
            return;
        }
        if (input.hasArray()) {
            byte[] b = input.array();
            int ofs = input.arrayOffset();
            int pos = input.position();
            int lim = input.limit();
            this.engineUpdate(b, ofs + pos, lim - pos);
            input.position(lim);
            return;
        }
        int n = input.remaining();
        byte[] tmp = new byte[n < 4096 ? n : 4096];
        while (n > 0) {
            int chunk = n < tmp.length ? n : tmp.length;
            input.get(tmp, 0, chunk);
            this.engineUpdate(tmp, 0, chunk);
            n = n - chunk;
        }
    }

    protected abstract byte[] engineDigest();

    // It writes the digest into `buf` and returns how many bytes it wrote.
    //
    // The base implementation computes the complete digest and copies it: there is no generic way
    // of producing half a digest, and that is why a `len` smaller than the real length is an error
    // and not a silent truncation. A truncated digest without the caller knowing is exactly the
    // kind of datum that is afterwards compared against another and matches when it should not.
    protected int engineDigest(byte[] buf, int offset, int len) throws DigestException {
        byte[] digest = this.engineDigest();
        if (len < digest.length) {
            throw new DigestException("partial digests not returned");
        }
        if (buf.length - offset < digest.length) {
            throw new DigestException("insufficient space in the output buffer to store the digest");
        }
        System.arraycopy(digest, 0, buf, offset, digest.length);
        return digest.length;
    }

    protected abstract void engineReset();

    // It only works if the subclass declares `Cloneable`. Cloning a half-done digest is the only
    // way of getting the hash of a prefix without reading the data again.
    @Override
    public Object clone() throws CloneNotSupportedException {
        if (this instanceof Cloneable) {
            return super.clone();
        }
        throw new CloneNotSupportedException();
    }
}
