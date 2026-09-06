import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.CipherSpi;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.Mac;
import javax.crypto.MacSpi;
import javax.crypto.NullCipher;
import javax.crypto.SealedObject;
import javax.crypto.ShortBufferException;

/**
 * Checks {@code javax.crypto} against JDK 25.
 *
 * <h2>What can be compared</h2>
 *
 * <p>Two things. {@link NullCipher}, which is the only cipher both sides have out of the box, and
 * with it the streams and the sealed object; and reading and writing
 * {@link EncryptedPrivateKeyInfo}, which is DER and depends on no algorithm.
 *
 * <p>What is not compared is asking for a real algorithm: the JDK ships providers with AES and this
 * library ships none, on purpose --registering a service that cannot be met would be worse than not
 * having it.
 *
 * <p>The provider machinery is checked apart, in {@link #machinery()}, because the JDK will not let
 * it run: see that method's note.
 *
 * <p>{@link #where()} returns the index of the first answer that differs, or -1.
 */
public class CRY1 {

    static final String[] EXPECTED = {
        "modes|1234123",
        "null-cipher|null|null|1|7|0000000000000000|null|null",
        "null-cipher-toString|Cipher.null, mode: not initialized, algorithm from: (no provider)",
        "update|0102030405|020304",
        "update-array|5|01020304050000000000",
        "doFinal|null|0102030405|0203",
        "doFinal-array|5|00000102030405000000",
        "buf-update|5|01020304050000000000",
        "buf-doFinal|5|01020304050000000000",
        "null-cipher-wrap|UnsupportedOperationException|UnsupportedOperationException|UnsupportedOperationException",
        "maxlen|2147483647|2147483647|null",
        "maxlen-null|NullPointerException",
        "gi-null|NoSuchAlgorithmException",
        "gi-empty|NoSuchAlgorithmException",
        "gi-2-parts|NoSuchAlgorithmException",
        "gi-4-parts|NoSuchAlgorithmException",
        "gi-prov-null|IllegalArgumentException",
        "gi-prov-odd|NoSuchProviderException",
        "output|4142434546",
        "input|65|4|4243454600000000|-1|false",
        "sealed|null|hello sealed",
        "epki|PBEWithMD5AndDES|010203|null|3012300b06092a864886f70d0105030403010203",
        "epki-odd|1.3.6.1.4|090807|300d300606042b0601040403090807",
        "oid|PBEWithMD5AndDES|3010300b06092a864886f70d010503040101|PBEWithMD5AndDES",
        "oid|PBEWithSHA1AndDESede|3011300c060a2a864886f70d010c0103040101|PBEWithSHA1AndDESede",
        "oid|PBEWithSHA1AndRC2_40|3011300c060a2a864886f70d010c0106040101|PBEWithSHA1AndRC2_40",
        "oid|PBEWithSHA1AndRC2_128|3011300c060a2a864886f70d010c0105040101|PBEWithSHA1AndRC2_128",
        "oid|PBEWithSHA1AndRC4_40|3011300c060a2a864886f70d010c0102040101|PBEWithSHA1AndRC4_40",
        "oid|PBEWithSHA1AndRC4_128|3011300c060a2a864886f70d010c0101040101|PBEWithSHA1AndRC4_128",
        "oid|PBES2|3010300b06092a864886f70d01050d040101|PBES2",
        "oid|PBKDF2WithHmacSHA1|3010300b06092a864886f70d01050c040101|PBKDF2WithHmacSHA1",
        "oid|AES|300f300a06086086480165030401040101|AES",
        "oid|AES_128/CBC/NoPadding|3010300b0609608648016503040102040101|AES_128/CBC/NoPadding",
        "oid|AES_256/CBC/NoPadding|3010300b060960864801650304012a040101|AES_256/CBC/NoPadding",
        "oid|DESede|300c300706052b0e030211040101|DESede",
        "oid|Blowfish|3011300c060a2b060104019755010102040101|Blowfish",
        "oid|HmacSHA256|300f300a06082a864886f70d0209040101|HmacSHA256",
        "oid|EC|300e300906072a8648ce3d0201040101|EC",
        "oid|DiffieHellman|3010300b06092a864886f70d010301040101|DiffieHellman",
        "oid|1.3.6.1.4.1.99999|300f300a06082b06010401868d1f040101|1.3.6.1.4.1.99999",
        "epki-odd-name|NoSuchAlgorithmException",
        "epki-null|NullPointerException",
        "epki-empty|IllegalArgumentException",
        "epki-short|EOFException",
        "epki-copy|true",
        "epki-keyspec|InvalidKeySpecException",
        "hierarchy|BadPaddingException,GeneralSecurityException,|GeneralSecurityException,|GeneralSecurityException,",
    };

    /** A toy cipher: every byte against the same mask. */
    public static class XorSpi extends CipherSpi {
        static String seen = "";

        @Override
        protected void engineSetMode(String mode) {
            seen = seen + "mode:" + mode + ";";
        }

        @Override
        protected void engineSetPadding(String padding) {
            seen = seen + "padding:" + padding + ";";
        }

        @Override
        protected int engineGetBlockSize() {
            return 4;
        }

        @Override
        protected int engineGetOutputSize(int inputLen) {
            return inputLen;
        }

        @Override
        protected byte[] engineGetIV() {
            return null;
        }

        @Override
        protected AlgorithmParameters engineGetParameters() {
            return null;
        }

        @Override
        protected void engineInit(int opmode, Key key, SecureRandom random) {
            seen = seen + "init:" + opmode + ";";
        }

        @Override
        protected void engineInit(int opmode, Key key, AlgorithmParameterSpec params,
                SecureRandom random) {
            engineInit(opmode, key, random);
        }

        @Override
        protected void engineInit(int opmode, Key key, AlgorithmParameters params,
                SecureRandom random) {
            engineInit(opmode, key, random);
        }

        @Override
        protected byte[] engineUpdate(byte[] input, int inputOffset, int inputLen) {
            if (input == null) {
                return null;
            }
            final byte[] out = new byte[inputLen];
            for (int i = 0; i < inputLen; i++) {
                out[i] = (byte) (input[inputOffset + i] ^ 0x5a);
            }
            return out;
        }

        @Override
        protected int engineUpdate(byte[] input, int inputOffset, int inputLen, byte[] output,
                int outputOffset) throws ShortBufferException {
            final byte[] r = engineUpdate(input, inputOffset, inputLen);
            if (r == null) {
                return 0;
            }
            if (output.length - outputOffset < r.length) {
                throw new ShortBufferException("too small");
            }
            System.arraycopy(r, 0, output, outputOffset, r.length);
            return r.length;
        }

        @Override
        protected byte[] engineDoFinal(byte[] input, int inputOffset, int inputLen) {
            return engineUpdate(input, inputOffset, inputLen);
        }

        @Override
        protected int engineDoFinal(byte[] input, int inputOffset, int inputLen, byte[] output,
                int outputOffset) throws ShortBufferException {
            return engineUpdate(input, inputOffset, inputLen, output, outputOffset);
        }
    }

    /** A toy authentication code: the sum of the bytes. */
    public static class SumSpi extends MacSpi {
        private int sum;

        @Override
        protected int engineGetMacLength() {
            return 1;
        }

        @Override
        protected void engineInit(Key key, AlgorithmParameterSpec params) {
            this.sum = 0;
        }

        @Override
        protected void engineUpdate(byte input) {
            this.sum += input;
        }

        @Override
        protected void engineUpdate(byte[] input, int offset, int len) {
            for (int i = 0; i < len; i++) {
                this.sum += input[offset + i];
            }
        }

        @Override
        protected byte[] engineDoFinal() {
            return new byte[] {(byte) this.sum};
        }

        @Override
        protected void engineReset() {
            this.sum = 0;
        }
    }

    /** A service that builds without reflection, so as not to depend on how classes are loaded. */
    static class Svc extends Provider.Service {
        Svc(Provider p, String type, String alg, String className) {
            super(p, type, alg, className, new ArrayList<String>(), new HashMap<String, String>());
        }

        @Override
        public Object newInstance(Object constructorParameter) {
            return "Cipher".equals(getType()) ? (Object) new XorSpi() : (Object) new SumSpi();
        }
    }

    /** A provider with two toy services. */
    static class TestProvider extends Provider {
        private static final long serialVersionUID = 1L;

        TestProvider() {
            super("KajiTest", "1.0", "two toy services");
            putService(new Svc(this, "Cipher", "XOR", "CRY1$XorSpi"));
            putService(new Svc(this, "Mac", "SUM", "CRY1$SumSpi"));
        }
    }

    /** What the package does, one line per check. */
    static String[] actual() throws Exception {
        final java.util.List<String> a = new java.util.ArrayList<String>();

        a.add("modes|" + Cipher.ENCRYPT_MODE + Cipher.DECRYPT_MODE + Cipher.WRAP_MODE
                + Cipher.UNWRAP_MODE + Cipher.PUBLIC_KEY + Cipher.PRIVATE_KEY + Cipher.SECRET_KEY);

        // The cipher that does not encrypt.
        final Cipher n = new NullCipher();
        a.add("null-cipher|" + n.getAlgorithm() + "|" + n.getProvider() + "|" + n.getBlockSize()
                + "|" + n.getOutputSize(7) + "|" + hex(n.getIV()) + "|" + n.getParameters()
                + "|" + n.getExemptionMechanism());
        a.add("null-cipher-toString|" + n);
        final byte[] d = {1, 2, 3, 4, 5};
        a.add("update|" + hex(n.update(d)) + "|" + hex(n.update(d, 1, 3)));
        final byte[] o = new byte[10];
        a.add("update-array|" + n.update(d, 0, 5, o) + "|" + hex(o));
        a.add("doFinal|" + hex(n.doFinal()) + "|" + hex(n.doFinal(d)) + "|" + hex(n.doFinal(d, 1, 2)));
        final byte[] o2 = new byte[10];
        a.add("doFinal-array|" + n.doFinal(d, 0, 5, o2, 2) + "|" + hex(o2));
        final ByteBuffer bi = ByteBuffer.wrap(d);
        final ByteBuffer bo = ByteBuffer.allocate(10);
        a.add("buf-update|" + n.update(bi, bo) + "|" + hex(bo.array()));
        bi.rewind();
        bo.clear();
        a.add("buf-doFinal|" + n.doFinal(bi, bo) + "|" + hex(bo.array()));
        a.add("null-cipher-wrap|" + attempt(new Wrap(n)) + "|" + attempt(new Unwrap(n))
                + "|" + attempt(new Aad(n)));

        // The export policy limits nothing.
        a.add("maxlen|" + Cipher.getMaxAllowedKeyLength("AES")
                + "|" + Cipher.getMaxAllowedKeyLength("DoesNotExist")
                + "|" + Cipher.getMaxAllowedParameterSpec("AES"));
        a.add("maxlen-null|" + attempt(new MaxNull()));

        // Malformed names and providers that are not there.
        a.add("gi-null|" + attempt(new Gi(null)));
        a.add("gi-empty|" + attempt(new Gi("")));
        a.add("gi-2-parts|" + attempt(new Gi("a/b")));
        a.add("gi-4-parts|" + attempt(new Gi("a/b/c/d")));
        a.add("gi-prov-null|" + attempt(new GiProv("AES", null)));
        a.add("gi-prov-odd|" + attempt(new GiProv("AES", "NeverExists")));

        // The streams, with the cipher that does not encrypt.
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final CipherOutputStream cos = new CipherOutputStream(output, new NullCipher());
        cos.write(65);
        cos.write(new byte[] {66, 67});
        cos.write(new byte[] {68, 69, 70}, 1, 2);
        cos.flush();
        cos.close();
        a.add("output|" + hex(output.toByteArray()));
        final CipherInputStream cis =
                new CipherInputStream(new ByteArrayInputStream(output.toByteArray()),
                        new NullCipher());
        final int first = cis.read();
        final byte[] rest = new byte[8];
        final int read = cis.read(rest, 0, 8);
        final int end = cis.read();
        a.add("input|" + first + "|" + read + "|" + hex(rest) + "|" + end
                + "|" + cis.markSupported());
        cis.close();

        // The sealed object.
        final SealedObject sealed = new SealedObject("hello sealed", new NullCipher());
        a.add("sealed|" + sealed.getAlgorithm() + "|"
                + sealed.getObject(new NullCipher()));

        // The structure of the encrypted private key.
        final byte[] der = {
            0x30, 0x12,
            0x30, 0x0b, 0x06, 0x09, 0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d,
            0x01, 0x05, 0x03,
            0x04, 0x03, 0x01, 0x02, 0x03};
        final EncryptedPrivateKeyInfo epki = new EncryptedPrivateKeyInfo(der);
        a.add("epki|" + epki.getAlgName() + "|" + hex(epki.getEncryptedData())
                + "|" + epki.getAlgParameters() + "|" + hex(epki.getEncoded()));
        final byte[] odd = {
            0x30, 0x0d,
            0x30, 0x06, 0x06, 0x04, 0x2b, 0x06, 0x01, 0x04,
            0x04, 0x03, 0x09, 0x08, 0x07};
        final EncryptedPrivateKeyInfo epki2 = new EncryptedPrivateKeyInfo(odd);
        a.add("epki-odd|" + epki2.getAlgName() + "|" + hex(epki2.getEncryptedData())
                + "|" + hex(epki2.getEncoded()));
        final String[] names = {
            "PBEWithMD5AndDES", "PBEWithSHA1AndDESede", "PBEWithSHA1AndRC2_40",
            "PBEWithSHA1AndRC2_128", "PBEWithSHA1AndRC4_40", "PBEWithSHA1AndRC4_128",
            "PBES2", "PBKDF2WithHmacSHA1", "AES", "AES_128/CBC/NoPadding",
            "AES_256/CBC/NoPadding", "DESede", "Blowfish", "HmacSHA256", "EC",
            "DiffieHellman", "1.3.6.1.4.1.99999"};
        for (int i = 0; i < names.length; i++) {
            final EncryptedPrivateKeyInfo e =
                    new EncryptedPrivateKeyInfo(names[i], new byte[] {1});
            a.add("oid|" + names[i] + "|" + hex(e.getEncoded()) + "|"
                    + new EncryptedPrivateKeyInfo(e.getEncoded()).getAlgName());
        }
        a.add("epki-odd-name|" + attempt(new Epki("NeverExists")));
        a.add("epki-null|" + attempt(new EpkiNull()));
        a.add("epki-empty|" + attempt(new EpkiEmpty()));
        a.add("epki-short|" + attempt(new EpkiBytes(new byte[] {1, 2, 3})));
        a.add("epki-copy|" + (epki.getEncryptedData() != epki.getEncryptedData()));
        a.add("epki-keyspec|" + attempt(new KeySpecNull(epki)));

        // The exception hierarchies.
        a.add("hierarchy|" + parents(javax.crypto.AEADBadTagException.class)
                + "|" + parents(javax.crypto.IllegalBlockSizeException.class)
                + "|" + parents(javax.crypto.DecapsulateException.class));

        return a.toArray(new String[a.size()]);
    }

    /**
     * Checks the provider machinery with a provider of our own.
     *
     * <p>This cannot be compared against the JDK: the JDK's {@code javax.crypto} demands that a
     * provider offering ciphers come in a file signed with an Oracle certificate, and throws
     * {@code SecurityException} for any other. It is what is left of the export restrictions, just
     * like {@link javax.crypto.ExemptionMechanism}, and this library does not copy it: the check
     * depends on a few particular certificates and protects nothing here.
     *
     * <p>What is checked is that {@code getInstance} finds the service, sets its mode and padding,
     * and delegates everything to it. The expected values do not come from the JDK but from the
     * definition of the toy cipher, which belongs to this very test.
     *
     * @return the index of the first answer that differs, or -1
     */
    public static int machinery() {
        final java.util.List<String> a = new java.util.ArrayList<String>();
        final String[] expected = {
            "prov|XOR/CBC/PKCS5Padding|KajiTest|4|mode:CBC;padding:PKCS5Padding;",
            "prov-toString|Cipher.XOR/CBC/PKCS5Padding, mode: not initialized,"
                    + " algorithm from: KajiTest",
            "prov-init|mode:CBC;padding:PKCS5Padding;init:1;|Cipher.XOR/CBC/PKCS5Padding,"
                    + " mode: initialized, algorithm from: KajiTest",
            "prov-encrypt|5b5859",
            "prov-decrypt|010203",
            "prov-mac|SUM|KajiTest|1",
            "prov-mac-no-init|IllegalStateException",
            "prov-mac-value|3c|03",
            "xor-stream|1215161b",
            "xor-stream-back|4|484f4c41",
        };
        try {
            // The provider machinery, with a provider of our own.
            Security.addProvider(new TestProvider());
            XorSpi.seen = "";
            final Cipher x = Cipher.getInstance("XOR/CBC/PKCS5Padding", "KajiTest");
            a.add("prov|" + x.getAlgorithm() + "|" + x.getProvider().getName()
                    + "|" + x.getBlockSize() + "|" + XorSpi.seen);
            a.add("prov-toString|" + x);
            x.init(Cipher.ENCRYPT_MODE, (Key) null);
            a.add("prov-init|" + XorSpi.seen + "|" + x);
            final byte[] encrypted = x.doFinal(new byte[] {1, 2, 3});
            a.add("prov-encrypt|" + hex(encrypted));
            final Cipher x2 = Cipher.getInstance("XOR");
            x2.init(Cipher.DECRYPT_MODE, (Key) null);
            a.add("prov-decrypt|" + hex(x2.doFinal(encrypted)));
            final Mac m = Mac.getInstance("SUM");
            a.add("prov-mac|" + m.getAlgorithm() + "|" + m.getProvider().getName()
                    + "|" + m.getMacLength());
            a.add("prov-mac-no-init|" + attempt(new MacNoInit(m)));
            m.init(null);
            m.update((byte) 10);
            m.update(new byte[] {20, 30});
            a.add("prov-mac-value|" + hex(m.doFinal()) + "|" + hex(m.doFinal(new byte[] {1, 2})));

            // The streams and the sealed object, with the toy cipher too.
            final ByteArrayOutputStream s2 = new ByteArrayOutputStream();
            final Cipher xe = Cipher.getInstance("XOR");
            xe.init(Cipher.ENCRYPT_MODE, (Key) null);
            final CipherOutputStream cos2 = new CipherOutputStream(s2, xe);
            cos2.write(new byte[] {72, 79, 76, 65});
            cos2.close();
            a.add("xor-stream|" + hex(s2.toByteArray()));
            final Cipher xd = Cipher.getInstance("XOR");
            xd.init(Cipher.DECRYPT_MODE, (Key) null);
            final CipherInputStream cis2 =
                    new CipherInputStream(new ByteArrayInputStream(s2.toByteArray()), xd);
            final byte[] back = new byte[4];
            final int n2 = cis2.read(back, 0, 4);
            cis2.close();
            a.add("xor-stream-back|" + n2 + "|" + hex(back));
        } catch (Throwable t) {
            return 9000;
        }
        if (a.size() != expected.length) {
            return 8000 + a.size();
        }
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).equals(expected[i])) {
                return i;
            }
        }
        return -1;
    }

    /** The chain of superclasses up to {@code Exception}, with simple names. */
    static String parents(Class<?> c) {
        final StringBuilder b = new StringBuilder();
        Class<?> p = c.getSuperclass();
        while (p != null && !p.getName().equals("java.lang.Exception")) {
            b.append(p.getSimpleName()).append(',');
            p = p.getSuperclass();
        }
        return b.toString();
    }

    /** The bytes in hexadecimal, or {@code "null"}. */
    static String hex(byte[] b) {
        if (b == null) {
            return "null";
        }
        final StringBuilder s = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            final int v = b[i] & 0xff;
            s.append("0123456789abcdef".charAt(v >> 4));
            s.append("0123456789abcdef".charAt(v & 0xf));
        }
        return s.toString();
    }

    /** Something run to see what it fails with. */
    interface Throwing {
        void run() throws Exception;
    }

    /** Runs it and returns "ok" or the simple name of whatever it threw. */
    static String attempt(Throwing r) {
        try {
            r.run();
            return "ok";
        } catch (Throwable t) {
            final String nn = t.getClass().getName();
            return nn.substring(nn.lastIndexOf('.') + 1);
        }
    }

    static class Wrap implements Throwing {
        private final Cipher c;

        Wrap(Cipher c) {
            this.c = c;
        }

        public void run() throws Exception {
            c.wrap(null);
        }
    }

    static class Unwrap implements Throwing {
        private final Cipher c;

        Unwrap(Cipher c) {
            this.c = c;
        }

        public void run() throws Exception {
            c.unwrap(new byte[] {1}, "AES", Cipher.SECRET_KEY);
        }
    }

    static class Aad implements Throwing {
        private final Cipher c;

        Aad(Cipher c) {
            this.c = c;
        }

        public void run() throws Exception {
            c.updateAAD(new byte[] {1});
        }
    }

    static class MaxNull implements Throwing {
        public void run() throws Exception {
            Cipher.getMaxAllowedKeyLength(null);
        }
    }

    static class Gi implements Throwing {
        private final String t;

        Gi(String t) {
            this.t = t;
        }

        public void run() throws Exception {
            Cipher.getInstance(t);
        }
    }

    static class GiProv implements Throwing {
        private final String t;
        private final String p;

        GiProv(String t, String p) {
            this.t = t;
            this.p = p;
        }

        public void run() throws Exception {
            Cipher.getInstance(t, p);
        }
    }

    static class Epki implements Throwing {
        private final String name;

        Epki(String name) {
            this.name = name;
        }

        public void run() throws Exception {
            new EncryptedPrivateKeyInfo(this.name, new byte[] {1});
        }
    }

    static class EpkiNull implements Throwing {
        public void run() throws Exception {
            new EncryptedPrivateKeyInfo((byte[]) null);
        }
    }

    static class EpkiEmpty implements Throwing {
        public void run() throws Exception {
            new EncryptedPrivateKeyInfo("PBEWithMD5AndDES", new byte[0]);
        }
    }

    static class EpkiBytes implements Throwing {
        private final byte[] b;

        EpkiBytes(byte[] b) {
            this.b = b;
        }

        public void run() throws Exception {
            new EncryptedPrivateKeyInfo(this.b);
        }
    }

    static class KeySpecNull implements Throwing {
        private final EncryptedPrivateKeyInfo e;

        KeySpecNull(EncryptedPrivateKeyInfo e) {
            this.e = e;
        }

        public void run() throws Exception {
            e.getKeySpec(new NullCipher());
        }
    }

    static class MacNoInit implements Throwing {
        private final Mac m;

        MacNoInit(Mac m) {
            this.m = m;
        }

        public void run() throws Exception {
            m.update((byte) 1);
        }
    }

    /**
     * The index of the first answer that differs from the JDK's, or -1.
     *
     * @return the index, or -1
     */
    public static int where() {
        final String[] a;
        try {
            a = actual();
        } catch (Throwable e) {
            return 9000;
        }
        if (a.length != EXPECTED.length) {
            return 8000 + a.length;
        }
        for (int i = 0; i < a.length; i++) {
            if (!a[i].equals(EXPECTED[i])) {
                return i;
            }
        }
        return -1;
    }

    public static void main(String[] args) throws Exception {
        final String[] a = actual();
        if (args.length > 0) {
            for (int i = 0; i < a.length; i++) {
                System.out.println(a[i]);
            }
            return;
        }
        final int i = where();
        System.out.println(i < 0 ? "no differences"
                : i + ":\n  ours=" + a[i] + "\n  jdk =" + EXPECTED[i]);
    }
}
