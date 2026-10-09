package java.security;

import java.nio.ByteBuffer;

// A cryptographic hash function, with the factory that looks for it among the providers.
//
// ===============================================================================================
// WHAT IS REAL HERE, AND WHAT IS NOT
// ===============================================================================================
//
// This is the only factory of `java.security` in KajiLibrary that has **real** algorithms behind
// it. `KajiProvider` registers six, written from scratch in this library and checked byte by byte
// against those of JDK 25 and against the vectors of the specifications (RFC 1321 for MD5, FIPS
// 180-4 for the SHA family):
//
//     MD5, SHA-1, SHA-224, SHA-256, SHA-384, SHA-512
//
// Everything else asked of `getInstance` throws `NoSuchAlgorithmException`, which is the right
// answer: there is no provider that offers it. In particular there is **no** SHA-3 and no SHAKE —
// the Keccak permutation is another whole algorithm and was not written.
//
// MD5 and SHA-1 are **broken for cryptographic use** and are registered all the same. It is not a
// contradiction with the rule of not lying: the class does not promise that the algorithm is
// secure, it promises that it returns the digest the specification defines, and that it fulfils
// exactly. They are still needed for reading old formats, checksums and inherited HMACs, and
// omitting them makes nobody safe: the only thing it does is make whoever needs them write a worse
// version.
//
// What is not there: `getInstance` does not read `java.security` from disk and does not discover
// providers through `ServiceLoader`; the list of providers is the one `Security` has in memory.
public abstract class MessageDigest extends MessageDigestSpi {

    private final String algorithm;

    // Where this instance came from. The factory sets it; a subclass built by hand has it null
    // until somebody registers it.
    private Provider provider;

    // Whether any byte has gone in since the last `digest()` or `reset()`. Only `toString` uses
    // it.
    private boolean inProgress;

    protected MessageDigest(String algorithm) {
        this.algorithm = algorithm;
    }

    // A digest of the algorithm asked for, of the first provider that offers it.
    //
    // The order matters and it is that of `Security.getProviders()`: the first wins, and that is
    // why inserting a provider at position 1 is enough to replace an algorithm in the whole
    // process.
    public static MessageDigest getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService("MessageDigest", algorithm);
            if (s != null) {
                return build(s, algorithm);
            }
            i = i + 1;
        }
        throw new NoSuchAlgorithmException(algorithm + " MessageDigest not available");
    }

    public static MessageDigest getInstance(String algorithm, String provider)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("missing provider");
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("no such provider: " + provider);
        }
        return getInstance(algorithm, p);
    }

    public static MessageDigest getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider.Service s = provider.getService("MessageDigest", algorithm);
        if (s == null) {
            throw new NoSuchAlgorithmException(
                "no such algorithm: " + algorithm + " for provider " + provider.getName());
        }
        return build(s, algorithm);
    }

    // It instantiates the service and wraps it if need be.
    //
    // If what the provider returns is a `MessageDigest` already, it is used as it is; if it is only
    // a `MessageDigestSpi`, it is wrapped in a delegate. Both cases exist because an algorithm that
    // lives inside the library can save itself the extra object.
    private static MessageDigest build(Provider.Service s, String algorithm)
            throws NoSuchAlgorithmException {
        Object o = s.newInstance(null);
        MessageDigest md;
        if (o instanceof MessageDigest) {
            md = (MessageDigest) o;
        } else if (o instanceof MessageDigestSpi) {
            md = new DigestDelegate((MessageDigestSpi) o, algorithm);
        } else {
            throw new NoSuchAlgorithmException(
                "class configured for MessageDigest is not a MessageDigestSpi: " + s.getClassName());
        }
        md.provider = s.getProvider();
        return md;
    }

    // The provider it came from, or null if it was built by hand.
    public final Provider getProvider() {
        return this.provider;
    }

    // Package-private: `Security` and the factories of the package need to be able to set it.
    final void setProvider(Provider p) {
        this.provider = p;
    }

    public void update(byte input) {
        this.engineUpdate(input);
        this.inProgress = true;
    }

    public void update(byte[] input, int offset, int len) {
        if (input == null) {
            throw new IllegalArgumentException("No input buffer given");
        }
        if (input.length - offset < len) {
            throw new IllegalArgumentException("Input buffer too short");
        }
        this.engineUpdate(input, offset, len);
        this.inProgress = true;
    }

    public void update(byte[] input) {
        if (input == null) {
            throw new IllegalArgumentException("No input buffer given");
        }
        this.engineUpdate(input, 0, input.length);
        this.inProgress = true;
    }

    public final void update(ByteBuffer input) {
        if (input == null) {
            throw new NullPointerException();
        }
        this.engineUpdate(input);
        this.inProgress = true;
    }

    // It closes the digest and returns it. After this the object is left reset and ready to be used
    // again — there is no need to build a new one per message.
    public byte[] digest() {
        byte[] result = this.engineDigest();
        this.inProgress = false;
        return result;
    }

    public int digest(byte[] buf, int offset, int len) throws DigestException {
        if (buf == null) {
            throw new IllegalArgumentException("No output buffer given");
        }
        if (buf.length - offset < len) {
            throw new IllegalArgumentException(
                "Output buffer too small for specified offset and length");
        }
        int numBytes = this.engineDigest(buf, offset, len);
        this.inProgress = false;
        return numBytes;
    }

    // It adds `input` and closes, all together. It is the shortcut for the case of a single block
    // of data.
    public byte[] digest(byte[] input) {
        this.update(input);
        return this.digest();
    }

    @Override
    public String toString() {
        String provName = this.provider == null ? "(no provider)" : this.provider.getName();
        String stateText = this.inProgress ? "<in progress>" : "<initialized>";
        return this.algorithm + " Message Digest from " + provName + ", " + stateText + "\n";
    }

    // It compares two digests **in constant time** with respect to which byte they differ at.
    //
    // This method is the reason why `Arrays.equals` is not enough. A normal `equals` stops at the
    // first difference, and that difference of time is measurable: whoever controls one of the two
    // arrays can go on discovering the other byte by byte, with 256 attempts per position instead
    // of 256^n for the total. Here every byte is always walked and it is accumulated with OR.
    //
    // The length does leak —there is no way of not leaking it— but the length of a digest is
    // public.
    public static boolean isEqual(byte[] digestOf, byte[] digestb) {
        if (digestOf == digestb) {
            return true;
        }
        if (digestOf == null || digestb == null) {
            return false;
        }
        int lenA = digestOf.length;
        int lenB = digestb.length;
        if (lenB == 0) {
            return lenA == 0;
        }
        int result = 0;
        result = result | (lenA - lenB);

        // `lenA` is always walked whole. When `i` goes past `lenB` the index collapses to 0 —the
        // sign shift gives 0 instead of 1— so a byte already seen is reread instead of going
        // outside the array, and the loop does not change length according to the data.
        int i = 0;
        while (i < lenA) {
            int indexB = ((i - lenB) >>> 31) * i;
            result = result | (digestOf[i] ^ digestb[indexB]);
            i = i + 1;
        }
        return result == 0;
    }

    public void reset() {
        this.engineReset();
        this.inProgress = false;
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    // The length of the digest in bytes.
    public final int getDigestLength() {
        return this.engineGetDigestLength();
    }

    // A clone with the same intermediate state, if the implementation allows it.
    @Override
    public Object clone() throws CloneNotSupportedException {
        if (this instanceof Cloneable) {
            return super.clone();
        }
        throw new CloneNotSupportedException();
    }
}

// The public face of a spi that is not at the same time a `MessageDigest`.
//
// It forwards each `engineX` to the wrapped spi. It exists only for the case where an external
// provider hands over a bare `MessageDigestSpi`; the digests of this library extend `MessageDigest`
// directly and do not go through here.
final class DigestDelegate extends MessageDigest implements Cloneable {

    private MessageDigestSpi spi;

    DigestDelegate(MessageDigestSpi spi, String algorithm) {
        super(algorithm);
        this.spi = spi;
    }

    @Override
    protected int engineGetDigestLength() {
        return this.spi.engineGetDigestLength();
    }

    @Override
    protected void engineUpdate(byte input) {
        this.spi.engineUpdate(input);
    }

    @Override
    protected void engineUpdate(byte[] input, int offset, int len) {
        this.spi.engineUpdate(input, offset, len);
    }

    @Override
    protected void engineUpdate(ByteBuffer input) {
        this.spi.engineUpdate(input);
    }

    @Override
    protected byte[] engineDigest() {
        return this.spi.engineDigest();
    }

    @Override
    protected int engineDigest(byte[] buf, int offset, int len) throws DigestException {
        return this.spi.engineDigest(buf, offset, len);
    }

    @Override
    protected void engineReset() {
        this.spi.engineReset();
    }

    // The clone has to take **its own** spi: two delegates sharing the spi would be the same digest
    // with two names, which is just the opposite of what is asked when cloning.
    @Override
    public Object clone() throws CloneNotSupportedException {
        DigestDelegate copy = (DigestDelegate) super.clone();
        copy.spi = (MessageDigestSpi) this.spi.clone();
        return copy;
    }
}
